#!/bin/sh
# End-to-end checks against a running server. Needs php, curl and a MySQL database loaded with db/mysql/schema.sql.
# usage: BASE=http://127.0.0.1:8080/api PROJECT=demo sh tests/smoke.sh
BASE=${BASE:-http://127.0.0.1:8080/api}
PROJECT=${PROJECT:-demo}
DIR=$(dirname "$0")
fail=0
code() { curl -s -o /tmp/body.json -w '%{http_code}' "$@"; }
check() { # name expected actual
  if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected $2 got $3 ($(cat /tmp/body.json))"; fail=1; fi
}
GOOD=$(php "$DIR/make_token.php" "$PROJECT" user-a password)
BODY='{"rating":4,"type":"Bug","message":"hello","appVersion":"1.0.0","sdkInt":34,"locale":"vi-VN","sentAt":1}'
J='Content-Type: application/json'

check "no version -> 404"        404 "$(code -X POST "$BASE/feedback" -H "$J" -d "$BODY")"
check "v9 -> 404"                404 "$(code -X POST "$BASE/v9/feedback" -H "$J" -d "$BODY")"
check "health v1"                200 "$(code "$BASE/v1/health")"
check "GET feedback -> 405"      405 "$(code "$BASE/v1/feedback")"
check "no token -> 401"          401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -d "$BODY")"
check "garbage token -> 401"     401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H 'Authorization: Bearer a.b.c' -d "$BODY")"
ANON=$(php "$DIR/make_token.php" "$PROJECT" anon-1 anonymous)
check "anonymous -> 401"         401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $ANON" -d "$BODY")"
OTHER=$(php "$DIR/make_token.php" other-project user-a password)
check "other project -> 401"     401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $OTHER" -d "$BODY")"
OLD=$(php "$DIR/make_token.php" "$PROJECT" user-a password -3600)
check "expired -> 401"           401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $OLD" -d "$BODY")"
TAMPER="$(echo "$GOOD" | cut -d. -f1).$(echo '{"sub":"admin"}' | base64 | tr -d '=\n' | tr '+/' '-_').$(echo "$GOOD" | cut -d. -f3)"
check "tampered payload -> 401"  401 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $TAMPER" -d "$BODY")"
check "bad rating -> 422"        422 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $GOOD" -d '{"rating":9,"type":"Bug","message":"x","sentAt":1}')"
check "wrong content-type -> 415" 415 "$(code -X POST "$BASE/v1/feedback" -H 'Content-Type: text/plain' -H "Authorization: Bearer $GOOD" -d "$BODY")"
check "valid -> 201"             201 "$(code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $GOOD" -d "$BODY")"
for i in 2 3 4 5; do code -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $GOOD" -d "$BODY" >/dev/null; done
check "6th in an hour -> 429"    429 "$(code -D /tmp/h.txt -X POST "$BASE/v1/feedback" -H "$J" -H "Authorization: Bearer $GOOD" -d "$BODY")"
grep -qi '^retry-after:' /tmp/h.txt && echo "ok   Retry-After header" || { echo "FAIL Retry-After header"; fail=1; }
exit $fail
