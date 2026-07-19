#!/usr/bin/env bash
# Shared helpers for CAPTCHA-protected login in verify scripts.
# Requires: curl, python3. Caller provides COOKIE_JAR and BASE_URL.
#
# captcha_issue → sets CAPTCHA_ID / CAPTCHA_ANSWER (needs CAPTCHA_REVEAL_ANSWER=true)
# login_json <email> <password> → prints JSON login body including fresh CAPTCHA

captcha_issue() {
  local body
  body="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE_URL/api/v1/auth/captcha")"
  CAPTCHA_ID="$(CAPTCHA_JSON="$body" python3 -c 'import json,os; print(json.loads(os.environ["CAPTCHA_JSON"])["captchaId"])')"
  CAPTCHA_ANSWER="$(CAPTCHA_JSON="$body" python3 -c 'import json,os; d=json.loads(os.environ["CAPTCHA_JSON"]); a=d.get("revealAnswer"); assert a, "revealAnswer missing — set CAPTCHA_REVEAL_ANSWER=true"; print(a)')"
  [[ -n "$CAPTCHA_ID" && -n "$CAPTCHA_ANSWER" ]] || return 1
}

login_json() {
  local user="$1"
  local pass="$2"
  captcha_issue
  LOGIN_USER="$user" LOGIN_PASS="$pass" CAPTCHA_ID="$CAPTCHA_ID" CAPTCHA_ANSWER="$CAPTCHA_ANSWER" python3 - <<'PY'
import json, os
print(json.dumps({
    "username": os.environ["LOGIN_USER"],
    "password": os.environ["LOGIN_PASS"],
    "captchaId": os.environ["CAPTCHA_ID"],
    "captchaAnswer": os.environ["CAPTCHA_ANSWER"],
    "rememberDevice": False
}))
PY
}
