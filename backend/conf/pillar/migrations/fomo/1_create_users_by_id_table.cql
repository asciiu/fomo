-- description: create users_by_id table
-- authoredAt: 1507698796000
-- up:

CREATE TABLE users_by_id (
  id uuid,
  first_name text,
  last_name text,
  email text,
  salt text,
  password_hash text,

  PRIMARY KEY (id)
)

-- down:

DROP TABLE users_by_id

