# app-monitor（capability 判定）— itonami（dapp）上の分散型設計

状態: 第 1 スライス実装（2026-10-20）。正本はこの文書。
追跡: Linear JUN-128（親 JUN-116）。

## 位置づけ

app-monitor は Activity Monitor —— 「何が動いていて、いくらかかっているか」。
**2 つの capability**（`process/list` は個人データ / `system/metrics` は
集計値）の分離がドメインの中核。

## このスライス: capability 判定コア

| 層 | 実体 | 状態 |
|---|---|---|
| 判定コア guest | `aiueos/monitor_capability.cljk` — grant-judgment（過剰付与・不足・**capability の畳み込み**を独立ビットで拒否）/ command-judgment（quit は確認必須・黙ったバッチ化禁止、Force Quit は区別できるプロバイダが無い限り提供しない）/ state-judgment（**未知の状態は :unknown に畳まない**） | **実装済み・実測** |
| 記述子 | `aiueos/monitor-capability.edn` | **実装済み** |
| parity | `test/app_monitor/capability_parity_test.cljk` — **実物の model.cljk**（mokuroku 経由でロード）の定数と一致（2 capability の分離 / states 閉じた集合かつ :unknown 無し / commands に force-quit 無し） | **実装済み・実測** |

実測: `amu test aiueos/monitor_capability.cljk` → **21/21**（jvm-kir / js / wasm）。
parity → 3 tests / 7 assertions / 0 failures。

## 正本の正確な規則（guest はこれに追従 —— model.cljk の明文）

- **2 つの capability は 1 つに畳まない**: 「メニューバーの CPU ゲージが
  全プロセス名を見る権限を要求することになる = capability model が防ぐ
  過剰付与そのもの」。guest の grant-judgment は過剰・不足・畳み込みを
  独立ビットで拒否。
- **quit のみが mutation で破壊的**: カーネルが確認を要求し、黙ってバッチ化
  することを拒む。**Force Quit は「両方を並べるのは両方無しより悪い」**ので
  提供しない（区別できるプロバイダが現れるまで）。
- **状態は unknown に畳まない**: 「:unknown を発明すると、プロバイダが持って
  いた唯一の情報を消すことになる」。閉じた集合は
  `#{:running :sleeping :stopped :zombie :idle}`、外はそのまま通す。
- PID は「identity であって軸ではない」（ソート不可）—— 判定対象外だが
  設計規則として記録。

## 未実装境界

- totals / busiest-first ソートの判定。page / source 層の判定。
- quit の確認プロトコル（カーネル面）は host。
- mon（DNA）化、aiueos session 載せ（fleet 共通）。
