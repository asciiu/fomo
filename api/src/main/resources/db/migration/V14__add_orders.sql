
-- orders shall have an internal id and an external id
CREATE TABLE "orders" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "user_api_key_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "exchange_order_id" text NOT NULL,
 "exchange_market_name" text NOT NULL,
 "order_type" text NOT NULL,
 "price" bigint NOT NULL,
 "quantity" bigint NOT NULL,
 "quantity_remaining" bigint NOT NULL,
 "side" text NOT NULL,
 "status" text NOT NULL,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP DEFAULT now(),
 "buy_time" timestamp,
 "buy_price" bigint,
 "buy_condition" text,
 "buy_conditions" text NOT NULL,
 "sell_time" timestamp,
 "sell_price" bigint,
 "sell_condition" text,
 "stop_loss_conditions" text,
 "take_profit_conditions" text
);

ALTER TABLE "orders" ADD CONSTRAINT "order_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "orders" ADD CONSTRAINT "order_api_key_fk"
  FOREIGN KEY("user_api_key_id") REFERENCES "user_api_keys"("id") ON DELETE CASCADE ON UPDATE CASCADE;
