# Google Drive Trigger — Dispatch-Verification Summary

**Scope:** Dispatch/routing logic only, for all 7 remote functions on the `google.drive`
trigger's single `DriveService` (`onFileCreate`, `onFolderCreate`, `onFileUpdate`,
`onFolderUpdate`, `onDelete`, `onFileTrash`, `onFolderTrash`), against the real, currently-shipped
listener/dispatcher code (`asyncapi-triggers/asyncapi/google.drive/`). No generator/codegen
changes involved. Whole-drive watch mode only (`watchFiles`/`mapEvents`) — specific-file/folder
watch mode is a registration-time config choice that doesn't change the dispatch logic being
tested here (see "Explicitly out of scope").

**Why this harness needed more than the `google.calendar` pattern:** Calendar's second real API
call (fetching the changed event) goes through its own local, overridable `http:Client` wrapper.
Drive's dispatcher makes its equivalent calls (`getFile`, twice per changed item — once bare, once
`fields`-restricted) through the third-party `ballerinax/googleapis.drive` connector directly,
whose base URL is hardcoded *inside the connector package itself* with no config-level override.
Pointing `constants.bal`'s `BASE_URL` at the mock (Calendar's approach) was insufficient on its
own — it redirects the watch/changes calls (which do go through this trigger's own `util.bal`
wrapper) but not the `getFile` calls.

**Method — patched local-only connector build:** rather than a network-level MITM proxy (ruled
out as disproportionate risk/complexity for this task), cloned
`ballerina-platform/module-ballerinax-googleapis.drive` into a throwaway local directory, added
one optional field (`baseUrl`, defaulting to the real `https://www.googleapis.com`) to
`ConnectionConfig`, and used it instead of the hardcoded constant in `Client.init()`. Built and
installed this patch **locally only**, under a completely different package identity —
`testorg/googleapis_drive_mockable` — via `bal pack` + `bal push --repository=local`. Never
published anywhere; no network call involved in installing it. Verified before and after that the
real `ballerinax/googleapis.drive` package cache (`~/.ballerina/repositories/central.ballerina.io/`)
was byte-for-byte unchanged (`diff` against a pre-patch baseline, exit code 0), and the patched
package lived in a fully separate repository directory (`~/.ballerina/repositories/local/`) that
nothing else on the machine resolves to. Removed after the harness run (see below) — the real
connector and the production trigger are completely unaffected by any of this.

Copied the production trigger files verbatim into `harness/`, with **three deliberate, documented
deviations**:
1. `constants.bal`'s `BASE_URL` → `http://localhost:8092` (the mock), same as Calendar's approach
   for `util.bal`'s watch/changes calls.
2. All five files that import the connector (`data_types.bal`, `dispatcher_service.bal`,
   `listener.bal`, `util.bal`, `watch_scheduler.bal`) → `import testorg/googleapis_drive_mockable
   as drive;` instead of `import ballerinax/googleapis.drive;`, so `getFile()` calls resolve
   through the patched connector.
3. `listener.bal` and `watch_scheduler.bal`'s `driveConnection` construction → added
   `baseUrl: BASE_URL`, activating the patch's override.

`harness_services.bal` attaches a `DriveService` that just logs `FIRED::DriveService::<function>`
per call, same pattern as Calendar's/GitHub's harnesses.

**Mock server** (`mock/`, port 8092) fakes: the OAuth token endpoint, `GET
.../changes/startPageToken` and `POST .../changes/watch` (watch registration), `GET .../changes`
(the changes list), `GET .../files/{fileId}` (both the bare and `fields=...`-restricted shapes
`mapEvents`/`identifyFileEvent`/`identifyFolderEvent` each call), and `POST .../channels/stop`. A
single `currentScenario` state (set by the driver via `POST /control/scenario` before each
dispatch POST, same pattern as Calendar's mock) drives both the `/changes` response and the
`fields`-restricted `/files/{id}` response consistently — `createdTime` is set relative to a fixed
`changeTime` to land on the correct side of the connector's exact 12-second
`isCreated`/`isUpdated` boundary (`util.bal`, `due <= 12s` → created, `> 12s` → updated), and
`trashed`/`removed` are set directly for the trash/delete scenarios.

**Fixtures:** synthetic, built directly from the connector's own `Change`/`File` record shapes and
the exact 12-second timing rule above — there's no open corpus of real Drive change-notification
payloads to draw from (same reasoning as Calendar: the real webhook ping carries no event data at
all).

## Result: 7 / 7 pass

All seven remote functions fired correctly against their respective scenarios. No dispatch/routing
bugs found in the production trigger code.

## Fixed: mock's watch-registration response returned HTTP 201 instead of 200

While de-risking watch registration before writing the full driver, `registerWatchChannel()` kept
retrying with no clear error. Root cause was in this harness's own `mock/mock_service.bal`, not
the production trigger: the watch endpoint responded with `check caller->respond(json);` — a bare
JSON payload on a POST — which Ballerina's `http` module defaults to HTTP 201 Created. The
connector's own `util.bal` `validateStatusCode` requires *exactly* 200, not any 2xx, so the
(valid, successful) response was being treated as a Drive API error. Same bug class as the
200-vs-201 issue found across 10 trigger packages during the Calendar harness work (#8920) — this
time surfacing in test infrastructure rather than production code. Fixed by constructing an
explicit `http:Response` with `statusCode = http:STATUS_OK`. Re-verified: registration succeeded
immediately afterward (`"Watch channel started in Google, id : test-channel-id"` in
`harness/harness_stderr.log`), and all 7 scenarios were unaffected by this fix (it's mock-only).

## Explicitly out of scope

- Watch-channel registration *correctness* against the real Google API — only verified that our
  own mock's fake registration response round-trips correctly.
- Channel renewal (`watch_scheduler.bal`'s `task:scheduleOneTimeJob` lifecycle) and the real
  domain-verification handshake (`listener.bal`'s `domainVerificationFileContent`,
  `ERR_UNAUTHORIZED_WEBHOOK_CHANNEL` retry loop) — registration-time concerns, not dispatch-routing.
- Specific-file/folder watch mode (`isWatchOnSpecificResource`/`isFolder`/
  `specificFolderOrFileId`, `watchFilesById`/`mapFileUpdateEvents`) — a registration-time config
  choice; the dispatch logic that runs afterward is identical to whole-drive mode either way.
- The real OAuth refresh-token flow — the mock's token endpoint accepts any request
  unconditionally; no real auth machinery was tested.

## Files in this directory

- `action_matrix.csv` — 7-row tracking sheet (import directly into your own sheet)
- `test_results.csv` — raw pass/fail output from the driver script
- `run_google_drive_harness.ps1` — the driver script (build, start, drive, stop, report)
- `harness/` — the generated Ballerina harness package (production files + the three deviations
  above + `harness_services.bal` logging shim)
- `mock/` — the local mock Google Drive API server

## Note on the patched connector

`testorg/googleapis_drive_mockable` (the locally-patched, never-published connector build used by
`harness/`) was removed from the local Ballerina package cache after this harness run completed,
along with the throwaway clone it was built from. `harness/Ballerina.toml`'s `[[dependency]]`
entry pointing at it is left in place for reproducibility — re-running this harness from scratch
requires re-doing the clone/patch/`bal push --repository=local` steps described above before
`bal build` will succeed again.
