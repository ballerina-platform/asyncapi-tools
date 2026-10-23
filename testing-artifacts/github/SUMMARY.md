# GitHub Trigger — Action-Level Verification Summary

**Scope:** All 265 remote functions across 75 service groups generated for the `github` webhook trigger.

**Method:** Attached all 75 generated service types to a single listener instance (harness in `harness/`), then sent one real HTTP POST per action — correct `X-GitHub-Event` header, valid HMAC-SHA256 signature, real payload body — and confirmed via log output that exactly the expected remote function fired. See `action_matrix.csv` for the full per-action result (columns: `Tested`, `CodegenOK`, `Issue`).

**Payload sources:**
- 154 actions: real example payloads from `octokit/webhooks` (MIT licensed), used only as local test fixtures — not redistributed.
- 111 actions: synthetic payloads generated directly from this project's own AsyncAPI schema (no third-party data), with the correct `action` value forced in per the spec's own event-identifier mapping.

## Result: 256 / 265 pass

## Confirmed bug: 9 events fail to dispatch (composite-identifier / action-field mismatch)

Root cause: the dispatcher's composite event-identifier logic appends the payload's `action` field to the event type whenever one is present (`eventIdentifier = eventType + "_" + action`). For these 9 events, GitHub's real payload **always** includes a required `action` field with a single fixed value, but `asyncapi.yml`'s `x-ballerina-event-type` for them is declared bare (no action suffix). At runtime the computed identifier never matches the generated match arm, so the request is accepted (HTTP 200/201, signature verified, JSON parsed fine) but **silently drops the event — no handler ever fires**.

| Event | Real action value | Currently expected identifier | Actually computed at runtime |
|---|---|---|---|
| `meta` | `deleted` | `meta` | `meta_deleted` |
| `deployment` | `created` | `deployment` | `deployment_created` |
| `repository_dispatch` | (user-supplied `event_type`) | `repository_dispatch` | `repository_dispatch_<event_type>` |
| `github_app_authorization` | `revoked` | `github_app_authorization` | `github_app_authorization_revoked` |
| `watch` | `started` | `watch` | `watch_started` |
| `commit_comment` | `created` | `commit_comment` | `commit_comment_created` |
| `deployment_status` | `created` | `deployment_status` | `deployment_status_created` |
| `custom_property_values` | `updated` | `custom_property_values` | `custom_property_values_updated` |
| `installation_target` | `renamed` | `installation_target` | `installation_target_renamed` |

**This is independent of the dispatcher routing fix already shipped on this branch** — it's a spec/extraction issue (the event identifier for these 9 channels should either declare their fixed action explicitly, e.g. `x-ballerina-event-type: "meta_deleted"`, or the identifier-config extraction should special-case single-fixed-action events). Reproducible with any of the 9 payload files in `payloads/` against the harness in `harness/`.

## Files in this directory
- `action_matrix.csv` — full 265-row tracking sheet (import directly into your own sheet)
- `payloads/` — one JSON payload per action (265 files)
- `payload_fetch_report.csv` — which payloads came from octokit vs synthetic
- `test_results.csv` — raw pass/fail output from the harness run
- `harness/` — the generated Ballerina test harness package (all 75 services on one listener + `harness_services.bal` logging shim)
