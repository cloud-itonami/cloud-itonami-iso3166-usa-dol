# Business Model: Independent DOL Labor-Standards Procurement Compliance Service — United States

Implementation: `src/dolcompliance/` — see README.md's Implementation
section. The Trust Controls below are enforced in code by
`dolcompliance.governor` (spec-basis/no-fabrication HARD check,
evidence-incomplete check, EO-11246 stale-authority-misattribution HARD
check, labor-standards-category-undetermined HARD check,
labor-standards-verification-missing HARD check, OFCCP-compliance-
missing HARD check, engagement-fee-mismatch check, confidence-floor/
actuation gate, double-draft/double-submit guards) and
`dolcompliance.phase` (`:filing/submit` absent from every phase's
`:auto` set).

## Classification

- Repository: `cloud-itonami-iso3166-usa-dol`
- ISO 3166 (agency-level): `USA-DOL`, parent `USA`
- Ooyake cross-reference: `gov.usa.dol` (United States Department of Labor)
- Activity: labor-standards track determination and evidence
  verification for federal contracts (Service Contract Act for
  services, Davis-Bacon Act/DBRA for construction, Walsh-Healey Public
  Contracts Act for supply — all three administered by DOL's Wage and
  Hour Division), plus OFCCP Section 503 (disability)/VEVRAA (veterans)
  nondiscrimination compliance for covered federal contractors
- Social impact: [:procurement-access-clarity :public-spend-transparency :labor-standards-compliance-transparency]

## Customer

- a federal contractor (prime or sub) whose contract's principal
  purpose is services, construction, or supply, confirming which
  labor-standards statute applies and that compliance evidence is on
  file before proceeding
- a federal contractor covered by OFCCP's jurisdiction, confirming
  current Section 503/VEVRAA compliance status — and NOT relying on a
  stale EO 11246 affirmative-action-plan understanding (see "The 2025
  EO 11246 rescission" in README.md)

## Offer

- labor-standards track determination (SCA/DBRA/PCA, by the contract's
  principal purpose) and evidence checklist
- OFCCP Section 503/VEVRAA compliance-status checklist for covered
  federal contractors, with an explicit EO-11246-rescission caveat so
  operators do not act on stale 2025-and-earlier guidance
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires DOL Labor-Standards Compliance Governor clearance and always
  escalates to human sign-off (`:filing/submit` is never automated at
  any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold
  that cannot be overridden by human approval alone — it must be
  corrected against a cited official source first
- treating the rescinded Executive Order 11246 as if it were a
  still-current OFCCP Affirmative Action Plan requirement is a HARD
  hold that cannot be overridden — EO 11246 was rescinded 2025-01-21 by
  EO 14173; OFCCP's live mandate today is Section 503 + VEVRAA only
- this service does **not** provide legal or tax advice; characterization
  and filing on the client's behalf beyond checklist/draft assistance
  routes to licensed counsel or a registered agent
- every requirement cites an official source (`en.wikipedia.org`
  during this research pass), never invented

## Boundary with adjacent actors (read before forking)

- **`cloud-itonami-iso3166-usa`**: the COUNTRY-level coordinator
  (general U.S. public-sector market entry). This repo is a narrower,
  deeper AGENCY-level leaf — most operators need the country-level
  blueprint plus only the agency-level blueprints that actually apply
  to their contract.
- **`com-etzhayyim-ooyake`** (etzhayyim/root): read-only civic-wayfinding
  mirror of government structure, non-commercial, barred from acting as or
  for the government (G3 impersonation ban). This blueprint is commercial
  and never claims to be the Department of Labor or an official channel.
- This blueprint assumes the operator is already a registered federal
  contractor (SAM.gov registration and FAR-Council-governed
  procurement mechanics are GOVERNMENT-WIDE prerequisites, operated by
  GSA and the FAR Council respectively, not DOL creations — see
  README.md "Fabrication traps addressed") and handles ONLY the
  DOL-specific labor-standards/OFCCP compliance layer on top.

## Scope this pass deliberately did NOT cover

To avoid fabricating unverified facts:

- **VETS-4212** veteran-employment reporting is not modeled. It was
  recalled from background knowledge but not independently confirmed
  via live fetch during this research pass — a future pass with a
  verified dossier could add it as a sixth catalog entry.
- **DFARS, CMMC, DCSA, GSAM** are never cited — none of these are DOL
  authorities (DoD and GSA respectively), and this actor's own facts
  stay in DOL-WHD/OFCCP's lane.
- **SAM.gov / FAR** are referenced only as government-wide
  prerequisites attributed to their actual operators (GSA / the FAR
  Council) — never re-derived as DOL's own creation.
- No specific OFCCP jurisdictional dollar threshold is invented — the
  engagement's own `:requires-ofccp-compliance?` flag drives the
  conditional gate, independently verified by the governor.
