# Gap Reclassification Report

## 1. Review Scope

This review reclassifies the existing design–implementation gaps for the three contract-facing topics explicitly prioritized in the prompt:

1. API path / method divergence
2. Request / response payload divergence
3. Error code / HTTP status divergence

It does **not** assume the old gap report is correct, and it does **not** treat current implementation as automatically authoritative. Each reclassification below is based on:

- the existing gap inventory in `design-implementation-gap.md`
- accepted decisions in `docs/01-requirements/decision-log.md`
- relevant design / contract authority
- current controller / DTO / exception-handler code
- existing API test coverage as repository evidence

## 2. Authority Sources Used

### Existing gap inventory

- `design-implementation-gap.md` — GAP-04, GAP-05, GAP-06 inventory and original conclusions

### Decision authority

- `docs/01-requirements/decision-log.md`
  - accepted decisions are authoritative when they exist
  - no accepted decision was found that explicitly resolves the current API path/method split for initial-review / rereview submit/revise endpoints

### Domain / design / contract authority

- `docs/03-contracts/api-spec.md`
  - error status semantics: `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404`, `409`, `422` (`api-spec.md:102-106`)
  - command path design principle (`api-spec.md:182-188`)
  - initial review APIs (`api-spec.md:763-885`)
  - rereview APIs (`api-spec.md:896-944`)
- `docs/02-ddd/application-service.md`
  - documents submit/revise use-case inputs, including `attachments`, and describes revise commands as submission-oriented (`application-service.md:199, 238-239`)
- `docs/02-ddd/permission-matrix.md`
  - role / scope semantics and 403-style authorization boundary expectations
- `docs/02-ddd/state-machine.md`
  - terminal-state and revision rules used for context when judging revise endpoints
- `docs/04-testing/specification-by-example.md`
  - examples continue to reference `POST /review-submissions/{submissionId}/initial-review/submit`
  - examples continue to reference `POST /review-submissions/{submissionId}/initial-review/revise`
  - examples continue to reference `POST /review-submissions/{submissionId}/rereview/submit`
  - examples continue to reference `POST /review-submissions/{submissionId}/rereview/revise`
- `docs/04-testing/test-scenarios.md`
  - revision scenarios continue to reference submission-rooted revise paths
  - unauthorized / forbidden / conflict semantics are explicitly part of test design

### Repository implementation evidence

- Controllers
  - `ReviewCaseController.java`
  - `SubmissionController.java`
  - `ScoreSheetController.java`
- Error mapping
  - `GlobalExceptionHandler.java`
  - `ApiKeyAuthFilter.java`
- DTOs
  - `SaveInitialReviewDraftRequest.java`
  - `SaveRereviewDraftRequest.java`
  - `SubmitRereviewRequest.java`
  - `ReviseRereviewRequest.java`
  - `SaveDraftResponse.java`
  - `StartInitialReviewResponse.java`
  - `RevisionResultResponse.java`
- API tests
  - `InitialReviewApiTest.java`
  - `RereviewApiTest.java`
  - `RevisionApiTest.java`
  - `ReviewCaseApiTest.java`
  - `SamplingControllerApiTest.java`
  - `SubmissionQueryApiTest.java`

## 3. Gap Inventory

| gap id | title / topic | current classification | recommended action | target artifact | priority |
|---|---|---|---|---|---|
| GAP-04 | API Path | `partially_aligned` | `update_code` or `update_spec` | `api-spec.md` or controllers | medium |
| GAP-05 | Request/Response | `conflict` | `update_code` | request / response DTOs | medium-high |
| GAP-06 | Error Code | `conflict` | `update_code` + `update_spec` | `GlobalExceptionHandler.java`, `api-spec.md` | high |

## 4. Reclassification Result Summary

### 4.1 Acceptable Divergences

None in this focused pass.

### 4.2 Unacceptable Divergences

- **GAP-05 Request / Response Payload divergence**
- **GAP-06 Error code / HTTP status divergence**

### 4.3 Decision-Required Divergences

- **GAP-04 API path / method divergence**

## 5. Detailed Gap Analysis

### GAP-04 [API path / method divergence]

- Original classification: `partially_aligned`
- Related authority sources:
  - `design-implementation-gap.md:198-215`
  - `docs/03-contracts/api-spec.md:772-885, 905-944`
  - `docs/02-ddd/application-service.md:199, 238-239`
  - `docs/04-testing/specification-by-example.md:129, 187, 244, 304, 353, 394`
  - `docs/04-testing/test-scenarios.md:579, 619`
- Related implementation evidence:
  - `ReviewCaseController.java:191-241`
  - `ReviewCaseController.java:306-387`
  - `SubmissionController.java:11-25`
  - `InitialReviewApiTest.java:199-357`
  - `RereviewApiTest.java:273-316`
  - `RevisionApiTest.java:168-288`
- Current repo behavior:
  - Initial-review draft is implemented at `POST /review-cases/{caseId}/initial-review/save-draft`, not spec `POST /review-cases/{caseId}/initial-review/draft`.
  - Initial-review submit is implemented at `POST /review-cases/{caseId}/initial-review/submit`, not spec `POST /review-submissions/{submissionId}/initial-review/submit`.
  - Initial-review revise is implemented at `POST /review-cases/{caseId}/submissions/{submissionId}/revise`, not spec `POST /review-submissions/{submissionId}/initial-review/revise`.
  - Rereview draft is implemented at `PUT /review-cases/{caseId}/rereview-draft`, not spec `POST /review-cases/{caseId}/rereview/draft`.
  - Rereview submit is implemented at `POST /review-cases/{caseId}/submit-rereview`, not spec `POST /review-submissions/{submissionId}/rereview/submit`.
  - Rereview revise is implemented at `POST /review-cases/{caseId}/revise-rereview`, not spec `POST /review-submissions/{submissionId}/rereview/revise`.
  - Existing API tests reinforce the implemented paths, not the spec paths.
- Test evidence:
  - `InitialReviewApiTest` explicitly calls `/start-initial-review`, `/initial-review/save-draft`, `/initial-review/submit`.
  - `RevisionApiTest` explicitly calls `/review-cases/{caseId}/submissions/{submissionId}/revise`.
  - `RereviewApiTest` explicitly calls `/rereview-draft`, `/submit-rereview`, `/revise-rereview`.
  - No contract tests assert the spec-defined submission-rooted submit/revise paths.
- Risk assessment:
  - This is frontend / integration contract surface.
  - Spec, examples, scenarios, and implementation are split.
  - Tests show the implementation is stable enough to be repo evidence, but they do **not** resolve which contract should be the long-term truth.
  - For contract-facing paths, accepting one side without a decision would create silent break risk for frontend and downstream integration.
- Can current implementation be accepted as repo truth: pending decision
- Reclassified as:
  - decision required
- Recommended minimal action:
  - raise_decision
  - then either `update_spec` or `update_code`
  - add_contract_test after the decision is made
- Why:
  - There is clear implementation evidence and clear design/spec evidence on both sides.
  - No accepted decision log entry was found that resolves the path model.
  - This matches the prompt’s default rule: API path / method design divergence should default to **decision required** when authority cannot unilaterally resolve it.

### GAP-05 [request / response payload divergence]

- Original classification: `conflict`
- Related authority sources:
  - `design-implementation-gap.md:219-234`
  - `docs/03-contracts/api-spec.md:781-810, 824-885, 925-944`
  - `docs/02-ddd/application-service.md:199, 238-239`
  - `docs/04-testing/specification-by-example.md:689, 1374, 1688`
  - `docs/04-testing/test-scenarios.md:802-824, 969-971`
- Related implementation evidence:
  - `SaveInitialReviewDraftRequest.java:9-31`
  - `SaveRereviewDraftRequest.java:7-21`
  - `SubmitRereviewRequest.java:6-17`
  - `ReviseRereviewRequest.java:6-17`
  - `SaveDraftResponse.java:5-20`
  - `RevisionResultResponse.java:7-29`
  - `ReviewCaseController.java:201-240`
  - `ReviewCaseController.java:316-371`
  - `InitialReviewApiTest.java:241-253, 294-296`
  - `RereviewApiTest.java:273-290`
  - `RevisionApiTest.java:172-177`
- Current repo behavior:
  - Draft request DTOs do **not** expose `attachments`, while the spec shows `attachments` in draft / revise payloads.
  - Initial-review draft response returns only `totalScore` via `SaveDraftResponse`, while the spec shows `submissionId`, `status`, and `stage`.
  - Initial-review submit returns `ApiResponse.ok(null)`, while the spec defines a structured response containing `submissionId`, `status`, `caseId`, `caseStatus`, `totalScore`, and `rereviewRequired`.
  - Submit rereview also returns `ApiResponse.ok(null)`, while the spec defines `submissionId`, `status`, `caseId`, `caseStatus`, and `finalScore`.
  - `RevisionResultResponse` returns `newSubmissionId`, `revisionNo`, `totalScore`, `status`, but omits spec fields like `oldSubmissionId`, `stage`, and `caseId`.
- Test evidence:
  - Tests support the current minimal responses:
    - `InitialReviewApiTest` asserts only `$.data.totalScore` for save draft.
    - `InitialReviewApiTest` submit flow asserts success/HTTP status only, not the spec-defined payload.
    - `RereviewApiTest` submit / revise flows assert success only, not the spec-defined payload.
    - `RevisionApiTest` asserts `newSubmissionId`, `revisionNo`, `status`, `totalScore`, but does not assert `oldSubmissionId`.
  - There are no contract tests enforcing the richer spec payloads.
- Risk assessment:
  - This is not just naming drift. It changes request/response schema on frontend-facing endpoints.
  - `attachments` omission materially affects traceability and revision-related evidence handling.
  - Returning `null` where the contract specifies structured data makes frontend integration ambiguous and invites ad-hoc follow-up queries.
  - The current implementation does not violate a separate accepted decision log entry that clearly mandates a smaller payload, and the explicit API contract is richer than the implementation.
- Can current implementation be accepted as repo truth: no
- Reclassified as:
  - unacceptable divergence
- Recommended minimal action:
  - update_code
  - add_contract_test
- Why:
  - The spec gives explicit payload shapes.
  - The implementation returns materially different schemas, including `null` instead of defined response objects.
  - For frontend / integration contracts, the prompt explicitly warns not to normalize null responses merely because the controller currently returns them.

### GAP-06 [error code / HTTP status divergence]

- Original classification: `conflict`
- Related authority sources:
  - `design-implementation-gap.md:238-250`
  - `docs/03-contracts/api-spec.md:102-106`
  - `docs/02-ddd/permission-matrix.md` (authorization boundary semantics)
  - `ApiKeyAuthFilter.java:15-18`
- Related implementation evidence:
  - `GlobalExceptionHandler.java:36-67`
  - `ActorContext.java:29-45`
  - `ApiKeyAuthFilter.java:37-58`
  - `SamplingControllerApiTest.java:113-118`
  - `InitialReviewApiTest.java:207-220, 355-357`
  - `ReviewCaseApiTest.java:266-269`
- Current repo behavior:
  - Missing / invalid `api-key` is handled in `ApiKeyAuthFilter` as HTTP `401`.
  - Role failure (`ActorContext.requireRole()` / `requireAnyRole()`) throws `UnauthorizedException`.
  - `GlobalExceptionHandler` maps `UnauthorizedException` to HTTP `403` while still emitting error code `UNAUTHORIZED`.
  - `MethodArgumentNotValidException` maps to HTTP `400` with error code `VALIDATION_ERROR`, but `VALIDATION_ERROR` is not listed in the API spec error-code table.
- Test evidence:
  - Tests already distinguish unauthenticated from authenticated-but-disallowed:
    - no `api-key` → `401` in `InitialReviewApiTest` and `ReviewCaseApiTest`
    - insufficient role → `403` in multiple API tests
    - `SamplingControllerApiTest` explicitly expects `403` with error code `UNAUTHORIZED`
  - Existing tests therefore reinforce the current split behavior, including the 403 + `UNAUTHORIZED` combination.
- Risk assessment:
  - The `401` vs `403` semantic split is security / contract sensitive.
  - The current implementation is internally inconsistent in naming: `UnauthorizedException` maps to 403 while the API spec reserves 401 for `UNAUTHORIZED`.
  - Even though tests currently support the implementation, this is still a contract mismatch on a high-risk surface.
  - `VALIDATION_ERROR` is lower risk than the 401/403 mismatch, but still a spec / runtime mismatch.
- Can current implementation be accepted as repo truth: no
- Reclassified as:
  - unacceptable divergence
- Recommended minimal action:
  - update_code for `UnauthorizedException` / naming / status alignment
  - update_spec only after the runtime semantics are settled
  - add_contract_test for unauthenticated vs forbidden cases
- Why:
  - The prompt explicitly says security / scope isolation and HTTP 401/403 semantics default toward **unacceptable divergence**.
  - There is explicit contract text in `api-spec.md` and explicit runtime behavior in `GlobalExceptionHandler.java`; they do not match.
  - This should not be normalized merely because current tests were written against the current implementation.

## 6. High-Risk First Fix Candidates

1. **GAP-06 Error code / HTTP status divergence**
   - High-risk because it affects security semantics and external contract meaning.
   - `401` vs `403` confusion is easy for frontend and gateway integration to mis-handle.

2. **GAP-05 Request / response payload divergence**
   - High-risk because frontend integration can be blocked or forced into undocumented follow-up behavior.
   - `attachments` omission and `null` submit responses weaken traceability and contract stability.

3. **GAP-04 API path / method divergence**
   - High-risk for integration stability, but should be resolved through a decision first rather than ad-hoc code or doc edits.

## 7. Gaps That Should Not Block Frontend Progress

- **GAP-04** should not block all frontend progress immediately **if** frontend work is explicitly aligned to the currently implemented endpoints within the same repo.
- However, GAP-04 **does block stable external contract commitment** and should block any claim that the current API spec and implementation are already aligned.

By contrast:

- **GAP-05** can directly block frontend work that needs spec-trusted response shapes.
- **GAP-06** can directly block reliable error-handling UX and integration semantics.

## 8. Decision Topics Requiring Human Confirmation

1. **Review workflow API resource model**
   - Option A: keep submission-rooted submit/revise endpoints from `api-spec.md`
   - Option B: adopt the implemented case-rooted command endpoints
   - Option C: support both temporarily, then deprecate one

2. **Rereview draft HTTP semantics**
   - Option A: spec-style `POST /review-cases/{caseId}/rereview/draft`
   - Option B: implementation-style `PUT /review-cases/{caseId}/rereview-draft`

These are decision topics because the current repo contains stable evidence on both the documentation and implementation sides, and this is frontend / integration contract surface.

## 9. Minimal Next-Step Plan

1. **Raise one decision topic for GAP-04**
   - decide the canonical resource model and path/method set for submit/revise/draft endpoints

2. **Fix GAP-06 next**
   - align unauthorized / forbidden runtime semantics with the chosen contract
   - add focused contract tests for unauthenticated vs forbidden responses

3. **Fix GAP-05 after or alongside GAP-06**
   - align request/response DTOs with the intended contract
   - add contract tests for submit/revise payload shape and non-null response bodies

4. **Then sync spec and tests**
   - after decisions / code fixes, update `api-spec.md`
   - update examples / scenarios only where the chosen contract differs
