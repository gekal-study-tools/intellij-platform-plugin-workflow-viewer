# Role
あなたは熟練したIntelliJ Platform Plugin開発者であり、かつモダンなFrontend（React/TypeScript）のエキスパートです。

# Goal
IntelliJ IDEAのプラグインとして動作する「独自のDSLフロー可視化ツール」を作成したいです。
エディタ上のテキスト（DSL）をリアルタイムに解析し、JCEF（Chromium）上のReactアプリでフロー図として描画する実装コードを提示してください。
拡張子 `.workflow` のファイルを対象とします。

# Tech Stack & Architecture
1. **Backend (Plugin):** Java 17+, IntelliJ Platform SDK, Gradle (Kotlin DSL).
    *   `.workflow` 拡張子のファイルを独自ファイルタイプとして登録する。
2. **Frontend (Webview):** React, TypeScript, Vite, React Flow (描画用), Dagre (自動レイアウト用).
3. **Communication:** JCEF (`JBCefBrowser`) を使用し、Javaから `browser.executeJavaScript` を通じてReact側の関数を呼び出す。

# Requirements
以下の仕様を満たすコードとプロジェクト構成を作成してください。

## 1. Project Structure
プロジェクトは Backend と Frontend を明確に分離してください。
root/
├── build.gradle.kts
├── src/main/kotlin/.../toolWindow/   # Java Plugin Logic
├── src/main/kotlin/.../file/         # File Type Definition
├── src/main/resources/META-INF/    # plugin.xml
└── frontend/                       # React Project (Vite)
    ├── src/
    ├── package.json
    └── vite.config.ts

## 2. Frontend Implementation (React)
* `frontend/src/App.tsx`:
    * `React Flow` を使用してノードとエッジを描画する。
    * `dagre` ライブラリを使用して、ノードの座標（x, y）を自動計算する（Top-to-Bottomレイアウト）。
    * `window.updateGraph(nodes, edges)` という関数をグローバルに公開し、外部（Java）からデータを受け取れるようにする。
    * データを受け取ったら、React Flowの `useNodesState`, `useEdgesState` を更新し、`fitView()` で全体を表示する。

## 3. Backend Implementation (Kotlin)
* `WorkflowFileType.kt`:
    * `.workflow` 拡張子を扱う `Language` および `LanguageFileType` を定義。
* `WorkflowFileEditorProvider.kt` & `WorkflowPreviewEditor.kt`:
    * `FileEditorProvider` を実装し、`.workflow` ファイルに対して `TextEditorWithPreview` を提供する。
    * `WorkflowPreviewEditor` は `JBCefBrowser` を保持し、ドキュメントの変更を監視してリアルタイムにプレビューを更新する。
* `DslParser.kt`:
    * テキストを行ごとに読み込み、単純な連鎖としてパースする。
    * 行の内容を `label` とし、前の行から次の行へエッジを張る。
    * 結果を JSON 文字列（nodes, edges）に変換する（Gsonを使用）。
    * `browser.cefBrowser.executeJavaScript("window.updateGraph(...)")` を実行してFrontendにデータを送る。

## 4. Configuration
* `plugin.xml`:
    * `fileEditorProvider` の登録。
    * `fileType` の登録。
* `build.gradle.kts`: 必要な依存関係（Gsonなど）の追加。

# Deliverables
以下のファイルの完全なコードを提示してください。
1. `build.gradle.kts` (dependencies part)
2. `frontend/package.json` (dependencies part)
3. `frontend/src/App.tsx` (Complete logic with Dagre layout)
4. `src/main/resources/META-INF/plugin.xml`
5. `src/main/java/.../DslGraphToolWindowFactory.kt`

実装は「とりあえず動く」レベルではなく、エラーハンドリングや型の整合性を考慮した実用的なレベルでお願いします。