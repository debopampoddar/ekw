package ekw.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(fileText, "fileText is null");
        //lines() handles CRLF and CR would help whoever reads it next
        List<String> lines = fileText.lines().toList();
        if (startLine < 1 || endLine < startLine || endLine > lines.size()) {
            throw new IllegalArgumentException("range L" + startLine + "-L" + endLine + " outside 1.." + lines.size());
        }
        return ofLines(lines.subList(startLine - 1, endLine));
    }

    /** Fingerprint of lines already split (no line endings inside). */
    public static String ofLines(List<String> lines) {
       Objects.requireNonNull(lines);
       StringBuilder builder = new StringBuilder();
       lines.forEach(line -> {
           builder.append(line).append("\n");
       });

       return sha256Hex(builder.toString()).substring(0, 16);
    }

    /** Full SHA-256 of a text (UTF-8), as 64 lowercase hex characters. */
    public static String sha256Hex(String text) {
        Objects.requireNonNull(text, "Fingerprint.sha256Hex");
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
