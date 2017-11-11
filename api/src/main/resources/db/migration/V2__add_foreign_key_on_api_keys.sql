ALTER TABLE "user_api_keys" ADD CONSTRAINT "user_api_keys_user_fk"
  FOREIGN KEY("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
