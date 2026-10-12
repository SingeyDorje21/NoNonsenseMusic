"""Deployment wrapper around the NoNonsenseMusic app: refuses request bodies over MAX_BODY before the app reads them.

FastAPI parses a whole JSON body before a route (and its rate limit) runs, and nothing caps its size: one 40 MB POST to
/auth/signin took the process from 85 to 197 MB (measured 11 Oct). The largest real request is about 50 listings, well
under 100 KB. Lives outside the repository so `git pull` never conflicts; lifespan and WebSocket traffic pass through.
"""
from music_backend.main import app as _app

MAX_BODY = 1_000_000


class _TooLarge(Exception):
    pass


async def _refuse(send):
    await send({"type": "http.response.start", "status": 413,
                "headers": [(b"content-type", b"application/json"), (b"connection", b"close")]})
    await send({"type": "http.response.body", "body": b'{"detail":"Request body too large"}'})


async def app(scope, receive, send):
    if scope["type"] != "http":
        return await _app(scope, receive, send)
    for name, value in scope.get("headers", []):
        if name == b"content-length":
            try:
                declared = int(value)
            except ValueError:
                declared = MAX_BODY + 1
            if declared > MAX_BODY:
                return await _refuse(send)
    received = 0
    started = False

    async def limited_receive():
        nonlocal received
        message = await receive()
        if message["type"] == "http.request":
            received += len(message.get("body", b""))
            if received > MAX_BODY:          # a chunked body with no Content-Length
                raise _TooLarge()
        return message

    async def tracked_send(message):
        nonlocal started
        if message["type"] == "http.response.start":
            started = True
        await send(message)

    try:
        await _app(scope, limited_receive, tracked_send)
    except _TooLarge:
        if not started:
            await _refuse(send)
