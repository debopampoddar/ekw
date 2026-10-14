package ekw.core;

import java.util.List;

/**
 * Citation fingerprints (design: stage 2, section 2.4): the first 16 hex characters of SHA-256 over the cited lines,
 * joined with LF, plus a trailing LF, after normalizing CRLF and CR to LF.
 *
 * <p>Chapter 2 of the development guide.
 */
public final class Fingerprint {
    private Fingerprint() {}

    /** Fingerprint of lines {@code startLine..endLine} (1-based, inclusive) of a file. Rejects ranges outside the file. */
    public static String ofRange(String fileText, int startLine, int endLine) {
        throw Todo.chapter(2, "Fingerprint.ofRange");
    }

    /** Fingerprint of lines already split (no line endings inside). */
    public static String ofLines(List<String> lines) {
        throw Todo.chapter(2, "Fingerprint.ofLines");
    }

    /** Full SHA-256 of a text (UTF-8), as 64 lowercase hex characters. */
    public static String sha256Hex(String text) {
        throw Todo.chapter(2, "Fingerprint.sha256Hex");
    }
}
