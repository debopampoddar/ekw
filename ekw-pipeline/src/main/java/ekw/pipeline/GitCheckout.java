package ekw.pipeline;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The code repository's checkout under .cache/repos/ (design: stage 2, section 3.2.1), with JGit. A full clone made
 * once; later opens fetch. JGit never runs builds or hooks.
 */
public final class GitCheckout implements AutoCloseable {
    private final Git git;

    private GitCheckout(Git git) {
        this.git = git;
    }

    /**
     * Clones {@code url} into {@code dir} on first use (no working tree yet); otherwise opens it and fetches.
     */
    public static GitCheckout open(String url, Path dir) {
        Git git = null;
        try {
            if (Files.exists(dir.resolve(".git"))) {
                git = Git.open(dir.toFile());
                git.fetch()
                   .setRemote("origin")
                   .setRemoveDeletedRefs(true)
                   .call();
            } else {
                Files.createDirectories(dir);
                git = Git.cloneRepository()
                         .setURI(url)
                         .setDirectory(dir.toFile())
                         .setNoCheckout(true)
                         .call();
            }
        } catch (IOException | GitAPIException e) {
            if (null != git) {
                git.close();
            }
            throw new IllegalStateException("git fetch failed for " + url +
                    ": " + url, e);
        }
        return new GitCheckout(git);
    }

    /**
     * Full SHA of origin/{@code branch} after the fetch.
     */
    public String head(String branch) {
        try {
            ObjectId id = git.getRepository()
                             .resolve("refs/remotes/origin/" + branch);
            if (null == id) {
                throw new IllegalArgumentException("unknown branch: " + branch);
            }
            return id.name();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Moves the working tree to {@code commit} (detached).
     */
    public void checkout(String commit) {
        try {
            git.checkout().setName(commit).setForced(true).call();
        } catch (GitAPIException e) {
            throw new IllegalStateException("checkout " + commit + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * The commit's committer time.
     */
    public Instant commitTime(String commit) {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            return Instant.ofEpochSecond(walk.parseCommit(ObjectId.fromString(commit))
                                             .getCommitTime());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Paths changed between two commits, renames detected: a rename is reported as "old -> new".
     */
    public List<String> changedPaths(String fromCommit, String toCommit) {
        Repository repository = git.getRepository();
        try (RevWalk walk = new RevWalk(git.getRepository());
             DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            RevCommit from = walk.parseCommit(ObjectId.fromString(fromCommit));
            RevCommit to = walk.parseCommit(ObjectId.fromString(toCommit));
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            List<String> out = new ArrayList<>();
            for (DiffEntry e : diffFormatter.scan(from.getTree(), to.getTree())) {
                out.add(switch (e.getChangeType()) {
                    case DELETE -> e.getOldPath();
                    case RENAME -> e.getOldPath() + " -> " + e.getNewPath();
                    default -> e.getNewPath();
                });
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        git.close();
    }
}
