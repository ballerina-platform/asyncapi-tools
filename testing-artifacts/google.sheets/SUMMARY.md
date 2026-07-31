# Google Sheets Trigger — Dispatch-Verification Summary

**Scope:** Dispatch/routing logic only, for both remote functions on the `google.sheets`
trigger's single `SheetRowService` (`onAppendRow`, `onUpdateRow`), against the real,
currently-shipped listener/dispatcher code (`asyncapi-triggers/asyncapi/google.sheets/`). No
generator/codegen changes involved.

**Why this harness looks different from Calendar's/Drive's — no mock server needed at all.**
Unlike Calendar/Drive/Mail, `google.sheets` has no native Google Cloud push API and registers no
watch channel. Delivery works entirely differently: the trigger's own README instructs the sheet
owner to bind a Google Apps Script (`Extensions > Apps Script`) to the spreadsheet, which calls
`UrlFetchApp.fetch()` on every edit, POSTing the full event payload directly. The dispatcher makes
zero outbound calls of its own — it checks the payload's `spreadsheetId` against the configured
value, then dispatches straight from the same payload's `eventType` field
(`"appendRow"`/`"updateRow"`). This is the simplest trigger investigated in the Google-family
rotation so far, structurally closer to (and even simpler than) GitHub's/HubSpot's self-contained
model.

**Method:** Copied the production trigger files verbatim into `harness/` — **zero deviations**,
nothing to redirect since there are no outbound calls to point anywhere. `harness_services.bal`
attaches a `SheetRowService` that logs `FIRED::SheetRowService::<function>` per call, same pattern
as the other harnesses. The driver (`run_google_sheets_harness.ps1`) starts the harness alone (no
second mock process) and POSTs two synthetic `GSheetEvent` fixtures directly at it.

**Payload source:** both fixtures (`payloads/event_append_row.json`, `event_update_row.json`) are
synthetic, built directly from `data_types.bal`'s `GSheetEvent` field list and the Apps Script
template in the trigger's own README (which shows exactly what fields a real event payload
contains).

## Result: 2 / 2 pass

`onAppendRow` and `onUpdateRow` both fired correctly against their respective fixtures. No
dispatch/routing bugs found.

## Not a dispatch bug, but a real finding worth flagging separately

While reviewing the dispatch code, found that `google.sheets` has **no authentication at all** on
its inbound webhook. The only check performed is whether the payload's `spreadsheetId` matches the
configured value — and a spreadsheet ID is not a secret, it's visible in the sheet's own shareable
URL. Anyone who has it can POST an arbitrary, fabricated payload straight to the listener and have
it dispatched as if it were a real edit. Every other webhook-based trigger in this codebase
(GitHub, HubSpot, Slack, Shopify, Twilio, and the hand-written IdentityServer trigger) verifies an
HMAC signature before dispatching; `google.sheets` is the one exception. This is explicitly **not**
a dispatch-routing bug (routing behaves correctly given a payload) and is out of scope for this
harness's own verification goal, but it's a real gap worth its own tracking — filed separately
rather than folded into this result.

## Explicitly out of scope

- N/A for the usual registration/renewal/OAuth exclusions - this trigger has none of those
  concerns at all (no watch channel, no OAuth flow, no scheduled renewal).
- The authentication gap noted above - out of scope for dispatch-routing verification, tracked as
  its own finding.

## Files in this directory

- `action_matrix.csv` - 2-row tracking sheet
- `payloads/` - 2 synthetic `GSheetEvent` JSON fixtures
- `test_results.csv` - raw pass/fail output from the driver script
- `run_google_sheets_harness.ps1` - the driver script (build, start, drive, stop, report)
- `harness/` - the generated Ballerina harness package (production files, verbatim, zero
  deviations, plus `harness_services.bal` logging shim)
