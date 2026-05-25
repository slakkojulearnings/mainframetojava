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
 * Java port of {@code app/cbl/CBACT02C.cbl} — "Read and print card data file".
 *
 * <p>Reads CARDFILE (CVACT02Y, RECLN=150) sequentially and writes each
 * record to SYSOUT.
 *
 * <h2>Output byte equivalence with COBOL</h2>
 *
 * <p>Unlike its sibling {@link CBACT03C}, this program emits each record
 * <em>once</em>. The inner-paragraph DISPLAY in
 * {@code 1000-CARDFILE-GET-NEXT} is commented out in the COBOL source (line
 * 96 of {@code app/cbl/CBACT02C.cbl}), so only the outer DISPLAY in the main
 * loop body (line 78) fires.
 *
 * <p>Order:
 * <ol>
 *   <li>{@code START OF EXECUTION OF PROGRAM CBACT02C\n}
 *   <li>For each record: {@code <150 bytes>\n}
 *   <li>{@code END OF EXECUTION OF PROGRAM CBACT02C\n}
 * </ol>
 *
 * <p>The same output-stream discipline as CBACT03C applies: raw
 * {@link OutputStream}, hard-coded {@code 0x0A} line feed, all literals
 * encoded into the target charset once.
 *
 * <h2>CLI</h2>
 *
 * <pre>
 *   java com.carddemo.batch.CBACT02C
 *        --cardfile &lt;path-to-carddata-file&gt;
 *        [--encoding ASCII|EBCDIC]    default ASCII
 *        [--reclen 150]               default 150, matches CVACT02Y RECLN
 * </pre>
 */
public final class CBACT02C {

    /** Declared record length from {@code app/cpy/CVACT02Y.cpy}. */
    public static final int DEFAULT_RECORD_LENGTH = 150;

    private static final byte LF = 0x0A;

    private static final String START_MSG = "START OF EXECUTION OF PROGRAM CBACT02C";
    private static final String END_MSG   = "END OF EXECUTION OF PROGRAM CBACT02C";
    private static final String ERROR_MSG = "ERROR READING CARDFILE";

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        int exitCode = new CBACT02C().run(parsed, System.out);
        System.out.flush();
        System.exit(exitCode);
    }

    /** Run with parsed args, writing raw bytes to {@code rawOut}. Returns the return code. */
    public int run(Args args, OutputStream rawOut) {
        Charset charset = Charset.forName(args.encoding.charsetName());
        byte[] startMsg = (START_MSG).getBytes(charset);
        byte[] endMsg   = (END_MSG).getBytes(charset);
        byte[] errorMsg = (ERROR_MSG).getBytes(charset);

        BufferedOutputStream out = (rawOut instanceof BufferedOutputStream)
                ? (BufferedOutputStream) rawOut
                : new BufferedOutputStream(rawOut);

        try (FixedRecordReader r = openReader(args)) {
            writeLine(out, startMsg);
            byte[] record;
            while ((record = r.nextRecord()) != null) {
                writeLine(out, record);
            }
            writeLine(out, endMsg);
            out.flush();
            return 0;
        } catch (IOException e) {
            try {
                writeLine(out, errorMsg);
                out.flush();
            } catch (IOException ignored) {}
            return 999;
        }
    }

    private FixedRecordReader openReader(Args args) throws IOException {
        FixedRecordReader.Mode mode = (args.encoding == Encoding.ASCII)
                ? FixedRecordReader.Mode.CRLF
                : FixedRecordReader.Mode.RAW;
        return FixedRecordReader.open(args.cardfile, args.recordLength, mode);
    }

    private static void writeLine(OutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
        out.write(LF);
    }

    public static final class Args {
        public final Path cardfile;
        public final Encoding encoding;
        public final int recordLength;

        Args(Path cardfile, Encoding encoding, int recordLength) {
            this.cardfile = cardfile;
            this.encoding = encoding;
            this.recordLength = recordLength;
        }

        public static Args parse(String[] argv) {
            Path cardfile = null;
            Encoding encoding = Encoding.ASCII;
            int recordLength = DEFAULT_RECORD_LENGTH;
            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];
                switch (a) {
                    case "--cardfile":
                        cardfile = Paths.get(requireNext(argv, i++));
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
            if (cardfile == null) {
                throw new IllegalArgumentException("--cardfile is required");
            }
            if (!Files.isReadable(cardfile)) {
                throw new IllegalArgumentException("--cardfile path is not readable: " + cardfile);
            }
            return new Args(cardfile, encoding, recordLength);
        }

        private static String requireNext(String[] argv, int i) {
            if (i + 1 >= argv.length) {
                throw new IllegalArgumentException("missing value after " + argv[i]);
            }
            return argv[i + 1];
        }

        private static void printHelp() {
            System.out.println("CBACT02C — read and print CARDFILE records");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java com.carddemo.batch.CBACT02C \\");
            System.out.println("       --cardfile <path>           [required]");
            System.out.println("       --encoding ASCII|EBCDIC     [default ASCII]");
            System.out.println("       --reclen <n>                [default 150]");
        }
    }

    /** Public factory for the verification harness. */
    public static Args harnessArgs(Path cardfile, Encoding encoding, int recordLength) {
        return new Args(cardfile, encoding, recordLength);
    }

    public CBACT02C() {}
}
