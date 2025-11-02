# 登録地点グループによるリスト表示機能の調査レポート

調査日: 2025-11-02  
調査者: 作業エージェント

## 要件概要

S0/S1画面（買い物リスト一覧）に以下の機能を追加する:
- タブ表示: 「購入状況」「購入場所」
- 「購入状況」: 現在の仕様（購入済み/未購入でグループ化）
- 「購入場所」: アイテムに紐付けられた地点ごとにグルーピング
- 初期表示は「購入状況」タブ

## 現状分析

### 画面構成
- **ファイル:** `app/src/main/java/com/mapshoppinglist/ui/home/ShoppingListScreen.kt`
- **画面名:** S1（設計書では買い物リスト一覧）
- **現在の表示:** 
  - 未購入/購入済みの一覧表示
  - LazyColumn でアイテムを表示
  - Checkbox でステータス変更

### データモデル
**エンティティ:**
- `items`: アイテム情報（id, title, note, is_purchased）
- `places`: 地点情報（id, name, lat_e6, lng_e6）
- `item_place`: 多対多関連（item_id, place_id）

**必要なクエリ:**
1. アイテムに紐付く地点リストの取得
2. 地点ごとのアイテムリストの取得（グループ化）

### UI要件

#### タブ構成
```
┌─────────────┬─────────────┐
│ 購入状況 ●  │  購入場所   │
└─────────────┴─────────────┘
```

#### 購入状況タブ（既存）
- 未購入アイテム一覧
- 購入済みアイテム一覧（折りたたみ可能）

#### 購入場所タブ（新規）
- 地点ごとにグループヘッダーを表示
- 各地点配下にアイテムリストを表示
- 地点未設定のアイテムは「未設定」グループに表示

**表示例:**
```
📍 イオン渋谷店 (3)
  □ 牛乳
  ☑ 卵
  □ パン

📍 セブンイレブン新宿店 (2)
  □ おにぎり
  □ コーヒー

📍 未設定 (1)
  □ ノート
```

## 実装方針

### 1. ViewModel の拡張
- `ShoppingListViewModel` にタブ選択状態を追加
- 地点ごとのグループデータを提供するStateFlowを追加

### 2. Repository/DAO の拡張
- 地点ごとのアイテムを取得するクエリを追加
  ```sql
  SELECT p.*, COUNT(ip.item_id) as item_count
  FROM places p
  LEFT JOIN item_place ip ON p.id = ip.place_id
  LEFT JOIN items i ON ip.item_id = i.id
  GROUP BY p.id
  ORDER BY p.name
  ```

### 3. UI コンポーネント
- `TabRow` でタブ切り替えUIを実装
- グループヘッダーコンポーネントを作成
- LazyColumn で地点とアイテムを表示

### 4. テスト
- タブ切り替えのUIテスト（Espresso）
- 地点グループ表示のスナップショットテスト
- データ取得ロジックの単体テスト

## 関連ファイル

### 既存ファイル（修正対象）
- `app/src/main/java/com/mapshoppinglist/ui/home/ShoppingListScreen.kt`
- `app/src/main/java/com/mapshoppinglist/ui/home/ShoppingListViewModel.kt`
- `app/src/main/java/com/mapshoppinglist/data/repository/ShoppingListRepository.kt`
- `app/src/main/java/com/mapshoppinglist/data/dao/ShoppingItemDao.kt`

### テストファイル（作成・修正）
- `app/src/androidTest/java/com/mapshoppinglist/ui/home/ShoppingListScreenTest.kt`
- `app/src/test/java/com/mapshoppinglist/ui/home/ShoppingListViewModelTest.kt`
- `app/src/test/java/com/mapshoppinglist/data/repository/ShoppingListRepositoryTest.kt`

## 技術的検討事項

### パフォーマンス
- 地点数が多い場合のグループ表示性能
- LazyColumn の最適化（stickyHeader 活用）

### UX
- グループの開閉状態の保持
- 地点なしアイテムの表示位置（最下部 or 最上部）
- グループ内のソート順（購入状況 or 作成日時）

### エッジケース
- 複数地点に紐づくアイテムの表示（重複表示 or 1箇所のみ）
- 地点削除時のグループ表示更新
- アイテム数0の地点の表示有無
