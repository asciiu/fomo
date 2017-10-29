DROP TABLE If EXISTS bittrex_trades;

CREATE TABLE orders(
 id SERIAL PRIMARY KEY,
 user_id UUID NOT NULL,
 exchange_name text NOT NULL,
 market_name text NOT NULL,
 created_time timestamp NOT NULL,
 completed_time timestamp,
 completed_condition text,
 price_actual decimal,
 quantity decimal,
 order_type text NOT NULL,
 status text NOT NULL,
 conditions jsonb
);

-- once an open order has completed there
-- will be a trade record associated with it
-- there may or may not be a close order on
-- a trade
CREATE TABLE trades(
 id SERIAL PRIMARY KEY,
 user_id UUID NOT NULL,
 open_order_id integer REFERENCES orders (id),
 close_order_id integer REFERENCES orders (id),
 exchange_name text NOT NULL,
 market_currency_short_name text,
 market_currency_long_name text,
 bought_price decimal,
 sold_price decimal,
 open_time timestamp NOT NULL,
 closed_time timestamp
);



