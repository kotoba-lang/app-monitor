(ns app-monitor.model
  "Activity Monitor's domain: what is running, and what it is costing.

  Two capabilities, not one. `process/list` enumerates processes and is
  personal data — a process list says which applications someone uses and
  often which documents they have open. `system/metrics` reads aggregate CPU,
  memory and load, which says none of that. Folding them together would mean
  a menu-bar CPU gauge had to be granted the ability to see every process
  name, which is exactly the over-granting the capability model exists to
  prevent."
  (:require [mokuroku.item :as item]
            [mokuroku.source :as source]))

(def process-capability "process/list")
(def metrics-capability "system/metrics")

(def columns
  [(source/attribute :name "Process Name" :string)
   (source/attribute :cpu "% CPU" :percent)
   (source/attribute :memory "Memory" :bytes)
   (source/attribute :user "User" :string)
   (source/attribute :threads "Threads" :number)
   (source/attribute :state "State" :string)
   ;; A pid is an identity, not an axis. Sorting by it means sorting by
   ;; roughly-when-it-started, which nobody asks for and which looks like a
   ;; meaningful order.
   (source/attribute :pid "PID" :number false)])

(def commands
  "Quit is the only mutation, and it is destructive: the kernel will demand a
  confirmation for it and refuse to batch it silently. There is no Force Quit
  until a provider exists that can distinguish the two — offering both when
  only one is implemented is worse than offering neither."
  #{:quit :copy-path})

(defn descriptor
  ([] (descriptor "All Processes"))
  ([scope]
   (source/descriptor
    {:id :app-monitor/processes
     :item-kind :process
     :label scope
     :capability process-capability
     :commands commands
     :attributes columns})))

(def states
  "The states a `process/list` provider may report. An unrecognised state is
  passed through rather than coerced — inventing :unknown would erase the one
  piece of information the provider actually had."
  #{:running :sleeping :stopped :zombie :idle})

(defn entry->item
  "Normalise one provider row.

  The id is the pid **and** the start time, not the pid alone. Pids are reused:
  a process can exit and its number be handed to something else between two
  refreshes, and a selection keyed on the number alone would quietly retarget —
  which for Quit means killing a process the user never selected."
  [{:keys [pid started name cpu memory user threads state] :as entry}]
  (item/item [pid (or started 0)]
             :process
             (or name (str "pid " pid))
             (cond-> {:pid pid
                      :name (or name (str "pid " pid))
                      :cpu cpu
                      :memory memory
                      :user user
                      :threads threads}
               (contains? entry :state) (assoc :state state))))

(defn listing->items [entries]
  (mapv entry->item entries))

(def busiest-first
  "The default: most expensive process at the top, which is the question
  Activity Monitor is opened to answer."
  [[:cpu :desc]])

(def default-query
  {:query/sort busiest-first :query/text "" :query/filters []})

;; ------------------------------------------------------------- aggregates

(defn totals
  "Fleet-level numbers derived from the same rows, so the header and the list
  can never disagree.

  Not read from `system/metrics`: two sources for one number is how a header
  ends up saying 68% while the visible rows add to 12%."
  [items]
  (let [cpus (keep #(item/attr % :cpu) items)
        mems (keep #(item/attr % :memory) items)]
    {:total/processes (count items)
     :total/cpu (reduce + 0 cpus)
     :total/memory (reduce + 0 mems)}))
