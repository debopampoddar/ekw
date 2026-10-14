package ekw.store;

import ekw.core.Todo;
import org.jdbi.v3.core.Jdbi;

import java.nio.file.Path;

/**
 * The derived database {@code .index/ekw.db} (SQLite). Opening it applies the Flyway migrations in
 * {@code src/main/resources/db/migration}.
 *
 * <p>Chapter 5 of the development guide.
 */
public final class FactsDatabase {

    private FactsDatabase() {}

    /** Creates the folder if needed, opens SQLite (foreign keys on, WAL journal), migrates, returns the database. */
    public static FactsDatabase open(Path file) {
        throw Todo.chapter(5, "FactsDatabase.open");
    }

    public Jdbi jdbi() {
        throw Todo.chapter(5, "FactsDatabase.jdbi");
    }
}
