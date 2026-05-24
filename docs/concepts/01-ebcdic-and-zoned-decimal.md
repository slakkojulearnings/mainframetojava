# EBCDIC and zoned-decimal numbers

> Why a credit-card balance is stored as 12 bytes that look almost like ASCII text, except the last byte is a curly brace.

## Why this matters for migration

The CardDemo sample data ships in two encodings: `app/data/EBCDIC/` (mainframe-native, IBM-1047) and `app/data/ASCII/` (Unix-friendly). They are not the same files — the bytes differ, the sizes differ, and the numeric fields use a sign-encoding scheme that doesn't exist in standard ASCII.

If a Java port reads `acctdata.txt` and parses `0000019400{` as a string, the balance will look like `19400{` — wrong. The correct decoded value is **+194.00**. Every signed numeric field in the data uses the same scheme. Getting this wrong means the Java port silently produces wrong financial output.

## EBCDIC vs ASCII — what's actually different

EBCDIC (Extended Binary Coded Decimal Interchange Code) is IBM's character encoding. It predates ASCII and groups characters differently. The character `0` is `0xF0` in EBCDIC vs `0x30` in ASCII. The character `A` is `0xC1` vs `0x41`.

CardDemo's EBCDIC sample files use code page **IBM-1047** (US/Canada with euro sign).

| What | EBCDIC byte | ASCII byte |
|---|---|---|
| `0` | `0xF0` | `0x30` |
| `1` | `0xF1` | `0x31` |
| `9` | `0xF9` | `0x39` |
| `A` | `0xC1` | `0x41` |
| `{` | `0xC0` | `0x7B` |
| `}` | `0xD0` | `0x7D` |
| space | `0x40` | `0x20` |
| CRLF | (not used — fixed-record format) | `0x0D 0x0A` |

The EBCDIC file is fixed-record (no line terminators); the ASCII file uses CRLF. That's why the same 50 account records take 15,000 bytes in EBCDIC (50 × 300) but 15,100 bytes in ASCII (50 × 302).

## Zoned-decimal numbers

COBOL stores numbers in many encodings. The simplest is **DISPLAY** (also called zoned-decimal), where each digit takes one byte and the byte looks like its ASCII/EBCDIC character.

A field declared `PIC 9(10)V99` is 12 bytes of digit characters representing 10 integer digits plus 2 implied decimal places. The decimal point is **not** stored — it's an interpretation rule.

```
Bytes (ASCII):  '0' '0' '0' '0' '0' '0' '0' '1' '9' '4' '0' '0'
Decoded:         0   0   0   0   0   0   0   1   9   4 . 0   0
Value:                                     1   9   4 . 0   0  =  194.00
```

Unsigned, this is straightforward. The complication is when the field is **signed**.

## Sign overpunch — the curly-brace trick

A COBOL field declared `PIC S9(10)V99` is signed. But the field is still 12 bytes of digit characters, so where does the sign go?

The answer: the **last byte** is modified to encode both its digit and the sign. This trick was efficient on punch cards (an extra "zone" punch over the last column carried the sign) and is preserved verbatim in the byte layout.

| Last byte | Means digit | Sign | Original last digit |
|---|---|---|---|
| `0`–`9` | 0–9 | unsigned | as-is |
| `{` | 0 | **+** | 0 |
| `A` | 1 | **+** | 1 |
| `B` | 2 | **+** | 2 |
| `C` | 3 | **+** | 3 |
| `D` | 4 | **+** | 4 |
| `E` | 5 | **+** | 5 |
| `F` | 6 | **+** | 6 |
| `G` | 7 | **+** | 7 |
| `H` | 8 | **+** | 8 |
| `I` | 9 | **+** | 9 |
| `}` | 0 | **−** | 0 |
| `J` | 1 | **−** | 1 |
| `K` | 2 | **−** | 2 |
| `L` | 3 | **−** | 3 |
| `M` | 4 | **−** | 4 |
| `N` | 5 | **−** | 5 |
| `O` | 6 | **−** | 6 |
| `P` | 7 | **−** | 7 |
| `Q` | 8 | **−** | 8 |
| `R` | 9 | **−** | 9 |

### Worked example, verified against sample data

From `app/data/ASCII/dailytran.txt` row 2, the transaction amount field (`TRAN-AMT PIC S9(09)V99`, 11 bytes) is:

```
0 0 0 0 0 0 9 1 9 0 }
```

Decoding:
- First 10 characters: digits `0000009190` → integer 9190
- Last byte `}` → digit **0**, sign **negative**
- Full integer: 91900
- With implied 2 decimal places: **−919.00**

The transaction description on that same row is `"Return item at Nitzsche, Lockman and Kuhlman"`. A return is a refund — a negative posting to the merchant — which confirms the −$919.00 decode is correct.

## Test-data gap to be aware of

`acctdata.txt` (50 account records) contains **zero negative-balance characters**. Every account has a non-negative current and cycle balance. The negative-overpunch decoder is therefore exercised only by `dailytran.txt`. When porting CBACT01C or CBACT04C (which read account balances), the negative path must be exercised with synthetic fixtures or it will be a silent gap in the test suite.

## How the codec must work

A zoned-decimal decoder takes:
- The raw bytes
- The PIC clause (digit count, scale, signed yes/no)
- The encoding (EBCDIC or ASCII)

…and returns a `BigDecimal` (never `double` — financial math demands exact decimal arithmetic).

The encoder is the inverse: given a `BigDecimal` and a PIC clause, produce the exact bytes the COBOL file would have. Round-tripping any value through encode→decode must return the same value, and decode→encode of any byte sequence the COBOL program would write must return the same bytes. That's the codec's contract.

## References (re-verifiable)

- IBM-1047 code page: any IBM documentation reference; widely available.
- Sample data: `app/data/ASCII/acctdata.txt` (15,100 bytes, 50 × 302), `app/data/EBCDIC/AWS.M2.CARDDEMO.ACCTDATA.PS` (15,000 bytes, 50 × 300).
- Negative-overpunch worked example: `app/data/ASCII/dailytran.txt` row 2.
- Account record layout: `app/cpy/CVACT01Y.cpy` (PIC clauses for balance fields).
- Transaction record layout: `app/cpy/CVTRA05Y.cpy` (`TRAN-AMT PIC S9(09)V99`).
