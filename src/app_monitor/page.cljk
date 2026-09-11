(ns app-monitor.page
  "SSR entry. All chrome comes from `mokuroku-ui`; this only says what a
  process row is called and how its numbers read."
  (:require [app-monitor.model :as model]
            [mokuroku.catalog :as catalog]
            [mokuroku-ui.core :as mui]))

(def view-opts
  {:columns [:name :cpu :memory]
   :formatters {:cpu mui/percent
                :memory mui/human-bytes
                :threads str}
   :noun "processes"
   :search-placeholder "Search processes"
   :empty-title "No processes"
   ;; A machine with no processes does not exist, so an empty list here means
   ;; the provider failed rather than that nothing is running. Saying so beats
   ;; a blank pane that reads as a working app with nothing to show.
   :empty-body "The process provider returned nothing, which should not happen on a running machine."
   :badge (fn [it]
            (case (:state (:item/attrs it))
              :zombie "Zombie"
              :stopped "Stopped"
              nil))
   :title "Activity Monitor"
   :description "What is running, and what it costs."})

(defn render [cat]
  (mui/->page (catalog/view cat) view-opts))

(defn render-html [cat]
  (mui/->html (catalog/view cat) view-opts))

(defn header-totals
  "The numbers above the list, derived from the same rows the list shows."
  [cat]
  (model/totals (:result/items (catalog/result cat))))
