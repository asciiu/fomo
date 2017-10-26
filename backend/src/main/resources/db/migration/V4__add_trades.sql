CREATE TABLE "bittrex_trades"(
 "id" SERIAL PRIMARY KEY,
 "market_name" VARCHAR NOT NULL,
 "is_open" boolean NOT NULL,
 "quantity" DECIMAL NOT NULL,
 "bid_price" DECIMAL,
 "created_on" TIMESTAMP NOT NULL,
 "purchased_on" TIMESTAMP,
 "purchased_price" DECIMAL
);
