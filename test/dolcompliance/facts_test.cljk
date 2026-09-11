(ns dolcompliance.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [dolcompliance.facts :as facts]))

(deftest service-contract-act-has-spec-basis
  (let [sb (facts/spec-basis :service-contract-act)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= :services (:contract-category sb)))
    (is (= 2500 (:threshold-usd sb)))
    (is (= "Wage and Hour Division (WHD), U.S. Department of Labor" (:owner-authority sb)))))

(deftest davis-bacon-act-has-spec-basis
  (let [sb (facts/spec-basis :davis-bacon-act)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= :construction (:contract-category sb)))
    (is (= 2000 (:threshold-usd sb)))
    (is (= "1931" (:enacted sb)))
    (is (= "Wage and Hour Division (WHD), U.S. Department of Labor" (:owner-authority sb)))))

(deftest walsh-healey-pca-has-spec-basis
  (let [sb (facts/spec-basis :walsh-healey-pca)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= :supply (:contract-category sb)))
    (is (= "Wage and Hour Division (WHD), U.S. Department of Labor" (:owner-authority sb)))))

(deftest ofccp-section503-vevraa-has-spec-basis
  (let [sb (facts/spec-basis :ofccp-section503-vevraa)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= "Office of Federal Contract Compliance Programs (OFCCP), U.S. Department of Labor" (:owner-authority sb)))
    (testing "the EO 11246 rescission caveat is present and correctly dated"
      (is (re-find #"2025-01-21" (:eo-11246-status-note sb)))
      (is (re-find #"(?i)rescind" (:eo-11246-status-note sb)))
      (is (re-find #"Executive Order 14173" (:eo-11246-status-note sb))))))

(deftest eo11246-rescission-boundary-is-not-a-filing-track
  (testing "the boundary entry is spec-basis-citable but never itself drafted/submitted"
    (let [sb (facts/spec-basis :eo11246-rescission-boundary)]
      (is (some? sb))
      (is (string? (:provenance sb)))
      (is (empty? (:required-evidence sb)))
      (is (false? (:filing-track? sb)))
      (is (false? (facts/filing-track? :eo11246-rescission-boundary)))
      (is (= "2025-01-21" (:rescinded sb))))))

(deftest filing-track-defaults-true-for-regulatory-entries
  (is (true? (facts/filing-track? :service-contract-act)))
  (is (true? (facts/filing-track? :davis-bacon-act)))
  (is (true? (facts/filing-track? :walsh-healey-pca)))
  (is (true? (facts/filing-track? :ofccp-section503-vevraa)))
  (is (false? (facts/filing-track? :unknown-track)) "no spec-basis at all -> not a filing track"))

(deftest unknown-track-has-no-spec-basis
  (is (nil? (facts/spec-basis :unknown-track)))
  (is (nil? (facts/spec-basis :zzz))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis :service-contract-act)
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? :service-contract-act all)))
    (is (not (facts/required-evidence-satisfied? :service-contract-act (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? :unknown-track all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage [:service-contract-act :davis-bacon-act :unknown-track])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["unknown-track"] (:missing-tracks c)))))

(deftest catalog-has-exactly-five-entries
  (is (= #{:service-contract-act :davis-bacon-act :walsh-healey-pca
           :ofccp-section503-vevraa :eo11246-rescission-boundary}
         facts/valid-tracks)))

(deftest labor-standards-track-mapping
  (is (= :service-contract-act (facts/labor-standards-track :services)))
  (is (= :davis-bacon-act (facts/labor-standards-track :construction)))
  (is (= :walsh-healey-pca (facts/labor-standards-track :supply)))
  (is (nil? (facts/labor-standards-track nil)))
  (is (nil? (facts/labor-standards-track :unknown))))

(deftest not-a-dol-authority-boundaries
  (testing "SAM.gov/FAR are never modeled as DOL-owned facts anywhere in the catalog"
    (doseq [[_ entry] facts/catalog]
      (is (not (re-find #"(?i)SAM\.gov" (str (:owner-authority entry))))
          "SAM.gov must never be attributed to DOL as owner-authority")
      (is (not (re-find #"(?i)FAR Council" (str (:owner-authority entry))))
          "the FAR Council must never be attributed to DOL as owner-authority"))))
