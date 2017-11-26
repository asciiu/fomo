
DROP TABLE IF EXISTS "user_api_keys";

CREATE TABLE "user_api_keys"(
  "id" UUID NOT NULL,
  "user_id" UUID NOT NULL,
  "exchange" VARCHAR NOT NULL,
  "api_key" VARCHAR NOT NULL,
  "secret"  VARCHAR NOT NULL,
  "description" VARCHAR,
  "created_on" TIMESTAMP DEFAULT now(),
  "updated_on" TIMESTAMP default current_timestamp,
  "validated_on" TIMESTAMP
);

ALTER TABLE "user_api_keys" ADD CONSTRAINT "user_api_keys_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
