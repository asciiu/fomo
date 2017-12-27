
-- this is the new trade system will represent trades
CREATE TABLE "markets" (
 "id" UUID PRIMARY KEY NOT NULL,
 "exchange_name" text NOT NULL,
 "market_name" text NOT NULL,
 "currency" text NOT NULL,
 "currency_long" text,
 "base_currency" text NOT NULL,
 "base_currency_long" text,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP default current_timestamp
);
