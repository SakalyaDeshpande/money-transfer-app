INSERT INTO currency (id, name, abbr)
VALUES
  (1, 'US Dollar','USD'),
  (2, 'Euro', 'EUR'),
  (3, 'GBP', 'GBP');

INSERT INTO transaction_status (id, name)
VALUES
       (1, 'Planned'),
       (2, 'Processing'),
       (3, 'Failed'),
       (4, 'Succeed');

INSERT INTO bank_account (account_holder_name, balance,  currency_id)
VALUES
  ('Sakalya Deshpande', 1000.5,  3),
  ('John Doe', 1000.5, 2),
  ('Jane Doe', 1000.5, 1);