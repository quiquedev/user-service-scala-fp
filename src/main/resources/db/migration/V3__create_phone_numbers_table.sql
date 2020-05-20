CREATE TABLE phone_numbers (
  user_id INTEGER,
  phone_number_id INTEGER,
  phone_number VARCHAR(500) NOT NULL,
  PRIMARY KEY(user_id, phone_number_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON phone_numbers TO ${dbAppUser};