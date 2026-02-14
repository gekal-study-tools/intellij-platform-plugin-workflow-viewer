package com.github.gekal.intellijplatformpluginworkflowviewer.editor

import com.github.gekal.intellijplatformpluginworkflowviewer.file.WorkflowFileType
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class WorkflowFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == WorkflowFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val document = FileDocumentManager.getInstance().getDocument(file)
            ?: throw IllegalArgumentException("Document not found for file: ${file.path}")
        val previewEditor = WorkflowPreviewEditor(project, document)
        return TextEditorWithPreview(textEditor, previewEditor)
    }

    override fun getEditorTypeId(): String = "workflow-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
