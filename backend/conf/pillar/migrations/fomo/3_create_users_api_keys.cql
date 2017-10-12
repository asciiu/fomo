-- description: create users_api_keys table
-- authoredAt: 1507830269000
-- up:

CREATE TABLE users_api_keys (
  user_id UUID,
  api_key text,
  secret text,

  PRIMARY KEY (user_id)
)

-- down:

DROP TABLE users_api_keys
