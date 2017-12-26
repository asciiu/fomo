
-- this is the new trade system will represent trades
CREATE TABLE "trade_history" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "trade_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "market_name" text NOT NULL,
 "currency" text NOT NULL,
 "currency_long" text NOT NULL,
 "currency_quantity" decimal NOT NULL,
 "base_currency" text NOT NULL,
 "base_currency_long" text NOT NULL,
 "base_quantity" decimal NOT NULL,
 "type" text NOT NULL,
 "bid_ask_price" decimal NOT NULL,
 "actual_price" decimal NOT NULL,
 "title" text,
 "summary" text,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP DEFAULT now()
);

ALTER TABLE "trade_history" ADD CONSTRAINT "trade_history_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "trade_history" ADD CONSTRAINT "trade_history_trade_fk"
  FOREIGN KEY("trade_id") REFERENCES "trades"("id") ON DELETE CASCADE ON UPDATE CASCADE;
