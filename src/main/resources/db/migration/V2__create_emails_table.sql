CREATE TABLE emails (
  user_id INTEGER,
  email_id INTEGER,
  email VARCHAR(500) NOT NULL,
  PRIMARY KEY(user_id, email_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON emails TO ${dbAppUser};