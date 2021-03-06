
DROP TABLE IF EXISTS "trades";

-- this is the new trade system will represent trades
CREATE TABLE "trades" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "user_api_key_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "market_name" text NOT NULL,
 "currency" text NOT NULL,
 "currency_long" text NOT NULL,
 "currency_quantity" decimal,
 "base_currency" text NOT NULL,
 "base_currency_long" text NOT NULL,
 "base_quantity" decimal NOT NULL,
 "status" text NOT NULL,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP DEFAULT now(),
 "buy_condition" text NOT NULL,
 "bought_time" timestamp,
 "bought_price" decimal,
 "bought_condition" text,
 "stop_loss_condition" text,
 "profit_condition" text,
 "sold_time" timestamp,
 "sold_price" decimal,
 "sold_condition" text
);

ALTER TABLE "trades" ADD CONSTRAINT "trade_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "trades" ADD CONSTRAINT "trade_api_key_fk"
  FOREIGN KEY("user_api_key_id") REFERENCES "user_api_keys"("id") ON DELETE CASCADE ON UPDATE CASCADE;
