# Material Components 非推奨API調査レポート

作成日: 2025-11-02  
担当: 作業エージェント（タスク1）

## 調査概要

Play Console で表示されている非推奨API使用の警告について、Material Components ライブラリの更新による対応可能性を調査した。

## 警告内容の再確認

```
お客様のアプリは、エッジ ツー エッジで非推奨の API またはパラメータを使用しています
- android.view.Window.setStatusBarColor
- android.view.Window.setNavigationBarColor

これらは次の場所で開始します:
- com.google.android.material.datepicker.m.I
```

## 調査結果

### 1. Material Components のバージョン状況

**現在のバージョン:** 1.13.0

**最新版の確認:**
- Google Maven リポジトリで確認: 1.13.0 が最新安定版
- 1.14.0 以降はまだリリースされていない（2025年11月2日時点）

**検証方法:**
```bash
# バージョン 1.14.0、1.15.0 などの存在を確認
curl -sI "https://dl.google.com/dl/android/maven2/com/google/android/material/material/1.14.0/material-1.14.0.pom"
# 結果: HTTP 404 (存在しない)
```

### 2. アプリケーションコード内での非推奨API使用状況

**調査方法:**
```bash
find app/src -name "*.kt" -type f -exec grep -l "setStatusBarColor\|setNavigationBarColor" {} \;
```

**結果:** 使用箇所なし

アプリケーションコードでは非推奨APIを直接使用していないことを確認。

### 3. DatePicker の使用状況

**調査方法:**
```bash
find app/src -name "*.kt" -type f -exec grep -l "DatePicker\|MaterialDatePicker" {} \;
```

**結果:** 使用箇所なし

警告の発生源である `com.google.android.material.datepicker` パッケージのコンポーネントは、アプリケーション内で使用されていない。

### 4. ビルドとLintの確認

**デバッグビルド:**
```
BUILD SUCCESSFUL
```

**Lint チェック:**
```
BUILD SUCCESSFUL
Wrote HTML report to file:///app/build/reports/lint-results-debug.html
```

ビルドおよびLintは正常に完了。非推奨API使用に関するアプリケーション側の問題は検出されず。

## 結論

### 現状の評価

1. **Material Components は最新版を使用している**
   - バージョン 1.13.0 が Google Maven で公開されている最新安定版
   - これ以上の更新は現時点では不可能

2. **非推奨API の使用はライブラリ内部のみ**
   - アプリケーションコードでは非推奨APIを使用していない
   - 警告は Material Components ライブラリの内部実装に起因
   - DatePicker コンポーネントはアプリで未使用

3. **実害はない**
   - ビルドは成功
   - Lint チェックも通過
   - エッジツーエッジ対応も完了済み

### 対応方針

**現時点での対応:** なし

**理由:**
- Material Components の最新安定版を既に使用している
- アプリケーションコードに問題はない
- 警告の原因となる DatePicker コンポーネントは未使用
- ライブラリ内部の実装は開発者側で制御不可能

**今後の対応:**
- Material Components の次期バージョン（1.14.0 以降）がリリースされた際に更新を検討
- Google が Material Components ライブラリ内部の非推奨API使用を修正することを待つ

## 補足: テスト失敗について

ビルド時に一部のユニットテストが失敗していることを確認：

```
DefaultPlacesRepositoryTest > updateNameTrimsAndUpdatesTimestamp FAILED
    java.lang.IllegalArgumentException at com.google.android.libraries.places:places@@3.5.0:1
```

これは Places API 関連のテストであり、Material Components とは無関係。別タスクでの対応が必要。

## 参考情報

- [Material Components for Android - Releases](https://github.com/material-components/material-components-android/releases)
- [Android Developers - Edge to Edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- Play Console 警告レポート: `requirements/reports/2025-11-02-play-console-warnings.md`
