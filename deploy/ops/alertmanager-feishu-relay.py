#!/usr/bin/env python3
"""Receive Alertmanager webhooks on loopback and forward safe text to Feishu."""

import json
import logging
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse
from urllib.request import Request, urlopen

MAX_BODY_BYTES = 1_048_576
MAX_ALERTS = 8


def webhook_url() -> str:
    value = os.environ.get("PXCZXN_FEISHU_WEBHOOK_URL", "")
    parsed = urlparse(value)
    if (
        parsed.scheme != "https"
        or parsed.netloc != "open.feishu.cn"
        or not parsed.path.startswith("/open-apis/bot/v2/hook/")
    ):
        raise RuntimeError("PXCZXN_FEISHU_WEBHOOK_URL must be a Feishu custom-bot webhook")
    return value


def format_alert(payload: dict) -> str:
    status = str(payload.get("status", "unknown")).upper()
    alerts = payload.get("alerts")
    if not isinstance(alerts, list):
        alerts = []
    lines = ["星语告警", f"状态：{status}"]
    for alert in alerts[:MAX_ALERTS]:
        labels = alert.get("labels") if isinstance(alert, dict) else {}
        annotations = alert.get("annotations") if isinstance(alert, dict) else {}
        if not isinstance(labels, dict):
            labels = {}
        if not isinstance(annotations, dict):
            annotations = {}
        name = labels.get("alertname", "未命名告警")
        severity = labels.get("severity", "unknown")
        summary = annotations.get("summary", "无摘要")
        lines.append(f"[{severity}] {name}：{summary}")
    if len(alerts) > MAX_ALERTS:
        lines.append(f"其余 {len(alerts) - MAX_ALERTS} 条告警已合并。")
    return "\n".join(lines)


def forward_to_feishu(payload: dict) -> None:
    body = json.dumps(
        {"msg_type": "text", "content": {"text": format_alert(payload)}},
        ensure_ascii=False,
    ).encode("utf-8")
    request = Request(
        webhook_url(), body, {"Content-Type": "application/json"}, method="POST"
    )
    with urlopen(request, timeout=10) as response:
        if response.status < 200 or response.status >= 300:
            raise RuntimeError(f"Feishu returned HTTP {response.status}")


class RelayHandler(BaseHTTPRequestHandler):
    def log_message(self, format: str, *args: object) -> None:
        logging.info("%s - %s", self.address_string(), format % args)

    def do_POST(self) -> None:  # noqa: N802 - required by BaseHTTPRequestHandler
        if self.path != "/alertmanager":
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY_BYTES:
                raise ValueError("invalid request body length")
            payload = json.loads(self.rfile.read(length))
            if not isinstance(payload, dict):
                raise ValueError("request body must be an object")
            forward_to_feishu(payload)
        except (ValueError, OSError, RuntimeError, json.JSONDecodeError) as error:
            logging.warning("alert relay delivery failed: %s", error)
            self.send_error(502, "alert delivery failed")
            return
        self.send_response(200)
        self.end_headers()


def main() -> None:
    if "--check-config" in sys.argv:
        webhook_url()
        return
    webhook_url()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    server = ThreadingHTTPServer(("127.0.0.1", 19093), RelayHandler)
    server.serve_forever()


if __name__ == "__main__":
    main()
