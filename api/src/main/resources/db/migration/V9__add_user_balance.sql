
-- this is the new trade system will represent trades
CREATE TABLE "user_balances" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "user_api_key_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "currency" text NOT NULL,
 "currency_long" text NOT NULL,
 "blockchain_address" text,
 "available" decimal NOT NULL default 0,
 "exchange_total" decimal NOT NULL default 0,
 "exchange_available" decimal NOT NULL default 0,
 "pending_deposit" decimal,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP default current_timestamp
);

ALTER TABLE "user_balances" ADD CONSTRAINT "user_balance_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;


ALTER TABLE "user_balances" ADD CONSTRAINT "user_balance_api_key_fk"
  FOREIGN KEY("user_api_key_id") REFERENCES "user_api_keys"("id") ON DELETE CASCADE ON UPDATE CASCADE;
