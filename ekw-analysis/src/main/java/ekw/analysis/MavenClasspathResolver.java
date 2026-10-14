package ekw.analysis;

import ekw.core.Todo;
import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.Map;

/**
 * Builds a module's effective POM (parents, properties, dependency management; no plugins run) and downloads its
 * dependency jars from Maven Central into a cache folder. Only POMs and jars are read; nothing is executed.
 *
 * <p>Chapter 4 of the development guide.
 */
public final class MavenClasspathResolver implements Facts.ClasspathResolver, AutoCloseable {

    /**
     * @param localRepository the jar cache, e.g. {@code <kb>/.cache/m2}
     * @param repositoryRoot  the checkout; its own POMs are read from disk, never downloaded
     */
    public MavenClasspathResolver(Path localRepository, Path repositoryRoot) {
        // TODO (chapter 4): create the repository system and session; index the repository's POMs.
    }

    /** groupId:artifactId -> pom.xml for every POM in the checkout (build output and test resources excluded). */
    public static Map<String, Path> reactorPoms(Path repositoryRoot) {
        throw Todo.chapter(4, "MavenClasspathResolver.reactorPoms");
    }

    /** The effective model of a pom.xml. */
    public Model effectiveModel(Path pom) {
        throw Todo.chapter(4, "MavenClasspathResolver.effectiveModel");
    }

    @Override
    public Facts.Classpath resolve(Facts.ModuleSpec module) {
        throw Todo.chapter(4, "MavenClasspathResolver.resolve");
    }

    @Override
    public void close() {
        // TODO (chapter 4): close the session and shut the repository system down.
    }
}
