package ekw.store;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The derived database {@code .index/ekw.db} (SQLite). Opening it applies the Flyway migrations in
 * {@code src/main/resources/db/migration}.
 *
 * <p>Chapter 5 of the development guide.
 */
public final class FactsDatabase {

    private static final String DB_MIGRATION_PATH = "db/migration";
    private final Jdbi jdbi;

    private FactsDatabase(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Creates the folder if needed, opens SQLite (foreign keys on, WAL journal), migrates, returns the database.
     */
    public static FactsDatabase open(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(10_000);

        SQLiteDataSource ds = new SQLiteDataSource(config);
        ds.setUrl("jdbc:sqlite:" + file.toAbsolutePath());

        Flyway.configure().dataSource(ds).locations("classpath:" + DB_MIGRATION_PATH).load().migrate();

        return new FactsDatabase(Jdbi.create(ds));
    }

    public Jdbi jdbi() {
        return jdbi;
    }
}
