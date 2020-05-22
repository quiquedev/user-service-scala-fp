CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  last_name VARCHAR(500) NOT NULL,
  first_name VARCHAR(500) NOT NULL,
  emails JSON NOT NULL,
  phone_numbers JSON NOT NULL
);

GRANT SELECT, INSERT, UPDATE, DELETE ON users TO ${dbAppUser};
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO ${dbAppUser};