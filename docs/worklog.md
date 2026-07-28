# 作業ログ (worklog)

## 全体の要点

- BW_clock TV版からスマホ版へ移植。縦画面フルスクリーンのアナログ時計アプリ (v1.0)
- ホーム画面ウィジェット版を追加。`drawClock()` をアプリとウィジェットで共有 (v1.1)
- 針スタイル (ROUNDED/SQUARED/TAPERED) と ja/en 言語切替を追加 (v1.2)

## やりかけ・未完了

- ウィジェット時計ズレ修正（項目1+2+4）を実装済みだが**未ビルド・未コミット**。ビルドと実機検証は Android Studio Panda 側で行う
- 実機検証項目: 数時間放置後のズレ実測、再起動後の継続動作、Doze 明け（画面点灯）の追いつき
- 項目3（SCREEN_ON 動的登録）は見送り。点灯瞬間の古い表示が実機で気になる場合のみ追加

## 前回の作業（2026-07-28）

- ズレ修正計画のうち項目1+2+4 をユーザー選択により Claude Code 側で実装
- `ClockWidgetReceiver.kt`: `setRepeating(RTC)` を廃止し、分境界への one-shot `setExactAndAllowWhileIdle(RTC_WAKEUP)` 自己連鎖方式へ。API 21-22 / 23-30 / 31+ の3分岐、`canScheduleExactAlarms()` false 時は inexact へ劣化。ウィジェット未配置時は連鎖停止
- `AndroidManifest.xml`: `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM`(maxSdk 32) / `RECEIVE_BOOT_COMPLETED` と `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` の intent-filter を追加
- 前セッション分（KDoc・調査ドキュメント）は 3575299 / 971e5b8 でコミット済みだったと確認
