-- USERS
CREATE TABLE "users"(
    "id" UUID NOT NULL,
    "first_name" VARCHAR NOT NULL,
    "last_name" VARCHAR NOT NULL,
    "email" VARCHAR NOT NULL NOT NULL,
    "password" VARCHAR NOT NULL NOT NULL,
    "salt" VARCHAR NOT NULL NOT NULL,
    "created_on" TIMESTAMP DEFAULT now(),
    "updated_on" TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX "users_email" ON "users"("email");
ALTER TABLE "users" ADD CONSTRAINT "users_id" PRIMARY KEY("id");

-- PASSWORD RESET CODES
CREATE TABLE "password_reset_codes"(
  "id" UUID NOT NULL,
  "code" VARCHAR NOT NULL,
  "user_id" UUID NOT NULL,
  "valid_to" TIMESTAMP NOT NULL
);
ALTER TABLE "password_reset_codes" ADD CONSTRAINT "password_reset_codes_id" PRIMARY KEY("id");
ALTER TABLE "password_reset_codes" ADD CONSTRAINT "password_reset_codes_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- REMEMBER ME TOKENS
CREATE TABLE "remember_me_tokens"(
  "id" UUID NOT NULL,
  "selector" VARCHAR NOT NULL,
  "token_hash" VARCHAR NOT NULL,
  "user_id" UUID NOT NULL,
  "valid_to" TIMESTAMP NOT NULL
);
ALTER TABLE "remember_me_tokens" ADD CONSTRAINT "remember_me_tokens_id" PRIMARY KEY("id");
ALTER TABLE "remember_me_tokens" ADD CONSTRAINT "remember_me_tokens_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
CREATE UNIQUE INDEX "remember_me_tokens_selector" ON "remember_me_tokens"("selector");

CREATE TABLE "user_api_keys"(
  "id" UUID PRIMARY KEY NOT NULL,
  "user_id" UUID NOT NULL,
  "exchange_name" VARCHAR NOT NULL,
  "api_key" VARCHAR NOT NULL,
  "secret"  VARCHAR NOT NULL,
  "description" VARCHAR,
  "status" VARCHAR NOT NULL,
  "created_on" TIMESTAMP DEFAULT now(),
  "updated_on" TIMESTAMP default current_timestamp
);


ALTER TABLE "user_api_keys" ADD CONSTRAINT "user_api_keys_unique" UNIQUE ("api_key", "secret");

ALTER TABLE "user_api_keys" ADD CONSTRAINT "user_api_keys_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
