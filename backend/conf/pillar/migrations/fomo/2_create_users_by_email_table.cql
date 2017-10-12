-- description: create users_by_email table
-- authoredAt: 1507830269000
-- up:

CREATE TABLE users_by_email (
  id uuid,
  first_name text,
  last_name text,
  email text,
  salt text,
  password_hash text,

  PRIMARY KEY (email)
)

-- down:

DROP TABLE users_by_email
