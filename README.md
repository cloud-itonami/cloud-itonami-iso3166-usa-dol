# cloud-itonami-iso3166-usa-dol

Open ISO 3166 Agency Blueprint for **USA-DOL**: United States Department
of Labor (DOL) — a USA-agency-level LEAF under the
`cloud-itonami-iso3166-usa` country-level coordinator.

This repository designs a forkable OSS business for an independent
compliance consultant: an already-registered federal contractor
(typically one already using `cloud-itonami-iso3166-usa` for general
U.S. market entry) gets a Compliance Advisor + independent **DOL
Labor-Standards Compliance Governor** to navigate the government-contract
labor-standards statute that applies to their contract's principal
purpose — the McNamara-O'Hara Service Contract Act (SCA) for services,
the Davis-Bacon Act / Davis-Bacon and Related Acts (DBRA) for
construction, or the Walsh-Healey Public Contracts Act (PCA) for
supply — plus OFCCP's current live nondiscrimination mandate (Section
503 of the Rehabilitation Act of 1973 for disability + VEVRAA for
veterans) when the contractor is covered by OFCCP's jurisdiction.

## No robotics premise — digital/data service exemption

Agency-specific compliance navigation is a pure data/software service with
no physical-domain work — the same exemption class as `cloud-itonami-6310`
and `cloud-itonami-gtin-*`. `blueprint.edn` sets
`:itonami.blueprint/robotics false` and `:required-technologies` lists only
real capabilities (`:identity`, `:forms`, `:dmn`, `:bpmn`, `:audit-ledger`),
no `:robotics`.

## Core Contract

```text
operator intake + prior filing/compliance history
        |
        v
Compliance Advisor -> DOL Labor-Standards Compliance Governor -> compliance draft, or human sign-off
        |
        v
gated filing / registration / compliance-program submission + audit ledger
```

No automated proposal can submit a filing or registration the governor
refuses, suppress a compliance record, or claim a legal conclusion the
governor has not cleared. `:filing/submit` is never in any phase's `:auto`
set — it always requires human sign-off.

## Implementation

`src/dolcompliance/` — a langgraph-clj StateGraph actor, same
containment shape as `cloud-itonami-iso3166-jpn-mof`'s `mofcompliance.*`
/ `cloud-itonami-iso3166-jpn-mod`'s `defensecompliance.*` /
`cloud-itonami-iso3166-jpn-moe`'s `greenprocurement.*` (advisor sealed
to proposals-only, independent governor, append-only ledger, `Store`
protocol swap, phase gate):

- `facts.cljc` — the SCA/DBRA/PCA labor-standards catalog (one track
  per `:contract-category`) plus the OFCCP Section 503/VEVRAA catalog
  entry, the ONLY source of regulatory-requirement facts the actor may
  cite. Five entries: `:service-contract-act`, `:davis-bacon-act`,
  `:walsh-healey-pca`, `:ofccp-section503-vevraa`, and a NEGATIVE/
  boundary entry `:eo11246-rescission-boundary` (`:filing-track?
  false`) that exists only so the governor has a citable spec-basis for
  REJECTING any proposal that treats **Executive Order 11246** as a
  still-current OFCCP requirement — **EO 11246 was rescinded 2025-01-21
  by EO 14173** ("Ending Illegal Discrimination and Restoring
  Merit-Based Opportunity"); as of 2026 OFCCP's EO-11246-based
  affirmative-action-plan mandate no longer exists. See "The 2025 EO
  11246 rescission" below.
- `governor.cljc` — the DOL Labor-Standards Compliance Governor: a
  spec-basis/no-fabrication HARD check, an evidence-incomplete check
  (across every applicable underlying track for the engagement), an
  **EO-11246 stale-authority-misattribution** HARD check (flagship
  defense), a **labor-standards-category-undetermined** HARD check
  (`:filing/submit`, unconditional — exactly one of SCA/DBRA/PCA must
  resolve from the engagement's `:contract-category`), a
  **labor-standards-verification-missing** HARD check (`:filing/submit`,
  unconditional), an **OFCCP-compliance-missing** HARD check
  (`:filing/submit`, CONDITIONAL on the engagement's own
  `:requires-ofccp-compliance?`), an independently-recomputed
  engagement-fee-mismatch check (three revenue lines: base fee +
  monitoring subscription + optional audit-export package), a
  confidence-floor/actuation gate, and double-draft/double-submit
  guards.
- `store.cljc` — `MemStore`/`DatomicStore` (via
  `kotoba-lang/langchain-store`'s entity field-spec `map->tx`/
  `pull->map`/`pull-pattern` AND its identity-schema/event-log
  helpers, not a hand-rolled `enc`/`dec*` or hand-rolled tx/pull pair)
  for the `engagement` entity, which tracks `:contract-category`, the
  unconditional labor-standards-verification gate, the conditional
  OFCCP gate, and the single actionable filing track's actuation
  state.
- `registry.cljc` — pure-function filing-draft/filing-submit record
  construction for the single actionable filing track
  (`:labor-compliance-package` — the operator's bundled compliance-
  readiness package, distinct from the regulatory catalog entries in
  `facts.cljc`).
- `dolcompliancellm.cljc` — the Compliance Advisor (mock LLM, proposals
  only).
- `operation.cljc` — the StateGraph: intake → advise → govern → decide
  → [request-approval →] commit/hold, `interrupt-before` on human
  approval.
- `phase.cljc` — phase 0→3 rollout; `:filing/draft`/`:filing/submit`
  are permanently absent from every phase's `:auto` set.

Ops: `:engagement/intake`, `:compliance/assess` (per underlying
regulatory-catalog-track evidence checklist — `:service-contract-act`/
`:davis-bacon-act`/`:walsh-healey-pca`/`:ofccp-section503-vevraa`/
`:eo11246-rescission-boundary`), `:filing/draft`, `:filing/submit` (the
latter two always target the single `:labor-compliance-package` track).

## The 2025 EO 11246 rescission (read this before citing OFCCP)

Executive Order 11246 (1965) was, for six decades, OFCCP's primary
authority requiring covered federal contractors to maintain written
Affirmative Action Plans addressing race/sex/national-origin.
**Executive Order 14173, "Ending Illegal Discrimination and Restoring
Merit-Based Opportunity," rescinded EO 11246 on 2025-01-21.** As of
2026, OFCCP no longer administers an EO-11246-based affirmative-
action-plan mandate.

OFCCP's remaining live statutory mandates are **Section 503 of the
Rehabilitation Act of 1973** (disability nondiscrimination/affirmative
action) and **VEVRAA** (Vietnam Era Veterans' Readjustment Assistance
Act of 1974, veteran nondiscrimination/affirmative action) —
STATUTORY authorities, unaffected by the EO 11246 rescission.

This actor encodes both facts explicitly:
`dolcompliance.facts/:ofccp-section503-vevraa` is the CURRENT live
OFCCP scope this actor's OFCCP track actually checks;
`dolcompliance.facts/:eo11246-rescission-boundary` is a NEGATIVE
boundary entry (`:filing-track? false`) that exists purely so
`dolcompliance.governor`'s `eo11246-stale-authority-misattribution`
check has a citable spec-basis for REJECTING — never asserting — a
proposal that treats EO 11246 as still current. This is the single
most important correctness detail in this repository.

## Fabrication traps addressed

This research pass explicitly verified and defended against:

- **EO 11246 is not current.** See above — the flagship defense in
  this repository.
- SCA/DBRA/PCA are **not** DOL-exclusive in the sense of "only apply
  to DOL-let contracts" — they apply GOVERNMENTWIDE to any qualifying
  federal contract regardless of which agency is the contracting
  party. This catalog's `:owner-authority` for that fact stays DOL's
  Wage and Hour Division (WHD, the administering/enforcing authority),
  never the contracting agency.
- SAM.gov (System for Award Management) and the FAR (Federal
  Acquisition Regulation) are GOVERNMENT-WIDE prerequisites, not DOL
  creations — SAM.gov is operated by GSA, and the FAR is jointly owned
  by the FAR Council (DoD + GSA + NASA Administrators). Neither is
  attributed to DOL anywhere in this catalog.
- No VETS-4212 veteran-employment-reporting requirement is modeled —
  recalled from background knowledge but not independently verified
  via live fetch during this research pass, so it is deliberately
  omitted rather than encoded on an unverified basis.
- No DFARS, CMMC, DCSA, or GSAM citation anywhere — none of these are
  DOL authorities (DFARS/CMMC/DCSA are DoD; GSAM is GSA).
- No fabricated OFCCP jurisdictional dollar threshold — an
  engagement's own `:requires-ofccp-compliance?` flag is asserted and
  independently verified by the governor, never derived from an
  invented numeric threshold.

## What this is NOT

- **Not the United States Department of Labor.** Commercial compliance
  navigation only.
- **Not legal advice.** Every regulatory claim must cite the official
  source, and final filings route to licensed counsel or a registered
  agent where the law requires licensed representation.

## Official surface

- https://www.dol.gov/
- https://www.dol.gov/agencies/whd (Wage and Hour Division)
- https://www.dol.gov/agencies/ofccp (OFCCP)

## Capability layer

Resolves via [`kotoba-lang/iso3166`](https://github.com/kotoba-lang/iso3166)
(code `USA-DOL`, `:parent "USA"`, cross-referenced to ooyake's
`gov.usa.dol`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
