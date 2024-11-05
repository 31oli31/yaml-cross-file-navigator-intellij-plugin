package com.project.yamlcrossfilenavigator.listeners

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.yaml.snakeyaml.Yaml
import java.io.File

class YamlEditorMouseListener : EditorMouseListener {

    private val logger: Logger = Logger.getInstance(YamlEditorMouseListener::class.java)

    override fun mousePressed(event: EditorMouseEvent) {
        if (event.area != EditorMouseEventArea.EDITING_AREA) return

        val editor = event.editor as? EditorEx ?: return
        val project = editor.project ?: return
        val psiFile = getYamlPsiFile(editor, project) ?: return
        val clickedOffset = editor.visualPositionToOffset(editor.xyToVisualPosition(event.mouseEvent.point))

        handleEditorClick(project, psiFile, clickedOffset, event)
    }

    private fun getYamlPsiFile(editor: EditorEx, project: Project): YAMLFile? =
        PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? YAMLFile

    private fun handleEditorClick(project: Project, psiFile: YAMLFile, offset: Int, event: EditorMouseEvent) {
        val clickedElement = psiFile.findElementAt(offset)
        val wordClicked = clickedElement?.text
        val yamlKeyValue = PsiTreeUtil.getParentOfType(clickedElement, YAMLKeyValue::class.java)
        val importPaths = extractImportPaths(psiFile.text)
        val currentFilePath = psiFile.virtualFile.parent.path

        if (yamlKeyValue != null && event.mouseEvent.isMetaDown) {
            handleYamlKeyClick(project, yamlKeyValue, importPaths, currentFilePath, wordClicked)
        }
    }

    private fun handleYamlKeyClick(
        project: Project,
        yamlKeyValue: YAMLKeyValue,
        importPaths: List<String>,
        currentFilePath: String,
        wordClicked: String?
    ) {
        val keyText = yamlKeyValue.keyText
        val valueText = yamlKeyValue.value?.text ?: return

        if (valueText.startsWith("*")) {
            navigateToAnchorReference(project, valueText.removePrefix("*"), importPaths, currentFilePath)
        } else if (keyText == "import") {
            resolveImportPath(project, wordClicked, currentFilePath)
        }
    }

    private fun navigateToAnchorReference(
        project: Project,
        anchorName: String,
        importPaths: List<String>,
        currentFilePath: String
    ) {
        for (importPath in importPaths) {
            val resolvedPath = resolvePath(currentFilePath, importPath)
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(resolvedPath)

            if (virtualFile != null && virtualFile.exists()) {
                if (locateAndOpenAnchor(project, virtualFile, anchorName)) {
                    break // Stop after the first match is found
                }
            } else {
                logger.info("File not found: $resolvedPath")
            }
        }
    }

    private fun locateAndOpenAnchor(project: Project, file: VirtualFile, anchorName: String): Boolean {
        val content = file.contentsToByteArray().toString(Charsets.UTF_8)
        val anchorPattern = Regex("&$anchorName\\b", RegexOption.MULTILINE)
        val match = anchorPattern.find(content) ?: return false

        val position = getLineAndColumn(content, match.range.first)
        openFileAtPosition(project, file, position.line, position.character)

        logger.info("Anchor '$anchorName' found in ${file.path} at line ${position.line}, character ${position.character}")
        return true
    }

    private fun getLineAndColumn(content: String, offset: Int): Position {
        val lines = content.substring(0, offset).lines()
        return Position(line = lines.size - 1, character = lines.lastOrNull()?.length ?: 0)
    }

    private fun openFileAtPosition(project: Project, file: VirtualFile, line: Int, character: Int) {
        val descriptor = OpenFileDescriptor(project, file, line, character)
        val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)

        editor?.caretModel?.moveToLogicalPosition(LogicalPosition(line, character))
    }

    private fun resolveImportPath(project: Project, importPath: String?, currentFilePath: String) {
        if (importPath == null) return
        val resolvedPath = resolvePath(currentFilePath, importPath)
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(resolvedPath)

        if (virtualFile != null && virtualFile.exists()) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            logger.info("Navigating to import path: $resolvedPath")
        } else {
            logger.info("File not found at path: $resolvedPath")
        }
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val cleanedPath = relativePath.removePrefix("./").removeSurrounding("'")
        return File(basePath, cleanedPath).canonicalPath
    }

    private fun extractImportPaths(yamlContent: String): List<String> {
        val yaml = Yaml()
        val importsSection = yamlContent.split(Regex("(?m)^-{3,}\\s*$"))[0].trim()
        val parsedData = yaml.load<Map<String, Any>?>(importsSection)

        return when (val importValue = parsedData?.get("import")) {
            is String -> listOf(importValue)
            is List<*> -> importValue.filterIsInstance<String>()
            else -> emptyList()
        }
    }

    data class Position(val line: Int, val character: Int)
}
