# Gap Reclassification Strengthening Pass

## 1. Scope of This Pass

This pass does **not** rerun the full gap reclassification. It strengthens the existing `docs/review/gap-reclassification-report.md` only in the four requested areas:

1. add reusable **Repo Truth Acceptance Criteria**
2. refine **GAP-06** into more precise sub-problems
3. add stronger downstream / design-supporting evidence for **GAP-05**
4. add a formal decision options table for **GAP-04**

All observations below are limited to:

- `design-implementation-gap.md`
- `docs/01-requirements/decision-log.md`
- `docs/03-contracts/api-spec.md`
- `docs/02-ddd/application-service.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/state-machine.md`
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`
- current controllers / DTOs / exception handling / security filter / API tests
- frontend / downstream dependency search **within this repo only**

## 2. Repo Truth Acceptance Criteria

The original report used the line:

- `Can current implementation be accepted as repo truth: yes / no / pending decision`

This pass makes that judgment explicit.

### 2.1 Can be accepted as repo truth

Current implementation can be accepted as repo truth **only if all of the following are true**:

1. **No accepted-decision conflict**
   - it does not violate `decision-log.md`
   - it does not violate already-accepted security, terminal-state, or traceability decisions

2. **No hard contract break**
   - either there is no formal external contract yet
   - or the current implementation still satisfies the formal contract in all material ways

3. **Stable repo evidence exists**
   - behavior is visible in actual controllers / handlers / DTOs / domain logic
   - and is reinforced by tests or repeated implementation patterns

4. **Design authority is weaker than runtime reality**
   - docs are clearly lagging, partial, internally inconsistent, or still tactical
   - updating docs is lower risk than changing code

5. **Security / compatibility risk stays low**
   - accepting runtime behavior would not weaken scope isolation, auth semantics, backward compatibility, or auditability

### 2.2 Cannot be accepted as repo truth

Current implementation must **not** be accepted as repo truth when any of the following is true:

1. it conflicts with an accepted decision
2. it weakens security / scope / ownership semantics
3. it breaks a formal API request/response/error contract
4. it creates traceability or backward-compatibility risk
5. tests only prove the implementation exists, but do not justify overriding the higher-weight authority

### 2.3 Must go to human decision first

Human decision is required before accepting runtime behavior as repo truth when:

1. both spec and implementation are stable enough to matter
2. no accepted decision unambiguously resolves the conflict
3. the divergence affects API path model, payload model, persistence strategy, or deprecation / compatibility strategy
4. unilaterally picking code or spec would create avoidable frontend / integration churn

### 2.4 Weighting model

Use the following weighting when deciding repo truth acceptance:

| Factor | Weight | How to use it |
|---|---|---|
| Accepted decisions | Highest hard gate | Cannot be silently overridden by implementation |
| Security / scope boundary | Highest hard gate | Default to reject runtime drift unless evidence clearly supports it |
| External API / contract stability | Very high | Contract-facing mismatch usually requires rejection or human decision |
| Stable runtime implementation | High evidence, not absolute authority | Shows what the repo currently does; does not automatically settle what it should do |
| Tests | Supporting evidence only | Show what behavior is exercised; do not by themselves justify spec override |
| Lower-priority design lag | Lowest | May be updated to match runtime if no higher-weight rule is violated |

## 3. GAP-06 Refinement

The original report treated GAP-06 as one block. It is more precise to split it into:

- **GAP-06A**: HTTP status semantic mismatch
- **GAP-06B**: error code / exception naming mismatch

### 3.1 GAP-06A HTTP Status Semantic Mismatch

- Authority source:
  - `api-spec.md:102-106`
  - `permission-matrix.md` authorization boundary semantics
  - `ApiKeyAuthFilter.java:37-58`
  - `GlobalExceptionHandler.java:36-45`
- Runtime current state:
  - missing / invalid `api-key` returns `401` in `ApiKeyAuthFilter`
  - role failure throws `UnauthorizedException`
  - `GlobalExceptionHandler` maps `UnauthorizedException` to `403`
- Test support:
  - `InitialReviewApiTest.java:355-357` and `ReviewCaseApiTest.java:267-269` support `401` for no `api-key`
  - `InitialReviewApiTest.java:207-220` supports `403` for wrong role / wrong owner
  - `SamplingControllerApiTest.java:113-118` supports `403` with error code `UNAUTHORIZED`
- Can current implementation be accepted as repo truth: no
- Reclassified as:
  - unacceptable divergence
- Recommended minimal action:
  - update_code
  - add_contract_test
- Why:
  - the runtime is currently using **correctly split statuses** at a high level (`401` unauthenticated, `403` forbidden), but it reaches that split through inconsistent semantics:
    - auth filter uses `401`
    - `UnauthorizedException` uses `403`
  - because this is auth / contract surface, the mismatch should be corrected rather than normalized

### 3.2 GAP-06B Error Code / Exception Naming Mismatch

This sub-area is **not one uniform issue**. It contains two different problems with different treatments.

#### 3.2.1 `UnauthorizedException` naming vs `403` semantics

- Authority source:
  - `api-spec.md:102-103`
  - `ActorContext.java:29-45`
  - `GlobalExceptionHandler.java:36-45`
- Runtime current state:
  - role failure path throws `UnauthorizedException`
  - exception handler returns `403`
  - error payload code is still `UNAUTHORIZED`
- Test support:
  - role-failure tests support the current runtime behavior
  - no tests justify the naming choice itself
- Can current implementation be accepted as repo truth: no
- Reclassified as:
  - unacceptable divergence
- Recommended minimal action:
  - update_code
- Why:
  - the current name/code pair is semantically misleading against the runtime’s own forbidden behavior and the spec’s `401/403` table
  - this is not only doc lag; it is a runtime naming / contract meaning mismatch

#### 3.2.2 `VALIDATION_ERROR` runtime code vs spec omission

- Authority source:
  - `GlobalExceptionHandler.java:60-67`
  - `api-spec.md:85-127`
- Runtime current state:
  - bean validation failures return HTTP `400` with code `VALIDATION_ERROR`
- Test support:
  - multiple API tests verify `400 BAD_REQUEST`
  - no test was found in this repo that explicitly asserts the `VALIDATION_ERROR` code string itself on a validation failure path
- Can current implementation be accepted as repo truth: yes, but only as runtime behavior pending spec sync
- Reclassified as:
  - acceptable divergence
- Recommended minimal action:
  - update_spec
  - optionally add_contract_test if `VALIDATION_ERROR` should become stable contract surface
- Why:
  - this issue is narrower than the 401/403 mismatch
  - it does not by itself weaken auth or scope isolation
  - current runtime behavior is stable enough, but the spec table is lagging
  - this should **not** be used to justify leaving GAP-06A unresolved

## 4. GAP-05 Evidence Strengthening

### 4.1 Downstream Evidence

#### A. Frontend code dependency

- Search scope: `frontends/quality-inspection-frontend`
- Searched for:
  - implemented endpoint strings
  - spec endpoint strings
  - payload fields such as `attachments`, `oldSubmissionId`, `rereviewRequired`, `caseStatus`
- Result:
  - **not found in this repo**

This means:

- there is currently no repo-local frontend evidence proving the frontend already depends on either the minimal implemented payloads or the richer spec payloads

#### B. API test dependency

- Found in this repo:
  - `InitialReviewApiTest` intentionally asserts only minimal response shape for save-draft (`$.data.totalScore`)
  - `InitialReviewApiTest` submit path checks success / status but not the spec-defined structured object
  - `RereviewApiTest` submit and revise paths check success only
  - `RevisionApiTest` asserts a partial revision result (`newSubmissionId`, `revisionNo`, `status`, `totalScore`) but does not assert `oldSubmissionId`

This means:

- repo-local API tests currently support the implemented minimal payloads
- but they do **not** prove that the richer payloads are unnecessary
- they also do **not** function as contract tests against the formal spec

#### C. Other downstream / integration / mock evidence

- Search result:
  - **not found in this repo**

No mock server, downstream module, or other integration artifact was found in this repo that explicitly depends on the spec-defined richer payloads.

#### D. Documentation examples as contract evidence

- Found:
  - `api-spec.md` explicitly defines richer payloads
  - `specification-by-example.md` and `test-scenarios.md` reinforce attachments / query-shape expectations
- Conclusion:
  - these are **design authority evidence**
  - they are **not the same thing** as proven downstream consumer dependency

### 4.2 Design-Supporting Evidence

#### `attachments` in draft / revise payload

- Supported by design authority beyond `api-spec.md`:
  - `application-service.md:154, 199, 238-239` — use-case signatures include `attachments`
  - `specification-by-example.md:1688-1724` — revision attachment isolation is explicitly modeled
  - `test-scenarios.md:802-824, 1180-1229` — detail responses and attachment isolation scenarios explicitly expect attachment metadata
- Strength:
  - **well supported by design authority**

#### structured submit response instead of `null`

- Supported by design authority:
  - `api-spec.md:824-837, 925-936`
- Additional design-supporting evidence:
  - `specification-by-example.md:160-165` and `test-scenarios.md:457-459` explicitly model post-submit business outcomes such as `caseStatus` and `rereviewRequired`
  - `application-service.md:155, 200` confirms submit use cases operate on both case and submission context
- Strength:
  - **partially supported beyond api-spec**
  - business outcome fields are supported
  - exact response envelope shape is still primarily contract-defined in `api-spec.md`

#### `oldSubmissionId` in revise response

- Supported by design authority:
  - `api-spec.md:879-883`
- Additional mandatory-authority support found in this pass:
  - none in the required authority set
- Strength:
  - **supported only by design authority / api-spec in this pass**

#### `stage` / `caseId` / `rereviewRequired`

- `rereviewRequired`
  - supported by `specification-by-example.md:165`
  - supported by `test-scenarios.md:457-459`
  - also appears in domain design (`domain-model.md`), but that file was not required for this strengthening pass
- `caseId`
  - business significance is broadly established across use-case and query design
  - but the exact requirement to echo `caseId` in submit / revise response remains primarily `api-spec`-driven in this pass
- `stage`
  - strongly relevant to revise correctness in `specification-by-example.md:1056-1064` and `test-scenarios.md:1782-1794`
  - but the exact requirement to return `stage` in the revise response body remains primarily `api-spec`-driven in this pass

### 4.3 Refined Conclusion

The original reclassification of GAP-05 as **unacceptable divergence** remains correct, but is now better supported:

1. **Downstream dependency evidence is limited**
   - frontend dependency on either payload shape was **not found in this repo**
   - API tests do support the current minimal implementation, but only as runtime evidence, not as stable external contract proof

2. **The richer payload is not purely “spec idealization”**
   - attachments are reinforced by application-service and testing authority
   - `rereviewRequired` and post-submit outcome fields are reinforced by examples / scenarios
   - `oldSubmissionId` is less strongly supported and is mostly contract-defined in `api-spec.md`

3. **Therefore the gap is still unacceptable**
   - not because every missing field has equal evidence weight
   - but because the implementation under-delivers on a contract surface where multiple design artifacts support richer semantics, and the tests do not justify collapsing that contract to `null`

## 5. GAP-04 Decision Options Table

| Option | Path model | HTTP method model | Advantages | Drawbacks / risks | Frontend impact | Existing API test impact | Spec / example / scenario impact | Backward compatibility impact | Recommended adoption condition |
|---|---|---|---|---|---|---|---|---|---|
| **A. Spec-first** | Keep submission-rooted submit / revise paths from `api-spec.md`; keep `/initial-review/draft` and `/rereview/draft` | Mostly `POST` command style as currently documented | Aligns with current formal contract; aligns with examples / scenarios; makes submit/revise operate on the submission resource explicitly | Requires controller changes; requires updating existing API tests; may require temporary compatibility layer | Frontend can build to one documented contract once stabilized, but current in-repo callers/tests would need to change | High test churn; current tests are implementation-shaped | Low doc churn because spec/examples/scenarios are already closer to this option | Risk of breaking any caller already using current controller paths | Use when contract clarity and submission-rooted semantics are preferred over preserving current implementation shape |
| **B. Implementation-first** | Adopt current case-rooted command paths such as `/initial-review/submit`, `/submit-rereview`, `/revise-rereview`, `/submissions/{submissionId}/revise` | Mixed `POST` / `PUT` model matching controllers | Preserves current controllers and tests; lowest immediate backend churn | Requires broad spec/example/scenario rewrites; current path model is inconsistent even internally (`case-rooted` + nested `submissions/{submissionId}` + special rereview verbs); risks normalizing a tactical shape as contract truth | Frontend can match current runtime immediately, but would be building against a contract not yet ratified | Low test churn because current API tests already support it | High doc churn across `api-spec`, examples, scenarios, possibly app-service wording | Low short-term runtime breakage, but higher long-term cleanup risk if the model is later reconsidered | Use only if human decision explicitly chooses runtime continuity over contract refactoring |
| **C. Dual-track then deprecate** | Support both spec paths and current implementation paths for a transition period | Potentially mixed during transition | Reduces immediate break risk; creates migration space for frontend / downstream consumers | Highest complexity; duplicate handlers / mappings; larger test matrix; deprecation needs explicit policy and timeline | Frontend can migrate gradually, but only if one path is clearly marked canonical | Test suite grows because both tracks need coverage | Requires spec to document canonical + deprecated behavior clearly | Best short-term compatibility, but risk of long-lived ambiguity if deprecation is not enforced | Use only if there are confirmed downstream consumers that cannot move in one step |

### 5.1 Recommended Decision Framing

The decision should be framed as:

> Which API resource model should be canonical for review submit / revise operations in this repo: submission-rooted contract-first semantics, current case-rooted command endpoints, or a temporary compatibility bridge?

This framing is better than asking “should spec or code win?” because:

- it forces discussion of resource ownership
- it exposes HTTP method consistency
- it surfaces frontend and backward-compatibility consequences
- it avoids treating current controller existence as automatic authority

### 5.2 Information Still Missing

The following evidence was **not found in this repo** and would materially improve the decision:

1. real frontend code already calling these endpoints
2. external integration clients or mock servers depending on one path set
3. gateway routing constraints that would favor one path model
4. explicit accepted decision about whether submit / revise should be case-rooted or submission-rooted

## 6. What This Changes in the Original Reclassification Report

This strengthening pass changes the original report in the following ways:

1. **Repo truth judgment is now rule-based**
   - the original report used yes / no / pending decision without a reusable acceptance standard

2. **GAP-06 should no longer be treated as one undifferentiated problem**
   - 06A (`401/403` semantics) remains **unacceptable divergence**
   - 06B contains two different treatments:
     - `UnauthorizedException` naming mismatch → **unacceptable divergence**
     - `VALIDATION_ERROR` spec omission → **acceptable divergence**

3. **GAP-05 now has a stronger evidence chain**
   - repo-local frontend dependency: **not found**
   - API test support for minimal payload: **found**
   - richer payload support from design authority: **partially found beyond `api-spec.md`**

4. **GAP-04 is now decision-ready**
   - the original report said “decision required”
   - this pass adds a concrete options table suitable for human review

## 7. Minimal Follow-up Actions

1. **Decision prep**
   - use the GAP-04 options table to raise one explicit decision topic

2. **High-risk correction prep**
   - split GAP-06 into separate tasks:
     - HTTP status semantics
     - exception / error-code naming cleanup
     - optional spec sync for `VALIDATION_ERROR`

3. **Contract test prep**
   - create follow-up task slices for:
     - submit / revise response shape
     - unauthenticated vs forbidden semantics
     - attachment-bearing draft / revise payloads

4. **Out-of-scope observation**
   - `specification-by-example.md` and `state-machine.md` still reflect the pre-D-021 rule that `CLOSED` blocks all revision. This was not expanded in this pass, but it remains a separate consistency issue outside the three target gaps.
