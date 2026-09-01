# Google Calendar Trigger — Dispatch-Verification Summary

**Scope:** Dispatch/routing logic only, for all 3 remote functions on the `google.calendar`
trigger's single `CalendarService` (`onNewEvent`, `onEventUpdate`, `onEventDelete`), against the
real, currently-shipped listener/dispatcher code
(`asyncapi-triggers/asyncapi/google.calendar/`). No generator/codegen changes involved.

**Why this harness looks different from GitHub's/HubSpot's:** this trigger doesn't verify a
static secret against an inbound payload. It registers a "watch channel" with Google's real
Calendar API on startup, validates inbound pings by header (`X-Goog-Channel-ID`/
`X-Goog-Resource-ID`), then makes a *second* real call back to Google to fetch the actual
changed event and decides which remote function to call by comparing timestamps on whatever
comes back. None of that is testable with a self-contained "sign and POST" approach like GitHub's.

**Method:** Built a local mock Google API server (`mock/`, port 8091) faking the OAuth token
refresh endpoint, `POST .../events/watch`, `GET .../events`, and `POST .../channels/stop`. Copied
the production trigger files verbatim into `harness/` with **one deliberate, documented
deviation**: `BASE_URL` in `constants.bal` points at the local mock
(`http://localhost:8091`) instead of `https://www.googleapis.com`, since that constant is
otherwise hardcoded with no config-based way to redirect it. `harness_services.bal` attaches a
`CalendarService` that just logs `FIRED::CalendarService::<function>` per call, same pattern as
GitHub's harness. The driver (`run_google_calendar_harness.ps1`) starts both processes, and for
each of the 3 scenarios: tells the mock which synthetic event fixture to serve next, POSTs a
"something changed" ping to the harness with the known channel/resource ID headers the mock's
watch-registration response established, and checks the harness log for the expected `FIRED::`
line.

**Payload source:** all 3 fixtures (`payloads/event_response_*.json`) are synthetic, built
directly from `ballerinax/googleapis.calendar`'s own published `EventResponse`/`Event`/`Time`
record definitions (v3.2.0, the exact version this trigger depends on) — there's no
GitHub-style open corpus of real Calendar webhook payloads to draw from, since the real webhook
body carries no event data at all.

## Result: 3 / 3 pass

`onNewEvent`, `onEventUpdate`, and `onEventDelete` all fired correctly against their respective
fixtures (new event: `created` == `updated`; updated event: `created` != `updated`; deleted
event: missing `created`/`updated`/`start`/`end`, matching a real Calendar API cancellation
entry). No dispatch/routing bugs found.

## Fixed: invalid-request path returned HTTP 201 instead of 200

While probing the invalid-request path (wrong channel/resource ID headers) during manual
de-risking, the rejection response came back as HTTP 201 instead of 200. Root cause:
`dispatcher_service.bal`'s rejection branch called `check caller->respond(http:STATUS_OK);` —
passing the bare int constant `http:STATUS_OK` gets treated by Ballerina as a JSON payload, not
a status code, and `ballerina/http`'s `buildResponse()` defaults POST responses built this way
to 201 Created. Same bug class found across 10 of the 15 trigger packages (tracked separately);
fixed here by responding with a properly-constructed `http:Response` instead. Verified directly:
the invalid-request path now returns `200`, confirmed via a direct request against the running
harness with mismatched channel/resource ID headers. The 3 dispatch scenarios above were
re-verified afterward with no regressions.

## Explicitly out of scope

- Watch-channel registration *correctness* against the real Google API — only verified that our
  own mock's fake registration response round-trips correctly.
- Channel renewal (`watch_scheduler.bal`'s `task:scheduleOneTimeJob` lifecycle) — a time-based
  concern a short-lived dispatch test can't exercise.
- The real OAuth refresh-token flow — the mock's token endpoint accepts any request
  unconditionally; no real auth machinery was tested.

## Files in this directory

- `action_matrix.csv` — 3-row tracking sheet (import directly into your own sheet)
- `payloads/` — one synthetic `EventResponse` JSON fixture per scenario (3 files)
- `test_results.csv` — raw pass/fail output from the driver script
- `run_google_calendar_harness.ps1` — the driver script (build, start, drive, stop, report)
- `harness/` — the generated Ballerina harness package (production files + one `BASE_URL`
  deviation + `harness_services.bal` logging shim)
- `mock/` — the local mock Google API server
