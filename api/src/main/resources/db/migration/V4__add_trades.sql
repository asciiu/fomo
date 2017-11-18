-- this is the new trade system will represent trades
CREATE TABLE "trades" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "market_name" text NOT NULL,
 "market_currency_abbrev" text NOT NULL,
 "market_currency_name" text NOT NULL,
 "base_currency_abbrev" text NOT NULL,
 "base_currency_name" text NOT NULL,
 "quantity" decimal NOT NULL,
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
 "sell_conditions" text
);

ALTER TABLE "trades" ADD CONSTRAINT "trade_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;


