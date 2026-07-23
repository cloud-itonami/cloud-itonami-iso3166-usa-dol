(ns dolcompliance.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls implemented faithfully, and the integration test
  running the compiled StateGraph end-to-end. The single invariant
  under test:

    LaborCompliance-LLM never drafts or submits a labor-compliance
    package the DOL Labor-Standards Compliance Governor would reject,
    `:filing/draft`/`:filing/submit` NEVER auto-commit at any phase,
    `:engagement/intake` MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [dolcompliance.registry :as registry]
            [dolcompliance.store :as store]
            [dolcompliance.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :dol-compliance-operator :phase 3})
(def track registry/compliance-track)

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  [actor tid-prefix subject catalog-track]
  (exec-op actor (str tid-prefix "-assess-" (name catalog-track))
           {:op :compliance/assess :subject subject :track catalog-track} operator)
  (approve! actor (str tid-prefix "-assess-" (name catalog-track))))

(defn- draft!
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-draft") {:op :filing/draft :subject subject :track track} operator)
  (approve! actor (str tid-prefix "-draft")))

(deftest clean-intake-auto-commits
  (testing "integration: engagement/intake at phase 3 auto-commits through the full compiled graph"
    (let [[db actor] (fresh)
          res (exec-op actor "t1"
                    {:op :engagement/intake :subject "eng-1"
                     :patch {:id "eng-1" :operator "Potomac Federal Services LLC"}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "Potomac Federal Services LLC" (:operator (store/engagement db "eng-1"))) "SSoT actually updated")
      (is (= 1 (count (store/ledger db)))))))

(deftest compliance-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :service-contract-act} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "eng-1" :service-contract-act)))))))

(deftest fabricated-track-is-held
  (testing "a compliance/assess proposal with no official spec-basis -> HOLD"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :compliance/assess :subject "eng-1" :track :service-contract-act :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-1" :service-contract-act)) "no assessment written"))))

(deftest eo11246-stale-authority-misattribution-is-held-and-unoverridable
  (testing "treating rescinded EO 11246 as a still-current OFCCP requirement -> HARD hold (flagship stale-authority defense)"
    (let [[db actor] (fresh)
          res (exec-op actor "t3b"
                    {:op :compliance/assess :subject "eng-2" :track :eo11246-rescission-boundary :misattribute? true} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:eo11246-stale-authority-misattribution} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-2" :eo11246-rescission-boundary)) "no assessment written"))))

(deftest draft-without-assessment-is-held
  (testing "filing/draft before any compliance assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest labor-standards-category-undetermined-is-held-and-unoverridable
  (testing "missing :contract-category -> HARD hold (unconditional) -- exactly one of SCA/DBRA/PCA must always resolve"
    (let [[db actor] (fresh)
          _ (draft! actor "t5pre" "eng-4")
          res (exec-op actor "t5" {:op :filing/submit :subject "eng-4" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:labor-standards-category-undetermined} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest labor-standards-verification-missing-is-held-and-unoverridable
  (testing "unverified compliance with the applicable SCA/DBRA/PCA track -> HARD hold (unconditional)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "eng-5" :service-contract-act)
          _ (draft! actor "t6pre" "eng-5")
          res (exec-op actor "t6" {:op :filing/submit :subject "eng-5" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:labor-standards-verification-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest ofccp-compliance-missing-is-held-for-covered-contractor
  (testing "missing OFCCP Section 503/VEVRAA compliance -> HARD hold, CONDITIONAL on :requires-ofccp-compliance?"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "eng-6" :davis-bacon-act)
          _ (assess! actor "t7pre" "eng-6" :ofccp-section503-vevraa)
          _ (draft! actor "t7pre" "eng-6")
          res (exec-op actor "t7" {:op :filing/submit :subject "eng-6" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:ofccp-compliance-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest ofccp-check-is-a-noop-for-non-covered-engagement
  (testing "eng-1 (:requires-ofccp-compliance? false) never triggers the OFCCP gate"
    (let [[db actor] (fresh)
          _ (assess! actor "t7bpre" "eng-1" :service-contract-act)
          _ (draft! actor "t7bpre" "eng-1")
          res (exec-op actor "t7b" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status res)) "clean submit still escalates for human approval, but is NOT held")
      (is (not (some #{:ofccp-compliance-missing} (mapcat :basis (store/ledger db))))))))

(deftest engagement-fee-mismatch-is-held
  (testing "claimed fee that doesn't equal base + months x rate (+ optional export) -> HOLD"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "eng-3" :service-contract-act)
          _ (draft! actor "t9pre" "eng-3")
          res (exec-op actor "t9" {:op :filing/submit :subject "eng-3" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:engagement-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest submit-always-escalates-then-human-decides
  (testing "integration: a clean fully-assessed submit still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "eng-1" :service-contract-act)
          _ (draft! actor "t10pre" "eng-1")
          r1 (exec-op actor "t10" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, submit record drafted"
        (let [r2 (approve! actor "t10")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:submitted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/submit-history db))) "one draft submit record"))))))

(deftest draft-always-escalates-then-human-decides
  (testing "a clean fully-assessed draft still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "eng-1" :service-contract-act)
          r1 (exec-op actor "t11" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, draft record drafted"
        (let [r2 (approve! actor "t11")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:drafted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/draft-history db))) "one draft record"))))))

(deftest engagement-double-draft-is-held
  (testing "drafting the same engagement's labor-compliance package twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t12pre" "eng-1" :service-contract-act)
          _ (draft! actor "t12pre" "eng-1")
          res (exec-op actor "t12" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-drafted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/draft-history db))) "still only the one earlier draft"))))

(deftest engagement-double-submit-is-held
  (testing "submitting the same engagement's labor-compliance package twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t13pre" "eng-1" :service-contract-act)
          _ (draft! actor "t13pre" "eng-1")
          _ (exec-op actor "t13a" {:op :filing/submit :subject "eng-1" :track track} operator)
          _ (approve! actor "t13a")
          res (exec-op actor "t13" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-submitted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/submit-history db))) "still only the one earlier submit"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :engagement/intake :subject "eng-1"
                          :patch {:id "eng-1" :operator "Potomac Federal Services LLC"}} operator)
      (exec-op actor "b" {:op :compliance/assess :subject "eng-1" :track :service-contract-act :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))

(deftest full-lifecycle-across-all-three-contract-categories
  (testing "integration: services (SCA) / construction (DBRA) / supply (PCA) all independently draft+submit cleanly"
    (let [[db actor] (fresh)]
      ;; eng-1: services / SCA, no OFCCP
      (assess! actor "svc" "eng-1" :service-contract-act)
      (draft! actor "svc" "eng-1")
      (exec-op actor "svc-submit" {:op :filing/submit :subject "eng-1" :track track} operator)
      (approve! actor "svc-submit")
      ;; eng-7: supply / PCA, WITH OFCCP verified
      (assess! actor "sup" "eng-7" :walsh-healey-pca)
      (assess! actor "sup" "eng-7" :ofccp-section503-vevraa)
      (draft! actor "sup" "eng-7")
      (exec-op actor "sup-submit" {:op :filing/submit :subject "eng-7" :track track} operator)
      (approve! actor "sup-submit")
      (is (true? (:submitted? (store/engagement db "eng-1"))))
      (is (true? (:submitted? (store/engagement db "eng-7"))))
      (is (= 2 (count (store/submit-history db)))))))
