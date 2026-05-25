-- Sample test data for CardDemo
-- Loaded automatically by Spring (spring.sql.init.mode=always)

INSERT INTO customer VALUES
('000000001', '123456789', '750', '19800515', 'John', 'Michael', 'Doe', '123 Main St', 'Apt 4B', '', 'NY', '10001', 'US', '2125551234', '2125559876', 'DL', '123456789', 'Y');

INSERT INTO account VALUES
('12345678901', 'A', 5000.00, 10000.00, 2000.00, 1500.00, 800.00, '20200101', '20250131', '20200101', 'GRP001');

INSERT INTO card VALUES
('4532123456789012', '12345678901', 'John Michael Doe', 'A', '202501');

INSERT INTO card_xref VALUES
('4532123456789012', '000000001', '12345678901');

INSERT INTO transaction VALUES
('0000000000000001', '4532123456789012', 'PUR', '5411', 'POS', 45.50, 'Gas Station', '20240615120000', '20240615120030', 'MERCH001', 'Shell Gas', 'New York', '10001');
