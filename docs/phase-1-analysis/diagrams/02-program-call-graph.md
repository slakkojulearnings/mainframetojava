# Program Call Graph and Dependencies

## Batch Program Call Chain

```mermaid
graph TD
    DALYTRAN["Daily Transaction File<br/>DALYTRAN.PS"]
    CBTRN02C["CBTRN02C<br/>Transaction Poster"]
    TCATBALF["TCATBALF<br/>Category Balances"]
    CBACT04C["CBACT04C<br/>Interest Calculator"]
    SYSTRAN["SYSTRAN<br/>System Transactions"]
    CBSTM03A["CBSTM03A<br/>Statement Creator"]
    STMTFILE["STATEMNT<br/>Statements"]
    CBTRN03C["CBTRN03C<br/>Transaction Reporter"]
    TRANREPT["TRANREPT<br/>Report Output"]
    
    DALYTRAN --> CBTRN02C
    CBTRN02C --> TCATBALF
    TCATBALF --> CBACT04C
    CBACT04C --> SYSTRAN
    SYSTRAN --> CBSTM03A
    CBSTM03A --> STMTFILE
    CBSTM03A --> CBTRN03C
    CBTRN03C --> TRANREPT
    
    style CBTRN02C fill:#cce5ff
    style CBACT04C fill:#cce5ff
    style CBSTM03A fill:#cce5ff
    style CBTRN03C fill:#cce5ff
```

## Batch Utility Programs

```mermaid
graph LR
    CBACT01C["CBACT01C<br/>Account Reader<br/>CALL COBDATFT"]
    CBACT02C["CBACT02C<br/>Card Reader"]
    CBACT03C["CBACT03C<br/>XRef Reader"]
    CBCUS01C["CBCUS01C<br/>Customer Reader"]
    COBSWAIT["COBSWAIT<br/>Wait Utility<br/>CALL MVSWAIT"]
    
    style CBACT01C fill:#e8f4f8
    style CBACT02C fill:#e8f4f8
    style CBACT03C fill:#e8f4f8
    style CBCUS01C fill:#e8f4f8
    style COBSWAIT fill:#e8f4f8
```

## CICS Entry Points and Navigation

```mermaid
graph TD
    COSGN00C["COSGN00C<br/>SIGNON<br/>Authenticates User"]
    
    COSGN00C -->|Admin User| COADM01C["COADM01C<br/>ADMIN MENU"]
    COSGN00C -->|Regular User| COMEN01C["COMEN01C<br/>USER MENU"]
    
    COADM01C -->|XCTL| COUSR00C["COUSR00C<br/>User List"]
    COADM01C -->|XCTL| COUSR01C["COUSR01C<br/>User Add"]
    COADM01C -->|XCTL| COUSR02C["COUSR02C<br/>User Update"]
    COADM01C -->|XCTL| COUSR03C["COUSR03C<br/>User Delete"]
    COADM01C -->|XCTL| CORPT00C["CORPT00C<br/>Report Submit"]
    
    COMEN01C -->|XCTL| COACTUPC["COACTUPC<br/>Account Update"]
    COMEN01C -->|XCTL| COACTVWC["COACTVWC<br/>Account View"]
    COMEN01C -->|XCTL| COCRDLIC["COCRDLIC<br/>Card List"]
    COMEN01C -->|XCTL| COCRDSLC["COCRDSLC<br/>Card Select"]
    COMEN01C -->|XCTL| COCRDUPC["COCRDUPC<br/>Card Update"]
    COMEN01C -->|XCTL| COTRN00C["COTRN00C<br/>Tran List"]
    COMEN01C -->|XCTL| COTRN01C["COTRN01C<br/>Tran View"]
    COMEN01C -->|XCTL| COTRN02C["COTRN02C<br/>Tran Add<br/>CALL CSUTLDTC"]
    COMEN01C -->|XCTL| COBIL00C["COBIL00C<br/>Bill Payment"]
    
    COUSR00C --> USRSEC["USRSEC<br/>User Security File"]
    COUSR01C --> USRSEC
    COUSR02C --> USRSEC
    COUSR03C --> USRSEC
    
    COACTUPC --> ACCTDAT["ACCTDAT<br/>Account Data"]
    COACTVWC --> ACCTDAT
    
    COCRDLIC --> CARDXREF["CARDXREF<br/>Card XRef"]
    COCRDSLC --> CARDXREF
    COCRDUPC --> CARDXREF
    
    COTRN00C --> TRANSACT["TRANSACT<br/>Transactions"]
    COTRN01C --> TRANSACT
    COTRN02C --> TRANSACT
    
    COBIL00C --> ACCTDAT
    CORPT00C --> JOBS["JOBS<br/>Transient Data Queue"]
    
    style COSGN00C fill:#ffcccc
    style COADM01C fill:#fff4cc
    style COMEN01C fill:#fff4cc
    style COUSR00C fill:#cce5ff
    style COUSR01C fill:#cce5ff
    style COUSR02C fill:#cce5ff
    style COUSR03C fill:#cce5ff
    style COACTUPC fill:#cce5ff
    style COACTVWC fill:#cce5ff
    style COCRDLIC fill:#cce5ff
    style COCRDSLC fill:#cce5ff
    style COCRDUPC fill:#cce5ff
    style COTRN00C fill:#cce5ff
    style COTRN01C fill:#cce5ff
    style COTRN02C fill:#cce5ff
    style COBIL00C fill:#cce5ff
    style CORPT00C fill:#cce5ff
    style USRSEC fill:#f0f0f0
    style ACCTDAT fill:#f0f0f0
    style CARDXREF fill:#f0f0f0
    style TRANSACT fill:#f0f0f0
    style JOBS fill:#f0f0f0
```

## Account Operations Subgraph

```mermaid
graph LR
    ACCTMENU["Account Menu"]
    ACCTMENU --> COACTVWC["View Account<br/>COACTVWC<br/>READ ACCTDAT, CARDXREF"]
    ACCTMENU --> COACTUPC["Update Account<br/>COACTUPC<br/>READ UPDATE ACCTDAT"]
    
    COACTVWC --> ACCTDAT["ACCTDAT<br/>Account Master<br/>VSAM KSDS"]
    COACTUPC --> ACCTDAT
    
    COACTVWC --> CARDXREF["CARDXREF<br/>Card Cross-Ref<br/>VSAM KSDS + AIX"]
    COACTUPC --> CARDXREF
    
    style COACTVWC fill:#cce5ff
    style COACTUPC fill:#cce5ff
    style ACCTDAT fill:#f0f0f0
    style CARDXREF fill:#f0f0f0
```

## Credit Card Operations Subgraph

```mermaid
graph LR
    CARDMENU["Card Menu"]
    CARDMENU --> COCRDLIC["List Cards<br/>COCRDLIC<br/>BROWSE CARDXREF"]
    CARDMENU --> COCRDSLC["Select Card<br/>COCRDSLC<br/>READ CARDXREF"]
    CARDMENU --> COCRDUPC["Update Card<br/>COCRDUPC<br/>READ UPDATE CARDXREF"]
    
    COCRDLIC --> CARDXREF["CARDXREF<br/>Card XRef VSAM"]
    COCRDSLC --> CARDXREF
    COCRDUPC --> CARDXREF
    
    COCRDSLC --> CUSTDAT["CUSTDAT<br/>Customer Data"]
    COCRDUPC --> CUSTDAT
    
    style COCRDLIC fill:#cce5ff
    style COCRDSLC fill:#cce5ff
    style COCRDUPC fill:#cce5ff
    style CARDXREF fill:#f0f0f0
    style CUSTDAT fill:#f0f0f0
```

## Transaction Operations Subgraph

```mermaid
graph LR
    TRANMENU["Transaction Menu"]
    TRANMENU --> COTRN00C["List Trans<br/>COTRN00C<br/>BROWSE TRANSACT"]
    TRANMENU --> COTRN01C["View Trans<br/>COTRN01C<br/>READ TRANSACT"]
    TRANMENU --> COTRN02C["Add Trans<br/>COTRN02C<br/>WRITE TRANSACT<br/>CALL CSUTLDTC"]
    
    COTRN00C --> TRANSACT["TRANSACT<br/>Transaction Master<br/>VSAM KSDS + AIX"]
    COTRN01C --> TRANSACT
    COTRN02C --> TRANSACT
    
    COTRN02C --> CSUTLDTC["CSUTLDTC<br/>Date Validator<br/>CALL CEEDAYS"]
    
    COTRN02C --> ACCTDAT["ACCTDAT<br/>Account Data<br/>for validation"]
    
    style COTRN00C fill:#cce5ff
    style COTRN01C fill:#cce5ff
    style COTRN02C fill:#cce5ff
    style CSUTLDTC fill:#e8f4f8
    style TRANSACT fill:#f0f0f0
    style ACCTDAT fill:#f0f0f0
```

## User Management Subgraph

```mermaid
graph LR
    USERMENU["User Management"]
    USERMENU --> COUSR00C["List Users<br/>COUSR00C<br/>BROWSE USRSEC"]
    USERMENU --> COUSR01C["Add User<br/>COUSR01C<br/>WRITE USRSEC"]
    USERMENU --> COUSR02C["Update User<br/>COUSR02C<br/>READ UPDATE USRSEC"]
    USERMENU --> COUSR03C["Delete User<br/>COUSR03C<br/>DELETE USRSEC"]
    
    COUSR00C --> USRSEC["USRSEC<br/>User Security<br/>VSAM KSDS"]
    COUSR01C --> USRSEC
    COUSR02C --> USRSEC
    COUSR03C --> USRSEC
    
    style COUSR00C fill:#cce5ff
    style COUSR01C fill:#cce5ff
    style COUSR02C fill:#cce5ff
    style COUSR03C fill:#cce5ff
    style USRSEC fill:#f0f0f0
```

## Direct CALL Dependencies

```mermaid
graph LR
    CBACT01C["CBACT01C"]
    COBDATFT["COBDATFT<br/>Date Utility"]
    CBACT01C -->|CALL| COBDATFT
    
    CBACT04C["CBACT04C"]
    CBACT04C -->|CALL| COBDATFT
    
    COTRN02C["COTRN02C"]
    CSUTLDTC["CSUTLDTC<br/>Date Validator"]
    COTRN02C -->|CALL| CSUTLDTC
    
    CSUTLDTC -->|CALL| CEEDAYS["CEEDAYS<br/>External CICS Library"]
    
    COBSWAIT["COBSWAIT"]
    MVSWAIT["MVSWAIT<br/>MVS Wait Routine"]
    COBSWAIT -->|CALL| MVSWAIT
    
    style COBDATFT fill:#e8f4f8
    style CSUTLDTC fill:#e8f4f8
    style CEEDAYS fill:#f0f0f0
    style MVSWAIT fill:#f0f0f0
```

## Master File Relationships

```mermaid
graph TD
    ACCTFILE["ACCTDATA<br/>Account Master<br/>VSAM KSDS"]
    CARDFILE["CARDDATA<br/>Card Master<br/>VSAM KSDS<br/>+ AIX on Acct#"]
    CUSTFILE["CUSTDATA<br/>Customer Master<br/>VSAM KSDS"]
    XREFFILE["CARDXREF<br/>Card XRef<br/>VSAM KSDS<br/>+ AIX"]
    TRANFILE["TRANSACT<br/>Transaction Master<br/>VSAM KSDS<br/>+ AIX on Date"]
    USRSECFILE["USRSEC<br/>User Security<br/>VSAM KSDS"]
    
    CARDFILE -->|Acct# lookup| ACCTFILE
    XREFFILE -->|Contains| CARDFILE
    XREFFILE -->|Links| ACCTFILE
    XREFFILE -->|Links| CUSTFILE
    TRANFILE -->|References| XREFFILE
    TRANFILE -->|References| ACCTFILE
    USRSECFILE -->|Authentication| ALL["All CICS Programs"]
    
    style ACCTFILE fill:#f0f0f0
    style CARDFILE fill:#f0f0f0
    style CUSTFILE fill:#f0f0f0
    style XREFFILE fill:#f0f0f0
    style TRANFILE fill:#f0f0f0
    style USRSECFILE fill:#f0f0f0
    style ALL fill:#e8f4f8
```

## Copybook/Data Structure Dependencies

```mermaid
graph TD
    CVACT01Y["CVACT01Y<br/>Account Record<br/>Layout"]
    CVACT02Y["CVACT02Y<br/>Card Record<br/>Layout"]
    CVACT03Y["CVACT03Y<br/>XRef Record<br/>Layout"]
    CVTRA01Y["CVTRA01Y<br/>Category Balance<br/>Layout"]
    CVTRA02Y["CVTRA02Y<br/>Interest/Fee<br/>Layout"]
    CVTRA05Y["CVTRA05Y<br/>Transaction<br/>Record Layout"]
    CVTRA06Y["CVTRA06Y<br/>Daily Trans<br/>Input Layout"]
    CVCUS01Y["CVCUS01Y<br/>Customer<br/>Record Layout"]
    CSUSR01Y["CSUSR01Y<br/>User Security<br/>Record Layout"]
    
    CBACT01C["CBACT01C"] --> CVACT01Y
    CBACT02C["CBACT02C"] --> CVACT02Y
    CBACT03C["CBACT03C"] --> CVACT03Y
    CBACT04C["CBACT04C"] --> CVTRA01Y
    CBACT04C --> CVTRA02Y
    CBTRN01C["CBTRN01C"] --> CVTRA06Y
    CBTRN02C["CBTRN02C"] --> CVTRA06Y
    CBTRN03C["CBTRN03C"] --> CVTRA05Y
    
    CBSTM03A["CBSTM03A"] --> CVTRA05Y
    CBSTM03A --> CVACT01Y
    
    COACTUPC["COACTUPC"] --> CVACT01Y
    COUSR00C["COUSR00C"] --> CSUSR01Y
    COTRN02C["COTRN02C"] --> CVTRA05Y
    
    style CVACT01Y fill:#fff4cc
    style CVACT02Y fill:#fff4cc
    style CVACT03Y fill:#fff4cc
    style CVTRA01Y fill:#fff4cc
    style CVTRA02Y fill:#fff4cc
    style CVTRA05Y fill:#fff4cc
    style CVTRA06Y fill:#fff4cc
    style CVCUS01Y fill:#fff4cc
    style CSUSR01Y fill:#fff4cc
```
