package com.github.gekal.intellijplatformpluginworkflowviewer.editor

import com.github.gekal.intellijplatformpluginworkflowviewer.parser.DslParser
import com.google.gson.Gson
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.jcef.JBCefBrowser
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class WorkflowPreviewEditor(private val project: Project, private val document: Document) : UserDataHolderBase(), FileEditor {
    private val browser = JBCefBrowser()
    private val gson = Gson()

    init {
        browser.loadURL("http://localhost:5173")
        
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                refreshGraph()
            }
        }, this)

        // Wait for browser to load before first refresh
        // In a real implementation, we might want to wait for a "ready" signal from React
        // For now, let's just trigger it.
        // browser.jbCefClient.addLoadHandler(...) could be used for more robust triggering.
    }

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
