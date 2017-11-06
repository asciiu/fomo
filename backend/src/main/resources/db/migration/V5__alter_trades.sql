
-- remove declaration from previous migration
DROP TABLE IF EXISTS trades;

-- this is the new trade system will represent trades
CREATE TABLE trades(
 id UUID PRIMARY KEY NOT NULL,
 user_id UUID REFERENCES users (id),
 exchange_name text NOT NULL,
 market_name text NOT NULL,
 market_currency_abbrev text NOT NULL,
 market_currency_name text NOT NULL,
 base_currency_abbrev text NOT NULL,
 base_currency_name text NOT NULL,
 quantity decimal NOT NULL,
 status text NOT NULL,
 created_on TIMESTAMP DEFAULT now(),
 updated_on TIMESTAMP DEFAULT now(),
 buy_time timestamp,
 buy_price decimal,
 buy_condition_id UUID,
 buy_conditions jsonb,
 sell_time timestamp,
 sell_price decimal,
 sell_condition_id UUID,
 sell_conditions jsonb
);



