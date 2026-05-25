package com.carddemo.batch;

import com.carddemo.codec.Encoding;
import com.carddemo.vsam.FixedRecordReader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Java port of {@code app/cbl/CBACT03C.cbl} — "Read and print account cross-reference data file".
 *
 * <p>The COBOL original opens the XREFFILE VSAM KSDS sequentially, reads each
 * 50-byte record, and writes it to SYSOUT as a DISPLAY. The Java port does
 * the same: reads each record from a fixed-record file, writes the raw 50
 * bytes followed by a {@code 0x0A} byte to stdout.
 *
 * <h2>Output byte equivalence with COBOL</h2>
 *
 * <p>This port writes raw bytes directly to the output stream — it does not
 * use {@code PrintStream} or {@code println}. Reasons:
 * <ol>
 *   <li>The line separator is hard-coded to {@code "\n"} ({@code 0x0A}).
 *       {@code System.lineSeparator()} returns {@code \r\n} on Windows; the
 *       verification toolchain (GnuCOBOL 3.2.0 on Linux per
 *       {@code phases.txt}) emits {@code \n}. Hard-coding makes the output
 *       platform-independent.
 *   <li>The record bytes are written without re-encoding through the JVM
 *       default charset. If the encoding is EBCDIC, the bytes go out as
 *       EBCDIC; if ASCII, ASCII.
 *   <li>The literal strings {@code START OF EXECUTION...} and
 *       {@code END OF EXECUTION...} are encoded into the target charset
 *       once at startup, so the output is uniformly in the requested
 *       encoding.
 * </ol>
 *
 * <p>The COBOL program emits, in order:
 * <ol>
 *   <li>{@code START OF EXECUTION OF PROGRAM CBACT03C\n}
 *   <li>For each record:
 *     <ol>
 *       <li>{@code <50 bytes>\n} — emitted inside paragraph
 *           {@code 1000-XREFFILE-GET-NEXT} (line 96 of the COBOL).
 *       <li>{@code <50 bytes>\n} — emitted in the main {@code PERFORM UNTIL}
 *           loop body (line 78).
 *     </ol>
 *   <li>{@code END OF EXECUTION OF PROGRAM CBACT03C\n}
 * </ol>
 *
 * <p>The duplicate DISPLAY is not a typo — it appears in the source twice
 * and the byte-equivalence contract requires reproducing it exactly. See
 * {@code app/cbl/CBACT03C.cbl} lines 78 and 96.
 *
 * <h2>Reading order and EOF semantics</h2>
 *
 * <p>The COBOL FILE STATUS contract maps {@code "10"} to "end of file". In
 * the COBOL code, hitting EOF inside {@code 1000-XREFFILE-GET-NEXT} sets
 * {@code END-OF-FILE='Y'} and the routine returns <em>without</em> the inner
 * DISPLAY firing. The main loop then exits before its outer DISPLAY fires.
 * The Java port mirrors this: when {@link FixedRecordReader#nextRecord()}
 * returns {@code null}, we exit the loop without emitting anything for that
 * pseudo-record.
 *
 * <h2>CLI</h2>
 *
 * <pre>
 *   java com.carddemo.batch.CBACT03C
 *        --xreffile &lt;path-to-cardxref-file&gt;
 *        [--encoding ASCII|EBCDIC]         default ASCII
 *        [--reclen 50]                     default 50, matches CVACT03Y RECLN
 * </pre>
 */
public final class CBACT03C {

    /** Declared record length from {@code app/cpy/CVACT03Y.cpy}. */
    public static final int DEFAULT_RECORD_LENGTH = 50;

    /** Single line feed byte. Hard-coded for platform-independent output. */
    private static final byte LF = 0x0A;

    private static final String START_MSG = "START OF EXECUTION OF PROGRAM CBACT03C";
    private static final String END_MSG   = "END OF EXECUTION OF PROGRAM CBACT03C";
    private static final String ERROR_MSG = "ERROR READING XREFFILE";

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        int exitCode = new CBACT03C().run(parsed, System.out);
        // Flush before exit, since we wrote raw bytes.
        System.out.flush();
        System.exit(exitCode);
    }

    /**
     * Run the program against a parsed argument set, writing output to the
     * given stream as raw bytes. Returns the COBOL-equivalent return code
     * (0 = success, nonzero = error).
     *
     * <p>Stream-injectable so the verification harness can capture stdout
     * without subprocessing.
     */
    public int run(Args args, OutputStream rawOut) {
        Charset charset = Charset.forName(args.encoding.charsetName());
        byte[] startMsg = (START_MSG).getBytes(charset);
        byte[] endMsg   = (END_MSG).getBytes(charset);
        byte[] errorMsg = (ERROR_MSG).getBytes(charset);

        // Buffer for performance. Caller's stream may already be buffered;
        // wrapping a BufferedOutputStream around BufferedOutputStream is
        // harmless but redundant — we accept the redundancy for simplicity.
        BufferedOutputStream out = (rawOut instanceof BufferedOutputStream)
                ? (BufferedOutputStream) rawOut
                : new BufferedOutputStream(rawOut);

        try (FixedRecordReader r = openReader(args)) {
            writeLine(out, startMsg);
            byte[] record;
            while ((record = r.nextRecord()) != null) {
                // Mirror the two DISPLAY calls in the COBOL source.
                writeLine(out, record);
                writeLine(out, record);
            }
            writeLine(out, endMsg);
            out.flush();
            return 0;
        } catch (IOException e) {
            try {
                writeLine(out, errorMsg);
                out.flush();
            } catch (IOException ignored) {
                // best-effort error reporting; original abend already imminent
            }
            // COBOL's 9999-ABEND-PROGRAM calls CEE3ABD with abend code 999.
            return 999;
        }
    }

    private FixedRecordReader openReader(Args args) throws IOException {
        FixedRecordReader.Mode mode = (args.encoding == Encoding.ASCII)
                ? FixedRecordReader.Mode.CRLF
                : FixedRecordReader.Mode.RAW;
        return FixedRecordReader.open(args.xreffile, args.recordLength, mode);
    }

    /** Write a sequence of bytes followed by a {@code 0x0A} (LF). */
    private static void writeLine(OutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
        out.write(LF);
    }

    // ---------------------------------------------------------------------
    // CLI parsing
    // ---------------------------------------------------------------------

    /** Parsed CLI arguments. Public for harness access. */
    public static final class Args {
        public final Path xreffile;
        public final Encoding encoding;
        public final int recordLength;

        Args(Path xreffile, Encoding encoding, int recordLength) {
            this.xreffile = xreffile;
            this.encoding = encoding;
            this.recordLength = recordLength;
        }

        public static Args parse(String[] argv) {
            Path xreffile = null;
            Encoding encoding = Encoding.ASCII;
            int recordLength = DEFAULT_RECORD_LENGTH;
            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];
                switch (a) {
                    case "--xreffile":
                        xreffile = Paths.get(requireNext(argv, i++));
                        break;
                    case "--encoding":
                        encoding = Encoding.valueOf(requireNext(argv, i++).toUpperCase());
                        break;
                    case "--reclen":
                        recordLength = Integer.parseInt(requireNext(argv, i++));
                        break;
                    case "-h":
                    case "--help":
                        printHelp();
                        System.exit(0);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown argument: " + a);
                }
            }
            if (xreffile == null) {
                throw new IllegalArgumentException("--xreffile is required");
            }
            if (!Files.isReadable(xreffile)) {
                throw new IllegalArgumentException("--xreffile path is not readable: " + xreffile);
            }
            return new Args(xreffile, encoding, recordLength);
        }

        private static String requireNext(String[] argv, int i) {
            if (i + 1 >= argv.length) {
                throw new IllegalArgumentException("missing value after " + argv[i]);
            }
            return argv[i + 1];
        }

        private static void printHelp() {
            System.out.println("CBACT03C — read and print XREFFILE records");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java com.carddemo.batch.CBACT03C \\");
            System.out.println("       --xreffile <path>             [required]");
            System.out.println("       --encoding ASCII|EBCDIC       [default ASCII]");
            System.out.println("       --reclen <n>                  [default 50]");
        }
    }

    /** Public factory for the verification harness — avoids touching argv parsing. */
    public static Args harnessArgs(Path xreffile, Encoding encoding, int recordLength) {
        return new Args(xreffile, encoding, recordLength);
    }

    /** Allow the harness to construct without main(). */
    public CBACT03C() {}
}
