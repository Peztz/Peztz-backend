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
response_dir="$(mktemp -d "${TMPDIR:-/tmp}/peztz-report-concurrency.XXXXXX")"
cleanup() {
  rm -rf "${response_dir}"
}
trap cleanup EXIT

request_report() {
  curl --fail-with-body --silent --show-error --get \
    "${spring_base_url}/api/reports/daily" \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --data-urlencode "petId=${PET_ID}" \
    --data-urlencode "date=${REPORT_DATE}"
}

request_report > "${response_dir}/first.json" &
first_pid=$!
request_report > "${response_dir}/second.json" &
second_pid=$!

wait "${first_pid}"
wait "${second_pid}"

python3 - "${response_dir}/first.json" "${response_dir}/second.json" <<'PY'
import json
import pathlib
import sys

reports = [json.loads(pathlib.Path(path).read_text()) for path in sys.argv[1:]]
report_ids = {report.get("reportId") for report in reports}
statuses = [report.get("status") for report in reports]

if len(report_ids) != 1 or None in report_ids:
    raise SystemExit(f"Concurrent requests returned different reportIds: {report_ids}")
if statuses != ["READY", "READY"]:
    raise SystemExit(f"Concurrent report generation did not complete successfully: {statuses}")

print("Concurrent response check passed")
print("reportId=" + reports[0]["reportId"])
print("statuses=" + ",".join(statuses))
print("totalLogCount=" + str(reports[0].get("totalLogCount")))
print("sensorLogCount=" + str(reports[0].get("sensorLogCount")))
PY
