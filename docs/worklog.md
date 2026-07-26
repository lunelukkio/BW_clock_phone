# 作業ログ (worklog)

## 全体の要点

- BW_clock TV版からスマホ版へ移植。縦画面フルスクリーンのアナログ時計アプリ (v1.0)
- ホーム画面ウィジェット版を追加。`drawClock()` をアプリとウィジェットで共有 (v1.1)
- 針スタイル (ROUNDED/SQUARED/TAPERED) と ja/en 言語切替を追加 (v1.2)

## やりかけ・未完了

- ウィジェット時計の実時間ズレの修正が未着手。原因は `ClockWidgetReceiver.kt` の `setRepeating(AlarmManager.RTC, ...)` が inexact かつ非 WAKEUP で、Doze/App Standby により遅延すること
- 修正計画4項目は `docs/handoff.md` に記録済み。実装は Android Studio Panda 側で進める
- `ACTION_SCREEN_ON` 動的登録案は、ウィジェットのみのアプリではプロセス非常駐のため効かない可能性があり要検証
- KDoc コメント追加分がウィジェット6ファイルで未コミット

## 前回の作業（2026-07-26）

- ウィジェット時計のズレを調査し、描画ではなく更新スケジューリングが原因と特定。`drawClock()` は描画直前の時刻を使うためズレは常に遅れ方向のみ
- 再起動時にアラームが復活しない欠陥も発見（`BOOT_COMPLETED` 不在 + `updatePeriodMillis="0"`）
- `AGENTS.md` と `docs/handoff.md` を新規作成し、修正計画を記録
