# 2025-10-19 CI / Lint アップデート報告

## 対応概要
- GitHub Actions の `android-ci.yml` から `dorny/test-reporter@v2` を廃止し、`jacocoTestReport` の CSV を `cicirello/jacoco-badge-generator@v2` で Job Summary に表示するよう更新。
- `app/build.gradle.kts` に Jacoco プラグインを導入し、`testDebugUnitTest` からレポートを生成する `jacocoTestReport` タスクを定義。
- 通知ワーカーおよび現在地プロバイダで `POST_NOTIFICATIONS` / `ACCESS_FINE_LOCATION` 等の権限チェックを追加し、Lint の MissingPermission エラーを解消。
- 未使用リソース（ブランド色・未使用文字列）を削除し、アダプティブアイコンへ `monochrome` を追加。
- `androidx.compose.ui:ui-test-junit4-android` / `ui-test` をバージョンカタログに登録し、`UseTomlInstead` 警告に対応。

## 残課題
- Lint が提案する依存バージョン更新（AGP 8.13.0 など）は影響範囲が広いため今回の作業では保留。別タスクで検討する。
- ローカルでの `./gradlew testDebugUnitTest jacocoTestReport` 実行時に `/Users/takah/.gradle` 配下の書き込み権限エラーが発生し、ビルド完了を確認できていない。CI での挙動を要確認。
