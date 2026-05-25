package com.carddemo.verification;

import com.carddemo.batch.CBACT03C;
import com.carddemo.codec.Encoding;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end byte-equivalence test for CBACT03C against the cardxref fixture.
 *
 * <p>Runs the Java port against {@code app/data/ASCII/cardxref.txt} (the
 * 50-record fixture, with the FILLER stripping inconsistency documented in
 * {@code docs/concepts/01-ebcdic-and-zoned-decimal.md}), captures the
 * bytes it emits to stdout, and diffs them against an analytically-derived
 * expected stream constructed by {@link Cbact03cExpected}.
 *
 * <p>The test path to the source repo is resolved relative to the
 * {@code carddemo.source} system property if set, otherwise via the
 * {@code CARDDEMO_SOURCE} environment variable, otherwise by walking up
 * from the test working directory looking for {@code app/data/ASCII/cardxref.txt}.
 *
 * <p>If no fixture can be located, the test is <em>skipped</em> rather than
 * failed — this lets the suite run successfully even when the source repo
 * isn't checked out alongside this one.
 */
class Cbact03cVerificationTest {

    @Test
    void java_port_emits_expected_bytes_for_cardxref_ascii() throws Exception {
        Path xreffile = locateFixture("app/data/ASCII/cardxref.txt");
        assumeTrue(xreffile != null && Files.isReadable(xreffile),
                "cardxref.txt fixture not found — set -Dcarddemo.source=<path-to-aws-mainframe-modernization-carddemo>");

        // Run the Java port and capture stdout bytes.
        ByteArrayOutputStream actualOut = new ByteArrayOutputStream();
        CBACT03C program = new CBACT03C();
        CBACT03C.Args args = CBACT03C.harnessArgs(xreffile, Encoding.ASCII,
                CBACT03C.DEFAULT_RECORD_LENGTH);
        int rc = program.run(args, actualOut);

        assertThat(rc).as("return code").isEqualTo(0);

        // Generate the analytically-derived expected output.
        byte[] expected = Cbact03cExpected.generate(xreffile, Encoding.ASCII,
                CBACT03C.DEFAULT_RECORD_LENGTH);
        byte[] actual = actualOut.toByteArray();

        ByteDiff.Result diff = ByteDiff.diff(expected, actual);
        assertThat(diff.identical)
                .as("byte-equivalence check: %s", diff)
                .isTrue();
    }

    /** Walk up from cwd looking for the fixture; check system property and env var first. */
    private static Path locateFixture(String relative) {
        String prop = System.getProperty("carddemo.source");
        if (prop != null) {
            Path p = Paths.get(prop, relative);
            if (Files.isReadable(p)) return p;
        }
        String env = System.getenv("CARDDEMO_SOURCE");
        if (env != null) {
            Path p = Paths.get(env, relative);
            if (Files.isReadable(p)) return p;
        }
        // Walk up from cwd, looking for a sibling repo named like
        // "aws-mainframe-modernization-carddemo" or any dir containing the
        // relative path.
        Path cwd = Paths.get(".").toAbsolutePath().normalize();
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("aws-mainframe-modernization-carddemo").resolve(relative);
            if (Files.isReadable(candidate)) return candidate;
            // Also try sibling directories at each level.
            Path parent = dir.getParent();
            if (parent != null) {
                Path sibling = parent.resolve("aws-mainframe-modernization-carddemo").resolve(relative);
                if (Files.isReadable(sibling)) return sibling;
            }
        }
        return null;
    }
}
