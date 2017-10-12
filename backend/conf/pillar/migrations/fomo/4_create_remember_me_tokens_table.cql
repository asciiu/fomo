-- description: create remember_me_tokens table
-- authoredAt: 1507846232000
-- up:

CREATE TABLE remember_me_tokens (
  selector text,
  token_hash text,
  user_id UUID,
  valid_to timestamp,

  PRIMARY KEY (selector)
)

-- down:

DROP TABLE remember_me_tokens
