package ekw.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs from day one: proves the build, the test setup and the chapter switch work. */
@Tag("skeleton")
class SkeletonTest {

    @Test
    void runsOnJava25OrNewer() {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(25);
    }

    @Test
    void stubsNameTheirChapter() {
        assertThat(Todo.chapter(2, "Fingerprint.ofRange")).hasMessage(
                "TODO (development guide, chapter 2): Fingerprint.ofRange");
    }
}
