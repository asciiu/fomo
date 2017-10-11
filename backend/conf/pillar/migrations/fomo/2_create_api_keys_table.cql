-- description: create api_keys table
-- authoredAt: 1507700427000
-- up:

CREATE TABLE api_keys (
  id uuid,
  user_id uuid,
  key text,
  secret text,
  created_at timestamp,
  updated_at timestamp,

  PRIMARY KEY (id)
)

-- down:

DROP TABLE api_keys
