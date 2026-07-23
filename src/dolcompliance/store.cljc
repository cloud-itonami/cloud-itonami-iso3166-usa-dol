(ns dolcompliance.store
  "SSoT for the USA-DOL (Department of Labor) compliance actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior cloud-itonami actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store, using `langchain-store.core` for BOTH
                        the entity field-spec (`map->tx`/`pull->map`/
                        `pull-pattern`) AND the shared EDN-blob codec +
                        identity schema + event-log helpers, instead of
                        a hand-rolled `enc`/`dec*` (ADR-2607141600).

  Both implement the same protocol and pass the same contract
  (test/dolcompliance/store_contract_test.clj).

  The primary entity here is an `engagement` -- one operator's
  compliance engagement, carrying:

    - `:contract-category` (`:services`/`:construction`/`:supply`/nil)
      -- which of SCA/DBRA/PCA applies (mutually exclusive by the
      contract's principal purpose, grounded in
      `dolcompliance.facts/labor-standards-track`).
    - the engagement-level unconditional gate:
      `:labor-standards-verified?` -- compliance with whichever of
      SCA/DBRA/PCA the `:contract-category` selects has been verified
      -- required before any `:filing/submit`, regardless of anything
      else about the engagement.
    - the CONDITIONAL gate: `:requires-ofccp-compliance?` /
      `:ofccp-compliance-verified?` (only applies to a covered federal
      contractor under OFCCP's jurisdiction, grounded in
      `:ofccp-section503-vevraa`) -- a no-op gate when the engagement's
      own `:requires-ofccp-compliance?` flag is false.
    - the single actionable filing track's actuation state:
      `:drafted?`/`:draft-number`/`:submitted?`/`:submit-number` for
      `dolcompliance.registry/compliance-track`
      (`:labor-compliance-package`) -- unlike sibling actors with
      multiple independent regulatory filing tracks, this actor manages
      exactly ONE package (see `dolcompliance.registry` docstring), so
      no per-track field-name indirection is needed.

  `:compliance/assess` proposals are stored per underlying regulatory
  catalog track (`:service-contract-act`/`:davis-bacon-act`/
  `:walsh-healey-pca`/`:ofccp-section503-vevraa`/
  `:eo11246-rescission-boundary`) via `assessment-of`, keyed
  [engagement-id catalog-track] -- these feed
  `dolcompliance.governor`'s evidence-incomplete check across the
  engagement's applicable tracks before the labor-compliance-package
  filing/draft or filing/submit may proceed.

  The ledger stays append-only on every backend."
  (:require [dolcompliance.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (assessment-of [s engagement-id track] "committed track assessment, or nil")
  (ledger [s])
  (draft-history [s] "the append-only filing-draft history")
  (submit-history [s] "the append-only filing-submit history")
  (next-draft-sequence [s track])
  (next-submit-sequence [s track])
  (engagement-drafted? [s engagement-id])
  (engagement-submitted? [s engagement-id])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering the happy path
  (draft, submit) plus the governor's own dossier-grounded checks: a
  clean :services/SCA case (eng-1, includes the compliance-audit export
  package revenue line), an unregistered-track fabrication-defense +
  EO-11246-stale-authority-misattribution-defense case (eng-2), a
  fee-mismatch case (eng-3), a contract-category-undetermined case
  (eng-4), a labor-standards-verification-missing case (eng-5), a
  :construction/DBRA engagement missing OFCCP Section 503/VEVRAA
  verification (eng-6), and a clean :supply/PCA engagement WITH OFCCP
  verified (eng-7, exercising the third contract category end-to-end)."
  []
  {:engagements
   {"eng-1" {:id "eng-1" :operator "Potomac Federal Services LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? true :export-fee 150000 :claimed-fee 1550000.0
             :contract-category :services
             :labor-standards-verified? true
             :requires-ofccp-compliance? false :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-2" {:id "eng-2" :operator "Chesapeake Contract Advisors Inc" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? true :export-fee 150000 :claimed-fee 1550000.0
             :contract-category :services
             :labor-standards-verified? true
             :requires-ofccp-compliance? false :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-3" {:id "eng-3" :operator "Piedmont Staffing Solutions LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1800000.0
             :contract-category :services
             :labor-standards-verified? true
             :requires-ofccp-compliance? false :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-4" {:id "eng-4" :operator "Cascadia Program Services LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :contract-category nil
             :labor-standards-verified? false
             :requires-ofccp-compliance? false :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-5" {:id "eng-5" :operator "Tidewater Delivery Partners LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :contract-category :services
             :labor-standards-verified? false
             :requires-ofccp-compliance? false :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-6" {:id "eng-6" :operator "Blue Ridge Builders of Virginia LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :contract-category :construction
             :labor-standards-verified? true
             :requires-ofccp-compliance? true :ofccp-compliance-verified? false
             :drafted? false :submitted? false
             :status :intake}
    "eng-7" {:id "eng-7" :operator "Great Lakes Equipment Manufacturing LLC" :portal "SAM.gov (government-wide, operated by GSA)"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :contract-category :supply
             :labor-standards-verified? true
             :requires-ofccp-compliance? true :ofccp-compliance-verified? true
             :drafted? false :submitted? false
             :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------
;; Both backends' `commit-record!` build the draft/submit record the
;; same way -- these two pure helpers are the shared step, mirroring
;; every sibling actor's `draft-filing!`/`submit-filing!` shape.

(defn- do-draft! [engagement-id track seq-n]
  (registry/register-draft engagement-id track seq-n))

(defn- do-submit! [engagement-id track seq-n]
  (registry/register-submit engagement-id track seq-n))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (assessment-of [_ engagement-id track] (get-in @a [:assessments engagement-id track]))
  (ledger [_] (:ledger @a))
  (draft-history [_] (:draft-records @a))
  (submit-history [_] (:submit-records @a))
  (next-draft-sequence [_ track] (get-in @a [:draft-sequences track] 0))
  (next-submit-sequence [_ track] (get-in @a [:submit-sequences track] 0))
  (engagement-drafted? [_ engagement-id]
    (boolean (get-in @a [:engagements engagement-id :drafted?])))
  (engagement-submitted? [_ engagement-id]
    (boolean (get-in @a [:engagements engagement-id :submitted?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :assessment/set
      (let [[engagement-id track] path]
        (swap! a assoc-in [:assessments engagement-id track] payload))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            seq-n (next-draft-sequence s track)
            result (do-draft! engagement-id track seq-n)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:draft-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge
                                  {:drafted? true :draft-number (get result "draft_number")})
                       (update :draft-records registry/append result))))
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            seq-n (next-submit-sequence s track)
            result (do-submit! engagement-id track seq-n)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:submit-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge
                                  {:submitted? true :submit-number (get result "submit_number")})
                       (update :submit-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :draft-sequences {} :draft-records []
                           :submit-sequences {} :submit-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

;; Entity field-spec drives map<->tx<->pull for `engagement` via
;; `langchain-store.core` -- no hand-rolled `engagement->tx`/
;; `pull->engagement` pair (ADR-2607141600 increment 2, `underwriting.store`
;; reference-entity-adopter pattern). Booleans use `:coerce boolean` so a
;; never-set attribute reads back as `false`, matching MemStore.
(def ^:private engagement-spec
  {:id                            {:attr :engagement/id}
   :operator                      {:attr :engagement/operator}
   :portal                        {:attr :engagement/portal}
   :base-fee                      {:attr :engagement/base-fee}
   :monthly-rate                  {:attr :engagement/monthly-rate}
   :monitoring-months             {:attr :engagement/monitoring-months}
   :audit-export?                 {:attr :engagement/audit-export? :coerce boolean}
   :export-fee                    {:attr :engagement/export-fee}
   :claimed-fee                   {:attr :engagement/claimed-fee}
   :contract-category             {:attr :engagement/contract-category}
   :labor-standards-verified?     {:attr :engagement/labor-standards-verified? :coerce boolean}
   :requires-ofccp-compliance?    {:attr :engagement/requires-ofccp-compliance? :coerce boolean}
   :ofccp-compliance-verified?    {:attr :engagement/ofccp-compliance-verified? :coerce boolean}
   :drafted?                      {:attr :engagement/drafted? :coerce boolean}
   :draft-number                  {:attr :engagement/draft-number}
   :submitted?                    {:attr :engagement/submitted? :coerce boolean}
   :submit-number                 {:attr :engagement/submit-number}
   :status                        {:attr :engagement/status}})

(def ^:private schema
  (merge
   (ls/identity-schema [:engagement/id :assessment/key :ledger/seq
                        :draft-record/seq :submit-record/seq
                        :draft-sequence/track :submit-sequence/track])))

(defn- assessment-key [engagement-id track] (str engagement-id "::" (name track)))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (ls/pull->map engagement-spec :id
                  (d/pull (d/db conn) (ls/pull-pattern engagement-spec) [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(ls/pull->map engagement-spec :id
                             (d/pull (d/db conn) (ls/pull-pattern engagement-spec) [:engagement/id %])))
         (sort-by :id)))
  (assessment-of [_ engagement-id track]
    (ls/dec* (d/q '[:find ?p . :in $ ?k
                   :where [?a :assessment/key ?k] [?a :assessment/payload ?p]]
                 (d/db conn) (assessment-key engagement-id track))))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (draft-history [_] (ls/read-stream conn :draft-record/seq :draft-record/record))
  (submit-history [_] (ls/read-stream conn :submit-record/seq :submit-record/record))
  (next-draft-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :draft-sequence/track ?t] [?e :draft-sequence/next ?n]]
            (d/db conn) track)
        0))
  (next-submit-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :submit-sequence/track ?t] [?e :submit-sequence/next ?n]]
            (d/db conn) track)
        0))
  (engagement-drafted? [s engagement-id]
    (boolean (:drafted? (engagement s engagement-id))))
  (engagement-submitted? [s engagement-id]
    (boolean (:submitted? (engagement s engagement-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(ls/map->tx engagement-spec value)])

      :assessment/set
      (let [[engagement-id track] path]
        (d/transact! conn [{:assessment/key (assessment-key engagement-id track)
                            :assessment/payload (ls/enc payload)}]))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            seq-n (next-draft-sequence s track)
            result (do-draft! engagement-id track seq-n)
            next-n (inc seq-n)]
        (d/transact! conn
                     [(ls/map->tx engagement-spec {:id engagement-id :drafted? true
                                                   :draft-number (get result "draft_number")})
                      {:draft-sequence/track track :draft-sequence/next next-n}
                      {:draft-record/seq (count (draft-history s)) :draft-record/record (ls/enc (get result "record"))}])
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            seq-n (next-submit-sequence s track)
            result (do-submit! engagement-id track seq-n)
            next-n (inc seq-n)]
        (d/transact! conn
                     [(ls/map->tx engagement-spec {:id engagement-id :submitted? true
                                                   :submit-number (get result "submit_number")})
                      {:submit-sequence/track track :submit-sequence/next next-n}
                      {:submit-record/seq (count (submit-history s)) :submit-record/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv #(ls/map->tx engagement-spec %) (vals engagements)))) s))

(defn datomic-store
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  []
  (datomic-store (demo-data)))
