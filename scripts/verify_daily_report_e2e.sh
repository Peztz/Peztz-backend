#!/usr/bin/env bash
set -euo pipefail

required_variables=(ACCESS_TOKEN PET_ID REPORT_DATE)
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
done

if ! command -v curl >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
  echo "curl and python3 are required." >&2
  exit 1
fi

spring_base_url="${SPRING_BASE_URL:-http://127.0.0.1:18080}"

echo "Checking Spring health at ${spring_base_url}"
curl --fail --silent --show-error "${spring_base_url}/actuator/health" >/dev/null

request_report() {
  curl --fail-with-body --silent --show-error --get \
    "${spring_base_url}/api/reports/daily" \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --data-urlencode "petId=${PET_ID}" \
    --data-urlencode "date=${REPORT_DATE}"
}

echo "Requesting the report for pet=${PET_ID}, date=${REPORT_DATE}"
first_response="$(request_report)"

printf '%s' "${first_response}" | python3 -c '
import json
import sys

report = json.load(sys.stdin)
required = {
    "reportId", "petId", "petName", "date", "status", "totalLogCount",
    "summary", "behaviorCards", "environmentCard", "careTips", "riskLevel",
    "warnings", "disclaimer", "generatedAt"
}
missing = sorted(required.difference(report))
if missing:
    raise SystemExit("Missing response fields: " + ", ".join(missing))
if report["status"] != "READY":
    raise SystemExit("Report generation failed with status=" + str(report["status"]))
if report["totalLogCount"] <= 0:
    raise SystemExit("The selected pet/date has no logs; choose a date with real data")
if report["riskLevel"] not in {"NORMAL", "ATTENTION", "URGENT"}:
    raise SystemExit("Invalid riskLevel=" + str(report["riskLevel"]))
if not isinstance(report["behaviorCards"], list) or not isinstance(report["careTips"], list):
    raise SystemExit("Structured card arrays are invalid")
print("status=READY")
print("reportId=" + report["reportId"])
print("totalLogCount=" + str(report["totalLogCount"]))
print("behaviorCardCount=" + str(len(report["behaviorCards"])))
print("riskLevel=" + report["riskLevel"])
print("generatedAt=" + report["generatedAt"])
'

first_report_id="$(printf '%s' "${first_response}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["reportId"])')"
second_response="$(request_report)"
second_report_id="$(printf '%s' "${second_response}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["reportId"])')"

if [[ "${first_report_id}" != "${second_report_id}" ]]; then
  echo "The second request returned a different reportId; persisted report reuse failed." >&2
  exit 1
fi

echo "Persistence check passed: the second request reused reportId=${second_report_id}"
echo "E2E verification passed: PostgreSQL -> Spring -> FastAPI -> OpenAI -> PostgreSQL -> API response"
