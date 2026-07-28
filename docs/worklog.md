# 作業ログ (worklog)

## 全体の要点

- BW_clock TV版からスマホ版へ移植。縦画面フルスクリーンのアナログ時計アプリ (v1.0)
- ホーム画面ウィジェット版を追加。`drawClock()` をアプリとウィジェットで共有 (v1.1)
- 針スタイル (ROUNDED/SQUARED/TAPERED) と ja/en 言語切替を追加 (v1.2)

## やりかけ・未完了

- ウィジェット時計ズレ修正は実装・コミット済みで、エミュレータと実機 Pixel 6a の両方で検証完了。残りは実機の長期放置ズレ実測と端末再起動（BOOT_COMPLETED）確認
- 実機のアプリ内設定はアンインストールで初期化されたため、ユーザーによる再設定が必要
- 項目3（SCREEN_ON 動的登録）は見送り。点灯瞬間の古い表示が実機で気になる場合のみ追加

## 前回の作業（2026-07-28）

- ズレ修正を実装しコミット（28152e6 + 0edf822）: 分境界への one-shot `setExactAndAllowWhileIdle(RTC_WAKEUP)` 自己連鎖方式 + BOOT_COMPLETED / MY_PACKAGE_REPLACED / MainActivity.onResume の3復活経路
- エミュレータ（Android 16）と実機 Pixel 6a（Android 17）で検証完了: `USE_EXACT_ALARM` 自動付与、分境界 exact alarm の長時間自走、ウィジェット表示の時刻一致、force-stop → アプリ起動で復活
- 実機の旧 APK（release-key 署名）と Run の debug 署名の不一致で一度アンインストールが発生（設定消失）。再発防止に keystore.properties + signingConfigs で debug/release 共通署名を配線
