# JCL and the daily batch

> The mainframe's equivalent of a shell script + Kubernetes Job manifest, in a syntax from 1964.

## Why this matters for migration

Every batch program in CardDemo is launched by a **JCL job**. The JCL declares which datasets the COBOL program reads and writes, how much memory it gets, and what happens if it fails. You cannot migrate the COBOL programs in isolation — you need to migrate the surrounding JCL contract too, because that contract defines the program's interface with the rest of the system.

The Java port replaces a JCL job with a Java program that takes the same logical inputs and produces the same logical outputs. The wire format of those inputs/outputs (byte layouts) must match for verification to work.

## What a JCL job looks like

A simple JCL job has three things: a **JOB** card (who's running this), one or more **EXEC** steps (which programs run), and **DD** statements (which datasets each step reads/writes).

`app/jcl/INTCALC.jcl` (interest calculation) compresses to roughly:

```jcl
//INTCALC  JOB CLASS=A,MSGCLASS=H
//STEP010  EXEC PGM=CBACT04C,PARM='2022071800'
//STEPLIB  DD DSN=AWS.M2.CARDDEMO.LOADLIB,DISP=SHR
//TCATBAL  DD DSN=AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS,DISP=SHR
//XREFFILE DD DSN=AWS.M2.CARDDEMO.XREFFILE.VSAM.KSDS,DISP=SHR
//ACCTFILE DD DSN=AWS.M2.CARDDEMO.ACCTFILE.VSAM.KSDS,DISP=SHR
//DISCGRP  DD DSN=AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS,DISP=SHR
//TRANSACT DD DSN=AWS.M2.CARDDEMO.SYSTRAN(+1),DISP=(NEW,CATLG,DELETE)
//SYSOUT   DD SYSOUT=*
```

What it says:
- `EXEC PGM=CBACT04C,PARM='2022071800'` — run the program `CBACT04C` with the run-date `2022-07-18 00:00` as a command-line argument.
- `STEPLIB` — where to find the compiled program.
- Five `DD` statements — five datasets the program will reference by logical name (`TCATBAL`, `XREFFILE`, etc.). Inside the COBOL program, those logical names are `SELECT … ASSIGN TO TCATBAL`.
- `SYSTRAN(+1)` — Generation Data Group, "next generation". A new output file is created; the previous generation is kept and renamed automatically.
- `SYSOUT=*` — capture program stdout to the job log.

In Java, this becomes:

```
java -cp carddemo.jar com.carddemo.batch.CBACT04C \
  --run-date 2022-07-18T00:00 \
  --tcatbal /data/tcatbalf.dat \
  --xreffile /data/xreffile.dat \
  --acctfile /data/acctfile.dat \
  --discgrp /data/discgrp.dat \
  --output /data/systran.G0042.dat
```

The Java program reads the same byte layouts, runs the same logic, writes the same byte layout. JCL becomes flags.

## The CardDemo daily batch

`scripts/run_full_batch.sh` orchestrates 14 jobs in a specific order. Each is a separate `EXEC` in a separate JCL.

```
 1.  CLOSEFIL   close online files so batch can have exclusive access
 2.  ACCTFILE   refresh ACCTFILE from a flat-file snapshot
 3.  CARDFILE   refresh CARDFILE
 4.  XREFFILE   refresh XREFFILE
 5.  CUSTFILE   refresh CUSTFILE
 6.  TRANBKP    back up TRANFILE
 7.  DISCGRP    refresh DISCGRP
 8.  TCATBALF   refresh TCATBALF
 9.  TRANTYPE   refresh TRANTYPE
10.  DUSRSECJ   refresh user security file
11.  POSTTRAN   post the day's transactions to ACCTFILE (program CBTRN02C)
12.  INTCALC    calculate interest, write interest transactions (program CBACT04C)
13.  TRANBKP    back up TRANFILE again
14.  COMBTRAN   merge daily + interest transactions
15.  TRANIDX    rebuild the alternate index on TRANFILE
16.  OPENFIL    reopen files for online access
```

Of these, steps 11–15 are the **business-critical** path. Steps 1–10 are setup/refresh. Step 16 is teardown.

## What each business step does

- **POSTTRAN (CBTRN02C)** reads `DALYTRAN` (the day's transaction file, format = `CVTRA05Y`, 350-byte records). For each transaction, it looks up the card in XREFFILE, the account in ACCTFILE, validates available credit, updates the account balance, and appends the transaction to TRANFILE. Rejected transactions go to `DALYREJS`. ~300 transactions typical per day in the sample data.
- **INTCALC (CBACT04C)** walks TCATBALF (one record per `account × transaction-category × group`). For each, looks up the discount-group rate from DISCGRP, computes monthly interest as `balance × annual-rate ÷ 1200`, writes an interest-charge transaction to a new TRANSACT generation, and updates the account's current balance in ACCTFILE.
- **COMBTRAN** simply concatenates the day's posted transactions and the day's interest transactions into TRANFILE.
- **TRANIDX** rebuilds the alternate index on TRANFILE (keyed on the processed-timestamp at bytes 304–329) so the online system can scan transactions chronologically.

## Why the order matters

POSTTRAN must complete before INTCALC because INTCALC reads account balances that POSTTRAN has just updated. If you run them in parallel, INTCALC sees stale balances and the interest calculation is wrong.

The Java port must preserve this serialization. A naive "let's parallelize the batch" rewrite breaks the migration unless dataflow is re-analyzed. For Phase 1–6 of this repo, the Java port preserves the order exactly.

## What you'll see in a successful run

Each JCL job returns a **condition code** (`RC` in the job log). `RC=0000` means success. `RC=0004` is warnings (typically end-of-file as expected). Anything else is an error and the next job in the chain is held.

The Java port must return the same exit codes for the same input conditions. This is part of the "100% accuracy" contract.

## References (re-verifiable)

- Batch driver: `scripts/run_full_batch.sh`.
- Individual JCL: `app/jcl/CLOSEFIL.jcl`, `ACCTFILE.jcl`, `POSTTRAN.jcl`, `INTCALC.jcl`, `COMBTRAN.jcl`, `TRANIDX.jcl`, `OPENFIL.jcl`.
- Interest formula source: `app/cbl/CBACT04C.cbl` — search for `COMPUTE WS-MONTHLY-INT`.
- Posting logic: `app/cbl/CBTRN02C.CBL`.
