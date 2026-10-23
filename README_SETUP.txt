DEVICE SETUP (manual)
1. Generate a long random key for each hospital/device (at least 32 random characters).
2. After importing database.sql, insert a device row. Example SQL (replace values):
   INSERT INTO sms_devices(hospital_id, device_key_hash, active)
   VALUES ('hospital01', SHA2('PUT_A_LONG_RANDOM_DEVICE_KEY_HERE',256), 1);
3. In Android app, enter the exact same device key (plain key; DB stores SHA-256 hash).
4. Test enqueue with POST JSON and admin key:
   curl -X POST https://YOURDOMAIN/smsgateway/api/enqueue.php \
     -H 'Content-Type: application/json' \
     -d '{"admin_key":"YOUR_GATEWAY_ADMIN_KEY","hospital_id":"hospital01","to":"+919876543210","message":"Approved test message"}'
Use your own number first. API admin key must remain server-side.
