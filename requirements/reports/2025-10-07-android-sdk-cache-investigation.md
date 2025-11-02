# GitHub Actions における Android SDK セットアップ高速化調査

## 調査目的
- 新設したコンポジットアクション `android-gradle` が毎回 Android SDK のセットアップを実行するため、ワークフロー実行時間が長くなる問題を緩和する方策を整理する。
- GitHub ホストランナー／セルフホストランナー双方で利用可能なキャッシュ戦略や代替アクションを比較検討する。

## ランナー環境の前提
- GitHub ホストランナー (ubuntu-22.04 など) には `/usr/local/lib/android/sdk` 配下に主要な Build-tools・Platforms がプリインストールされている。2024 年 10 月 20 日時点の Ubuntu 20.04 イメージでも NDK 27.2.x などが搭載されている。citeturn2search2
- Self-hosted ランナーは今回利用予定がないため対象外とする。

## キャッシュ活用案
### 1. SDK 再インストール回避 (GitHub ホストランナー)
- 既定ディレクトリ `/usr/local/lib/android/sdk` を直接利用すれば、SDK コマンドラインツールの再取得は不要。追加の `sdkmanager` 実行無しでも最新 NDK や build-tools が同ディレクトリに存在する。citeturn2search2
- プラットフォームやビルドツールが不足するケースでは `sdkmanager` で都度追加。ダウンロード結果を `actions/cache` で `/usr/local/lib/android/sdk/build-tools` や `/usr/local/lib/android/sdk/system-images` に保存すると、次回以降の取得を省略できる。citeturn2search0turn1search2
- ただし GitHub のキャッシュ容量は 1 リポジトリあたり合計 10 GB の制限があり、7 日間アクセスされないキャッシュは自動削除される。citeturn1search2

### 2. NDK/追加コンポーネントのみキャッシュ
- 既存 SDK を使用しつつ、NDK やシステムイメージなど大きいオプションコンポーネントのみ個別キャッシュを行う。例: `/usr/local/lib/android/sdk/ndk` をキー `ndk-${{ inputs.ndk-version }}` で保存し、ヒット時は `sdkmanager` をスキップ。citeturn5search4

## 実装検討ポイント
- コンポジットアクション側で `checkout` を行っているため、ワークフロー本体での追加 `checkout` が不要か検討する (重複ダウンロードを避ける)。
- `actions/cache` はワークフロー呼び出し元で制御するため、コンポジットアクションは「キャッシュを復元済みであること」を前提とした軽量な処理 (`sdkmanager` 実行は必要最小限) を提供する設計が望ましい。
- SDK パス / キャッシュパスは書き込み権限を確認する。GitHub ホストランナーでは `/usr/local/lib/android/sdk` 自体は読み取り済みなので、必要に応じて `$HOME/.android` など書き込み可能なパスをキャッシュ対象にする。citeturn2search0

## 追加リスク
- キャッシュが破棄された場合に `sdkmanager` でリカバリできるよう、ライセンス同意 (`yes | sdkmanager --licenses`) や再インストール手順をワークフローに残しておく。
- GitHub Runner の定期イメージ更新でプリインストールバージョンが変動するため、`hashFiles('gradle/libs.versions.toml')` 等を組み合わせたキー再生成の仕組みを整える。

## 実装計画
- **コンポジットアクションの仕様**
  - 追加入力 `ensure-android-components`（既定値: `false`）を定義。`false` の場合は Android SDK インストール処理をスキップし、環境変数 `ANDROID_HOME`/`ANDROID_SDK_ROOT` を `/usr/local/lib/android/sdk` に設定するのみとする。
  - `ensure-android-components=true` の場合のみ `android-actions/setup-android` を実行し、`required-build-tools`・`required-platforms` 入力（カンマ区切り）で指定されたコンポーネントを `sdkmanager` で確認/取得する。
  - `sdkmanager` 実行時はキャッシュ復元後であることを前提に、ミッシング時のみダウンロードが走るようログを明示する。

- **キャッシュ設計**
  - ワークフロー側で `actions/cache@v4` を利用し、キーは `android-sdk-${{ runner.os }}-${{ hashFiles('gradle/libs.versions.toml') }}` を基本とする。追加で API レベルが異なるジョブはサフィックス（例: `-api${{ env.CI_VERSION_MINOR }}`）を付与。
  - 対象パスは `/usr/local/lib/android/sdk/build-tools`, `/usr/local/lib/android/sdk/platforms`, `/usr/local/lib/android/sdk/platform-tools`, `$HOME/.android` を想定。必要に応じて `system-images` や `ndk` を追加。
  - キャッシュミス時のフォールバックとして、ジョブ終了時に `if: steps.cache.outputs.cache-hit != 'true'` で再保存を実施する。

- **導入ステップ**
  1. `android-gradle` に新入力を追加し、環境変数設定や `sdkmanager` 分岐を実装。
  2. `android-ci.yml` / `android-release.yml` / `android-promote.yml` にキャッシュステップを追加し、既定では `ensure-android-components=false` とする。
  3. Firebase Test Lab 等で追加コンポーネントが必要なジョブのみ `ensure-android-components=true` と `required-build-tools` / `required-platforms` を指定。
  4. ドキュメントにキャッシュキーやフォールバック手順を追記し、ワークフローコメントにも注意書きを追加する。

## 検証方針
- ブランチ push で `android-ci.yml` がキャッシュヒットすることを `actions/cache` のログで確認する。
- release 向け PR で `ensure-android-components=true` ジョブがキャッシュヒット時はダウンロードをスキップし、ミス時のみ `sdkmanager` ログが出力されることを確認する。
- Firebase Test Lab Secret 未設定の場合、`firebase-test` ジョブがスキップされる挙動が維持されているかレビューする。
