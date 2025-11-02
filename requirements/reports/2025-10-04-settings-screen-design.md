# 2025-10-04 設定系画面（OSS / プライバシーポリシー）設計メモ

## OSSライセンス画面
- 目的: 導入ライブラリのライセンスを利用者が確認できるようにする。
- ライブラリ: Google Play services OSS Licenses (`com.google.android.gms:play-services-oss-licenses`) と `oss-licenses-plugin` を導入。
- ビルド設定
  - `gradle/libs.versions.toml` に `playServicesOssLicenses` と `ossLicensesPlugin` を追加。
  - `app/build.gradle.kts` で依存関係とプラグインを適用。
  - `MapShoppingListApplication` で初期化不要。`OssLicensesMenuActivity` を直接起動する。
- UI
  - `OssLicensesRoute`（新規）: Compose 画面。
    - `TopAppBar` + 戻るボタン。
    - 説明テキストと「ライセンス一覧を開く」ボタン。
    - ボタン押下時に `context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))` で標準ライセンス画面を起動。
    - 起動後は本画面に戻れるよう `OssLicensesMenuActivity` の戻る操作に任せる。
    - ライセンス画面タイトルは `OssLicensesMenuActivity.setActivityTitle(getString(...))` でアプリ固有文言に変更。
- ナビゲーション
  - `Destinations.OSS_LICENSES` を追加し、ShoppingList のメニューから遷移。

## プライバシーポリシー画面
- 目的: 位置情報等の取り扱いとユーザー権利を明示する。
- コンテンツ: 固定テキスト（ドラフトは別紙 `2025-10-04-privacy-policy-draft.md`）。
- UI
  - `PrivacyPolicyRoute`（新規）: `Scaffold` + `TopAppBar`。
  - 本文は `LazyColumn` で見出しと段落を `Text` 表示。
  - 位置情報の利用目的、保存期間、第三者提供なし、権限の再設定方法、問い合わせ先を盛り込む。
- ナビゲーション
  - `Destinations.PRIVACY_POLICY` を追加し、メニューから遷移。

## ShoppingList のメニュー構成
- `TopAppBar` の `actions` に `IconButton`(MoreVert) + `DropdownMenu`。
- メニュー項目
  1. 「地点を管理」→ `PLACE_MANAGEMENT`
  2. 「プライバシーポリシー」→ `PRIVACY_POLICY`
  3. 「オープンソースライセンス」→ `OSS_LICENSES`
- メニュー項目の `onClick` で `DropdownMenu` を閉じた後にコールバックを呼ぶ。

## テスト方針
- `OssLicensesRoute` のユニットテスト: ボタン押下で `Intent` が発行されることをロジックレベルで確認（`ContextWrapper` + 偽実装）。
- `PrivacyPolicyRoute` は静的テキストのため、スクリーンショット or コンポジションテストが望ましいが、最低でも `PrivacyPolicyViewModel` 不要と判断し UI スナップショットテストは今回は省略。
- メニューまわりは `ShoppingListViewModel` 未使用のため、`ShoppingListScreen` の Compose テストでメニュー項目表示/クリックを検証することを検討。

