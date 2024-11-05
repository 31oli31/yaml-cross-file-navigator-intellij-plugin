package com.project.yamlcrossfilenavigator.listeners

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlGotoDeclarationHandler : GotoDeclarationHandler {
    private val logger: Logger = Logger.getInstance(YamlGotoDeclarationHandler::class.java)

    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        if (element == null) return null

        val psiFile: PsiFile = element.containingFile
        if (psiFile !is YAMLFile) return null

        val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
        if (yamlKeyValue != null) {
            val markupModel = editor.markupModel
            val startOffset = yamlKeyValue.textRange.startOffset
            val endOffset = yamlKeyValue.textRange.endOffset


            markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                null,
                HighlighterTargetArea.EXACT_RANGE
            )

            return arrayOf(yamlKeyValue)
        }

        return null
    }

    override fun getActionText(context: DataContext): String {
        return "Navigate to YAML Definition"
    }
}
