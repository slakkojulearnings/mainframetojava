-- CardDemo Database Schema
-- Auto-created by Spring Boot (spring.jpa.hibernate.ddl-auto=create-drop for testing)
-- For production, use Flyway or Liquibase migrations

CREATE TABLE IF NOT EXISTS account (
    account_id VARCHAR(11) PRIMARY KEY,
    active_status VARCHAR(1),
    curr_bal NUMERIC(13,2),
    credit_limit NUMERIC(13,2),
    cash_credit_limit NUMERIC(13,2),
    curr_cyc_credit NUMERIC(13,2),
    curr_cyc_debit NUMERIC(13,2),
    open_date VARCHAR(8),
    expiration_date VARCHAR(8),
    reissue_date VARCHAR(8),
    group_id VARCHAR(6)
);

CREATE TABLE IF NOT EXISTS customer (
    cust_id VARCHAR(9) PRIMARY KEY,
    ssn VARCHAR(9),
    fico_score VARCHAR(3),
    dob VARCHAR(8),
    first_name VARCHAR(20),
    middle_name VARCHAR(20),
    last_name VARCHAR(20),
    addr_line1 VARCHAR(30),
    addr_line2 VARCHAR(30),
    addr_line3 VARCHAR(30),
    addr_state_code VARCHAR(2),
    addr_zip VARCHAR(5),
    addr_country_code VARCHAR(2),
    phone1 VARCHAR(10),
    phone2 VARCHAR(10),
    govt_id VARCHAR(20),
    eft_account_id VARCHAR(17),
    pri_card_holder_ind VARCHAR(1)
);

CREATE TABLE IF NOT EXISTS card (
    card_num VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(11),
    embossed_name VARCHAR(26),
    active_status VARCHAR(1),
    expiration_date VARCHAR(6),
    FOREIGN KEY (account_id) REFERENCES account(account_id)
);

CREATE TABLE IF NOT EXISTS transaction (
    tran_id VARCHAR(16) PRIMARY KEY,
    card_num VARCHAR(16),
    type_code VARCHAR(3),
    cat_code VARCHAR(4),
    source VARCHAR(3),
    amount NUMERIC(11,2),
    desc VARCHAR(30),
    orig_ts VARCHAR(14),
    proc_ts VARCHAR(14),
    merchant_id VARCHAR(15),
    merchant_name VARCHAR(26),
    merchant_city VARCHAR(13),
    merchant_zip VARCHAR(5),
    FOREIGN KEY (card_num) REFERENCES card(card_num)
);

CREATE TABLE IF NOT EXISTS card_xref (
    card_num VARCHAR(16) PRIMARY KEY,
    cust_id VARCHAR(9),
    account_id VARCHAR(11),
    FOREIGN KEY (cust_id) REFERENCES customer(cust_id),
    FOREIGN KEY (account_id) REFERENCES account(account_id)
);

CREATE INDEX IF NOT EXISTS idx_card_account ON card(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_card ON transaction(card_num);
CREATE INDEX IF NOT EXISTS idx_card_xref_account ON card_xref(account_id);
CREATE INDEX IF NOT EXISTS idx_card_xref_cust ON card_xref(cust_id);
