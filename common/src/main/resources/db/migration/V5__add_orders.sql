
-- orders shall have an internal id and an external id
CREATE TABLE "orders" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "user_api_key_id" UUID NOT NULL,
 "exchange_name" text NOT NULL,
 "exchange_order_id" text NOT NULL,
 "exchange_market_name" text NOT NULL,
 "market_name" text NOT NULL,
 "side" text NOT NULL,
 "type" text NOT NULL,
 "price" decimal NOT NULL,
 "quantity" decimal NOT NULL,
 "quantity_remaining" decimal NOT NULL,
 "status" text NOT NULL,
 "conditions" jsonb NOT NULL,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP DEFAULT now()
);

CREATE TABLE "order_fills" (
  "id" UUID PRIMARY KEY NOT NULL,
  "order_id" UUID NOT NULL,
  "condition" jsonb NOT NULL,
  "price" decimal NOT NULL,
  "quantity" decimal NOT NULL,
  "created_on" TIMESTAMP DEFAULT now(),
  "updated_on" TIMESTAMP DEFAULT now()
);

ALTER TABLE "orders" ADD CONSTRAINT "order_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "orders" ADD CONSTRAINT "order_api_key_fk"
  FOREIGN KEY("user_api_key_id") REFERENCES "user_api_keys"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "order_fills" ADD CONSTRAINT "order_fill_order_fk"
  FOREIGN KEY("order_id") REFERENCES "orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;
