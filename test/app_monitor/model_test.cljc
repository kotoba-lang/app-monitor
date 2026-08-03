(ns app-monitor.model-test
  (:require [app-monitor.model :as model]
            [app-monitor.page :as page]
            [app-monitor.source :as source]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [design-quality.audit :as dq]
            [mokuroku.catalog :as catalog]
            [mokuroku.item :as item]))

(def entries
  [{:pid 1 :started 100 :name "launchd" :cpu 0.1 :memory 12582912 :user "root"
    :threads 4 :state :running}
   {:pid 412 :started 200 :name "WindowServer" :cpu 8.0 :memory 268435456
    :user "_windowserver" :threads 12 :state :running}
   {:pid 903 :started 300 :name "nbb" :cpu 42.5 :memory 134217728 :user "jun"
    :threads 6 :state :running}])

(defn- cat-of [es]
  (catalog/refresh (catalog/catalog (source/fixture-source "All Processes" es)
                                    model/default-query)))

(deftest identity-is-pid-plus-start-time
  ;; Pids are reused. A selection keyed on the number alone would quietly
  ;; retarget when a pid is recycled between refreshes — and for Quit that
  ;; means killing a process the user never selected.
  (testing "the same pid with a different start time is a different item"
    (let [a (model/entry->item {:pid 903 :started 300 :name "nbb"})
          b (model/entry->item {:pid 903 :started 999 :name "something else"})]
      (is (not= (:item/id a) (:item/id b)))))

  (testing "a recycled pid does not inherit the old selection"
    (let [c (-> (cat-of entries) (catalog/select [903 300]))
          ;; nbb exited; a new process took pid 903
          recycled (catalog/with-items
                     c (model/listing->items
                        [{:pid 903 :started 777 :name "clojure" :cpu 3.0
                          :memory 1024 :user "jun" :threads 2 :state :running}]))]
      (is (empty? (:selection/ids (:catalog/selection recycled)))
          "the new process is NOT selected")
      (is (= #{[903 300]} (:selection/dropped (:catalog/selection recycled)))
          "and the app can say the selected process exited"))))

(deftest busiest-first-is-the-default
  (is (= ["nbb" "WindowServer" "launchd"]
         (mapv :item/label (:result/items (catalog/result (cat-of entries)))))
      "the question Activity Monitor is opened to answer"))

(deftest quit-is-destructive-and-says-so
  (let [c (catalog/select (cat-of entries) [903 300])
        p (catalog/propose c :quit)]
    (is (true? (:proposal/destructive? p)))
    (is (true? (:proposal/requires-confirmation? p)))
    (is (= "process/list" (:proposal/capability p)))
    (is (= :process/signal (:proposal/effect p))))

  (testing "quitting a multi-selection still demands confirmation"
    (let [p (catalog/propose (catalog/select-all (cat-of entries)) :quit)]
      (is (= 3 (count (:proposal/targets p))))
      (is (true? (:proposal/requires-confirmation? p)))))

  (testing "force-quit is not offered, because no provider implements it"
    (is (= :unknown-command
           (:proposal/refused (catalog/propose (cat-of entries) :force-quit))))))

(deftest totals-come-from-the-rows-on-screen
  ;; Two sources for one number is how a header says 68% while the visible
  ;; rows add to 12%.
  (let [t (page/header-totals (cat-of entries))]
    (is (= 3 (:total/processes t)))
    (is (= 50.6 (:total/cpu t)))
    (is (= 415236096 (:total/memory t)))))

(deftest the-two-capabilities-are-separate
  ;; A menu-bar CPU gauge must not need the right to read every process name.
  (is (not= model/process-capability model/metrics-capability))
  (is (= "process/list" (:source/capability (model/descriptor))))
  (testing "a denied grant is distinguishable from an impossible empty table"
    (is (source/denied? source/denied))
    (is (source/suspicious-empty? (source/granted [])))
    (is (not (source/suspicious-empty? (source/granted entries))))))

(deftest an-unrecognised-state-is-passed-through-not-coerced
  (let [it (model/entry->item {:pid 7 :started 1 :name "odd" :state :uninterruptible})]
    (is (= :uninterruptible (item/attr it :state))
        "coercing to :unknown would erase what the provider actually knew")
    (is (not (contains? model/states (item/attr it :state))))))

(deftest window-meets-the-design-quality-floor
  (let [pages {"listing" (page/render (cat-of entries))
               "selection" (page/render (catalog/select-all (cat-of entries)))
               "awaiting-grant" (page/render
                                 (catalog/catalog
                                  (source/fixture-source "All Processes" [])
                                  model/default-query))}
        {:keys [overall pages] :as report} (dq/audit pages {:extra-axes dq/extra-axes})]
    (println "design-quality: aggregate" overall)
    (doseq [[nm r] (sort-by key pages)] (println " " nm (:overall r)))
    (is (>= overall 98.0) (pr-str (:findings report)))
    (doseq [[nm r] pages] (is (>= (:overall r) 98.0) nm))))

(deftest the-empty-state-does-not-pretend-a-machine-runs-nothing
  (let [html (page/render-html (catalog/catalog
                                (source/fixture-source "All Processes" [])
                                model/default-query))]
    (is (str/includes? html "process/list"))))
