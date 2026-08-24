import json
from typing import Any


DAILY_REPORT_PROMPT_VERSION = "daily-report-v2"

DAILY_REPORT_SYSTEM_PROMPT = (
    "당신은 반려동물 모니터링 로그를 보호자가 이해하기 쉬운 한국어로 요약하는 AI입니다. "
    "제공된 사실과 수치만 사용하고, 로그가 없거나 근거가 부족하면 그 사실을 명시하세요. "
    "질병을 진단하거나 수의사를 사칭하지 마세요. URGENT는 명확한 반복 이상행동이나 위험 신호가 "
    "입력에 있을 때만 사용하고, 필요한 경우 전문 수의사 상담을 권고하세요. "
    "evidence에는 입력에 실제 존재하는 시간, 이벤트 종류 또는 측정값만 적으세요. "
    "입력의 occurredAt 시각은 ISO 8601 UTC일 수 있습니다. 모든 시각을 Asia/Seoul(UTC+9)로 "
    "변환하고 보호자에게는 HH:mm 또는 YYYY-MM-DD HH:mm KST 형식으로 표현하세요. 원본 UTC의 "
    "Z 표기나 긴 ISO 8601 문자열을 summary, description, evidence, careTips, warnings에 그대로 "
    "노출하지 마세요. reportDate는 Asia/Seoul 기준 날짜입니다. "
    "개인정보를 추측하지 말고 출력은 지정된 구조를 따르세요."
)


def build_daily_report_messages(payload: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {
            "role": "system",
            "content": DAILY_REPORT_SYSTEM_PROMPT,
        },
        {
            "role": "user",
            "content": (
                f"프롬프트 버전: {DAILY_REPORT_PROMPT_VERSION}\n"
                "다음 JSON은 하루 동안 수집된 반려동물 모니터링 데이터입니다. "
                "앱 카드에 표시할 일일 리포트를 생성하세요.\n"
                + json.dumps(payload, ensure_ascii=False)
            ),
        },
    ]
