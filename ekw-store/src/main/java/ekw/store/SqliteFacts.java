package ekw.store;

import ekw.analysis.AnnotationFact;
import ekw.analysis.CallFact;
import ekw.analysis.ConfigKeyFact;
import ekw.analysis.DependencyFact;
import ekw.analysis.ErrorCodeFact;
import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.analysis.MemberFact;
import ekw.analysis.ModuleFact;
import ekw.analysis.SpiFact;
import ekw.analysis.TypeFact;
import ekw.core.Evidence;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Facts for one snapshot (system, commit): the analyzer writes through {@link #sink}, everything else reads through
 * the {@link Facts.FactsStore} methods. Reads always use the snapshot marked current.
 *
 * <p>Chapter 5 of the development guide.
 */
public final class SqliteFacts implements Facts.FactsStore {

    private static final String CURRENT = "(SELECT commit_sha FROM snapshot WHERE system = :s AND current = 1)";
                                            // … WHERE system = :s AND commit_sha = " + CURRENT + " AND …
    private static final String JOINING_DELIMITER = ",";
    private final FactsDatabase db;
    public SqliteFacts(FactsDatabase db) {
        this.db = db;
    }

    /**
     * Opens a snapshot (replacing facts left by an earlier attempt at the same commit). Commit it to make it current.
     */
    public Sink sink(String system, String commit, String commitTime, String analyzer) {
        Handle h = db.jdbi().open();
        h.begin();
        for (String tableName : List.of("call_site"
                , "supertype"
                , "symbol"
                , "type_ref"
                , "annotation"
                , "config_key"
                , "error_code"
                , "spi_registration"
                , "dependency"
                , "module"
                , "file"
                , "snapshot")) {
            h.execute("DELETE FROM " + tableName + " WHERE system = ? AND commit_sha = ?", system, commit);
        }
        h.execute("INSERT INTO snapshot (system, commit_sha, commit_time, analyzed_at, analyzer, current) "
                + "VALUES (?, ?, ?, datetime('now'), ?, 0)", system, commit, commitTime, analyzer);
        return new Sink(h, system, commit);
    }

    /**
     * Writes facts in one transaction. {@link #commit()} makes the snapshot current; {@link #close()} without it rolls back.
     */
    public static final class Sink implements Facts.FactSink, AutoCloseable {
        private final Handle handle;
        private final String system;
        private final String commit;
        private final PreparedBatch symbols;
        private final PreparedBatch supertypes;
        private final PreparedBatch calls;
        private final Map<String, Integer> callsOnLine = new HashMap<>();

        private Sink(Handle h, String system, String commit) {
            this.handle = h;
            this.system = system;
            this.commit = commit;
            symbols = h.prepareBatch("INSERT OR REPLACE INTO symbol (system, commit_sha, id, owner_id, kind, simple_name, "
                    + "module, signature, modifiers, path, start_line, end_line, body_hash, javadoc, is_test) VALUES "
                    + "(:system, :commit_sha, :id, :owner, :kind, :name, :module, :sig, :mods, :path, :start, :end, :hash, :doc, :test)");
            supertypes = h.prepareBatch("INSERT OR IGNORE INTO supertype (system, commit_sha, type_id, super_id, relation) "
                    + "VALUES (:system, :commit_sha, :type_id, :super, :relation)");
            calls = h.prepareBatch("INSERT OR REPLACE INTO call_site (system, commit_sha, caller_id, callee_id, path, line, "
                    + "col, flags, unresolved_reason) VALUES (:system, :commit_sha, :caller, :callee, :path, :line, :col, :flags, :why)");
        }

        @Override
        public void type(TypeFact typeFact) {
            symbols.bind("system", system)
                    .bind("commit_sha", commit)
                    .bind("id", typeFact.id().value())
                    .bind("owner", (String) null)
                    .bind("kind", typeFact.kind().name())
                    .bind("name", simpleName(typeFact.id().value()))
                    .bind("module", typeFact.module())
                    .bind("sig", (String) null)
                    .bind("mods", joined(typeFact.modifiers()))
                    .bind("path", typeFact.span().path())
                    .bind("start", typeFact.span().startLine())
                    .bind("end", typeFact.span().endLine())
                    .bind("hash", (String) null)
                    .bind("doc", typeFact.javadocFirstSentence().orElse(null))
                    .bind("test", typeFact.test() ? 1 : 0)
                    .add();
            for (SymbolId sup : typeFact.supertypes()) {
                supertypes.bind("system", system)
                          .bind("commit_sha", commit)
                          .bind("type_id", typeFact.id().value())
                          .bind("super", sup.value())
                          .bind("relation", "EXTENDS_OR_IMPLEMENTS")
                          .add();
            }
            flushIfLarge();
        }

        @Override
        public void member(MemberFact memberFact) {
            symbols.bind("system", system)
                   .bind("commit_sha", commit)
                   .bind("id", memberFact.id().value())
                   .bind("owner", memberFact.owner().value())
                   .bind("kind", memberFact.kind().name())
                   .bind("name", memberName(memberFact.id().value()))
                   .bind("module", (String) null)
                   .bind("sig", memberFact.signature())
                   .bind("mods", joined(memberFact.modifiers()))
                   .bind("path", memberFact.span().path())
                   .bind("start", memberFact.span().startLine())
                   .bind("end", memberFact.span().endLine())
                   .bind("hash", memberFact.bodyHash())
                   .bind("doc", memberFact.javadocFirstSentence().orElse(null))
                   .bind("test", 0)
                   .add();
            flushIfLarge();
        }

        @Override
        public void call(CallFact callFact) {
            String path = callFact.site().path();
            int line = callFact.site().startLine();

            calls.bind("system", system)
                .bind("commit_sha", commit)
                .bind("caller", callFact.caller().value())
                .bind("callee", callFact.callee().map(SymbolId::value).orElse(null))
                .bind("path", path)
                .bind("line", line)
                .bind("col", callsOnLine.merge(path + ":" + line, 1, Integer::sum))
                .bind("flags", joined(callFact.flags().stream().map(Enum::name).toList()))
                .bind("why", callFact.unresolvedReason().orElse(null))
                .add();
            flushIfLarge();
        }

        // Later chapters (part C) store these fact kinds; ignore them for now.
        @Override
        public void annotation(AnnotationFact a) {
        }

        @Override
        public void configKey(ConfigKeyFact k) {
        }

        @Override
        public void errorCode(ErrorCodeFact e) {
        }

        @Override
        public void spi(SpiFact s) {
        }

        @Override
        public void module(ModuleFact m) {
        }

        @Override
        public void dependency(DependencyFact d) {
        }

        private void flushIfLarge() {
            if (symbols.size() + calls.size() + supertypes.size() >= 5_000)
                flush();
        }

        private void flush() {
            if (symbols.size() > 0) symbols.execute();
            if (supertypes.size() > 0) supertypes.execute();
            if (calls.size() > 0) calls.execute();
        }

        /** Makes this snapshot the current one for the system. */
        public void commit() {
            flush();
            handle.execute("UPDATE snapshot SET current = (commit_sha = ?) WHERE system = ?"
                    , commit
                    , system);
            handle.commit();
        }

        @Override
        public void close() {
            if (handle.isInTransaction())
                handle.rollback();
            handle.close();
        }
    }

    @Override
    public Optional<TypeFact> type(String system, SymbolId id) {
        return db.jdbi().withHandle(h ->
                h.createQuery("SELECT * FROM symbol WHERE system = :s AND commit_sha = "
                         + CURRENT + " AND id = :id AND owner_id IS NULL")
                 .bind("s", system)
                 .bind("id", id.value())
                 .map((rs, ctx) -> typeFact(h, rs)).findFirst());
    }

    @Override
    public List<TypeFact> findTypes(String system, String name, int limit) {
        boolean qualified = name.contains(".");
        String where = qualified ? "id = :q" : "simple_name = :n";
        return db.jdbi().withHandle(h ->
                h.createQuery("SELECT * FROM symbol WHERE system = :s AND commit_sha = "
                         + CURRENT + " AND owner_id IS NULL AND " + where + " ORDER BY is_test, id LIMIT :limit")
                 .bind("s", system)
                 .bind("q", "java:" + name)
                 .bind("n", name)
                 .bind("limit", limit)
                 .map((rs, ctx) -> typeFact(h, rs)).list());
    }

    @Override
    public List<MemberFact> members(String system, SymbolId type) {
        return db.jdbi().withHandle(h ->
                h.createQuery("SELECT * FROM symbol WHERE system = :s AND commit_sha = "
                         + CURRENT + " AND owner_id = :t ORDER BY start_line")
                 .bind("s", system)
                 .bind("t", type.value())
                 .map((rs, ctx) -> memberFact(rs)).list());
    }

    @Override
    public List<CallFact> callers(String system, SymbolId callee, int limit) {
        return calls(system, "callee_id = :x", callee.value(), limit);
    }

    @Override
    public List<CallFact> callees(String system, SymbolId caller, int limit) {
        return calls(system, "caller_id = :x", caller.value(), limit);
    }

    @Override
    public List<TypeFact> implementations(String system, SymbolId type, int limit) {
        return db.jdbi().withHandle(h ->
                h.createQuery("SELECT y.* FROM supertype t JOIN symbol y ON y.system = t.system "
                         + "AND y.commit_sha = t.commit_sha AND y.id = t.type_id WHERE t.system =" +
                         " :s AND t.commit_sha = " + CURRENT
                         + " AND t.super_id = :x ORDER BY y.id LIMIT :limit")
                 .bind("s", system)
                 .bind("x", type.value())
                 .bind("limit", limit)
                 .map((rs, ctx) -> typeFact(h, rs)).list());
    }

    @Override
    public Optional<MemberFact> memberByBodyHash(String system, String bodyHash) {
        List<MemberFact> found = db.jdbi().withHandle(h ->
                h.createQuery("SELECT * FROM symbol WHERE system = :s AND commit_sha = "
                         + CURRENT + " AND body_hash = :b LIMIT 2")
                 .bind("s", system)
                 .bind("b", bodyHash)
                 .map((rs, ctx) -> memberFact(rs)).list());
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty(); // ambiguous counts as not found
    }

    @Override
    public Map<SymbolId, Double> rank(String system) {
        Map<SymbolId, Double> out = new LinkedHashMap<>();
        db.jdbi().useHandle(h ->
                h.createQuery("SELECT id, rank FROM symbol WHERE system = :s AND commit_sha = "
                         + CURRENT
                         + " AND owner_id IS NULL AND rank IS NOT NULL ORDER BY rank DESC")
                 .bind("s", system)
                 .map((rs, ctx) ->
                         Map.entry(new SymbolId(rs.getString(1))
                                 , rs.getDouble(2)))
                 .forEach(e -> out.put(e.getKey(), e.getValue())));
        return out;
    }

    // Part C fills these; they return nothing until then.
    @Override
    public List<ConfigKeyFact> configKeys(String system, String keyFragment, int limit) {
        return List.of();
    }

    @Override
    public List<ErrorCodeFact> errorCodes(String system, String code) {
        return List.of();
    }

    @Override
    public List<DependencyFact> dependencies(String system, String fragment, int limit) {
        return List.of();
    }

    private List<CallFact> calls(String system, String where, String value, int limit) {
        return db.jdbi().withHandle(h ->
                h.createQuery("SELECT * FROM call_site WHERE system = :s AND commit_sha = "
                         + CURRENT + " AND " + where + " ORDER BY path, line LIMIT :limit")
                 .bind("s", system).bind("x", value)
                 .bind("limit", limit)
                 .map((rs, ctx) ->
                         CallFact.builder()
                                 .caller(new SymbolId(rs.getString("caller_id")))
                                 .callee(Optional.ofNullable(rs.getString("callee_id")).map(SymbolId::new))
                                 .site(location(rs, rs.getInt("line"), rs.getInt("line")))
                                 .flags(flags(rs.getString("flags")))
                                 .unresolvedReason(Optional.ofNullable(rs.getString("unresolved_reason")))
                                 .build())
                 .list());
    }

    private static TypeFact typeFact(Handle h, ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        List<SymbolId> supers = h.createQuery("SELECT super_id FROM supertype WHERE system = :s AND commit_sha = :c "
                                         + "AND type_id = :t ORDER BY super_id").bind("s", rs.getString("system"))
                                 .bind("c", rs.getString("commit_sha")).bind("t", id).mapTo(String.class).map(SymbolId::new).list();
        return TypeFact.builder()
                .id(new SymbolId(id))
                .module(rs.getString("module"))
                .kind(Facts.TypeKind.valueOf(rs.getString("kind")))
                .modifiers(words(rs.getString("modifiers")))
                .span(location(rs, rs.getInt("start_line"), rs.getInt("end_line")))
                .javadocFirstSentence(Optional.ofNullable(rs.getString("javadoc")))
                .supertypes(supers)
                .test(rs.getInt("is_test") == 1)
                .build();
    }

    private static MemberFact memberFact(ResultSet rs) throws SQLException {
        return MemberFact.builder()
                .id(new SymbolId(rs.getString("id")))
                .owner(new SymbolId(rs.getString("owner_id")))
                .kind(Facts.MemberKind.valueOf(rs.getString("kind")))
                .signature(rs.getString("signature"))
                .modifiers(words(rs.getString("modifiers")))
                .span(location(rs, rs.getInt("start_line"), rs.getInt("end_line")))
                .bodyHash(rs.getString("body_hash"))
                .javadocFirstSentence(Optional.ofNullable(rs.getString("javadoc")))
                .build();
    }

    private static Evidence.CodeLocation location(ResultSet rs, int start, int end) throws SQLException {
        return new Evidence.CodeLocation(rs.getString("system")
                , rs.getString("commit_sha")
                , rs.getString("path")
                , start
                , end
                ,Optional.empty());
    }

    static String joined(Collection<String> words) {
        return words.stream().sorted().collect(Collectors.joining(JOINING_DELIMITER));
    }

    private static Set<String> words(String s) {
        return s == null || s.isBlank() ? Set.of()
                                        : new TreeSet<>(Arrays.asList(s.split(JOINING_DELIMITER)));
    }

    private static Set<Facts.CallFlag> flags(String s) {
        EnumSet<Facts.CallFlag> out = EnumSet.noneOf(Facts.CallFlag.class);
        if (s != null && !s.isBlank()) {
            for (String f : s.split(JOINING_DELIMITER)) {
                out.add(Facts.CallFlag.valueOf(f));
            }
        }
        return out;
    }

    static String simpleName(String typeId) {
        return typeId.substring(typeId.lastIndexOf('.') + 1);
    }

    static String memberName(String memberId) {
        String afterHash = memberId.substring(memberId.indexOf('#') + 1);
        int paren = afterHash.indexOf('(');
        return paren < 0 ? afterHash : afterHash.substring(0, paren);
    }
}
