# 未紐付けアイテム向けカテゴリ判定バックエンド仕様

作成日: 2026-03-30

## 1. 目的

Android アプリの未紐付けアイテム近接通知で使用する「アイテム名 -> 周辺店舗カテゴリ」の判定 API を、別プロジェクトのバックエンドとして実装するための仕様を定義する。

このバックエンドは店舗検索そのものを担当しない。責務は、アイテム名から Google Places の検索に適したカテゴリ候補を返すことに限定する。

## 2. スコープ

### 対象

- アイテム名のカテゴリ推定
- Places 検索向けカテゴリの返却
- 推定根拠の最小限ログ
- 再利用しやすいキャッシュ

### 非対象

- Android アプリ実装
- Google Places 検索そのもの
- 通知送信
- バックエンド管理画面
- ユーザー認証基盤の詳細実装

## 3. 前提

- バックエンドは AWS 上で稼働させる
- 実装言語は Node.js
- 生成 AI は可能な限り AWS リソースを利用する
- そのためカテゴリ推定モデルは Amazon Bedrock を第一候補とする
- Android アプリはこのバックエンドへ REST API を呼び出し、その応答をもとに Places API を実行する

## 4. 推奨構成

### 4.1 構成概要

- Amazon API Gateway
- AWS Lambda (Node.js 22 もしくはサポート中の Node.js LTS)
- Amazon Bedrock
- Amazon DynamoDB
- Amazon CloudWatch Logs / Metrics

### 4.2 役割分担

- API Gateway
  - HTTPS エンドポイント公開
  - リクエストバリデーション
  - 将来の認証 / レート制御の入口
- Lambda
  - 入力正規化
  - キャッシュ参照
  - Bedrock 呼び出し
  - Places 用カテゴリ配列の返却
- Bedrock
  - アイテム名からカテゴリ候補を推定
  - JSON Schema に従う構造化出力を返す
- DynamoDB
  - アイテム名ごとのカテゴリ判定キャッシュ
  - TTL による自動期限切れ
- CloudWatch
  - 呼び出し回数、エラー、レイテンシ、キャッシュヒット率の監視

## 5. API 仕様

### 5.1 エンドポイント

- `POST /v1/item-category:classify`

### 5.2 リクエスト

```json
{
  "itemName": "牛乳",
  "locale": "ja-JP",
  "country": "JP",
  "maxCategories": 3
}
```

### 5.3 バリデーション

- `itemName`
  - 必須
  - 前後空白除去後 1 文字以上 64 文字以下
- `locale`
  - 任意
  - 省略時は `ja-JP`
- `country`
  - 任意
  - 省略時は `JP`
- `maxCategories`
  - 任意
  - 1 以上 5 以下
  - 省略時は `3`

### 5.4 レスポンス

```json
{
  "normalizedItemName": "牛乳",
  "categories": [
    {
      "placeType": "supermarket",
      "confidence": 0.91,
      "reason": "日常的な食品で、スーパーでの購入可能性が高い"
    },
    {
      "placeType": "convenience_store",
      "confidence": 0.67,
      "reason": "少量販売が一般的でコンビニでも扱われる"
    }
  ],
  "cacheHit": false,
  "modelVersion": "bedrock-model-id-or-profile",
  "generatedAt": "2026-03-30T12:34:56Z"
}
```

### 5.5 エラーレスポンス

- `400 Bad Request`
  - 入力不正
- `429 Too Many Requests`
  - レート制限
- `500 Internal Server Error`
  - Bedrock / DynamoDB / Lambda 内部異常
- `503 Service Unavailable`
  - 一時的な推論不能

エラー形式:

```json
{
  "code": "INVALID_ARGUMENT",
  "message": "itemName must not be blank"
}
```

## 6. 出力カテゴリ仕様

返却値 `categories[].placeType` は Google Places の place type をそのまま返す。

初期対象候補:

- `supermarket`
- `grocery_store`
- `convenience_store`
- `drugstore`
- `home_goods_store`
- `hardware_store`
- `food_store`
- `market`
- `store`

### 6.1 出力ルール

- `confidence` は 0.0 から 1.0
- `categories` は信頼度降順
- 重複カテゴリは禁止
- `maxCategories` を超えて返さない
- 判定不能時は `categories: []` を返す

## 7. Bedrock 推論仕様

### 7.1 推奨 API

- Amazon Bedrock `Converse` API

### 7.2 推奨方式

- Structured Outputs を使い、JSON Schema に従う応答を強制する

### 7.3 期待するモデル責務

- アイテム名を正規化する
- 一般消費者が購入可能な店舗カテゴリを推定する
- Places で使えるカテゴリのみを返す
- 不明な場合は推測を広げすぎず空配列を返す

### 7.4 モデルへの制約

- 固有店舗名を返さない
- Place type 以外の独自カテゴリを返さない
- 過剰に広いカテゴリ列挙をしない
- 医薬品や危険物など特殊商品の場合は保守的に返す

## 8. キャッシュ仕様

### 8.1 保存先

- DynamoDB テーブル `item_category_cache`

### 8.2 パーティションキー

- `cacheKey`
  - 例: `JP#ja-JP#牛乳`

### 8.3 保存項目

- `cacheKey`
- `normalizedItemName`
- `categories`
- `modelVersion`
- `createdAt`
- `expiresAt`

### 8.4 TTL

- 初期値 30 日
- 頻繁に使われる一般名詞は 30 日固定でよい
- 仕様変更時は `modelVersion` を変えて再生成できるようにする

## 9. 認証 / セキュリティ

### 9.1 最低限要件

- TLS 必須
- API Gateway 側でレート制限を持つ
- リクエスト / レスポンスの個人情報は扱わない
- Bedrock へのアクセスは Lambda 実行ロールからのみ許可する

### 9.2 認証方針

- MVP では認証方式を別途決定事項とする
- 候補:
  - Amazon Cognito ベースの JWT
  - API Gateway 側の追加保護

注記:
- モバイルアプリに長期利用の秘密情報を埋め込む前提は避ける

## 10. 非機能要件

### 10.1 性能

- P50 700ms 以内
- P95 2000ms 以内
- キャッシュヒット時は P95 300ms 以内を目標

### 10.2 可用性

- 一時障害時はアプリ側が現在のフォールバック検索へ戻れるようにする

### 10.3 監視

- 4xx / 5xx 件数
- Lambda duration
- Bedrock 呼び出し失敗率
- DynamoDB キャッシュヒット率
- カテゴリ空配列率

## 11. Lambda 実装方針

- Node.js + TypeScript を推奨
- API Gateway Lambda proxy integration を使う
- 入出力スキーマは `zod` 等で検証
- ログは JSON 構造化
- 可能なら AWS Lambda Powertools for TypeScript を利用する

## 12. アプリ連携要件

Android アプリはこの API を呼び出して `categories[].placeType` を受け取り、そのカテゴリを使って Places Nearby Search (New) もしくは同等のカテゴリ検索を行う。

アプリ側の期待動作:

1. アイテム名を送る
2. place type の配列を受け取る
3. 上位カテゴリで周辺店舗検索を行う
4. 結果が弱いときのみ従来のアイテム名検索へフォールバックする

## 13. 未確定事項

- API 認証方式
- Bedrock で使う具体モデル ID
- place type の最終採用集合
- キャッシュ TTL の最終値
- Nearby Search (New) への完全移行時期

## 14. 受け入れ条件

- 指定 JSON Schema どおりのレスポンスを返せる
- `牛乳`, `洗剤`, `トイレットペーパー`, `電池` など代表アイテムで妥当なカテゴリを返せる
- 判定不能語では空配列を返せる
- DynamoDB キャッシュが動作する
- API 障害時にクライアントがフォールバック可能なエラー形式を返す

## 15. 参考

- AWS Bedrock Structured Outputs
- AWS Bedrock Converse API
- API Gateway Lambda proxy integration
- DynamoDB TTL
- Lambda Powertools for TypeScript
- Google Places Place Types / Nearby Search (New)
