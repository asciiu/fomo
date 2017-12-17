
DROP TABLE IF EXISTS "trades";

-- this is the new trade system will represent trades
CREATE TABLE "trades" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "user_api_key_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "market_name" text NOT NULL,
 "market_currency" text NOT NULL,
 "market_currency_long" text NOT NULL,
 "base_currency" text NOT NULL,
 "base_currency_long" text NOT NULL,
 "base_quantity" decimal NOT NULL,
 "market_quantity" decimal,
 "status" text NOT NULL,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP DEFAULT now(),
 "buy_time" timestamp,
 "buy_price" decimal,
 "buy_condition" text,
 "buy_conditions" text NOT NULL,
 "sell_time" timestamp,
 "sell_price" decimal,
 "sell_condition" text,
 "stop_loss_conditions" text,
 "take_profit_conditions" text
);

ALTER TABLE "trades" ADD CONSTRAINT "trade_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "trades" ADD CONSTRAINT "trade_api_key_fk"
  FOREIGN KEY("user_api_key_id") REFERENCES "user_api_keys"("id") ON DELETE CASCADE ON UPDATE CASCADE;
