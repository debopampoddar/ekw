package ekw.pipeline;

import ekw.core.Todo;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * The code repository's checkout under .cache/repos/ (design: stage 2, section 3.2.1), with JGit. A full clone made
 * once; later opens fetch. JGit never runs builds or hooks.
 *
 * <p>Chapter 3 of the development guide.
 */
public final class GitCheckout implements AutoCloseable {

    private GitCheckout() {
    }

    /**
     * Clones {@code url} into {@code dir} on first use (no working tree yet); otherwise opens it and fetches.
     */
    public static GitCheckout open(String url, Path dir) {
        throw Todo.chapter(3, "GitCheckout.open");
    }

    /**
     * Full SHA of origin/{@code branch} after the fetch.
     */
    public String head(String branch) {
        throw Todo.chapter(3, "GitCheckout.head");
    }

    /**
     * Moves the working tree to {@code commit} (detached).
     */
    public void checkout(String commit) {
        throw Todo.chapter(3, "GitCheckout.checkout");
    }

    /**
     * The commit's committer time.
     */
    public Instant commitTime(String commit) {
        throw Todo.chapter(3, "GitCheckout.commitTime");
    }

    /**
     * Paths changed between two commits, renames detected: a rename is reported as "old -> new".
     */
    public List<String> changedPaths(String fromCommit, String toCommit) {
        throw Todo.chapter(3, "GitCheckout.changedPaths");
    }

    @Override
    public void close() {
        throw Todo.chapter(3, "GitCheckout.close");
    }
}
