package com.github.gekal.intellijplatformpluginworkflowviewer.file

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object WorkflowLanguage : Language("Workflow")

object WorkflowFileType : LanguageFileType(WorkflowLanguage) {
    override fun getName() = "Workflow File"
    override fun getDescription() = "Workflow DSL file"
    override fun getDefaultExtension() = "workflow"
    override fun getIcon(): Icon? = null
}
