# Role
あなたは熟練したIntelliJ Platform Plugin開発者であり、かつモダンなFrontend（React/TypeScript）のエキスパートです。

# Goal
IntelliJ IDEAのプラグインとして動作する「独自のDSLフロー可視化ツール」を作成します。
エディタ上のテキスト（DSL）をリアルタイムに解析し、JCEF（Chromium）上のReactアプリでフロー図として描画します。
拡張子 `.workflow` のファイルを対象とします。

# Tech Stack & Architecture
1. **Backend (Plugin):** Java 17+, IntelliJ Platform SDK, Gradle (Kotlin DSL).
    *   `.workflow` 拡張子のファイルを独自ファイルタイプとして登録する。
    *   `TextEditorWithPreview` を使用し、テキストエディタとプレビューエディタを並べて表示する。
    *   `PropertiesComponent` を使用して、ユーザーが手動で調整したノードの座標を永続化する。
2. **Frontend (Webview):** React, TypeScript, Vite, React Flow (描画用), Dagre (自動レイアウト用).
3. **Communication:**
    *   Java -> JS: `browser.executeJavaScript` を通じて `window.updateGraph(nodes, edges)` を呼び出す。
    *   JS -> Java: `JBCefJSQuery` を使用し、ノードの移動、リフレッシュ、リセット操作を通知する。

# Requirements

## 1. Project Structure
root/
├── build.gradle.kts
├── src/main/kotlin/.../file/         # File Type Definition
├── src/main/kotlin/.../editor/       # Editor & Preview Implementation
├── src/main/kotlin/.../parser/       # DSL Parser
├── src/main/resources/META-INF/    # plugin.xml
└── frontend/                       # React Project (Vite)
    ├── src/
    ├── package.json
    └── vite.config.ts

## 2. Frontend Implementation (React)
* `frontend/src/App.tsx`:
    * `React Flow` を使用してノードとエッジを描画する。
    * `dagre` ライブラリを使用して、初期座標が未設定の場合の自動レイアウト（Top-to-Bottom）を行う。
    * `window.updateGraph(nodes, edges)` を公開。
    * ノードのドラッグ終了時、`cefQuery` を使用して新座標をJava側に送信する。
    * 操作パネル（Refresh, Reset Layout）を右下に配置。

## 3. Backend Implementation (Kotlin)
* `WorkflowFileType.kt`:
    * `.workflow` 拡張子を扱う `Language` および `LanguageFileType` を定義。
* `WorkflowFileEditorProvider.kt`:
    * `FileEditorProvider` を実装。`.workflow` ファイルに対して `TextEditorWithPreview` を提供。
* `WorkflowPreviewEditor.kt`:
    * `JBCefBrowser` を保持し、Reactアプリを表示.
    * `JBCefJSQuery` でフロントエンドからの通知（座標保存、リフレッシュ、リセット）をハンドル。
    * ドキュメントの変更を監視し、リアルタイムにプレビューを更新。
    * ノードの座標を `PropertiesComponent` に保存・取得。
* `DslParser.kt`:
    * GroovyライクなDSLを解析し、ノード（steps）とエッジ（transitions）を抽出。
    * 形式:
        * `step('node_id', ...)` -> ノード
        * `transition(from: 'source', to: 'target')` -> エッジ
    * 従来の1行1ノード形式にもフォールバックとして対応。

## 4. Configuration
* `plugin.xml`:
    * `fileType` および `fileEditorProvider` の登録。
* `build.gradle.kts`:
    * `gson` 依存関係の追加。
    * `intellijPlatform` プラグインの設定。