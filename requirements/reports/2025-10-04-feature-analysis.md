# 2025-10-04 機能追加調査メモ

## 共通前提
- プロジェクトは Jetpack Compose + MVVM + Clean Architecture で構成。
- 依存性注入は `MapShoppingListApplication` で手動管理。
- 文字列リソースは `app/src/main/res/values/strings.xml` に集約されている。

## 1. 登録済み地点の修正・削除
- データ層
  - `PlaceEntity` (`app/src/main/java/com/mapshoppinglist/data/local/entity/PlaceEntity.kt`): 名称・緯度経度・最終利用日時・`isActive` を保持。名称更新は可能だが、現状 UI からの編集経路なし。
  - `PlacesDao` (`.../data/local/dao/PlacesDao.kt`): `update`・`delete` メソッドを保有。全件取得は `loadAll()`（名称順ではなく利用状態/日時順）。
  - `DefaultPlacesRepository` (`.../data/repository/DefaultPlacesRepository.kt`): `addPlace`・`deletePlace` は実装済みだが名称更新 API が存在しない。削除時はアイテム紐づきが外れるかを確認 → `ItemPlaceCrossRef` の `onDelete = CASCADE` により自動削除済み。
  - `refreshActiveState` がリンク操作時のみ呼ばれるため、名称更新時のアクティブ状態維持方針を検討する必要あり。
- ドメイン層
  - `PlacesRepository` インターフェースに更新系メソッドなし。新たに名称更新用シグネチャを追加する必要がある。
  - 既存の `DeletePlaceUseCase` は repository 経由削除後に `GeofenceSyncScheduler.scheduleImmediateSync()` を呼び出す。削除 UI はこのユースケースを使えばジオフェンスの購読解除要件を満たす。
  - `BuildGeofenceSyncPlanUseCase` はアクティブ地点との差分から削除対象ジオフェンスを算出。地点削除後に同期が走れば購読解除が反映される。
- プレゼンテーション層
  - 現行 UI では地点の一覧・編集画面が存在せず、`RecentPlacesRoute` は閲覧のみで編集不可。
  - アイテム詳細 (`ItemDetailRoute` / `ItemDetailViewModel`) で紐づき解除は可能だが地点そのものの編集・削除は不可。
  - ナビゲーションは `MapShoppingListApp` で Compose Navigation を構成しており、新規画面を追加する際は `Destinations` にエントリ追加が必要。
- テスト
  - `DefaultPlacesRepository` のユニットテスト未実装。ユースケースは `PlaceUseCasesTest` (`app/src/test/java/com/mapshoppinglist/domain/usecase/PlaceUseCasesTest.kt`) で削除時の同期呼び出しを確認している。更新機能追加時は同様のテストを追加する必要がある。

## 2. オープンソースライセンス表示画面
- 現状アプリ内に OSS ライセンスを表示する機能は未実装。
- `third_party/licenses/` 配下に `dm_sans_OFL.txt` が存在し、フォントライセンスを手動で表示する必要がある可能性。
- Google OSS Licenses ライブラリ等は依存関係に含まれていないため、自前で一覧を用意するか、ライブラリ導入を検討する必要あり。
- ナビゲーション経路がないため、新規画面とメニュー（例: TopAppBar のオーバーフローメニューや設定画面）を設ける必要がある。

## 3. プライバシーポリシー画面
- 位置情報利用に関する説明は要求仕様のみで、アプリ内の表示画面は存在しない。
- プライバシーポリシー文面を固定テキストとして Compose 画面化する想定。将来的に外部URL参照とするか、アプリ内静的ページとするかを決定する必要がある。
- こちらもナビゲーション先が存在しないため、OSS ライセンス画面同様に画面遷移ルートを整備し、トップバーやメニューからアクセスできるようにする。

## 関連リソースと今後の検討ポイント
- 画面追加に伴い、`MapShoppingListApp` の NavHost と `ui.theme` のスタイル調整が必要になる可能性。
- 文字列や文面は `strings.xml` へ追加し、必要に応じて段落レイアウト（`LazyColumn` + `Text`）を設計する。
- 新規 ViewModel / UseCase を追加する場合、`MapShoppingListApplication` にも依存の追記が必要。
- UI テストは未整備。画面追加後に Compose テストか Robolectric を用いたユニットテストの追加を検討する。

