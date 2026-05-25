# apply-commits.ps1
#
# Stages and commits the NOVA Phase 1/2 deliverables to F:\mainframetojava in
# logical chunks. Run from PowerShell at any directory:
#
#     pwsh -File F:\mainframetojava\apply-commits.ps1
#
# This script does NOT push. After running, inspect `git log` and push manually
# when ready:
#
#     cd F:\mainframetojava
#     git push origin main
#
# The script is idempotent in the trivial sense: each commit only runs if its
# files have changes staged. Re-running after a clean state is a no-op.

Set-Location 'F:\mainframetojava'

function Commit-If-Changes {
    param(
        [Parameter(Mandatory=$true)] [string] $message,
        [Parameter(Mandatory=$true)] [string[]] $files
    )
    foreach ($f in $files) {
        if (Test-Path $f) {
            git add $f
        }
    }
    $staged = git diff --cached --name-only
    if ([string]::IsNullOrWhiteSpace($staged)) {
        Write-Host ("Skipping commit (no changes staged): " + $message)
        return
    }
    Write-Host ("Committing: " + $message)
    git commit -m $message
}

# Commit 1: Foundation docs + branch README. (May already be partially in place;
# `git add` for files that exist is harmless.)
Commit-If-Changes -message "Phase 0: foundation docs and verification strategy" -files @(
    'README.md',
    'phases.txt',
    'transcript-cleaned.txt',
    '.gitignore',
    'docs/concepts/01-ebcdic-and-zoned-decimal.md',
    'docs/concepts/02-vsam-ksds-and-aix.md',
    'docs/concepts/03-jcl-and-the-daily-batch.md',
    'verification/README.md'
)

# Commit 2: Java multi-module skeleton.
Commit-If-Changes -message "Phase 1a: Maven multi-module Java 17 skeleton" -files @(
    'java/pom.xml',
    'java/README.md',
    'java/cobol-codec/pom.xml',
    'java/vsam-io/pom.xml',
    'java/batch-programs/pom.xml',
    'java/verification/pom.xml'
)

# Commit 3: cobol-codec module (zoned-decimal + overpunch + text codec + PIC clause).
Commit-If-Changes -message "Phase 1b: cobol-codec — zoned-decimal, overpunch, PIC clause, EBCDIC/ASCII" -files @(
    'java/cobol-codec/src/main/java/com/carddemo/codec/Encoding.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/PicClause.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/Overpunch.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/ZonedDecimalCodec.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/ZonedValue.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/TextCodec.java',
    'java/cobol-codec/src/main/java/com/carddemo/codec/Field.java',
    'java/cobol-codec/src/test/java/com/carddemo/codec/OverpunchTest.java',
    'java/cobol-codec/src/test/java/com/carddemo/codec/ZonedDecimalCodecTest.java'
)

# Commit 4: vsam-io module (fixed-record reader for ASCII CRLF and EBCDIC RAW).
Commit-If-Changes -message "Phase 1c: vsam-io — FixedRecordReader for ASCII and EBCDIC fixtures" -files @(
    'java/vsam-io/src/main/java/com/carddemo/vsam/FixedRecordReader.java'
)

# Commit 5: Pilot 1 — CBACT03C port + verification harness.
Commit-If-Changes -message "Phase 2: pilot CBACT03C port with byte-equivalence verification" -files @(
    'java/batch-programs/src/main/java/com/carddemo/batch/CBACT03C.java',
    'java/verification/src/main/java/com/carddemo/verification/ByteDiff.java',
    'java/verification/src/main/java/com/carddemo/verification/Cbact03cExpected.java',
    'java/verification/src/test/java/com/carddemo/verification/Cbact03cVerificationTest.java'
)

Write-Host ""
Write-Host "Done. Current log:"
git log --oneline -10
Write-Host ""
Write-Host "To push to your GitHub remote:"
Write-Host "    cd F:\mainframetojava"
Write-Host "    git push origin main"
