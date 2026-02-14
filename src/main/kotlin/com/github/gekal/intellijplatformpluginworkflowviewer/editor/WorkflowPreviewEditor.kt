package com.github.gekal.intellijplatformpluginworkflowviewer.editor

import com.github.gekal.intellijplatformpluginworkflowviewer.parser.DslParser
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
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
    private val virtualFile: VirtualFile? = FileDocumentManager.getInstance().getFile(document)
    private val properties = PropertiesComponent.getInstance(project)
    private val positionsKey: String = "workflow_positions_${virtualFile?.path ?: "unknown"}"

    init {
        jsQuery.addHandler { request ->
            val json = JsonParser.parseString(request).asJsonObject
            when (json.get("type").asString) {
                "NODE_MOVED" -> {
                    val nodeId = json.get("nodeId").asString
                    val position = json.get("position").asJsonObject
                    val x = position.get("x").asDouble
                    val y = position.get("y").asDouble
                    saveNodePosition(nodeId, x, y)
                }

                "REFRESH" -> {
                    refreshGraph()
                }

                "RESET" -> {
                    resetNodePositions()
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

    private fun saveNodePosition(nodeId: String, x: Double, y: Double) {
        val currentPositions = getStoredPositions().toMutableMap()
        currentPositions[nodeId] = DslParser.Position(x, y)
        properties.setValue(positionsKey, gson.toJson(currentPositions))
    }

    private fun resetNodePositions() {
        properties.unsetValue(positionsKey)
    }

    private fun getStoredPositions(): Map<String, DslParser.Position> {
        val json = properties.getValue(positionsKey) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, DslParser.Position>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private var isUpdatingFromWebview = false

    private fun refreshGraph() {
        val text = document.text
        val (nodes, edges) = DslParser.parse(text)
        val storedPositions = getStoredPositions()

        val updatedNodes = nodes.map { node ->
            val storedPos = storedPositions[node.id]
            if (storedPos != null) {
                node.copy(position = storedPos)
            } else {
                node
            }
        }

        val nodesJson = gson.toJson(updatedNodes)
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
