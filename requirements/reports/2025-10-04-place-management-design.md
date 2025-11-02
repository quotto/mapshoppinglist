# 2025-10-04 地点管理機能 設計メモ

## 目的
- 登録済み地点の名称変更・削除をユーザーが自己完結できるようにする。
- 削除時は紐づくアイテムとジオフェンス購読を確実に解除し、整合性を維持する。
- トップ画面のメニューから遷移できる専用画面を用意し、リスト操作に特化したUIを提供する。

## ドメイン/データ層の変更
- `PlacesRepository`
  - 追加: `suspend fun loadAll(): List<Place>` 既存順序（`is_active DESC, last_used_at DESC`）採用。
  - 追加: `suspend fun updateName(placeId: Long, newName: String)` 名称のトリムとバリデーション（空文字不可）をリポジトリ層で保証。
- `DefaultPlacesRepository`
  - 新メソッド `loadAll()` と `updateName()` を実装。
  - `updateName()` 内で `placesDao.findById()` → `copy(name=trimmed, lastUsedAt=now)` → `placesDao.update()`。
  - 名称更新はアクティブ状態維持。`lastUsedAt` を更新して最近利用順に反映。
- DAO
  - `PlacesDao`: 既存 `loadAll()` をそのまま再利用。追加SQL不要。
- ユースケース
  - `LoadAllPlacesUseCase`（新規）: リポジトリから全件取得しそのまま返却。
  - `UpdatePlaceNameUseCase`（新規）: 名称更新後に `GeofenceSyncScheduler.scheduleImmediateSync()` は不要と判断（位置情報に変化なし）。ただし再登録を強制したい場合に備え、引数フラグで制御できるよう `scheduleOnRename: Boolean = false` を用意。
  - 既存 `DeletePlaceUseCase`: 挙動は現状維持。地点削除後に `scheduleImmediateSync()` が呼ばれるためジオフェンス購読は差分同期で削除される。

## プレゼンテーション層の追加
- パッケージ: `ui.placemanage`
  - `PlaceManagementViewModel`
    - 依存: `LoadAllPlacesUseCase`, `UpdatePlaceNameUseCase`, `DeletePlaceUseCase`。
    - 状態: `PlaceManagementUiState`
      - `isLoading`, `places: List<ManagedPlaceUiModel>`, `dialogState`（編集/削除確認の状態）, `errorMessage`, `snackbarMessage`。
    - 初期化時に `loadAllPlaces()` を実行。削除・更新後も再ロード。
    - エラーハンドリング: 共通例外をSnackbar表示。
    - バリデーション: 名称が空白の場合は `dialogState` にエラーメッセージをセットし保存拒否。
  - `PlaceManagementRoute`
    - `Scaffold` + `TopAppBar`（戻るボタン付き）。
    - `LazyColumn` で `ManagedPlaceRow` を表示（名称、状態バッジ（`isActive`））。
    - 行のアイコンボタン: 編集（ペンアイコン）、削除（ゴミ箱）。
    - 編集ダイアログ: `AlertDialog` + `OutlinedTextField`。
    - 削除確認ダイアログ: `AlertDialog`。

## ナビゲーション
- `MapShoppingListApp` の `Destinations` に `PLACE_MANAGEMENT` を追加。
- `ShoppingListRoute` の overflow メニューから遷移。
- 戻り動作: `navController.popBackStack()`。

## テスト方針
- `UpdatePlaceNameUseCaseTest`: 正常系（トリム・更新・スケジューラ未呼び出し）と例外系（空名称→例外）。
- `PlaceManagementViewModelTest`: 
  - 初期読込で `places` が流れてくること。
  - 編集確定でユースケース呼び出しと再読み込みが行われること。
  - 削除確定で `DeletePlaceUseCase` 呼び出し。
- `DefaultPlacesRepositoryTest`: テスト用 in-memory DB で名称更新がDBへ反映されることを確認。

## 非機能・UI検討
- リストが空の場合は空状態メッセージを表示。
- `isActive` 地点には「通知対象」ラベル、非アクティブには「未通知」ラベルを表示し、ユーザーが削除時の影響を理解しやすくする。
- Snackbar を用いて更新完了/削除完了を通知。

