# カラーハイアラキー再設計調査（2025-10-04）

## 調査メモ
- Material Design のカラーシステムでは、一次アクションには *Primary*、サポート的な操作や入力には *Secondary*、カテゴリー区別や追加アクセントには *Tertiary* を推奨。特に浮動アクションや主要ボタンを Primary に、補助アクションは Secondary/Tertiary で差別化する。citeturn0search0turn0search1
- 色による階層化は「ユーザーがどの順番で注視してほしいか」を意識し、主要CTAを最も高い視認性で、補助CTAは控えめな彩度または別ヒューで表現することが勧められている。citeturn0search3
- アクセシビリティ確保のため、テキストと背景のコントラスト比は少なくとも 4.5:1（通常文字）を満たす必要がある。citeturn0search2

## 現状課題
- 「お店を追加」「検索で探す」など探索系アクションにプライマリ系の色を使用しているため、リスト項目や他のプライマリアクションと視覚的区別が付きづらい。
- アクセントカラー（赤 #EA5F52）は削除系アクションに限定したい要件があるため、探索系アクションには別の色体系が必要。

## 配色案
### 案A: Secondary（ネイビーブルー）を専用アクションに割り当て
- Hue: 変化が最小の類似色（#214699）を Secondary に設定。
- 適用: 「検索で探す」ボタン/FABのラベル背景、タブやフィルタなど補助的アクション。
- テキスト色: `onSecondary = #FFFFFF` でコントラスト比 8.7:1 以上を確保。
- メリット: ブランドの一貫性を維持しつつプライマリより深いトーンで差別化。
- 注意: ダークテーマでは `secondary` と `surface` のコントラストが 2:1 前後まで落ちるため、`secondaryContainer` を使用し `onSecondaryContainer` でテキスト表示を行う。

### 案B: Tertiary（ティール）で探索アクションを表現
- 推奨カラー: #1F7D76（Teal系）
  - 白テキストとのコントラスト比: 4.94:1 → WCAG AA 準拠。
  - ダークサーフェス(#0F1C2B)とのコントラスト比: 3.48:1（アイコンなど大きめ要素向け）。文字には #FFFFFF など高コントラスト色を合わせる。
- 適用: 「お店を追加」「検索で探す」など探索・関連アクション、検索候補チップの背景。
- 演出: Primary Container を背景に敷いた上で Tertiary をボタンとして重ねると階層化が明確。
- 注意: ダークモードではトーンを `tertiary = #33B9AA`、`onTertiary = #003F39` のように入れ替えることで対比を確保。

### 案C: Neutral Variant を基調としたトーナルカード + アイコンバッジ
- 背景: `surfaceVariant = #E2F1FA`（ライト）/`#314659`（ダーク）
- 操作ボタン: `primaryContainer` に白字で “主要” ボタン、「検索/最近」などは `surfaceVariant` 上に `OutlinedButton`（罫線: `outline = #A9BCCC`）。
- クリック誘導: ボタン内部にティールのアイコンバッジ (#1F7D76) を置くことで視認性を向上。
- メリット: 彩度を抑えたままアイコンバッジで差別化できるため、ボタン自体を派手にしなくても良い。
- 注意: 強調度が下がるので、検索が頻繁なユーザー操作であれば A または B とのハイブリッド採用を推奨。

## UI適用プラン案
1. `colors.xml` に Secondary/Tertiary 系統の新トーンを追加し、`Theme.kt` で `secondary`, `secondaryContainer`, `tertiary`, `tertiaryContainer` を更新。
2. `ShoppingListScreen` のダイアログ内ボタンを `ButtonDefaults.buttonColors` で `containerColor = MaterialTheme.colorScheme.secondary` または `tertiary` に変更し、テキストは `onSecondary` / `onTertiary` を使用。
3. 検索候補チップは `SuggestionChip` + `colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)` などを検討。
4. ダークテーマでのコントラスト確認後、`MaterialTheme.colorScheme` の `inverse*` ロールも調整。
5. ドキュメント（requirements.md）で色役割の再定義を追記し、開発メンバー向けに使い分けルールを提示。

## 次のステップ
- 案B（ティール活用）を採用する。ライトテーマは #1F7D76 / #A9DDD4、ダークテーマは #33B9AA / #0B4943 を `tertiary` 系として定義。
- Contrast チェック結果を Figma/E2Eで共有し、WCAG基準に合致することをレビュー。
- 実装対象コンポーネント（ボタン、チップ、FAB 等）を洗い出し、スプリント内で差し替えを計画。
