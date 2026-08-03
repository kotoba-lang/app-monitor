# app-monitor

**Activity Monitor, on [`mokuroku`](https://github.com/kotoba-lang/mokuroku).**
What is running, and what it costs.

Design: [ADR-2608035000](https://github.com/com-junkawasaki/root/blob/main/90-docs/adr/2608035000-app-standard-application-suite-on-a-shared-catalog-kernel.edn).

## Two capabilities, on purpose

`process/list` enumerates processes and is **personal data** — a process list
says which applications someone uses and often which documents they have open.
`system/metrics` reads aggregate CPU, memory and load, and says none of that.

Folding them into one grant would mean a menu-bar CPU gauge had to be given
the ability to read every process name. They are separate so it does not.

## A process is a pid *and* a start time

Pids are reused. A process can exit and its number be handed to something else
between two refreshes, and a selection keyed on the number alone would quietly
retarget — which for Quit means killing a process the user never selected.

```clojure
(:item/id (model/entry->item {:pid 903 :started 300 ...}))  ;; => [903 300]
```

The test suite asserts exactly this: select pid 903, let it exit, let a new
process take 903, and the selection must come back **empty** with the old id
reported as dropped.

## Totals come from the rows on screen

`page/header-totals` sums the same items the list renders. Reading the header
from `system/metrics` while the list comes from `process/list` is how a header
ends up saying 68% while the visible rows add to 12%.

## Test

```sh
clojure -M:local:test    # sibling checkouts
clojure -M:test          # pinned git deps
clojure -M:lint
```

design-quality: 100.00 on listing / selection / awaiting-grant (2026-08-03).
