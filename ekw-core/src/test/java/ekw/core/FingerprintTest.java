package ekw.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ch02")
class FingerprintTest {

    @Test
    void hashesSelectedLinesWithTrailingNewline() {
        String file = "a\nb\nc\n";
        assertThat(Fingerprint.ofRange(file, 2, 3)).isEqualTo(Fingerprint.sha256Hex("b\nc\n").substring(0, 16));
    }

    @Test
    void lineEndingsDoNotChangeTheFingerprint() {
        assertThat(Fingerprint.ofRange("x\r\ny\r\n", 1, 2)).isEqualTo(Fingerprint.ofRange("x\ny\n", 1, 2));
    }

    @Test
    void rejectsRangesOutsideTheFile() {
        assertThatThrownBy(() -> Fingerprint.ofRange("x\n", 1, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
