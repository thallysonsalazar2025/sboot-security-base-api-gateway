from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse
import json
import threading

HOST = "0.0.0.0"
PORT = 8080
UNAVAILABLE_COMPANY = "00000000-0000-0000-0000-000000000503"
RECOVERY_COMPANY = "00000000-0000-0000-0000-000000000504"
_recovery_hits = 0
_lock = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    def _write(self, status, payload, content_type="application/json"):
        body = payload if isinstance(payload, bytes) else json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        correlation_id = self.headers.get("X-Correlation-Id")
        if correlation_id:
            self.send_header("X-Correlation-Id", correlation_id)
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        global _recovery_hits
        parsed = urlparse(self.path)
        if parsed.path != "/__admin/reset":
            self._write(404, {"error": "not_found"})
            return
        with _lock:
            _recovery_hits = 0
        self._write(204, b"", content_type="application/json")

    def do_GET(self):
        global _recovery_hits
        parsed = urlparse(self.path)
        if parsed.path == "/health":
            self._write(200, {"status": "UP"})
            return
        if parsed.path != "/secullum/events":
            self._write(404, {"error": "not_found"})
            return

        query = parse_qs(parsed.query)
        company_id = query.get("companyId", [""])[0]
        period = query.get("period", [""])[0]
        if not company_id or len(period) != 7:
            self._write(400, {"error": "companyId and period are required"})
            return
        if company_id == UNAVAILABLE_COMPANY:
            self._write(503, {"error": "secullum_unavailable"})
            return
        if company_id == RECOVERY_COMPANY:
            with _lock:
                _recovery_hits += 1
                hit = _recovery_hits
            if hit == 1:
                self._write(503, {"error": "temporary_secullum_failure"})
                return

        first_day = f"{period}-01"
        self._write(200, [
            {"eventType": "REGULAR_HOURS", "eventDate": first_day, "hours": 160, "value": 0},
            {"eventType": "OVERTIME_50", "eventDate": first_day, "hours": 8, "value": 0}
        ])

    def log_message(self, fmt, *args):
        print("secullum-stub", fmt % args, flush=True)


if __name__ == "__main__":
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
