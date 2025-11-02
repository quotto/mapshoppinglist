# 2025-09-30 Geofence EXIT 対応メモ

## 背景
- タスク9-1後の検証で、ジオフェンスに初回入場後に同エリアへ再入場しても `GeofenceReceiver` が発火しない不具合が判明。
- 原因調査の結果、ジオフェンス登録時に `ENTER` のみ監視しており、境界外に出たタイミングを検知できていなかった。

## 対応
1. `GeofenceRegistrar` の遷移タイプを `ENTER | EXIT` に変更。
2. `GeofenceReceiver` で `EXIT` を受信した際にもログを残し、同期ワーカーをキックするように調整。
3. 端末再起動時は GeofenceRegistry の差分ではなく全件再登録となるよう、`GeofenceSyncWorker` に `forceRebuild` フラグを追加し `BootCompletedReceiver` から強制再同期を実行。

## 確認
- 変更後、ローカルログでは `EXIT` も記録されるようになり、再入場時に `ENTER` が再通知されることを目視で確認（実機ログキャプチャ）。
- `./gradlew test` を実行し、ユニットテストが成功することを確認。

## メモ
- クールダウンが 2 時間固定になっている点は、要件書の「最短5分」と乖離があるため別タスクで見直しが必要。
