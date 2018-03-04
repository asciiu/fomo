
CREATE TABLE "user_devices" (
 "id" UUID PRIMARY KEY NOT NULL,
 "user_id" UUID NOT NULL,
 "device_id" text NOT NULL,
 "device_type" text NOT NULL,
 "device_token" text NOT NULL,
 "created_on" TIMESTAMP DEFAULT now(),
 "updated_on" TIMESTAMP default current_timestamp
);

ALTER TABLE "user_devices" ADD CONSTRAINT "user_device_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
