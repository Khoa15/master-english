# DoraDodo API server (PHP 8.1+)

Needs `pdo_mysql`, `curl` and `openssl`. No Composer packages.

## Setup
1. Create the database and load `../db/mysql/schema.sql`.
2. `cp .env.example .env` and fill in `DB_*`, `FIREBASE_PROJECT_ID` (the `project_id` of `app/google-services.json`) and a long random `RATE_LIMIT_SALT`.
3. Serve `public/` over HTTPS (the app sends ID tokens). Dev: `php -S 0.0.0.0:8080 -t public public/index.php`.
4. In the app's `local.properties`: `api.base.url=https://your-host/api/`.

## API
Every route is versioned: `/api/v1/...`. A breaking change gets `/api/v2/...` beside it; v1 stays for installed apps.

| Route | Auth | Notes |
|---|---|---|
| `GET /api/v1/health` | none | |
| `POST /api/v1/feedback` | `Authorization: Bearer <Firebase ID token>`, signed-in accounts only (anonymous rejected) | 201 `{accepted, serverTime}`; 401, 422, 429 (`Retry-After`) |

Limits (env): `RATE_IP_PER_HOUR` 30, `RATE_USER_PER_HOUR` 5, `RATE_USER_PER_DAY` 20. Behind a proxy set `TRUSTED_PROXIES`.

## Test
With the server and DB running: `BASE=http://127.0.0.1:8080/api PROJECT=demo sh tests/smoke.sh`
(`FIREBASE_PROJECT_ID` must equal `PROJECT`; the helper installs a throw-away certificate in the verifier's cache.)
