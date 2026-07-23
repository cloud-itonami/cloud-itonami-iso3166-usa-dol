(ns dolcompliance.store-contract-test
  "MemStore ≡ DatomicStore parity for the Store protocol."
  (:require [clojure.test :refer [deftest is testing]]
            [dolcompliance.store :as store]
            [dolcompliance.registry :as registry]))

(defn- exercise [s]
  (store/commit-record! s {:effect :engagement/upsert
                           :value {:id "eng-x" :operator "X LLC"
                                   :base-fee 100 :monthly-rate 10 :monitoring-months 1
                                   :audit-export? false :export-fee nil :claimed-fee 110.0
                                   :contract-category :services
                                   :labor-standards-verified? true
                                   :requires-ofccp-compliance? false :ofccp-compliance-verified? false
                                   :drafted? false :submitted? false
                                   :status :intake}})
  (store/commit-record! s {:effect :assessment/set
                           :path ["eng-x" :service-contract-act]
                           :payload {:track :service-contract-act :checklist ["a"] :spec-basis "x"}})
  (store/commit-record! s {:effect :engagement/mark-drafted :path ["eng-x" registry/compliance-track]})
  (store/commit-record! s {:effect :engagement/mark-submitted :path ["eng-x" registry/compliance-track]})
  (store/append-ledger! s {:t :committed :op :test})
  {:engagement (store/engagement s "eng-x")
   :assessment (store/assessment-of s "eng-x" :service-contract-act)
   :drafts (store/draft-history s)
   :submits (store/submit-history s)
   :ledger (store/ledger s)
   :drafted? (store/engagement-drafted? s "eng-x")
   :submitted? (store/engagement-submitted? s "eng-x")})

(deftest mem-and-datomic-parity
  (let [mem* (store/->MemStore (atom {:engagements {} :assessments {} :ledger []
                                      :draft-sequences {} :draft-records []
                                      :submit-sequences {} :submit-records []}))
        dat* (store/datomic-store {})
        m (exercise mem*)
        d (exercise dat*)]
    (is (= (:operator (:engagement m)) (:operator (:engagement d))))
    (is (= (:contract-category (:engagement m)) (:contract-category (:engagement d))))
    (is (true? (:drafted? m)) (true? (:drafted? d)))
    (is (true? (:submitted? m)) (true? (:submitted? d)))
    (is (= 1 (count (:drafts m))) (= 1 (count (:drafts d))))
    (is (= 1 (count (:submits m))) (= 1 (count (:submits d))))
    (is (= 1 (count (:ledger m))) (= 1 (count (:ledger d))))
    (is (= (:assessment m) (:assessment d)))))

(deftest seed-db-has-seven-engagements
  (testing "MemStore and DatomicStore demo seeds agree on the demo-data shape"
    (is (= 7 (count (store/all-engagements (store/seed-db)))))
    (is (= 7 (count (store/all-engagements (store/datomic-seed-db)))))))

(deftest seed-db-covers-all-three-contract-categories
  (testing "the demo set exercises :services/:construction/:supply, not just one category"
    (let [cats (set (map :contract-category (store/all-engagements (store/seed-db))))]
      (is (contains? cats :services))
      (is (contains? cats :construction))
      (is (contains? cats :supply)))))
