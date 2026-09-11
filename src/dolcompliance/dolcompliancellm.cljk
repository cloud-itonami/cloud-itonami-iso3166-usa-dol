(ns dolcompliance.dolcompliancellm
  "LaborCompliance-LLM client -- the *contained intelligence node* for
  the USA-DOL (Department of Labor) compliance actor.

  It normalizes engagement intake, drafts a per-track (`:service-
  contract-act`/`:davis-bacon-act`/`:walsh-healey-pca`/`:ofccp-
  section503-vevraa`/`:eo11246-rescission-boundary`) compliance evidence
  checklist, drafts the labor-compliance-package filing-draft action,
  and drafts the labor-compliance-package filing-submit action.
  CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real DOL/OFCCP filing. Every output is censored
  downstream by `dolcompliance.governor` before anything touches the
  SSoT, and `:filing/draft`/`:filing/submit` proposals NEVER auto-commit
  at any phase -- see README Core Contract.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. Two test-only injection flags exist purely to exercise the
  governor's fabrication defenses without needing an actual bad LLM:
  `:no-spec?` (assess an unregistered track) and `:misattribute?`
  (assess `:eo11246-rescission-boundary` while WRONGLY claiming
  Executive Order 11246 is still a current OFCCP Affirmative Action
  Plan requirement -- the governor's
  `eo11246-stale-authority-misattribution` check must catch this)."
  (:require [dolcompliance.facts :as facts]
            [dolcompliance.registry :as registry]
            [dolcompliance.store :as store]))

(defn- normalize-intake
  [_db {:keys [patch]}]
  {:summary    (str "engagement intake record updated: " (pr-str (keys patch)))
   :rationale  "normalization of input patch only -- no new facts generated."
   :cites      (vec (keys patch))
   :effect     :engagement/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-track
  "Per-track (`:service-contract-act`/`:davis-bacon-act`/`:walsh-
  healey-pca`/`:ofccp-section503-vevraa`/`:eo11246-rescission-
  boundary`) compliance evidence checklist draft. `:no-spec?` injects
  the failure mode we must defend against: proposing a checklist for a
  track with NO official spec-basis. `:misattribute?` injects the
  OTHER failure mode: assessing `:eo11246-rescission-boundary` while
  wrongly claiming Executive Order 11246 is still a current OFCCP
  Affirmative Action Plan requirement."
  [_db {:keys [track no-spec? misattribute?]}]
  (let [track (if no-spec? :unknown-track track)
        sb (facts/spec-basis track)]
    (cond
      (nil? sb)
      {:summary    (str (name track) " has no official spec-basis on file")
       :rationale  "track not registered in dolcompliance.facts -- requirements are never guessed."
       :cites      []
       :effect     :assessment/set
       :value      {:track track :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}

      (and misattribute? (= track :eo11246-rescission-boundary))
      {:summary    (str (name track) " -- proposal treats EO 11246 as still-current (deliberately incorrect test proposal)")
       :rationale  "this proposal is intentionally wrong -- exercises the governor's eo11246-stale-authority-misattribution check"
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track :checklist (:required-evidence sb) :spec-basis (:provenance sb)
                    :claims-eo11246-affirmative-action-plan-required? true}
       :stake      nil
       :confidence 0.5}

      :else
      {:summary    (str (name track) " (" (:owner-authority sb) ") -- "
                        (count (:required-evidence sb)) " required evidence item(s) proposed")
       :rationale  (str "official source: " (:provenance sb) " / legal basis: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-draft
  "Draft the actual labor-compliance-package FILING-DRAFT action.
  ALWAYS `:stake :actuation/draft-filing`."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)
        track registry/compliance-track]
    {:summary    (str subject " labor-compliance package filing-draft proposal"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "track=" (name track) " portal=" (:portal e))
                   "engagement not found")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-drafted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/draft-filing
     :confidence (if e 0.9 0.3)}))

(defn- propose-submit
  "Draft the actual labor-compliance-package FILING-SUBMIT action.
  ALWAYS `:stake :actuation/submit-filing` -- a real-world DOL/OFCCP-
  facing labor-compliance-package submission. Reflects readiness across
  ALL THREE gates the governor independently re-verifies: the
  contract's SCA/DBRA/PCA category resolution (unconditional), labor-
  standards verification for that track (unconditional), and OFCCP
  Section 503/VEVRAA compliance (conditional on
  `:requires-ofccp-compliance?`)."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)
        track registry/compliance-track
        category-ok? (boolean (facts/labor-standards-track (:contract-category e)))
        labor-ok? (:labor-standards-verified? e)
        ofccp-ok? (or (not (:requires-ofccp-compliance? e)) (:ofccp-compliance-verified? e))]
    {:summary    (str subject " labor-compliance package filing-submit proposal"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "contract-category=" (:contract-category e)
                        " labor-standards-verified?=" (:labor-standards-verified? e)
                        " ofccp-compliance-verified?=" (:ofccp-compliance-verified? e)
                        " claimed-fee=" (:claimed-fee e))
                   "engagement not found")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-submitted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/submit-filing
     :confidence (if (and e category-ok? labor-ok? ofccp-ok?) 0.9 0.3)}))

(defprotocol Advisor
  (-advise [this db request] "Return a proposal map for `request`."))

(defrecord MockAdvisor []
  Advisor
  (-advise [_ db {:keys [op] :as request}]
    (case op
      :engagement/intake   (normalize-intake db request)
      :compliance/assess   (assess-track db request)
      :filing/draft        (propose-draft db request)
      :filing/submit       (propose-submit db request)
      {:summary "unknown op" :rationale "unsupported" :cites []
       :effect :noop :value {} :stake nil :confidence 0.0})))

(defn mock-advisor [] (->MockAdvisor))

(defn trace [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :track (:track request)
   :summary (:summary proposal)
   :confidence (:confidence proposal)
   :stake (:stake proposal)})
