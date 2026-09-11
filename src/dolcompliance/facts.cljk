(ns dolcompliance.facts
  "United States Department of Labor (DOL) federal-contractor labor-
  standards compliance catalog -- the ONLY source of regulatory-
  requirement facts this actor is allowed to cite
  (`dolcompliance.governor`'s spec-basis check enforces that every
  proposal touching `:compliance/assess`, `:filing/draft`, or
  `:filing/submit` cites this catalog and nothing invented).

  Every fact below was verified via web search against `en.wikipedia.org`
  during this repo's research pass (2026-07-22/23). Five catalog
  entries, each with its own owner authority and legal basis -- do NOT
  merge them into one undifferentiated 'DOL requirement':

    :service-contract-act   -- McNamara-O'Hara Service Contract Act of
                                1965 (SCA), 41 U.S.C. §§ 6701-6707.
                                Applies to federal SERVICES contracts
                                (principal purpose = furnishing services
                                through service employees), contract
                                value > $2,500. The
                                :contract-category :services track.
    :davis-bacon-act         -- Davis-Bacon Act (1931) / Davis-Bacon and
                                Related Acts (DBRA), 40 U.S.C.
                                §§ 3141-3148. Requires prevailing wages
                                on federal/federally-assisted
                                CONSTRUCTION contracts > $2,000. DOL's
                                Wage and Hour Division (WHD) sets and
                                publishes the wage determinations. The
                                :contract-category :construction track.
    :walsh-healey-pca        -- Walsh-Healey Public Contracts Act (PCA)
                                -- the third statute WHD administers,
                                governing labor standards on federal
                                SUPPLY contracts (distinct from
                                SCA=services and DBRA=construction). The
                                :contract-category :supply track.
    :ofccp-section503-vevraa -- OFCCP's CURRENT live nondiscrimination
                                mandate: Section 503 of the
                                Rehabilitation Act of 1973 (disability)
                                + the Vietnam Era Veterans' Readjustment
                                Assistance Act of 1974 (VEVRAA) --
                                STATUTORY authorities, unaffected by the
                                2025 EO 11246 rescission (see the
                                boundary entry immediately below).
    :eo11246-rescission-boundary -- a NEGATIVE/boundary catalog entry,
                                `:filing-track? false` (nothing is ever
                                drafted/submitted against it). It exists
                                so `dolcompliance.governor`'s
                                eo11246-stale-authority-misattribution
                                check has a citable spec-basis for
                                REJECTING any proposal that treats
                                Executive Order 11246 (1965) as a
                                CURRENT OFCCP affirmative-action-plan
                                requirement -- EO 11246 was RESCINDED
                                2025-01-21 by EO 14173 ('Ending Illegal
                                Discrimination and Restoring
                                Merit-Based Opportunity'). As of 2026
                                OFCCP's EO-11246-based affirmative-
                                action-plan mandate no longer exists.

  What this catalog deliberately does NOT claim (fabrication traps this
  repo's research dossier explicitly flagged -- see README/docs):
    - does NOT attribute SAM.gov registration (System for Award
      Management) to DOL -- it is a GOVERNMENT-WIDE prerequisite
      operated by GSA, not a DOL-specific requirement;
    - does NOT attribute the FAR (Federal Acquisition Regulation) to
      DOL -- it is jointly owned by the FAR Council (DoD + GSA + NASA
      Administrators), not a DOL authority;
    - does NOT claim SCA/DBRA are DOL-exclusive in the sense of 'only
      apply to DOL-let contracts' -- SCA/DBRA apply GOVERNMENTWIDE to
      any qualifying federal contract regardless of which agency is the
      contracting party (a DoD or GSA services/construction contract
      can still carry SCA/DBRA labor standards); this catalog's
      `:owner-authority` for that fact stays DOL-WHD (the administering/
      enforcing authority), never the contracting agency;
    - does NOT include VETS-4212 veteran-employment reporting -- recalled
      from background knowledge but NOT independently verified via live
      fetch during this research pass; deliberately omitted rather than
      encoded on an unverified basis;
    - does NOT cite DFARS, CMMC, DCSA, or GSAM anywhere -- none of these
      are DOL authorities (DFARS/CMMC/DCSA are DoD; GSAM is GSA);
    - does NOT invent a specific OFCCP jurisdictional dollar threshold
      for when Section 503/VEVRAA obligations attach -- an engagement's
      own `:requires-ofccp-compliance?` flag is asserted by the
      operator/advisor and independently verified by the governor, not
      derived from a fabricated numeric threshold.")

(def catalog
  {:service-contract-act
   {:name "McNamara-O'Hara Service Contract Act (SCA)"
    :name-en "McNamara-O'Hara Service Contract Act of 1965 -- labor standards for federal service contracts"
    :owner-authority "Wage and Hour Division (WHD), U.S. Department of Labor"
    :legal-basis "41 U.S.C. §§ 6701-6707 (McNamara-O'Hara Service Contract Act of 1965)"
    :enacted "1965"
    :contract-category :services
    :threshold-usd 2500
    :threshold-note "applies where the contract's principal purpose is furnishing services through service employees, contract value > $2,500"
    :official-portal "https://www.dol.gov/agencies/whd/government-contracts/service-contracts"
    :provenance "https://en.wikipedia.org/wiki/McNamara%E2%80%93O%27Hara_Service_Contract_Act"
    :provenance-secondary
    ["https://en.wikipedia.org/wiki/Wage_and_Hour_Division"]
    :process-description "A federal services contract whose principal purpose is furnishing services through service employees, valued above $2,500, must comply with SCA prevailing wage and fringe-benefit determinations for the labor classifications performing the work. WHD enforces SCA."
    :required-evidence
    ["contract principal-purpose determination record (services, not construction or supply)"
     "SCA wage determination on file for the applicable labor classifications"
     "fringe-benefit compliance record"]}

   :davis-bacon-act
   {:name "Davis-Bacon Act / Davis-Bacon and Related Acts (DBRA)"
    :name-en "Davis-Bacon Act (1931) and Davis-Bacon and Related Acts -- prevailing wages on federal construction contracts"
    :owner-authority "Wage and Hour Division (WHD), U.S. Department of Labor"
    :legal-basis "40 U.S.C. §§ 3141-3148 (Davis-Bacon Act, 1931, and the Davis-Bacon Related Acts)"
    :enacted "1931"
    :contract-category :construction
    :threshold-usd 2000
    :threshold-note "requires prevailing wages on federal/federally-assisted construction contracts > $2,000"
    :official-portal "https://www.dol.gov/agencies/whd/government-contracts/construction"
    :provenance "https://en.wikipedia.org/wiki/Davis%E2%80%93Bacon_Act"
    :provenance-secondary
    ["https://en.wikipedia.org/wiki/Wage_and_Hour_Division"]
    :process-description "A federal or federally-assisted construction contract valued above $2,000 must pay locally-prevailing wages, as set and published by WHD in wage determinations for the relevant locality and labor classification."
    :required-evidence
    ["contract principal-purpose determination record (construction, not services or supply)"
     "WHD wage determination on file for the project locality/labor classification"
     "certified payroll compliance record"]}

   :walsh-healey-pca
   {:name "Walsh-Healey Public Contracts Act (PCA)"
    :name-en "Walsh-Healey Public Contracts Act -- labor standards for federal supply contracts"
    :owner-authority "Wage and Hour Division (WHD), U.S. Department of Labor"
    :legal-basis "Walsh-Healey Public Contracts Act -- the third of the three major government-contract statutes WHD administers, alongside DBRA and SCA"
    :contract-category :supply
    :official-portal "https://www.dol.gov/agencies/whd/government-contracts"
    :provenance "https://en.wikipedia.org/wiki/Wage_and_Hour_Division"
    :process-description "A federal SUPPLY contract (manufacture or furnishing of materials, supplies, articles, or equipment) is subject to Walsh-Healey Public Contracts Act labor standards, distinct from SCA (services) and DBRA (construction). WHD administers all three."
    :required-evidence
    ["contract principal-purpose determination record (supply, not services or construction)"
     "Walsh-Healey compliance record for the applicable labor classifications"]}

   :ofccp-section503-vevraa
   {:name "OFCCP nondiscrimination compliance -- Section 503 (disability) + VEVRAA (veterans)"
    :name-en "OFCCP's current live mandate: Section 503 of the Rehabilitation Act of 1973 + Vietnam Era Veterans' Readjustment Assistance Act of 1974 (VEVRAA)"
    :owner-authority "Office of Federal Contract Compliance Programs (OFCCP), U.S. Department of Labor"
    :legal-basis "Section 503 of the Rehabilitation Act of 1973 (disability nondiscrimination/affirmative action) + Vietnam Era Veterans' Readjustment Assistance Act of 1974 (VEVRAA, veteran nondiscrimination/affirmative action) -- both STATUTORY authorities, unaffected by the 2025 rescission of Executive Order 11246 (see `:eo11246-rescission-boundary`)"
    :official-portal "https://www.dol.gov/agencies/ofccp"
    :provenance "https://en.wikipedia.org/wiki/Office_of_Federal_Contract_Compliance_Programs"
    :provenance-secondary
    ["https://en.wikipedia.org/wiki/Executive_Order_11246"]
    :eo-11246-status-note
    "Executive Order 11246 (1965), OFCCP's FORMER primary affirmative-action authority for race/sex/national-origin, was RESCINDED 2025-01-21 by Executive Order 14173 ('Ending Illegal Discrimination and Restoring Merit-Based Opportunity'). As of 2026, OFCCP no longer administers an EO-11246-based affirmative-action-plan mandate -- current live statutory scope is Section 503 + VEVRAA only. See `:eo11246-rescission-boundary` below."
    :process-description "A covered federal contractor's Section 503 (disability) and VEVRAA (veteran) nondiscrimination and affirmative-action obligations to OFCCP remain live statutory requirements, independent of and unaffected by the 2025 rescission of Executive Order 11246."
    :required-evidence
    ["Section 503 disability nondiscrimination/affirmative-action compliance record"
     "VEVRAA veteran nondiscrimination/affirmative-action compliance record"]}

   :eo11246-rescission-boundary
   {:name "Executive Order 11246 (1965) affirmative-action mandate -- RESCINDED 2025-01-21, historical fact only"
    :name-en "Executive Order 11246 -- rescinded by Executive Order 14173 on 2025-01-21; NOT a current OFCCP requirement (boundary entry)"
    :owner-authority "Executive Office of the President (rescission authority via EO 14173) -- OFCCP no longer administers EO 11246; this entry exists to REJECT, not assert, an EO-11246-based claim"
    :legal-basis "Executive Order 11246 (1965) was rescinded by Executive Order 14173, 'Ending Illegal Discrimination and Restoring Merit-Based Opportunity', signed 2025-01-21"
    :rescinded "2025-01-21"
    :official-portal "https://en.wikipedia.org/wiki/Executive_Order_11246"
    :provenance "https://en.wikipedia.org/wiki/Executive_Order_11246"
    :provenance-secondary
    ["https://en.wikipedia.org/wiki/Office_of_Federal_Contract_Compliance_Programs"]
    :filing-track? false
    :process-description "Executive Order 11246 required covered federal contractors to maintain written Affirmative Action Plans (AAPs) addressing race/sex/national-origin, enforced by OFCCP. That order was RESCINDED 2025-01-21 by EO 14173. A proposal that treats an EO-11246-based Affirmative Action Plan as a still-current OFCCP filing requirement is asserting a stale, no-longer-live authority -- OFCCP's live mandate today is Section 503 + VEVRAA only (`:ofccp-section503-vevraa`)."
    :required-evidence []}})

(def valid-tracks (set (keys catalog)))

(defn spec-basis [track] (get catalog track))

(defn coverage
  ([] (coverage (keys catalog)))
  ([tracks]
   (let [have (filter catalog tracks) missing (remove catalog tracks)]
     {:requested (count tracks) :covered (count have)
      :covered-tracks (vec (sort (map name have)))
      :missing-tracks (vec (sort (map name missing)))
      :note "R0 catalog seed -- service-contract-act + davis-bacon-act + walsh-healey-pca + ofccp-section503-vevraa + eo11246-rescission-boundary, USA-DOL agency scope"})))

(defn required-evidence-satisfied? [track submitted]
  (when-let [{:keys [required-evidence]} (spec-basis track)]
    (= (count required-evidence) (count (filter (set submitted) required-evidence)))))

(defn evidence-checklist [track] (:required-evidence (spec-basis track) []))

(defn filing-track?
  "Does `track`'s catalog entry represent something that is ever itself
  drafted/submitted as a filing (as opposed to a citation-only /
  boundary entry like `:eo11246-rescission-boundary`)? Defaults to true
  when the catalog entry does not say otherwise -- only
  `:eo11246-rescission-boundary` opts out today."
  [track]
  (let [sb (spec-basis track)]
    (boolean (and sb (not (false? (:filing-track? sb)))))))

(defn labor-standards-track
  "Which catalog track applies for a federal contract of
  `contract-category` (`:services`/`:construction`/`:supply`)? SCA,
  DBRA, and PCA are mutually exclusive by the contract's principal
  purpose -- exactly one always applies to a covered federal contract.
  Returns nil for an unrecognized/undetermined category."
  [contract-category]
  (case contract-category
    :services :service-contract-act
    :construction :davis-bacon-act
    :supply :walsh-healey-pca
    nil))
