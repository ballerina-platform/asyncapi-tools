# Google Mail Trigger — Dispatch-Verification Summary

**Scope:** Dispatch/routing logic only, for all 7 remote functions on the `google.mail` trigger's
single `GmailService`, against the real, currently-shipped listener/dispatcher/connector code
(`asyncapi-triggers/asyncapi/google.mail/` on `ballerinax/googleapis.gmail` 4.2.0, the exact
version pinned in production `Dependencies.toml`). No generator/codegen changes involved.

## Method

Unlike `google.drive`, no connector patching was needed. The real `ballerinax/googleapis.gmail`
connector's `Client.init()` already exposes a `serviceUrl` parameter with a real-URL default -
`util.bal`'s `readMessage`/`readThread`/`listHistory` just weren't passing it. The harness's copy
of `util.bal` adds one deviation: a `MOCK_GMAIL_SERVICE_URL` constant passed as the second argument
to the three `gmail:Client` constructions, pointing dispatch-critical connector calls at the local
mock. `constants.bal` gets the same kind of deviation for `PUBSUB_BASE_URL`/`GMAIL_BASE_URL` (the
two locally-constructed `http:Client`s used for Pub/Sub provisioning and Gmail watch/stop, both
otherwise hardcoded with no override). Every other file (`data_types.bal`, `dispatcher_service.bal`,
`listener.bal`, `service_types.bal`, `watch_scheduler.bal`) is copied verbatim.

The mock (`mock/mock_service.bal`) fakes 9 endpoints: OAuth token exchange, the 4-call Pub/Sub
provisioning sequence (topic create, get/set IAM policy, subscription create), Gmail
`watch`/`stop`, and the 3 dispatch-critical connector calls (`history`, `messages/{id}`,
`threads/{id}`). The Pub/Sub subscription-create response always echoes a **fixed** resource name
regardless of the uuid-generated name the trigger actually requests, since that uuid isn't
knowable in advance - the driver uses this fixed value as the `subscription` field of its
simulated Pub/Sub push. A `/control/scenario` test-only endpoint selects which fixture the next
`history`/`messages`/`threads` calls return.

7 scenarios were driven (`payloads/history_scenario{1-7}.json` + matching `message_scenario*.json`,
plus `thread_scenario2.json`), covering every remote function and the multi-function fan-out cases
identified by reading `dispatch()` directly: a new inbox message always fires `onNewEmail`, plus
`onNewThread` if it's the first message in its thread, plus `onNewAttachment` if it has one; a
`STARRED` label add/remove fires both the generic and the starred-specific event from the same
history entry.

## Result: 6 / 11 checks pass (4 of 7 remote functions work; 3 never fire)

| Scenario | Expected | Result |
|---|---|---|
| 1 - new email, no thread/attachment | `onNewEmail` | **FAIL** |
| 2 - new email, starts a thread | `onNewEmail`, `onNewThread` | **FAIL** (both) |
| 3 - new email with attachment | `onNewEmail`, `onNewAttachment` | **FAIL** (both) |
| 4 - non-starred label added | `onEmailLabelAdded` | PASS |
| 5 - starred label added | `onEmailLabelAdded`, `onEmailStarred` | PASS (both) |
| 6 - non-starred label removed | `onEmailLabelRemoved` | PASS |
| 7 - starred label removed | `onEmailLabelRemoved`, `onEmailStarRemoved` | PASS (both) |

## Root cause: a real production bug, not a mock artifact

`dispatcher_service.bal:136` gates all new-message handling on the embedded message's own
`labelIds` field:

```ballerina
gmail:Message? msg = newMessage.message;
if msg is gmail:Message && msg.labelIds is string[] {   // <- always false
    foreach var labelId in <string[]>msg.labelIds {
        match labelId {
            INBOX => {
                check self.dispatchNewMessage(newMessage);
                check self.dispatchNewThread(newMessage);
            }
        }
    }
}
```

But `ballerinax/googleapis.gmail`'s own `convertOASListHistoryResponseToListHistoryResponse`
(`data_mappings.bal:416-423`) unconditionally strips every `messagesAdded[].message` down to just
`id` and `threadId` before the trigger ever sees it:

```ballerina
HistoryMessageAdded[] processedMessageAddedMessages = from oas:HistoryMessageAdded msg in history.messagesAdded ?: []
    select {
        // list response does not return any other info.
        message: {
            threadId: msg.message?.threadId ?: EMPTY_STRING,
            id: msg.message?.id ?: EMPTY_STRING
        }
    };
```

`labelIds` is never populated on that object - so `msg.labelIds is string[]` can never be `true`,
`dispatchNewMessage`/`dispatchNewThread` are never called, and `dispatchNewAttachment` (only
reachable from inside `dispatchNewMessage`) is never called either. **A new email arriving in the
inbox is silently dropped by the trigger in production, every time**, regardless of what a real
user does. This reproduces against the exact connector version (4.2.0) pinned in production, so
it isn't a version-skew artifact of the harness.

By contrast, `onEmailLabelAdded`/`onEmailStarred`/`onEmailLabelRemoved`/`onEmailStarRemoved` check
a *different* field - the outer `labelIds` on `HistoryLabelAdded`/`HistoryLabelRemoved` itself
(the label-change list, not the embedded message's current labels) - which the same mapping
function does preserve correctly. That's why those 4 pass cleanly.

This is a dispatcher-vs-connector field-shape mismatch, not a bug this harness's own mock or
fixtures could paper over: the mock was intentionally built to match the connector's documented
`Message`/`History` types (see `googleapis.gmail.oas/types.bal`), and even with fully realistic
`labelIds` present in the raw JSON the mock returns, the connector's own conversion step discards
it before the trigger's dispatch logic ever runs. Tracked as its own finding rather than folded
into a routing-bug fix, since the fix boundary (dispatcher logic vs. connector mapping vs. filing
upstream against `ballerinax/googleapis.gmail`) needs its own decision.

## Explicitly out of scope

- Pub/Sub topic/subscription/IAM correctness and watch renewal (`INTERVAL_TO_WATCH` = 1 day,
  `watch_scheduler.bal`) - registration/lifecycle concerns, not dispatch-routing, and never fire
  in a short test run anyway.
- The real OAuth flow - the mock's token endpoint accepts any refresh attempt unconditionally.
- Whether Gmail's real History API actually would have populated `labelIds` on `messagesAdded`
  entries if the connector hadn't stripped it - not something a harness against the connector's
  public API surface can observe either way. The bug is real regardless of which layer "should"
  have carried the field, since the trigger only ever sees what the connector hands it.

## Files in this directory

- `action_matrix.csv` - 7-row tracking sheet (4 `CodegenOK=Yes`, 3 `CodegenOK=No` with the root
  cause noted per row)
- `payloads/` - 7 `History` fixtures, 7 `Message` fixtures, 1 `MailThread` fixture (only scenario
  2 reaches `readThread`)
- `test_results.csv` - raw pass/fail output from the driver script (11 rows - some scenarios check
  more than one expected `FIRED::` line)
- `run_google_mail_harness.ps1` - the driver script (build, start mock + harness, drive all 7
  scenarios, stop, report)
- `harness/` - the generated Ballerina harness package (production files verbatim except
  `constants.bal` and `util.bal`, both documented inline, plus `harness_services.bal` logging shim)
- `mock/` - the local mock Pub/Sub + Gmail API server (`mock_service.bal`)
