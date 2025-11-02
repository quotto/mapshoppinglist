# GitHub Actions を用いた Android CI/CD 調査

## 目的
- GitHub Actions を基盤とした CI/CD パイプライン構築のための選択肢・実装ポイントを整理する。
- ユニットテスト、計装テスト、Play Console へのアップロード、トラックのプロモート、バージョン番号付与、Secrets 管理の方法を検討する。

## GitHub Actions ワークフロー構成案
- `push`（`main`/`release` 以外）: `actions/checkout` → JDK セットアップ → Gradle キャッシュ活用 → `./gradlew test assembleDebug` 実行。<https://docs.github.com/en/actions>
- `pull_request`（base=`release`）: 上記に加えて計装テスト → リリースビルド作成 → Play Console 内部テストトラックへアップロード。
- `push`（`release` にマージ後）: 既存内部テストのバージョンコードをプロダクションにプロモート。

## 計装テスト実行手段
- GitHub Actions 上の Android エミュレータは `ReactiveCircus/android-emulator-runner` アクションで起動し、API レベルやデバイス構成を指定可能。API 35 等の最新イメージに対応しており、ワークフロー内で `./gradlew connectedAndroidTest` を実行できる。<https://dev.to/luisramos018/android-cicd-with-github-actions-1hag>
- Firebase Test Lab を併用する場合は `wzieba/firebase-test-lab` アクションで APK/Bundle をアップロードして複数デバイスで並列実行できる。GitHub Secrets にサービスアカウント JSON を登録して利用する。<https://github.com/wzieba/Firebase-Test-Lab>
- 最小/最大 API レベルのエミュレータを GitHub Actions で順次起動すると時間がかかるため、最小 API はエミュレータ、最大 API は Firebase Test Lab などで代替する選択肢も存在。

## Play Console へのアップロード
- Gradle Play Publisher (GPP) v3.10.1 以降は GitHub Actions から Play Developer API にアクセスし内部テストトラックへアップロード可能。`publish` タスクは `play` 拡張で JSON 資格情報とトラック指定を行う。<https://developers.googleblog.com/en/announcing-gradle-play-publisher-3-10/>
- GPP の `promoteArtifacts` タスクを利用すると、内部テスト・アルファ・ベータ等からプロダクションにバイナリをプロモートできる。`fromTrack` と `toTrack`、`releaseStatus` を指定することで審査提出前までを自動化できる。<https://runningcode.github.io/gradle-play-publisher/>
- GPP を利用する場合、Play Console でサービスアカウントを作成し JSON をダウンロード、GitHub Actions Secret に登録する運用が必要。

## Secrets / 認証情報の扱い
- GitHub Actions Secrets はリポジトリまたは環境単位で登録し、ワークフロー内で `${{ secrets.NAME }}` として参照する。暗号化され、ジョブログに自動マスクされる。<https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions>
- Android の `local.properties` に相当する値は、ワークフロー実行時に `gradle.properties` や環境変数に書き出すテンポラリファイルとして注入するのが一般的。Secrets の値は `printf` などでファイル化し、ジョブ終了時に削除する。

## バージョン番号採番方針
- メジャーバージョンは `build.gradle.kts` の `versionMajor`（例）を手動更新し、CI はマイナー・パッチを計算。Gradle の `VersionCatalog` や `version.properties` を導入すると管理しやすい。
- マイナーバージョンを PR 単位で採番する場合、`pull_request` ジョブで `git rev-list` から release ブランチの PR 数をカウントし versionName を決定する、あるいは PR タイトルに記載してマージ時にタグ生成する方法がある。
- パッチバージョンをビルドごとに採番するには、GitHub Actions のラン番号 (`github.run_number`) やコミットハッシュの短縮値を利用して `versionCode` へ反映する例が多い。<https://docs.github.com/en/actions/learn-github-actions/contexts#github-context>

## 留意点
- 計装テストの安定性を高めるため、エミュレータ起動後に `adb logcat` を記録し、失敗時にアーティファクトとして保存する運用を検討。
- Play Console へのアップロードには `PLAY_DEVELOPER_CONSOLE` API 権限を持つサービスアカウント、クラウドプロジェクトのセットアップが必須。
- リリースビルド時の署名キーストア・パスワードも Secrets 化し、キャッシュしないよう `actions/cache` 除外のパスで管理する。

## 実装結果メモ（2025-10-05）
- `_reusable-android-build.yml` を新設し、Gradle 実行・Secrets 注入・アーティファクト管理を共通化。
- `android-ci.yml`（フィーチャーブランチ）、`android-release.yml`（release 向け PR）、`android-promote.yml`（release への push）を作成。
- Release PR ワークフローでは API 29/35 のエミュレータ計装テストに加え、Firebase Test Lab 実行（Secret 設定時のみ）と内部テストトラックへの自動アップロードを実装。
- `gradle/version.properties`/`app/build.gradle.kts` でメジャー・マイナー（PR 番号）・パッチ（Run 番号）採番を自動化し、Gradle Play Publisher 3.10.1 を導入。
- Secrets は `MAPS_API_KEY`、`PLAY_SERVICE_ACCOUNT_JSON`、`ANDROID_KEYSTORE_*`、`FIREBASE_TEST_LAB_SA_JSON` を想定し、ワークフロー終了時に一時ファイルを削除する運用とした。

## 実装結果メモ（2025-10-07）
- `_reusable-android-build.yml` を廃止し、コンポジットアクション `./.github/actions/android-gradle` で Java/Android セットアップと Gradle 実行を共通化。
- 各ワークフローはコンポジットアクションでタスクを実行し、成果物アップロードはワークフローファイル側で制御する構成へ変更。
- Firebase Test Lab ジョブは Secret 未設定時にジョブごとスキップし、bundle 作成はコンポジットアクションで実行するように整理。
- Android SDK の取得コスト削減のため、`actions/cache@v4` で `/usr/local/lib/android/sdk/{build-tools,platforms,platform-tools}` などを復元し、必要なジョブのみ `ensure-android-components=true` で `sdkmanager` を起動するようにした。
