package com.github.gekal.intellijplatformpluginworkflowviewer.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // This tool window is deprecated. Use the editor preview instead.
    }

    override fun shouldBeAvailable(project: Project) = false
}
