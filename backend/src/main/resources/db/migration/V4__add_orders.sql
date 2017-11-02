CREATE TABLE orders(
 id UUID PRIMARY KEY NOT NULL,
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
 id UUID PRIMARY KEY NOT NULL,
 user_id UUID NOT NULL,
 open_order_id UUID REFERENCES orders (id),
 close_order_id UUID REFERENCES orders (id),
 exchange_name text NOT NULL,
 market_currency_short_name text,
 market_currency_long_name text,
 bought_price decimal,
 bought_time timestamp
 sold_price decimal,
 sold_time timestamp NOT NULL,
);



