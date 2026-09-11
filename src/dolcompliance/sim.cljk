(ns dolcompliance.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean :services/SCA
  engagement through intake -> assess service-contract-act ->
  labor-compliance-package filing draft (escalate/approve/commit) ->
  filing submit (escalate/approve/commit), then a :construction/DBRA
  engagement requiring OFCCP compliance through the same lifecycle plus
  OFCCP assessment, then a :supply/PCA engagement through the same
  lifecycle plus OFCCP assessment, then shows HARD-hold scenarios
  grounded in the dossier: fabrication defense, EO-11246 stale-
  authority-misattribution defense, fee mismatch, undetermined
  contract-category, missing labor-standards verification, missing
  OFCCP compliance, and double-draft/double-submit."
  (:require [langgraph.graph :as g]
            [dolcompliance.registry :as registry]
            [dolcompliance.store :as store]
            [dolcompliance.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :dol-compliance-operator :phase 3})
(def track registry/compliance-track)

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== engagement/intake eng-1 (clean) ==")
    (println (exec-op actor "t1" {:op :engagement/intake :subject "eng-1"
                                  :patch {:id "eng-1" :operator "Potomac Federal Services LLC"}} operator))

    (println "== compliance/assess eng-1/service-contract-act (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :service-contract-act} operator))
    (println (approve! actor "t2"))

    (println "== filing/draft eng-1 labor-compliance-package (always escalates -- actuation/draft-filing) ==")
    (let [r (exec-op actor "t3" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t3")))

    (println "== filing/submit eng-1 labor-compliance-package (always escalates -- actuation/submit-filing) ==")
    (let [r (exec-op actor "t4" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t4")))

    (println "== compliance/assess eng-2/service-contract-act (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :compliance/assess :subject "eng-2" :track :service-contract-act :no-spec? true} operator))

    (println "== compliance/assess eng-2/eo11246-rescission-boundary (stale-authority misattribution -> HARD hold) ==")
    (println (exec-op actor "t5b" {:op :compliance/assess :subject "eng-2" :track :eo11246-rescission-boundary :misattribute? true} operator))

    (println "== compliance/assess eng-3 (service-contract-act, sets up fee-mismatch) ==")
    (println (exec-op actor "t6" {:op :compliance/assess :subject "eng-3" :track :service-contract-act} operator))
    (println (approve! actor "t6"))
    (println (exec-op actor "t6c" {:op :filing/draft :subject "eng-3" :track track} operator))
    (println (approve! actor "t6c"))
    (println "== filing/submit eng-3 (fee mismatch -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :filing/submit :subject "eng-3" :track track} operator))

    (println "== filing/draft eng-4 (:contract-category nil -- no track applies yet, so evidence-incomplete is vacuously satisfied; still always escalates) ==")
    (let [r (exec-op actor "t8" {:op :filing/draft :subject "eng-4" :track track} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t8")))
    (println "== filing/submit eng-4 (contract-category still undetermined -> HARD hold) ==")
    (println (exec-op actor "t8b" {:op :filing/submit :subject "eng-4" :track track} operator))

    (println "== compliance/assess eng-5/service-contract-act (sets up labor-standards-verification-missing) ==")
    (println (exec-op actor "t10" {:op :compliance/assess :subject "eng-5" :track :service-contract-act} operator))
    (println (approve! actor "t10"))
    (println (exec-op actor "t10c" {:op :filing/draft :subject "eng-5" :track track} operator))
    (println (approve! actor "t10c"))
    (println "== filing/submit eng-5 (labor-standards-verification-missing -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :filing/submit :subject "eng-5" :track track} operator))

    (println "== compliance/assess eng-6 (construction/DBRA, requires OFCCP, sets up ofccp-compliance-missing) ==")
    (println (exec-op actor "t12" {:op :compliance/assess :subject "eng-6" :track :davis-bacon-act} operator))
    (println (approve! actor "t12"))
    (println (exec-op actor "t12c" {:op :compliance/assess :subject "eng-6" :track :ofccp-section503-vevraa} operator))
    (println (approve! actor "t12c"))
    (println (exec-op actor "t12d" {:op :filing/draft :subject "eng-6" :track track} operator))
    (println (approve! actor "t12d"))
    (println "== filing/submit eng-6 (ofccp-compliance-missing -> HARD hold) ==")
    (println (exec-op actor "t13" {:op :filing/submit :subject "eng-6" :track track} operator))

    (println "== compliance/assess eng-7 (supply/PCA, requires + has OFCCP verified) ==")
    (println (exec-op actor "t14" {:op :compliance/assess :subject "eng-7" :track :walsh-healey-pca} operator))
    (println (approve! actor "t14"))
    (println (exec-op actor "t14c" {:op :compliance/assess :subject "eng-7" :track :ofccp-section503-vevraa} operator))
    (println (approve! actor "t14c"))
    (println (exec-op actor "t14d" {:op :filing/draft :subject "eng-7" :track track} operator))
    (println (approve! actor "t14d"))
    (println "== filing/submit eng-7 (clean -- escalates, then commits) ==")
    (let [r (exec-op actor "t15" {:op :filing/submit :subject "eng-7" :track track} operator)]
      (println r)
      (println (approve! actor "t15")))

    (println "== filing/draft eng-1 AGAIN (double-draft -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :filing/draft :subject "eng-1" :track track} operator))

    (println "== filing/submit eng-1 AGAIN (double-submit -> HARD hold) ==")
    (println (exec-op actor "t17" {:op :filing/submit :subject "eng-1" :track track} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft records ==")
    (doseq [r (store/draft-history db)] (println r))

    (println "== submit records ==")
    (doseq [r (store/submit-history db)] (println r))))
