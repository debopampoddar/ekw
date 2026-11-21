package ekw.analysis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline: each constant of an enum is one ENUM_CONSTANT member (chapter 4, visit(EnumConstantDeclaration)). */
@Tag("ch04")
class JdtJavaAnalyzerEnumTest {

    @Test
    void emitsOneMemberPerEnumConstant(@TempDir Path root) throws Exception {
        Path file = root.resolve("src/main/java/demo/Isolation.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                package demo;

                public enum Isolation {
                    /** Reads may see uncommitted changes. */
                    READ_UNCOMMITTED,
                    READ_COMMITTED
                }
                """);
        ModuleSpec module = ModuleSpec.builder()
                .system("demo").commit("c0ffee").name("demo").root(root)
                .sourceRoots(List.of(root.resolve("src/main/java"))).javaRelease("21").build();
        CollectingSink sink = new CollectingSink();
        new JdtJavaAnalyzer(root).analyze(module, new Facts.Classpath(List.of(), List.of()), sink);

        assertThat(sink.members).filteredOn(m -> m.kind() == Facts.MemberKind.ENUM_CONSTANT)
                .extracting(m -> m.id().value(), MemberFact::signature, MemberFact::javadocFirstSentence)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("java:demo.Isolation#READ_UNCOMMITTED",
                                "demo.Isolation READ_UNCOMMITTED", java.util.Optional.of("Reads may see uncommitted changes.")),
                        org.assertj.core.groups.Tuple.tuple("java:demo.Isolation#READ_COMMITTED",
                                "demo.Isolation READ_COMMITTED", java.util.Optional.empty()));
    }
}
