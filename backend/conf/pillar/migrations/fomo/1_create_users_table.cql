-- description: create users table
-- authoredAt: 1507698796000
-- up:

CREATE TABLE users (
  id timeuuid,
  first_name text,
  last_name text,
  email text,
  password_hash text,
  created_at timestamp,
  updated_at timestamp,

  PRIMARY KEY (id)
)

-- down:

DROP TABLE users

