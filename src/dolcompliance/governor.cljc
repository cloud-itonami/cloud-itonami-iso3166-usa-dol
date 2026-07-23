(ns dolcompliance.governor
  "DOL Labor-Standards Compliance Governor -- the independent compliance
  layer that earns the LaborCompliance-LLM the right to commit. The LLM
  has no notion of which of the Service Contract Act (SCA)/Davis-Bacon
  Act (DBRA)/Walsh-Healey Public Contracts Act (PCA) actually applies
  to a given federal contract's principal purpose, whether labor-
  standards compliance for that track has actually been verified,
  whether OFCCP Section 503/VEVRAA nondiscrimination compliance is
  actually complete for a covered federal contractor, whether a
  proposal is quietly treating the RESCINDED Executive Order 11246 as
  if it were still a current OFCCP Affirmative Action Plan requirement
  (a documented stale-authority trap -- EO 11246 was rescinded
  2025-01-21 by EO 14173), whether a claimed engagement fee actually
  equals base + months x rate (+ optional export package), or when a
  draft stops being a draft and becomes a real-world DOL-facing filing,
  so this MUST be a separate system able to *reject* a proposal and
  fall back to HOLD.

  `:itonami.blueprint/governor` is `:dol-labor-standards-compliance-
  governor` (blueprint.edn).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual filing, registration, or compliance-program submission
  requires DOL Labor-Standards Compliance Governor clearance and always
  escalates to human sign-off'; 'a false or fabricated regulatory-
  requirement claim is a HARD hold that cannot be overridden by human
  approval alone') names exactly the checks below.

  Nine checks, in priority order, ALL HARD violations except the
  confidence/actuation gate: a human approver CANNOT override the hard
  ones. The confidence/actuation gate is SOFT: it asks a human to look
  (low confidence / actuation), and the human may approve -- but see
  `dolcompliance.phase`: for `:stake :actuation/draft-filing`/
  `:actuation/submit-filing` NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                    -- did the compliance-track
                                         proposal cite an OFFICIAL
                                         source (`dolcompliance.facts`),
                                         or invent one?
    2. Evidence incomplete           -- for the labor-compliance-package
                                         `:filing/draft`/`:filing/submit`,
                                         has EVERY applicable underlying
                                         regulatory track (the SCA/DBRA/
                                         PCA track selected by the
                                         engagement's own
                                         `:contract-category`, plus
                                         `:ofccp-section503-vevraa` only
                                         when the engagement's own
                                         `:requires-ofccp-compliance?`
                                         flag says it applies) actually
                                         been assessed with a full
                                         evidence checklist on file?
    3. EO-11246 stale-authority
       misattribution                 -- for `:compliance/assess`/
                                         `:filing/draft`/`:filing/submit`,
                                         does the proposal treat the
                                         RESCINDED Executive Order 11246
                                         as if it were a still-current
                                         OFCCP Affirmative Action Plan
                                         requirement? A documented
                                         stale-authority trap -- EO
                                         11246 was rescinded 2025-01-21
                                         by EO 14173; OFCCP's live
                                         mandate today is Section 503 +
                                         VEVRAA only. Flagship defense
                                         for this actor.
    4. Labor-standards category
       undetermined                    -- for `:filing/submit`,
                                         UNCONDITIONALLY verify the
                                         engagement's `:contract-category`
                                         (`:services`/`:construction`/
                                         `:supply`) resolves to one of
                                         SCA/DBRA/PCA via
                                         `dolcompliance.facts/labor-
                                         standards-track` -- exactly one
                                         of the three ALWAYS applies to
                                         a covered federal contract, so
                                         this is unconditional, not
                                         conditional on anything else
                                         about the engagement.
    5. Labor-standards verification
       missing                          -- for `:filing/submit`,
                                         UNCONDITIONALLY verify
                                         `:labor-standards-verified?`
                                         is true -- compliance with
                                         whichever of SCA/DBRA/PCA
                                         applies is a documented
                                         prerequisite to any federal
                                         contract, not conditional on
                                         anything else about the
                                         engagement.
    6. OFCCP-compliance missing      -- for `:filing/submit`, when the
                                         engagement declares
                                         `:requires-ofccp-compliance?
                                         true` (covered federal
                                         contractor), INDEPENDENTLY
                                         verify `:ofccp-compliance-
                                         verified?` is true.
    7. Engagement fee mismatch       -- for `:filing/submit`,
                                         INDEPENDENTLY recompute whether
                                         the engagement's own `:claimed-
                                         fee` equals `base-fee +
                                         monthly-rate x monitoring-
                                         months` (+ optional export-fee
                                         when `:audit-export?` is true).
    8. Confidence floor / actuation
       gate                            -- LLM confidence below
                                         threshold, OR the op is
                                         `:filing/draft`/`:filing/submit`
                                         (REAL acts) -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off the engagement's own `:drafted?`/`:submitted?` facts (never a
  `:status` value) -- unlike sibling actors with multiple independent
  filing tracks, this actor manages exactly ONE
  (`dolcompliance.registry/compliance-track`), so no per-track
  indirection is needed here either."
  (:require [clojure.string :as str]
            [dolcompliance.facts :as facts]
            [dolcompliance.registry :as registry]
            [dolcompliance.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real DOL labor-compliance package and submitting it are
  the two real-world actuation events this actor performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent DOL's labor-standards/OFCCP requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "proposal cites no official spec-basis -- cannot be treated as a DOL compliance requirement"}]))))

(defn- applicable-tracks
  "Which underlying `dolcompliance.facts` catalog tracks must be
  assessed before the labor-compliance-package may be drafted/submitted
  for `engagement`? Always whichever of `:service-contract-act`/
  `:davis-bacon-act`/`:walsh-healey-pca` matches the engagement's
  `:contract-category`; `:ofccp-section503-vevraa` is added only when
  the engagement's own `:requires-ofccp-compliance?` flag says it
  applies."
  [engagement]
  (cond-> #{}
    (facts/labor-standards-track (:contract-category engagement))
    (conj (facts/labor-standards-track (:contract-category engagement)))

    (true? (:requires-ofccp-compliance? engagement))
    (conj :ofccp-section503-vevraa)))

(defn- evidence-incomplete-violations
  "For the labor-compliance-package `:filing/draft`/`:filing/submit`,
  EVERY applicable underlying track's required evidence checklist must
  actually be satisfied."
  [{:keys [op subject track]} st]
  (when (and (contains? #{:filing/draft :filing/submit} op) (= track registry/compliance-track))
    (let [e (store/engagement st subject)
          needed (applicable-tracks e)
          missing (remove (fn [t]
                             (let [a (store/assessment-of st subject t)]
                               (and a (facts/required-evidence-satisfied? t (:checklist a)))))
                           needed)]
      (when (seq missing)
        [{:rule :evidence-incomplete
          :detail (str subject " has incomplete evidence checklist for track(s): "
                      (str/join "," (map name (sort missing))))}]))))

(defn- eo11246-misattribution-violations
  "For `:compliance/assess`/`:filing/draft`/`:filing/submit`, a
  proposal that claims Executive Order 11246 is still a current OFCCP
  Affirmative Action Plan requirement is a HARD violation --
  `dolcompliance.facts`'s `:eo11246-rescission-boundary` entry is the
  citable spec-basis for REJECTING this claim, not for asserting it.
  EO 11246 was rescinded 2025-01-21 by EO 14173; OFCCP's live mandate
  today is Section 503 + VEVRAA only."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (when (true? (:claims-eo11246-affirmative-action-plan-required? (:value proposal)))
      [{:rule :eo11246-stale-authority-misattribution
        :detail "Executive Order 11246 was rescinded 2025-01-21 by EO 14173 -- it cannot be cited as a current OFCCP Affirmative Action Plan requirement; OFCCP's live statutory mandate today is Section 503 (disability) + VEVRAA (veterans) only"}])))

(defn- labor-standards-category-undetermined-violations
  "For `:filing/submit`, UNCONDITIONALLY verify the engagement's
  `:contract-category` resolves to one of SCA/DBRA/PCA -- exactly one
  ALWAYS applies to a covered federal contract, regardless of the
  engagement's other circumstances."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (facts/labor-standards-track (:contract-category e))
        [{:rule :labor-standards-category-undetermined
          :detail (str subject " has no determined :contract-category (:services/:construction/:supply) -- cannot select which of SCA/DBRA/PCA applies; submit proposal cannot proceed")}]))))

(defn- labor-standards-verification-missing-violations
  "For `:filing/submit`, UNCONDITIONALLY verify
  `:labor-standards-verified?` is true -- compliance with whichever of
  SCA/DBRA/PCA applies is a documented prerequisite to any federal
  contract, not conditional on anything else about the engagement."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (true? (:labor-standards-verified? e))
        [{:rule :labor-standards-verification-missing
          :detail (str subject " has not verified labor-standards compliance for its applicable SCA/DBRA/PCA track -- submit proposal cannot proceed")}]))))

(defn- ofccp-compliance-missing-violations
  "For `:filing/submit`, when the engagement declares
  `:requires-ofccp-compliance? true` (covered federal contractor),
  INDEPENDENTLY verify `:ofccp-compliance-verified?` is true.
  CONDITIONAL on the engagement's own ground truth -- a no-op for an
  engagement below OFCCP's jurisdiction."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-ofccp-compliance? e))
                 (not (true? (:ofccp-compliance-verified? e))))
        [{:rule :ofccp-compliance-missing
          :detail (str subject " has not verified OFCCP Section 503/VEVRAA nondiscrimination compliance -- submit proposal cannot proceed")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate (+ optional
  export-fee)."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " claimed fee (" (:claimed-fee e)
                      ") does not match independently recomputed value (" (registry/compute-engagement-fee e) ")")}]))))

(defn- already-drafted-violations
  "Refuses to draft the SAME engagement's labor-compliance package twice."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/draft) (= track registry/compliance-track))
    (when (store/engagement-drafted? st subject)
      [{:rule :already-drafted
        :detail (str subject " already has a draft on file")}])))

(defn- already-submitted-violations
  "Refuses to submit the SAME engagement's labor-compliance package twice."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (when (store/engagement-submitted? st subject)
      [{:rule :already-submitted
        :detail (str subject " already has a submission on file")}])))

(defn check
  "Censors a LaborCompliance-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (eo11246-misattribution-violations request proposal)
                           (labor-standards-category-undetermined-violations request st)
                           (labor-standards-verification-missing-violations request st)
                           (ofccp-compliance-missing-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :track      (:track request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
