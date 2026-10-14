package ekw.analysis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline: analyzes a 4-file sample project (src/test/resources/samples/shapes) with the JDK as the only library. */
@Tag("ch04")
class JdtJavaAnalyzerSampleTest {

    private static Path sample() throws URISyntaxException {
        return Path.of(JdtJavaAnalyzerSampleTest.class.getResource("/samples/shapes").toURI());
    }

    private static CollectingSink analyze(Facts.AnalysisReport[] report) throws URISyntaxException {
        Path root = sample();
        Facts.ModuleSpec module = new Facts.ModuleSpec("shapes", "c0ffee", "shapes", root,
                List.of(root.resolve("src/main/java")), List.of(), "21");
        CollectingSink sink = new CollectingSink();
        report[0] = new JdtJavaAnalyzer(root).analyze(module, new Facts.Classpath(List.of(), List.of()), sink);
        return sink;
    }

    @Test
    void findsTypesWithKindsSpansAndSupertypes() throws Exception {
        Facts.AnalysisReport[] report = new Facts.AnalysisReport[1];
        CollectingSink sink = analyze(report);
        Map<String, Facts.TypeFact> types = sink.types.stream()
                .collect(Collectors.toMap(t -> t.id().value(), t -> t));

        assertThat(types).containsOnlyKeys("java:demo.Shape", "java:demo.Circle", "java:demo.Square", "java:demo.Gallery");
        assertThat(types.get("java:demo.Shape").kind()).isEqualTo(Facts.TypeKind.INTERFACE);
        assertThat(types.get("java:demo.Circle").kind()).isEqualTo(Facts.TypeKind.RECORD);
        assertThat(types.get("java:demo.Square").modifiers()).contains("public", "final");
        assertThat(types.get("java:demo.Square").supertypes()).extracting(Facts.SymbolId::value)
                .containsExactly("java:demo.Shape");

        // Spans start at the declaration, not at its Javadoc; paths are relative to the repository root.
        Facts.TypeFact circle = types.get("java:demo.Circle");
        assertThat(circle.span().path()).isEqualTo("src/main/java/demo/Circle.java");
        assertThat(circle.span().startLine()).isEqualTo(4);
        assertThat(circle.span().endLine()).isEqualTo(9);
        assertThat(circle.javadocFirstSentence()).contains("A circle given by its radius.");
        assertThat(types.get("java:demo.Gallery").span().startLine()).isEqualTo(9);
    }

    @Test
    void findsMembersWithIdsAndBodyHashes() throws Exception {
        CollectingSink sink = analyze(new Facts.AnalysisReport[1]);
        assertThat(sink.members).extracting(m -> m.id().value()).contains(
                "java:demo.Square#<init>(double)",
                "java:demo.Square#area()",
                "java:demo.Square#side",
                "java:demo.Gallery#add(demo.Shape)",
                "java:demo.Gallery#main(java.lang.String[])");
        assertThat(sink.members).allSatisfy(m -> assertThat(m.bodyHash()).hasSize(16));
    }

    @Test
    void resolvesEveryCallSiteAndFlagsThem() throws Exception {
        Facts.AnalysisReport[] report = new Facts.AnalysisReport[1];
        CollectingSink sink = analyze(report);

        assertThat(report[0].files()).isEqualTo(4);
        assertThat(report[0].callSites()).isEqualTo(17);
        assertThat(report[0].resolvedCallSites()).isEqualTo(17);

        // System.out.println(describe(s)) inside the lambda in print()
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.callee()).map(Facts.SymbolId::value).contains("java:java.io.PrintStream#println(java.lang.String)");
            assertThat(c.flags()).contains(Facts.CallFlag.IN_LAMBDA);
            assertThat(c.caller().value()).isEqualTo("java:demo.Gallery#print()");
        });
        // Shape::area passed to mapToDouble
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.callee()).map(Facts.SymbolId::value).contains("java:demo.Shape#area()");
            assertThat(c.flags()).contains(Facts.CallFlag.METHOD_REFERENCE);
        });
        // shapes.stream().mapToDouble(...): mapToDouble is called on the result of stream() and is given a method reference
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.callee()).map(Facts.SymbolId::value)
                    .contains("java:java.util.stream.Stream#mapToDouble(java.util.function.ToDoubleFunction)");
            assertThat(c.flags()).contains(Facts.CallFlag.CHAINED, Facts.CallFlag.PASSES_LAMBDA);
        });
        // new Circle(1) is a constructor call; c.radius() sits under an if
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.callee()).map(Facts.SymbolId::value).contains("java:demo.Circle#<init>(double)");
            assertThat(c.flags()).contains(Facts.CallFlag.CONSTRUCTOR_CALL);
        });
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.callee()).map(Facts.SymbolId::value).contains("java:demo.Circle#radius()");
            assertThat(c.flags()).contains(Facts.CallFlag.CONDITIONAL);
            assertThat(c.site().startLine()).isEqualTo(27);
        });
    }
}
