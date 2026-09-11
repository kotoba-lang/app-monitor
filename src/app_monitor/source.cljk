(ns app-monitor.source
  "The `process/list` seam.

  Nothing here reads a process table. The host supplies `list-fn`, and that is
  where the granted capability is spent. There is no fallback that shells out
  to `ps` when the grant is missing."
  (:require [app-monitor.model :as model]
            [mokuroku.source :as source]))

(defrecord ProcessSource [scope list-fn]
  source/ISource
  (-descriptor [_] (model/descriptor scope))
  (-fetch [_] (model/listing->items (list-fn))))

(defn process-source
  "A source over an injected `process/list` provider. LIST-FN returns a vector
  of entry maps."
  [scope list-fn]
  (->ProcessSource scope list-fn))

(defn fixture-source [scope entries]
  (process-source scope (constantly entries)))

(def denied
  {:process/state :denied
   :process/capability model/process-capability
   :process/entries []})

(defn granted [entries]
  {:process/state :granted
   :process/capability model/process-capability
   :process/entries (vec entries)})

(defn denied? [r] (= :denied (:process/state r)))

;; A machine with no processes does not exist, so unlike a directory listing
;; an empty process table is always a fault rather than a fact. It is still
;; reported as denied-or-empty rather than rendered as a plain empty list,
;; because the two have different fixes.
(defn suspicious-empty? [r]
  (and (not (denied? r)) (empty? (:process/entries r))))
