# Operator Guide

## First Deployment

1. Confirm the client already uses (or has completed the equivalent of)
   `cloud-itonami-iso3166-usa` for general U.S. federal-contractor
   market entry (SAM.gov registration, FAR-Council-governed procurement
   mechanics); this repo is an agency-specific supplement, not a
   substitute.
2. Register the client's intake: the contract's principal purpose
   (services / construction / supply — determines whether SCA, DBRA, or
   PCA applies), whether the operator is a covered federal contractor
   under OFCCP's jurisdiction, prior filing/compliance history if any.
3. Run the advisor in read-only mode against DOL's Wage and Hour
   Division (WHD) and OFCCP published guidance.
4. Compare the checklist against the client's current documentation
   for each applicable track (the SCA/DBRA/PCA track selected by
   `:contract-category` always applies; OFCCP Section 503/VEVRAA only
   for engagements the client flags as OFCCP-covered).
5. **Brief the client explicitly on the 2025 EO 11246 rescission** —
   if the client's existing compliance program still references an
   EO-11246-based Affirmative Action Plan, that authority no longer
   exists as of 2025-01-21; redirect them to the current Section
   503/VEVRAA scope.
6. Enable gated filing/compliance-draft assistance once the DOL
   Labor-Standards Compliance Governor contract is trusted; actual
   submission always requires human sign-off.

## Minimum Production Controls

- client-owned data store for compliance documents
- clear provenance (official source citation) for every requirement
  surfaced
- approval workflow for any filing, registration, or compliance-program
  submission
- named referral relationship with licensed counsel or a registered
  agent for anything beyond checklist/draft assistance
- monthly audit export

## Certification

Certified operators must prove data provenance, audit traceability, that
automated actions cannot bypass the DOL Labor-Standards Compliance
Governor, that no proposal cites the rescinded EO 11246 as a current
requirement, and a working referral relationship with licensed counsel
or a registered agent for whatever licensed representation federal
contracting law requires for actual DOL/OFCCP filings.
