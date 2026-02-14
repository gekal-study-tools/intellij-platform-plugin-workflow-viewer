package com.github.gekal.intellijplatformpluginworkflowviewer.toolWindow

import com.github.gekal.intellijplatformpluginworkflowviewer.file.WorkflowFileType
import com.google.gson.Gson
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(private val project: Project) {

        private val browser = JBCefBrowser()
        private val gson = Gson()

        init {
            // In development, you can use http://localhost:5173
            // For production, you would load the bundled index.html
            browser.loadURL("http://localhost:5173")

            setupListeners()
        }

        private fun setupListeners() {
            project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    refreshGraphIfWorkflow()
                }
            })

            // Also listen to document changes for real-time updates
            val editorManager = FileEditorManager.getInstance(project)
            editorManager.selectedTextEditor?.document?.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    refreshGraphIfWorkflow()
                }
            })
        }

        private fun refreshGraphIfWorkflow() {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
            if (file.fileType == WorkflowFileType) {
                refreshGraph()
            }
        }

        fun getContent(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.add(browser.component, BorderLayout.CENTER)

            val refreshButton = JButton("Refresh")
            refreshButton.addActionListener {
                refreshGraph()
            }
            panel.add(refreshButton, BorderLayout.SOUTH)

            return panel
        }

        private fun refreshGraph() {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            val text = editor.document.text
            val (nodes, edges) = DslParser.parse(text)
            
            val nodesJson = gson.toJson(nodes)
            val edgesJson = gson.toJson(edges)
            
            val js = "window.updateGraph($nodesJson, $edgesJson)"
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
    }
}

object DslParser {
    data class Node(val id: String, val data: Map<String, String>)
    data class Edge(val id: String, val source: String, val target: String)

    fun parse(text: String): Pair<List<Node>, List<Edge>> {
        val lines = text.lines().filter { it.isNotBlank() }
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        var prevId: String? = null
        lines.forEachIndexed { index, line ->
            val id = "node_$index"
            nodes.add(Node(id, mapOf("label" to line.trim())))
            
            if (prevId != null) {
                edges.add(Edge("edge_$index", prevId, id))
            }
            prevId = id
        }

        return Pair(nodes, edges)
    }
}
