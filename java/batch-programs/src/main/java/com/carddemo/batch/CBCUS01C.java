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
 * Java port of {@code app/cbl/CBCUS01C.cbl} — "Read and print customer data file".
 *
 * <p>Reads CUSTFILE (CVCUST02Y, RECLEN=500) sequentially and writes each
 * record to SYSOUT twice — faithful to the COBOL double-DISPLAY quirk where
 * the same record is displayed once inside the sub-paragraph and once in the
 * main loop.
 *
 * <h2>Output byte equivalence with COBOL</h2>
 *
 * <p>The COBOL program emits, in order:
 * <ol>
 *   <li>{@code START OF EXECUTION OF PROGRAM CBCUS01C\n}
 *   <li>For each record: two copies of {@code <500 bytes>\n} (double-display)
 *   <li>{@code END OF EXECUTION OF PROGRAM CBCUS01C\n}
 * </ol>
 *
 * <h2>CLI</h2>
 *
 * <pre>
 *   java com.carddemo.batch.CBCUS01C
 *        --custfile &lt;path-to-customer-data-file&gt;
 *        [--encoding ASCII|EBCDIC]    default ASCII
 *        [--reclen 500]               default 500, matches CVCUST02Y RECLEN
 * </pre>
 */
public final class CBCUS01C {

    /** Declared record length from {@code app/cpy/CVCUST02Y.cpy}. */
    public static final int DEFAULT_RECORD_LENGTH = 500;

    private static final byte LF = 0x0A;

    private static final String START_MSG = "START OF EXECUTION OF PROGRAM CBCUS01C";
    private static final String END_MSG   = "END OF EXECUTION OF PROGRAM CBCUS01C";
    private static final String ERROR_MSG = "ERROR READING CUSTFILE";

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        int exitCode = new CBCUS01C().run(parsed, System.out);
        System.out.flush();
        System.exit(exitCode);
    }

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
        return FixedRecordReader.open(args.custfile, args.recordLength, mode);
    }

    private static void writeLine(OutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
        out.write(LF);
    }

    public static final class Args {
        public final Path custfile;
        public final Encoding encoding;
        public final int recordLength;

        Args(Path custfile, Encoding encoding, int recordLength) {
            this.custfile = custfile;
            this.encoding = encoding;
            this.recordLength = recordLength;
        }

        public static Args parse(String[] argv) {
            Path custfile = null;
            Encoding encoding = Encoding.ASCII;
            int recordLength = DEFAULT_RECORD_LENGTH;
            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];
                switch (a) {
                    case "--custfile":
                        custfile = Paths.get(requireNext(argv, i++));
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
            if (custfile == null) {
                throw new IllegalArgumentException("--custfile is required");
            }
            if (!Files.isReadable(custfile)) {
                throw new IllegalArgumentException("--custfile path is not readable: " + custfile);
            }
            return new Args(custfile, encoding, recordLength);
        }

        private static String requireNext(String[] argv, int i) {
            if (i + 1 >= argv.length) {
                throw new IllegalArgumentException("missing value after " + argv[i]);
            }
            return argv[i + 1];
        }

        private static void printHelp() {
            System.out.println("CBCUS01C — read and print CUSTFILE records");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java com.carddemo.batch.CBCUS01C \\");
            System.out.println("       --custfile <path>             [required]");
            System.out.println("       --encoding ASCII|EBCDIC       [default ASCII]");
            System.out.println("       --reclen <n>                  [default 500]");
        }
    }

    public static Args harnessArgs(Path custfile, Encoding encoding, int recordLength) {
        return new Args(custfile, encoding, recordLength);
    }

    public CBCUS01C() {}
}
