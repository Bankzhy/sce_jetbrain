package com.gras_code.sce_jetbrain;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.lang.Language;

public class LanguageUtil {

    public static Language getCurrentLanguage(Project project, AnActionEvent editor) {
        if (editor == null || project == null) {
            return null;
        }

        // Get the PsiFile from the editor
        PsiFile psiFile = editor.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return null;
        }

        // Get the language of the PsiFile
        Language language = psiFile.getLanguage();
        return language;
    }
}
