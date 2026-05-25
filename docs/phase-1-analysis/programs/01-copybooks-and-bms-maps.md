# CardDemo Copybooks and BMS Maps

## COPYBOOKS (29 files)

### Utility and Infrastructure Copybooks

**1. COCOM01Y - CardDemo Communication Area**
- Purpose: Inter-program communication structure for passing data between CICS transactions
- Size: ~130 bytes
- Key Fields:
  - CDEMO-GENERAL-INFO: FROM-TRANID (4), FROM-PROGRAM (8), TO-TRANID (4), TO-PROGRAM (8), USER-ID (8), USER-TYPE (1), PGM-CONTEXT (1)
  - CDEMO-CUSTOMER-INFO: CUST-ID (9), CUST-FNAME (25), CUST-MNAME (25), CUST-LNAME (25)
  - CDEMO-ACCOUNT-INFO: ACCT-ID (11), ACCT-STATUS (1)
  - CDEMO-CARD-INFO: CARD-NUM (16)
  - CDEMO-MORE-INFO: LAST-MAP (7), LAST-MAPSET (7)
- Usage: Inter-transaction communication in all CICS programs

**2. CODATECN - Date Conversion Utility**
- Purpose: Date format conversion between YYYYMMDD and YYYY-MM-DD
- Size: ~80 bytes
- Key Fields:
  - CODATECN-IN-REC: TYPE (1), INP-DATE (20) with REDEFINEs
  - CODATECN-OUT-REC: OUTTYPE (1), OUT-DATE (20) with REDEFINEs
  - CODATECN-ERROR-MSG (38)
- Usage: Date conversions throughout system

**3. CSMSG01Y - Common Messages**
- Purpose: Standardized message text for application
- Size: ~100 bytes
- Key Fields: CCDA-MSG-THANK-YOU, CCDA-MSG-INVALID-KEY, other common messages
- Usage: Screen display programs

**4. CSMSG02Y - Abend Data Structure**
- Purpose: Exception handling and abend recovery
- Size: ~135 bytes
- Key Fields: ABEND-CODE (4), ABEND-CULPRIT (8), ABEND-REASON (50), ABEND-MSG (72)
- Usage: Error handling routines

**5. COTTL01Y - Screen Title Definitions**
- Purpose: Standardized title text for all screens
- Size: ~120 bytes
- Key Fields: CCDA-TITLE01 (40), CCDA-TITLE02 (40), CCDA-THANK-YOU (40)
- Usage: All CICS screen programs

**6. CSDAT01Y - Date/Time Working Storage**
- Purpose: Work area for current date/time in multiple formats
- Size: ~200 bytes
- Key Fields:
  - WS-CURDATE-DATA: YEAR (4), MONTH (2), DAY (2)
  - WS-CURTIME: HOURS (2), MINUTE (2), SECOND (2), MILSEC (2)
  - WS-CURDATE-MM-DD-YY, WS-CURTIME-HH-MM-SS, WS-TIMESTAMP
- Usage: Date/time display in all screens

**7. CSLKPCDY - Lookup Code Repository**
- Purpose: Validation lists for phone area codes, state codes, zip codes
- Size: ~1500 bytes (large reference table)
- Key Fields:
  - WS-US-PHONE-AREA-CODE-TO-EDIT with 500+ valid area codes
  - US-STATE-CODE-TO-EDIT with all 50 states + territories
  - US-STATE-ZIPCODE-TO-EDIT validation (500+ combinations)
- Usage: User input validation

**8. CSSETATY - Field Setting Template**
- Purpose: CICS BMS field attribute setting macro
- Size: ~30 bytes code
- Usage: Dynamic screen field manipulation

**9. CSSTRPFY - PF Key Mapping Procedure**
- Purpose: Maps CICS EIBAID values to PF key indicator flags
- Size: ~2KB code
- Key Fields: EVALUATE/WHEN statements for DFHENTER, DFHCLEAR, DFHPA1-PA2, DFHPF1-PF24
- Usage: Key event handlers in all CICS programs

**10. CSUSR01Y - Security User Data Structure**
- Purpose: User authentication and access control
- Size: 80 bytes
- Key Fields:
  - SEC-USR-ID (8)
  - SEC-USR-FNAME (20), SEC-USR-LNAME (20)
  - SEC-USR-PWD (8)
  - SEC-USR-TYPE (1): A=Admin, U=User
- Usage: COSGN00C (security/login)

**11. CSUTLDPY - Date Edit Working Storage**
- Purpose: Work area for date validation with error reporting
- Size: ~300 bytes
- Key Fields:
  - WS-EDIT-DATE-CCYYMMDD: Century, Year, Month, Day
  - WS-EDIT-FLGS: FLG-YEAR-ISVALID, FLG-MONTH-ISVALID, FLG-DAY-ISVALID
  - WS-DATE-VALIDATION-RESULT: SEVERITY, MSG-NO
- Usage: Date validation utilities

**12. CSUTLDWY - Date Edit Procedure Copybook**
- Purpose: Reusable date validation paragraphs
- Size: ~400 bytes code
- Routines: EDIT-DATE-CCYYMMDD, EDIT-YEAR-CCYY, EDIT-MONTH, EDIT-DAY, EDIT-DATE-OF-BIRTH
- Features: Leap year checks, DOB reasonableness (future date validation)
- Usage: All data entry programs requiring date validation

---

### Account Management Data Structures

**13. CUSTREC / CVCUS01Y - Customer Record**
- LRECL: 500 bytes
- Key Fields:
  - CUST-ID (9): Customer identifier
  - CUST-FIRST-NAME (25), CUST-MIDDLE-NAME (25), CUST-LAST-NAME (25)
  - CUST-ADDR-LINE-1 (50), CUST-ADDR-LINE-2 (50), CUST-ADDR-LINE-3 (50)
  - CUST-ADDR-STATE-CD (2), CUST-ADDR-COUNTRY-CD (3), CUST-ADDR-ZIP (10)
  - CUST-PHONE-NUM-1 (15), CUST-PHONE-NUM-2 (15): Phone in (NNN) NNN-NNNN format
  - CUST-SSN (9): 9-digit stored as numeric
  - CUST-GOVT-ISSUED-ID (20)
  - CUST-DOB-YYYYMMDD (10): Date of birth in CCYYMMDD format
  - CUST-EFT-ACCOUNT-ID (10)
  - CUST-PRI-CARD-HOLDER-IND (1): Y/N
  - CUST-FICO-CREDIT-SCORE (3): 300-850 range
- Usage: Account View (COACTVWC), Account Update (COACTUPC), Customer management

**14. CVACT01Y - Account Record**
- LRECL: 300 bytes
- Key Fields:
  - ACCT-ID (11): Primary key
  - ACCT-ACTIVE-STATUS (1): Y/N
  - ACCT-CURR-BAL (10V99): Current balance
  - ACCT-CREDIT-LIMIT (10V99): Maximum credit
  - ACCT-CASH-CREDIT-LIMIT (10V99): Cash advance limit
  - ACCT-OPEN-DATE (10), ACCT-EXPIRAION-DATE (10), ACCT-REISSUE-DATE (10)
  - ACCT-CURR-CYC-CREDIT (10V99), ACCT-CURR-CYC-DEBIT (10V99)
  - ACCT-ADDR-ZIP (10)
  - ACCT-GROUP-ID (10): For rate assignments
- Usage: Account operations (view, update, reports)

**15. CVACT02Y - Card Record**
- LRECL: 150 bytes
- Key Fields:
  - CARD-NUM (16): Primary key
  - CARD-ACCT-ID (11): Associated account
  - CARD-CVV-CD (3): Card verification value
  - CARD-EMBOSSED-NAME (50): Name on card
  - CARD-EXPIRAION-DATE (10)
  - CARD-ACTIVE-STATUS (1)
- Usage: Card View (COCRDSLC), Card Update (COCRDUPC), Card List (COCRDLIC)

**16. CVACT03Y - Card Cross-Reference Record**
- LRECL: 50 bytes
- Key Fields:
  - XREF-CARD-NUM (16): Card number (primary key)
  - XREF-CUST-ID (9): Associated customer
  - XREF-ACCT-ID (11): Associated account
- Purpose: Links cards to accounts and customers
- Usage: Card lookups, account associations

**17. CVCRD01Y - Card Common Work Area**
- LRECL: ~250 bytes
- Key Fields:
  - CCARD-AID (5) with 88-levels for all action keys
  - CCARD-NEXT-PROG (8), CCARD-NEXT-MAPSET (7), CCARD-NEXT-MAP (7)
  - CCARD-ERROR-MSG (75), CCARD-RETURN-MSG (75)
  - CC-ACCT-ID (11/PIC 9(11) REDEFINES)
  - CC-CARD-NUM (16/PIC 9(16) REDEFINES)
  - CC-CUST-ID (9/PIC 9(9) REDEFINES)
- Usage: All card transaction programs

---

### Transaction Management Data Structures

**18. CVTRA01Y - Transaction Category Balance Record**
- LRECL: 50 bytes
- Key Fields:
  - TRAN-CAT-KEY: TRANCAT-ACCT-ID (11), TRANCAT-TYPE-CD (2), TRANCAT-CD (4)
  - TRAN-CAT-BAL (9V99): Category-level spending by type
- Purpose: Supports interest calculations by category
- Usage: Transaction reporting, balance aggregation

**19. CVTRA02Y - Disclosure Group Record**
- LRECL: 50 bytes
- Key Fields:
  - DIS-GROUP-KEY: DIS-ACCT-GROUP-ID (10), DIS-TRAN-TYPE-CD (2), DIS-TRAN-CAT-CD (4)
  - DIS-INT-RATE (4V99): Annual interest rate
- Purpose: Interest rates and fees by category and account group
- Usage: Interest/fee calculation (CBACT04C)

**20. CVTRA03Y - Transaction Type Record**
- LRECL: 60 bytes
- Key Fields:
  - TRAN-TYPE (2): Type code (e.g., PU=Purchase, CA=Cash Advance)
  - TRAN-TYPE-DESC (50)
- Purpose: Master table of transaction type codes
- Usage: Transaction type reference lookup

**21. CVTRA04Y - Transaction Category Type Record**
- LRECL: 60 bytes
- Key Fields:
  - TRAN-CAT-KEY: TRAN-TYPE-CD (2), TRAN-CAT-CD (4)
  - TRAN-CAT-TYPE-DESC (50)
- Purpose: Transaction category descriptions
- Usage: Transaction add/edit, category validation

**22. CVTRA05Y - Transaction Record**
- LRECL: 350 bytes
- Key Fields:
  - TRAN-ID (16): Unique transaction identifier
  - TRAN-TYPE-CD (2), TRAN-CAT-CD (4): Type and category
  - TRAN-SOURCE (10), TRAN-DESC (100)
  - TRAN-AMT (9V99): Transaction amount
  - TRAN-MERCHANT-ID (9), TRAN-MERCHANT-NAME (50), TRAN-MERCHANT-CITY (50), TRAN-MERCHANT-ZIP (10)
  - TRAN-CARD-NUM (16): Card used
  - TRAN-ORIG-TS (26), TRAN-PROC-TS (26): Timestamps
- Purpose: Individual transaction/movement record
- Usage: COTRN01C (View), COTRN02C (Add), COTRN00C (List)

**23. CVTRA06Y - Daily Transaction Record**
- LRECL: 350 bytes
- Key Fields: Identical to CVTRA05Y (with DALYTRAN-* prefix variants)
- Purpose: Batch/daily transaction processing
- Usage: Batch reporting programs

**24. CVTRA07Y - Transaction Report Structure**
- LRECL: ~500 bytes
- Key Fields:
  - REPORT-NAME-HEADER: REPT-SHORT-NAME (38), REPT-LONG-NAME (41), dates
  - TRANSACTION-DETAIL-REPORT: Transaction ID, Account, Type, Category, Source, Amount
  - TRANSACTION-HEADER-1, TRANSACTION-HEADER-2: Column headers
  - REPORT-PAGE-TOTALS, REPORT-ACCOUNT-TOTALS, REPORT-GRAND-TOTALS: Summary lines
- Purpose: Report formatting structures
- Usage: CORPT00C (Report generation)

---

### Menu Configuration Copybooks

**25. COADM02Y - Admin Menu Options**
- LRECL: ~250 bytes
- Key Fields:
  - CDEMO-ADMIN-OPT-COUNT: 6 options
  - Option details: number, description, program name
  - Options: User List (COUSR00C), User Add (COUSR01C), User Update (COUSR02C), User Delete (COUSR03C), Transaction Type List, Transaction Type Maintenance
- Usage: COADM01C (Admin menu driver)

**26. COMEN02Y - Main Menu Options**
- LRECL: ~400 bytes
- Key Fields:
  - CDEMO-MENU-OPT-COUNT: 11 options
  - Option details: number, description (35), program (8), user type (1)
  - Options: Account View, Account Update, Card List, Card View, Card Update, Transaction List/View/Add, Reports, Bill Payment, Pending Auth View
- Usage: COMEN01C (Main menu driver)

---

### Legacy/Reporting Copybooks

**27. COSTM01 - Transaction Record (reporting layout)**
- LRECL: ~350 bytes
- Key Fields: Similar to CVTRA05Y but restructured for reporting
  - TRNX-KEY: TRNX-CARD-NUM (16), TRNX-ID (16)
  - TRNX-REST: Type, category, source, description, amount, merchant details, timestamps
- Usage: Reporting/extract programs

**28. UNUSED1Y - Deprecated Data Structure**
- LRECL: 80 bytes
- Purpose: Placeholder/legacy structure (appears unused)
- Fields: UNUSED-ID, UNUSED-FNAME, UNUSED-LNAME, UNUSED-PWD, UNUSED-TYPE, UNUSED-FILLER
- Usage: None (vestigial)

**29. (Various utility copybooks for procedural code)**

---

## BMS MAPS (17 files)

All BMS maps use standard 24x80 CICS terminal format and follow AWS CardDemo styling with titles, dates, times, and consistent error message display.

### Authentication

**COSGN00 - Login Screen**
- Purpose: User authentication entry point
- Key Fields:
  - USERID (8): Input field (GREEN, IC, UNPROT, MUSTFILL)
  - PASSWD (8): Password field (DRK - dark display, UNPROT, MUSTFILL)
  - Display: TRNNAME, TITLE01/02, CURDATE, PGMNAME, CURTIME, APPLID, SYSID
  - Messages: ERRMSG (78)
- Transaction: COSGN00C
- Notes: Creative dollar bill decoration, IC (Insert Cursor) on USERID

---

### Menu Screens

**COMEN01 - Main Menu**
- Purpose: Primary user menu with 11 selectable options
- Key Fields:
  - Menu items: OPTN001-OPTN012 (40 chars, FSET, NORM, BLUE, display-only)
  - OPTION (2): Numeric selector (FSET, IC, NUM, UNPROT, RIGHT-justified)
  - Options: Account View, Account Update, Card List, Card View, Card Update, Transactions (List/View/Add), Reports, Bill Payment, Pending Auth View
  - ERRMSG (78)
- Transaction: COMEN01C
- Navigation: PF keys for backward/forward/exit

**COADM01 - Admin Menu**
- Purpose: Administrator-only menu with 6 options
- Key Fields:
  - Menu items: OPTN001-OPTN012 (display-only)
  - OPTION (2): Numeric selector (IC, NUM, UNPROT)
  - Options: User List, User Add, User Update, User Delete, Transaction Type operations
  - ERRMSG (78)
- Transaction: COADM01C
- Restriction: Admin user type only

---

### Account Management Screens

**COACTVW - Account View (Read-Only)**
- Purpose: View account and associated customer details
- Key Fields:
  - Input: ACCTSID (11, IC, FSET, NORM, GREEN, PICIN='99999999999', MUSTFILL)
  - Display (ASKIP): Account status, dates, balances
  - Account section: ACSTTUS, ADTOPEN, ACRDLIM, AEXPDT, ACSHLIM, AREISDT, ACURBAL, ACRCYCR, AADDGRP, ACRCYDB
  - Customer section: ACSTNUM, ACSTSSN, ACSTDOB, ACSTFCO, ACSFNAM/ACSMNAM/ACSLNAM
  - Address section: ACSADL1/ACSADL2, ACSSTTE, ACSZIPC, ACSCITY, ACSCTRY
  - Phone section: ACSPHN1/ACSPHN2, ACSGOVT, ACSEFTC, ACSPFLG
  - Messages: INFOMSG (45), ERRMSG (78)
- Transaction: COACTVWC
- PF Keys: ENTER=Fetch, F3=Back, F4=Clear

**COACTUP - Account Update**
- Purpose: Update account details and customer information
- Key Fields:
  - Input (UNPROT): Account ID, status, dates (individual MM/DD/YYYY fields), limits, balances
  - Input (UNPROT): Customer name (individual fields), address, phone (area code/exchange/number), SSN (individual), DOB, government ID, credit score
  - Input (UNPROT): Account group, EFT account, primary cardholder flag
  - Messages: INFOMSG (45), ERRMSG (78)
  - Function keys: ENTER=Process, F3=Exit, F5=Save, F12=Cancel
- Transaction: COACTUPC
- Validation: Date format validation via CSUTLDPY/CSUTLDWY

---

### Credit Card Management Screens

**COCRDLI - Card Listing (Paginated)**
- Purpose: List up to 7 cards per account with selection
- Key Fields:
  - Input: ACCTSID (11, IC, GREEN), CARDSID (16, NORM)
  - Paginated rows (7 max): For each row: SEL (1, FSET, UNPROT), ACCTNO (11), CRDNUM (16), CRDSTS (1)
  - PAGENO (3): Current page display
  - Messages: INFOMSG (45), ERRMSG (78)
  - PF Keys: F3=Exit, F7=Backward, F8=Forward
- Transaction: COCRDLIC

**COCRDSL - Card Selection/View**
- Purpose: View individual card details and navigate to operations
- Key Fields:
  - Input: ACCTSID (11, IC, UNPROT), CARDSID (16, UNPROT)
  - Display: CRDNAME (50), CRDSTCD (1), EXPMON/EXPYEAR (2/4)
  - Messages: INFOMSG (40), ERRMSG (80)
  - PF Keys: ENTER=Search, F3=Exit
- Transaction: COCRDSLC

**COCRDUP - Card Update**
- Purpose: Update card details (name, status, expiry)
- Key Fields:
  - Input (PROT): ACCTSID (11, IC), CARDSID (16, NORM)
  - Editable (UNPROT): CRDNAME (50), CRDSTCD (1), EXPMON (2), EXPYEAR (4)
  - Display (PROT): EXPDAY (2, calculated from EXPYEAR/EXPMON)
  - Messages: INFOMSG (40), ERRMSG (80)
  - PF Keys: ENTER=Process, F3=Exit, F5=Save, F12=Cancel
- Transaction: COCRDUPC

---

### Transaction Management Screens

**COTRN00 - Transaction List (Paginated)**
- Purpose: Browse transaction history with search and selection
- Key Fields:
  - Input: TRNIDIN (16, FSET, NORM, UNPROT)
  - Paginated rows (10 max): SEL (1, FSET, UNPROT), TRNID (16), TDATE (8), TDESC (26), TAMT (12)
  - PAGENUM (8): Page number
  - Column headers: Sel, Transaction ID, Date, Description, Amount
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Continue, F3=Back, F7=Backward, F8=Forward
- Transaction: COTRN00C

**COTRN01 - Transaction View**
- Purpose: Detailed view of single transaction
- Key Fields:
  - Input: TRNIDIN (16, IC, UNPROT)
  - Display: TRNID, CARDNUM, TTYPCD, TCATCD, TRNSRC, TDESC, TRNAMT, TORIGDT, TPROCDT, MID, MNAME, MCITY, MZIP
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Fetch, F3=Back, F4=Clear, F5=Browse Tran
- Transaction: COTRN01C

**COTRN02 - Transaction Add**
- Purpose: Add new transaction (admin/test)
- Key Fields:
  - Input: ACTIDIN (11, IC, UNPROT), CARDNIN (16, UNPROT)
  - Data entry: TTYPCD, TCATCD, TRNSRC, TDESC, TRNAMT, TORIGDT, TPROCDT, MID, MNAME, MCITY, MZIP
  - Confirmation: CONFIRM (1)
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Continue, F3=Back, F4=Clear, F5=Copy Last Tran
- Transaction: COTRN02C
- Validation: Date validation via CSUTLDTC CICS call

**CORPT00 - Transaction Report**
- Purpose: Generate transaction reports with date range
- Key Fields:
  - Selection: MONTHLY, YEARLY, CUSTOM (radio button style)
  - Date range (custom): SDTMM/SDTDD/SDTYYYY (month/day/year inputs), EDTMM/EDTDD/EDTYYYY
  - Confirmation: CONFIRM (1) with prompt
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Continue, F3=Back
- Transaction: CORPT00C

**COBIL00 - Bill Payment**
- Purpose: Bill payment input and confirmation
- Key Fields:
  - Input: ACTIDIN (11, IC, UNPROT)
  - Display: CURBAL (14): Current balance (formatted)
  - Confirmation: CONFIRM (1) with Y/N prompt
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Continue, F3=Back, F4=Clear
- Transaction: COBIL00C

---

### User Management Screens (Admin Only)

**COUSR00 - User List (Paginated)**
- Purpose: Admin function to list and select users
- Key Fields:
  - Input: USRIDIN (8, FSET, NORM, UNPROT)
  - Paginated rows (10 max): SEL (1, FSET, UNPROT), USRID (8), FNAME (20), LNAME (20), UTYPE (1)
  - PAGENUM (8): Page number
  - Column headers: Sel, User ID, First Name, Last Name, Type
  - Instruction: "Type 'U' to Update or 'D' to Delete"
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Continue, F3=Back, F7=Backward, F8=Forward
- Transaction: COUSR00C
- Restriction: Admin only

**COUSR01 - Add User**
- Purpose: Create new user account
- Key Fields:
  - FNAME (20, IC, UNPROT): First name
  - LNAME (20, UNPROT): Last name
  - USERID (8, UNPROT): User ID (8 char)
  - PASSWD (8, DRK, UNPROT): Password (dark display)
  - USRTYPE (1, UNPROT): User type (A=Admin, U=User)
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Add User, F3=Back, F4=Clear, F12=Exit
- Transaction: COUSR01C
- Restriction: Admin only

**COUSR02 - Update User**
- Purpose: Modify existing user account
- Key Fields:
  - Input (before fetch): USRIDIN (8, IC, UNPROT): User ID search
  - Editable (after fetch, UNPROT): FNAME (20), LNAME (20), PASSWD (8, DRK), USRTYPE (1)
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Fetch, F3=Save&&Exit, F4=Clear, F5=Save, F12=Cancel
- Transaction: COUSR02C
- Restriction: Admin only

**COUSR03 - Delete User**
- Purpose: Delete user account with confirmation
- Key Fields:
  - Input: USRIDIN (8, IC, UNPROT): User ID to delete
  - Display (read-only after fetch): FNAME (20), LNAME (20), USRTYPE (1)
  - Messages: ERRMSG (78)
  - PF Keys: ENTER=Fetch, F3=Back, F4=Clear, F5=Delete
- Transaction: COUSR03C
- Restriction: Admin only

---

## Summary Statistics

**Copybooks:**
- Total: 29 files
- Utility/Infrastructure: 12 files (communication, dates, messages, titles, lookups, procedures)
- Data Structures: 17 files (customer, account, card, transaction, menu config)
- Size Range: 30 bytes (code) to 500 bytes (full records)
- Total Estimated Content: ~15,000 bytes

**BMS Maps:**
- Total: 17 files
- Standard Size: 24x80 (CICS terminal format)
- Screen Types:
  - Authentication: 1 (COSGN00)
  - Menu: 2 (COMEN01, COADM01)
  - Account: 2 (COACTVW, COACTUP)
  - Card: 3 (COCRDLI, COCRDSL, COCRDUP)
  - Transaction: 5 (COTRN00, COTRN01, COTRN02, CORPT00, COBIL00)
  - User: 4 (COUSR00, COUSR01, COUSR02, COUSR03)

**Key Integration Patterns:**
- COCOM01Y: Imported by all transaction programs for inter-program communication
- CSDAT01Y: Imported by all screen programs for date/time display
- CUSTREC/CVCUS01Y: Link to account management and card records
- CVTRA05Y/CVTRA06Y: Transaction records displayed via COTRN maps
- CSUSR01Y: Data validated via COUSR maps
- Date validation: CSUTLDWY/CSUTLDPY referenced by account update and transaction add screens
- Lookup tables: CSLKPCDY used for phone area code and state code validation
- PF key mapping: CSSTRPFY used by all interactive programs

**Field Attribute Patterns:**
- FSET: Field set flag (cursor positioning)
- IC: Insert Cursor (auto-position on display)
- UNPROT: Unprotected input field
- PROT: Protected display field (read-only)
- ASKIP: Auto-skip protected field
- NORM: Normal intensity
- BLUE: Blue color (information/reference data)
- GREEN: Green color (input prompts)
- DRK: Dark display (password masking)
- MUST FILL: Mandatory field
- NUM: Numeric validation
- RIGHT: Right-justified

---

**Documentation Generated:** 2025-05-25  
**All files follow Apache 2.0 licensing typical of AWS modernization projects**
