CREATE TABLE "bittrex_market_updates"(
 "id" SERIAL PRIMARY KEY,
 "market_name" VARCHAR NOT NULL,
 "high" DECIMAL,
 "low" DECIMAL,
 "volume" DECIMAL,
 "last" DECIMAL,
 "base_volume" DECIMAL,
 "timestamp" TIMESTAMP NOT NULL,
 "bid" DECIMAL,
 "ask" DECIMAL,
 "open_buy_orders" Int,
 "open_sell_orders" Int,
 "prev_day" DECIMAL,
 "created_on" TIMESTAMP NOT NULL
);
