# 作業ログ (worklog)

## 全体の要点

- BW_clock TV版からスマホ版へ移植。縦画面フルスクリーンのアナログ時計アプリ (v1.0)
- ホーム画面ウィジェット版を追加。`drawClock()` をアプリとウィジェットで共有 (v1.1)
- 針スタイル (ROUNDED/SQUARED/TAPERED) と ja/en 言語切替を追加 (v1.2)

## やりかけ・未完了

- ウィジェット時計ズレ修正（項目1+2+4 + force-stop 復旧）は実装・コミット済み。onResume 再アーム追加分の再ビルドが未了
- 実機検証項目: 数時間放置後のズレ実測、再起動後の継続動作、Doze 明けの追いつき、force-stop → アプリ起動での復活
- 項目3（SCREEN_ON 動的登録）は見送り。点灯瞬間の古い表示が実機で気になる場合のみ追加

## 前回の作業（2026-07-28）

- ズレ修正の項目1+2+4 を実装しコミット（28152e6）: `setRepeating(RTC)` を廃止し、分境界への one-shot `setExactAndAllowWhileIdle(RTC_WAKEUP)` 自己連鎖方式へ。API 3分岐 + inexact フォールバック、`USE_EXACT_ALARM` 等の permission 追加
- Android 16 エミュレータで動作検証: exact alarm が分境界（`origWhen=XX:00.000` / `window=0`）に登録され毎分自走、ウィジェット描画が実時刻と一致
- エミュレータの凍結は失敗した Run の force-stop が原因と特定。「force-stop 後はアプリを開いても復活しない」穴が判明し、承認を得て MainActivity.onResume での再アームを追加
