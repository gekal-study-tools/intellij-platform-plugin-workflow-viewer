package com.github.gekal.intellijplatformpluginworkflowviewer.editor

import com.github.gekal.intellijplatformpluginworkflowviewer.parser.DslParser
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class WorkflowPreviewEditor(private val project: Project, private val document: Document) : UserDataHolderBase(),
    FileEditor {
    private val browser = JBCefBrowser()
    private val gson = Gson()
    private val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    init {
        jsQuery.addHandler { request ->
            val json = JsonParser.parseString(request).asJsonObject
            when (json.get("type").asString) {
                "NODE_MOVED" -> {
                    val nodeId = json.get("nodeId").asString
                    val position = json.get("position").asJsonObject
                    val x = position.get("x").asDouble
                    val y = position.get("y").asDouble
                    updateNodePositionInDocument(nodeId, x, y)
                }

                "REFRESH" -> {
                    refreshGraph()
                }
            }
            null
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                val js = "window.cefQuery = function(obj) { ${jsQuery.inject("obj.request")} };"
                browser?.executeJavaScript(js, browser.url, 0)
                refreshGraph()
            }
        }, browser.cefBrowser)

        browser.loadURL("http://localhost:5173")

        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!isUpdatingFromWebview) {
                    refreshGraph()
                }
            }
        }, this)
    }

    private fun updateNodePositionInDocument(nodeId: String, x: Double, y: Double) {
        val index = nodeId.removePrefix("node_").toIntOrNull() ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            isUpdatingFromWebview = true
            try {
                val lines = document.text.lines().toMutableList()
                var currentIdx = 0
                for (i in lines.indices) {
                    if (lines[i].isNotBlank()) {
                        if (currentIdx == index) {
                            val line = lines[i].trim()
                            val posRegex = """(.*)\{x:\s*([\d.-]+),\s*y:\s*([\d.-]+)\}""".toRegex()
                            val match = posRegex.find(line)
                            val label = if (match != null) match.groupValues[1].trim() else line
                            lines[i] = "$label {x: ${x.toInt()}, y: ${y.toInt()}}"
                            break
                        }
                        currentIdx++
                    }
                }
                document.setText(lines.joinToString("\n"))
            } finally {
                isUpdatingFromWebview = false
            }
        }
    }

    private var isUpdatingFromWebview = false

    private fun refreshGraph() {
        val text = document.text
        val (nodes, edges) = DslParser.parse(text)
        val nodesJson = gson.toJson(nodes)
        val edgesJson = gson.toJson(edges)
        val js = "window.updateGraph($nodesJson, $edgesJson)"
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    override fun getComponent(): JComponent = browser.component

    override fun getPreferredFocusedComponent(): JComponent? = browser.component

    override fun getName(): String = "Workflow Preview"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        browser.dispose()
    }
}
