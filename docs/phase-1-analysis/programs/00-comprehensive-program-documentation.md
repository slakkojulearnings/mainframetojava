# CardDemo COBOL Programs - Comprehensive Documentation

## Table of Contents
1. [Batch Programs](#batch-programs)
2. [CICS Interactive Programs](#cics-interactive-programs)
3. [Utility Programs](#utility-programs)
4. [Summary Statistics](#summary-statistics)

---

## BATCH PROGRAMS

### 1. CBACT01C - Account File Reader & Processor

**Program Name & ID:** CBACT01C - Account File Reader

**Program Type:** BATCH

**Business Purpose:** Reads account master file (VSAM KSDS) sequentially and produces three output files for downstream processing: a formatted account file, an array-based account file with balance history, and variable-length records with account summary data. Serves as data extraction and reformatting utility.

**Input Files/Data:**
- ACCTFILE (VSAM KSDS, indexed): Account master records
  - Key: Account ID (11 numeric digits)
  - Record: Account ID + 289 bytes account data

**Output Files/Data:**
- OUTFILE (sequential): Formatted account records
  - Record format: Account ID, Active Status, Current Balance, Credit Limit, Cash Credit Limit, Dates (open/expiry/reissue), Current Cycle Credit/Debit, Group ID
- ARRYFILE (sequential): Array-based records with 5 occurrences of balance/debit pairs
  - Record format: Account ID + 5 sets of (Current Balance + Current Cycle Debit)
- VBRCFILE (variable-length): Variable records
  - VB1 record (12 bytes): Account ID + Active Status
  - VB2 record (39 bytes): Account ID + Balance + Credit Limit + Reissue Year

**Key Business Rules:**
- Account data extracted from master file and reformatted for different consumption patterns
- Current Cycle Debit defaults to 2525.00 if zero
- Array file stores multiple balance snapshots (support for 5 historical periods)
- Date formatting applied via external assembler program COBDATFT
- Variable-length file demonstrates COBOL record length handling

**Processing Logic:**
1. Open all input and output files
2. Loop through account file sequentially
3. For each account record:
   - Display account details to console
   - Populate and write formatted account record
   - Populate and write array-based record with test balances
   - Create and write 2 variable-length records (VB1=12 bytes, VB2=39 bytes)
4. Close all files and terminate

**Copybooks Used:**
- CVACT01Y: Account record definition
- CODATECN: Date conversion structure

**Called Programs:**
- COBDATFT: Date format conversion utility
- CEE3ABD: Runtime abend handler

**File Operations:**
- ACCTFILE: Sequential read access to VSAM KSDS
- OUTFILE, ARRYFILE, VBRCFILE: Sequential write operations
- All files use FILE STATUS clauses for error handling
- Variable-length file: RECORDING MODE IS V with RECORD IS VARYING

**Key Fields:**
- ACCT-ID: 11-digit account identifier (primary key)
- ACCT-ACTIVE-STATUS: Y/N indicator
- ACCT-CURR-BAL: Current account balance (signed numeric)
- ACCT-CREDIT-LIMIT: Maximum credit available
- ACCT-CASH-CREDIT-LIMIT: Maximum cash advance limit
- ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, ACCT-REISSUE-DATE: Date fields
- ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT: Current billing cycle totals
- ACCT-GROUP-ID: Account grouping identifier

**Edge Cases:**
- File not found on open operations
- End-of-file detection (status code 10)
- Zero current cycle debit values
- Variable-length record sizing constraints (10-80 bytes)
- Date conversion failures from COBDATFT

**Notes:**
- Mixed output file types (sequential, variable-length)
- Heavy use of PERFORM sections for modularity
- File error handling via FILE STATUS
- CardDemo version: v2.0-25-gdb72e6b-235

---

### 2. CBACT02C - Card File Reader

**Program Name & ID:** CBACT02C - Card File Reader

**Program Type:** BATCH

**Business Purpose:** Sequentially reads card master file (VSAM KSDS) and displays each card record for verification or reporting.

**Input Files/Data:**
- CARDFILE (VSAM KSDS): Card master records
  - Key: Card Number (16 alphanumeric)

**Output Files/Data:**
- Console output via DISPLAY statements

**Processing Logic:**
1. Open CARDFILE for input
2. Loop until EOF
3. Read and display each card record
4. Close file

**Copybooks Used:**
- CVACT02Y: Card record definition

**Edge Cases:**
- Missing CARDFILE
- Empty CARDFILE (immediate EOF)

**Notes:**
- Simple verification utility
- CardDemo version: v2.0-25-gdb72e6b-235

---

### 3. CBACT03C - Card-Account Cross Reference Reader

**Program Name & ID:** CBACT03C - Card-Account Cross Reference Reader

**Program Type:** BATCH

**Business Purpose:** Reads and displays card-to-account cross reference file for verification.

**Input Files/Data:**
- XREFFILE (VSAM KSDS): Card-Account cross reference
  - Key: Card Number (16 alphanumeric)

**Output Files/Data:**
- Console output

**Copybooks Used:**
- CVACT03Y: Cross reference record definition

**Notes:**
- Verification utility
- Establishes card-account relationship linkage

---

### 4. CBACT04C - Interest Calculator & Batch Processor

**Program Name & ID:** CBACT04C - Interest Calculator & Account Updater

**Program Type:** BATCH

**Business Purpose:** Complex batch program that reads transaction category balances, calculates interest by category, applies disclosure group rates, creates synthetic interest transactions, and updates account master records.

**Input Files/Data:**
- TCATBAL-FILE (VSAM KSDS, sequential): Transaction category balance records
  - Key: Account ID (11) + Type Code (2) + Category Code (4)
- XREF-FILE (VSAM KSDS, random): Card-Account cross reference (with alternate key on Account ID)
- DISCGRP-FILE (VSAM KSDS, random): Disclosure group interest rates
  - Key: Account Group ID (10) + Type (2) + Category (4)
- ACCOUNT-FILE (VSAM KSDS, I-O): Account master for read and update
- TRANSACT-FILE (sequential, output): Generated interest transactions

**Output Files/Data:**
- TRANSACT-FILE: Synthetic interest transaction records with DB2 timestamps

**Key Business Rules:**
- Interest calculated as: (Balance × Annual Rate) / 1200 = Monthly Interest
- Default disclosure group used if specific account group not found (status 23)
- Interest transactions created with system source and category 05
- Account balances updated with cumulative interest when account ID changes
- Cycle credit/debit reset to zero after interest posting
- All transactions include DB2-format timestamp (YYYYMMDD-HH.MM.SS.MMMM)

**Processing Logic:**
1. Open all 5 files
2. Read transaction category balance file sequentially
3. When account ID changes:
   - Update previous account with cumulative interest
   - Reset accumulator
   - Read new account and cross reference data
4. For each category balance:
   - Look up disclosure group for interest rate
   - Calculate monthly interest
   - Create synthetic transaction record
5. At EOF, update final account

**Copybooks Used:**
- CVTRA01Y: Transaction category balance record
- CVACT03Y: Card-Account cross reference record
- CVTRA02Y: Disclosure group record
- CVACT01Y: Account record
- CVTRA05Y: Transaction record definition

**Called Programs:**
- COBDATFT: Date formatting via Z-GET-DB2-FORMAT-TIMESTAMP
- CEE3ABD: Abend handler

**File Operations:**
- All files: FILE STATUS with error handling
- REWRITE: In-place account balance updates
- Random access: Cross reference, disclosure group lookups
- Sequential read: Category balances

**Key Fields:**
- TRANCAT-ACCT-ID: Account identifier from transaction category balance
- TRANCAT-TYPE-CD: Transaction type code
- TRANCAT-CD: Transaction category code
- TRAN-CAT-BAL: Balance amount for interest calculation
- DIS-INT-RATE: Annual interest rate from disclosure group
- WS-MONTHLY-INT: Calculated monthly interest
- WS-TOTAL-INT: Accumulated interest for account

**Edge Cases:**
- Disclosure group not found (uses DEFAULT group fallback)
- Account not found in account master
- Cross reference not found for account
- Interest rate of zero
- First account in file requires flag check
- Variable-length timestamp with millisecond precision

**Notes:**
- Complex multi-file join operation
- Demonstrates REWRITE for in-place record updates
- Default group lookup pattern for flexible rate assignment
- Accepts date parameter via LINKAGE SECTION
- CardDemo version: v2.0-25-gdb72e6b-235

---

### 5. CBCUS01C - Customer File Reader

**Program Name & ID:** CBCUS01C - Customer File Reader

**Program Type:** BATCH

**Business Purpose:** Reads customer master file (VSAM KSDS) and displays records for verification.

**Input Files/Data:**
- CUSTFILE (VSAM KSDS): Customer master records
  - Key: Customer ID (9 numeric)

**Output Files/Data:**
- Console output via DISPLAY

**Copybooks Used:**
- CVCUS01Y: Customer record definition

**Notes:**
- Simple verification utility

---

### 6. CBTRN01C - Transaction Posting Validator

**Program Name & ID:** CBTRN01C - Daily Transaction Posting Validator

**Program Type:** BATCH

**Business Purpose:** Reads daily transaction file and validates each transaction against master files without posting. Performs lookups to verify card/account/customer associations.

**Input Files/Data:**
- DALYTRAN-FILE (sequential): Daily transaction input
- CUSTOMER-FILE, XREF-FILE, CARD-FILE, ACCOUNT-FILE, TRANSACT-FILE (VSAM KSDS, random)

**Output Files/Data:**
- Console output via DISPLAY

**Key Business Rules:**
- Validates card existence in cross reference
- Validates account existence via cross reference lookup
- Performs lookups only; no modifications
- Reports success/failure to console

**Processing Logic:**
1. Open all files
2. Read transaction from daily file
3. Look up card in XREF to get account
4. Look up account in account master
5. Continue until EOF

**Copybooks Used:**
- CVTRA06Y: Daily transaction record
- CVCUS01Y, CVACT03Y, CVACT02Y, CVACT01Y, CVTRA05Y: Reference records

**File Operations:**
- DALYTRAN-FILE: Sequential read
- Others: Random reads with INVALID KEY handling

**Notes:**
- Validation-only program
- Read-only on all master files
- CardDemo version: v2.0-25-gdb72e6b-235

---

### 7. CBTRN02C - Transaction Posting & Validation

**Program Name & ID:** CBTRN02C - Daily Transaction Posting Engine

**Program Type:** BATCH

**Business Purpose:** Reads daily transaction input, validates transactions (card, account, credit limit, expiration), posts valid transactions to transaction master, updates account and category balance records, writes rejected transactions with failure reasons.

**Input Files/Data:**
- DALYTRAN-FILE (sequential): Daily transaction input
- TRANSACT-FILE (VSAM KSDS, random, output): Transaction master
- XREF-FILE, ACCOUNT-FILE, TCATBAL-FILE (VSAM KSDS, random, I-O): Reference/update files

**Output Files/Data:**
- TRANSACT-FILE: Posted transaction records
- ACCOUNT-FILE: Updated balance records
- TCATBAL-FILE: Updated category balance records
- DALYREJS-FILE (sequential): Rejected transactions with failure codes
- Console: Transaction and rejection counters

**Key Business Rules:**
- Validation:
  1. Card must exist in cross reference (error 100)
  2. Associated account must exist (error 101)
  3. Transaction amount ≤ available credit (error 102)
  4. Account must not be expired (error 103)
- Transaction amount applied to account:
  - Positive: added to current cycle credit
  - Negative: added to current cycle debit
- Transaction category balances created if not present, updated if existing
- Account balances updated with transaction amount
- Rejected transactions include validation failure reason code

**Processing Logic:**
1. Open all files
2. Read transaction from daily file
3. Validate:
   - Look up card in XREF
   - Read account record
   - Check credit available, expiration date
4. If valid:
   - Look up/create category balance record
   - REWRITE account with updated balance
   - WRITE transaction to transaction file
5. If invalid:
   - WRITE rejection record with failure code
6. Display transaction and rejection counts

**Copybooks Used:**
- CVTRA06Y, CVTRA05Y, CVACT03Y, CVACT01Y, CVTRA01Y: Record definitions

**Called Programs:**
- CEE3ABD: Abend handler

**File Operations:**
- DALYTRAN-FILE: Sequential read
- TRANSACT-FILE: Random WRITE
- XREF-FILE: Random READ with INVALID KEY
- ACCOUNT-FILE: Random READ/REWRITE (I-O mode)
- TCATBAL-FILE: Random READ/WRITE/REWRITE (I-O mode)
- DALYREJS-FILE: Sequential WRITE

**Key Fields:**
- WS-VALIDATION-FAIL-REASON: Error code (0=pass, 100-103=failure)
- WS-TRANSACTION-COUNT: Total transactions processed
- WS-REJECT-COUNT: Total rejections
- ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT: Account balances
- TRAN-CAT-BAL: Balance by category for interest calculation

**Edge Cases:**
- First transaction for a new category (WRITE vs REWRITE)
- Status code 23 on TCATBAL read (record not found)
- Expired accounts
- Over-limit transactions
- Missing account despite valid cross reference

**Notes:**
- Sophisticated validation and posting logic
- Maintains parallel balance tracking (account + category levels)
- Transaction category balances support interest calculations
- Return code 4 signals downstream processing of rejections
- CardDemo version: v2.0-25-gdb72e6b-235

---

### 8. CBTRN03C - Transaction Detail Report Generator

**Program Name & ID:** CBTRN03C - Transaction Detail Report

**Program Type:** BATCH

**Business Purpose:** Reads posted transaction file, filters by date range, enriches data with card/type/category descriptions, generates formatted report with page and account-level totals.

**Input Files/Data:**
- TRANSACT-FILE (sequential): Posted transaction records
- DATE-PARMS-FILE (sequential): Start and end dates for filtering
- XREF-FILE, TRANTYPE-FILE, TRANCATG-FILE (VSAM KSDS, random): Enrichment lookups

**Output Files/Data:**
- REPORT-FILE (sequential): Formatted transaction detail report with headers, detail lines, account subtotals, page subtotals, grand total

**Key Business Rules:**
- Report filtered by transaction timestamp (within START-DATE to END-DATE)
- Lookups performed for card-to-account, type code description, category description
- Account-level subtotals when account ID changes
- Page-level subtotals every 20 detail lines
- Report lines: 133 characters (standard printer width)

**Processing Logic:**
1. Open all files
2. Read date range parameters
3. Loop through transactions:
   - Filter by date range
   - On account change: print account subtotal, reset total
   - Check page full (20 lines): print page subtotal, reset
   - Print detail line with enriched data
   - Accumulate totals
4. At EOF: print final account subtotal, page total, grand total

**Copybooks Used:**
- CVTRA05Y, CVACT03Y, CVTRA03Y, CVTRA04Y, CVTRA07Y: Record definitions

**File Operations:**
- TRANSACT-FILE: Sequential read
- DATE-PARMS-FILE: Sequential read (single record)
- XREF-FILE, TRANTYPE-FILE, TRANCATG-FILE: Random lookups
- REPORT-FILE: Sequential WRITE

**Key Fields:**
- WS-START-DATE, WS-END-DATE: Report date range (YYYYMMDD format)
- WS-LINE-COUNTER: Lines printed for pagination
- WS-PAGE-TOTAL, WS-ACCOUNT-TOTAL, WS-GRAND-TOTAL: Running totals at 3 levels
- TRAN-PROC-TS: Transaction timestamp for date filtering

**Edge Cases:**
- No transactions in date range
- Transaction type/category not found
- Lookup failures cause program abend
- Last account total and grand total printed after EOF

**Notes:**
- Report generation with running totals at 3 levels
- Requires DATE-PARMS-FILE for date range
- Lookup failures trigger abend (no error handling)
- Uses FUNCTION MOD for page break detection
- CardDemo version: v2.0-25-gdb72e6b-235

---

## CICS INTERACTIVE PROGRAMS

### 9. COACTUPC - Account Update (CICS)

**Program Name & ID:** COACTUPC - Account Update

**Program Type:** CICS

**Business Purpose:** CICS transaction processing program that presents an account update screen, accepts input changes, validates all inputs, compares old vs new values, and updates ACCOUNT-FILE and CUSTOMER-FILE records with optimistic lock checking.

**Input/Output:**
- BMS Mapset: COACTUP (screen CACTUPA)
- Commarea: CARDDEMO-COMMAREA + WS-THIS-PROGCOMMAREA
- PF Keys: Enter (submit), F3 (exit), F5 (confirm), F12 (cancel)

**Key Business Rules:**
- Account ID immutable (search key only)
- Active Status: Y or N
- Credit/Cash Credit Limits: numeric
- Dates: valid CCYYMMDD format
- Customer names: alphabetic characters + spaces
- Phone numbers: (NNN) NNN-NNNN format
- SSN: XXX-XX-XXXX format with validation
- Monetary fields: NUMVAL-C validation
- Optimistic locking through old vs new comparison

**Processing Logic:**
1. HANDLE ABEND
2. Initialize and store transaction ID (CAUP), program name
3. Receive commarea from previous program
4. Remap PF keys (F3=exit, Enter=process, F5=confirm, F12=cancel)
5. Evaluate transaction flow:
   - PF03: Transfer to calling program via XCTL
   - Fresh entry: Display blank search screen
   - After validation: Display confirmation screen
   - After confirmation: Execute update
6. On input processing:
   - Receive map input
   - Edit all input fields
   - Compare old vs new values
   - If changes: validate and display confirmation
   - If no changes/errors: return to search screen

**Copybooks Used:**
- CVCRD01Y: Card record structure
- CSLKPCDY: Phone area code lookup
- COTTL01Y: Screen titles
- COACTUP: BMS map definition
- CSDAT01Y: Current date
- CSMSG01Y, CSMSG02Y: Messages and abend variables
- CSUSR01Y: User data
- CVACT01Y: Account record
- CVACT03Y: Card cross reference
- CVCUS01Y: Customer record
- COCOM01Y: Application commarea
- DFHBMSCA, DFHAID: IBM CICS structures

**Called Programs:**
- XCTL: To calling program, menus, card operations
- CICS services: HANDLE ABEND, SYNCPOINT, XCTL, SEND/RECEIVE MAP, READ/REWRITE dataset

**File Operations:**
- ACCTDAT (CICS READ, REWRITE): Account master
- CUSTDAT (CICS READ, REWRITE): Customer master
- Random access via RIDFLD by primary key

**Key Fields - Account:**
- ACCT-ID: 11-digit account number
- ACCT-ACTIVE-STATUS, ACCT-CURR-BAL, ACCT-CREDIT-LIMIT: Account attributes
- ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE: Dates
- ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT: Cycle totals

**Key Fields - Customer:**
- CUST-ID, CUST-FIRST-NAME, CUST-LAST-NAME: Identity
- CUST-ADDR-*, CUST-PHONE-*, CUST-SSN: Contact details
- CUST-DOB-YYYY-MM-DD, CUST-FICO-CREDIT-SCORE: Demographics

**Edge Cases:**
- Account search with zeros
- Account not found
- Duplicate update (data changed between fetch and update)
- Partial field updates
- Invalid date formats
- Invalid phone/SSN formats

**Notes:**
- Complex screen-based input validation
- Optimistic locking via data comparison
- Phone validation with area code lookup
- SSN validation with range rejection (0xx, 666, 900-999)
- Dual-file updates (ACCOUNT + CUSTOMER)
- Edit flag arrays for field-level errors
- CardDemo version: v2.0-25 (display) / v1.0-15 (earlier)

---

### 10. COACTVWC - Account View (CICS)

**Program Name & ID:** COACTVWC - Account View

**Program Type:** CICS

**Business Purpose:** CICS read-only display of account and customer data by account ID. Performs lookups and enrichment from three master files, presents formatted output without modification.

**Input/Output:**
- BMS Mapset: COACTVW (screen CACTVWA)
- Commarea: CARDDEMO-COMMAREA + WS-THIS-PROGCOMMAREA
- PF Keys: Enter (return to search), F3 (exit to menu)

**Key Business Rules:**
- Account ID: 11-digit non-zero number
- Read-only display (no update)
- Data from three master files:
  1. Card-Account cross reference (CXACAIX alternate index)
  2. Account master (ACCTDAT)
  3. Customer master (CUSTDAT)
- SSN formatted with dashes on display only

**Processing Logic:**
1. HANDLE ABEND
2. Check program entry mode:
   - Fresh: Display search screen
   - Reentry: Process input and read data
3. If fresh:
   - Display empty search screen
4. If reentry:
   - Receive map input
   - Edit account number
   - If invalid: return to search screen
   - If valid: 3-file lookup cascade
     a. GETCARDXREF-BYACCT: Read cross reference by account ID (alternate key)
     b. GETACCTDATA-BYACCT: Read account master
     c. GETCUSTDATA-BYCUST: Read customer master
   - Display enriched results

**Copybooks Used:**
- CVCRD01Y: Card record structure
- COTTL01Y: Screen titles
- COACTVW: BMS map definition
- CSDAT01Y: Current date
- CSMSG01Y, CSMSG02Y: Messages
- CSUSR01Y: User data
- CVACT01Y, CVACT02Y, CVACT03Y: Record layouts
- CVCUS01Y: Customer record
- COCOM01Y: Application commarea
- DFHBMSCA, DFHAID: IBM CICS structures
- CSSTRPFY: PF key handling

**Called Programs:**
- XCTL: To card operations or menu
- CICS services: HANDLE ABEND, SEND/RECEIVE MAP, READ dataset, XCTL, RETURN

**File Operations:**
- CXACAIX (alternate index): READ by account ID
- ACCTDAT: READ by account ID
- CUSTDAT: READ by customer ID
- Random access via RIDFLD and KEYLENGTH
- RESP and RESP2 for error handling

**Key Fields:**
- CC-ACCT-ID: 11-digit search key
- XREF-CUST-ID, XREF-CARD-NUM: Values from cross reference
- ACCT attributes: balance, limits, dates
- CUST attributes: name, address, phone, SSN, credit score

**Edge Cases:**
- Account not provided
- Non-numeric characters in account ID
- Account not found in cross reference
- Inconsistent data (cross reference found but account missing)
- Customer not found

**Error Handling:**
- Account validation errors with specific message
- File read errors with RESP/RESP2 codes
- NOTFND (23) as expected condition
- Other RESP codes trigger error message with details

**Notes:**
- Read-only complement to COACTUPC
- Multi-file lookup pattern (cross reference → account → customer)
- Alternate key access on cross reference
- Current date/time on all screens
- Customizable screen titles and messages
- No update logic
- CardDemo version: v1.0-15-g27d6c6f-68

---

### 11-27. Additional CICS Interactive Programs

**COADM01C** - Administration Menu
- CICS menu program routing to administrative functions
- Dynamic XCTL routing based on user selection
- User role verification (admin vs regular)

**COBIL00C** - Bill Payment Processing
- CICS program for account billing operations
- Reads account and transaction files
- Updates account balances on payment

**COMEN01C** - User Main Menu
- CICS menu for regular users
- Routes to account, card, transaction operations
- Dynamic program routing via XCTL

**CORPT00C** - Report Submission
- CICS program submitting batch reports to job queue
- WRITEQ TD to transient data queue (JOBS)
- Report request construction and submission

**COSGN00C** - Signon/Authentication
- CICS entry point program
- User authentication via USRSEC file
- Routes to admin menu (COADM01C) or user menu (COMEN01C)

**COCRDLIC** - Card List Inquiry
- CICS program for listing credit cards with pagination
- BROWSE operation on CARDXREF file
- Page scrolling support via PF keys

**COCRDSLC** - Card Detail Selection
- CICS program for viewing card details
- Reads card data via cross reference lookup
- Links to card update (COCRDUPC)

**COCRDUPC** - Card Update
- CICS program for updating credit card records
- CICS READ UPDATE / REWRITE pattern
- Field validation and dual-file updates

**COTRN00C** - Transaction List
- CICS transaction inquiry program
- BROWSE on TRANSACT file with pagination
- Date range and account filtering

**COTRN01C** - Transaction Detail View
- CICS program viewing single transaction
- Random READ of TRANSACT file
- Detailed transaction information display

**COTRN02C** - Transaction Add
- CICS program adding new transactions
- Validates transaction data
- CALL CSUTLDTC for date validation
- CICS WRITE to TRANSACT file

**COUSR00C** - User List
- CICS user management program
- BROWSE USRSEC with pagination
- Admin access only

**COUSR01C** - User Add
- CICS program creating new user records
- USRSEC file WRITE
- Password and profile assignment

**COUSR02C** - User Update
- CICS program updating user information
- CICS READ UPDATE / REWRITE
- Role and permission updates

**COUSR03C** - User Delete
- CICS program removing users
- CICS DELETE from USRSEC
- Audit trail logging

---

### 28. CSUTLDTC - Date Validation Utility

**Program Name & ID:** CSUTLDTC - Date Validation Utility

**Program Type:** CICS Utility

**Business Purpose:** Utility program for date validation using CICS CEEDAYS API. Called by COTRN02C and other transaction processing programs.

**Input/Output:**
- Passed via CALL statement from calling program
- Parameters: Date string (CCYYMMDD format), validation result

**Key Business Rules:**
- Validates date format and ranges
- Leap year handling
- Returns success/failure indicator

**Called Programs:**
- CEEDAYS: External CICS library routine for date conversion

**Notes:**
- Reusable date validation utility
- Centralizes date validation logic
- CALL-based interface for use by multiple programs

---

## Additional Batch Programs

### 29. COBSWAIT - Batch Wait Utility

**Program Name & ID:** COBSWAIT - Batch Wait Utility

**Program Type:** BATCH

**Business Purpose:** Batch utility providing wait/delay functionality for job synchronization.

**Key Business Rules:**
- Accepts wait parameter in centiseconds
- Calls external MVS WAIT routine

**Called Programs:**
- MVSWAIT: MVS wait routine

**Notes:**
- Minimal logic, primarily a wrapper around MVS service
- Used in JCL for job scheduling and synchronization

---

## Summary Statistics

**Total Programs:** 29
- **Batch Programs:** 9 (CBACT01C-CBACT04C, CBCUS01C, CBTRN01C-CBTRN03C, COBSWAIT)
- **CICS Interactive Programs:** 20 (COSGN00C, COADM01C, COMEN01C, COACTUPC, COACTVWC, COCRDLIC, COCRDSLC, COCRDUPC, COTRN00C, COTRN01C, COTRN02C, COBIL00C, CORPT00C, COUSR00C, COUSR01C, COUSR02C, COUSR03C, CSUTLDTC)

**File Type Distribution:**
- VSAM KSDS (Keyed Sequential Data Sets): Primary master storage
- Sequential Files: Batch input/output and GDG versioning
- Alternate Indexes: Cross-reference and timestamp-based lookups

**Key Integration Points:**
- Account Master (ACCTFILE/ACCTDAT): All programs access for validation/display
- Card Master (CARDFILE): Batch and interactive programs
- Customer Master (CUSTFILE/CUSTDAT): Account and billing operations
- Cross Reference (XREFFILE): Card-to-account lookup for all file operations
- Transaction Files (TRANFILE, DALYTRAN): Daily posting and history
- Category Balance Files (TCATBAL): Interest calculation tracking
- Disclosure Groups (DISCGRP): Rate assignments by account segment
- User Security (USRSEC): CICS authentication

**Daily Batch Workflow:**
1. CLOSEFIL - Close CICS files for batch processing
2. POSTTRAN - Post daily transactions (CBTRN02C)
3. INTCALC - Calculate interest (CBACT04C)
4. COMBTRAN - Combine transaction files
5. CREASTMT - Create account statements
6. TRANREPT - Generate transaction report (CBTRN03C)
7. PRTCATBL - Print category balances
8. TXT2PDF1 - Convert statements to PDF
9. OPENFIL - Reopen CICS files

**Technology Stack:**
- Language: COBOL (GnuCOBOL compatible)
- File System: VSAM KSDS with alternate indexes
- Online Component: CICS/TG (Transaction Server)
- BMS: Screen definition and mapping
- Batch Orchestration: JCL with IDCAMS, SORT, IEFBR14
- Date/Time: COBOL runtime libraries + external utilities

---

**Documentation Generated:** 2025-05-25  
**CardDemo Version References:**
- Batch Programs: v2.0-25-gdb72e6b-235
- CICS Interactive: v1.0-15-g27d6c6f-68 to v2.0-25

**Application Domain:** Credit card account and transaction management system with daily batch processing (interest calculation, statement generation, reporting) and interactive CICS-based inquiry and update capabilities.
