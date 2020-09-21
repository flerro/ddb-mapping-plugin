package com.github.flerro.ddbmapping;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.flerro.ddbmapping.FieldsCollector.collectFields;
import static com.github.flerro.ddbmapping.FieldSelectionDialog.show;

public class ActionHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean isValidFor(final Editor editor, final PsiFile file) {
        boolean isJavaFile = file instanceof PsiJavaFile;
        if (!isJavaFile) return false;

        boolean noProjectSelected = editor.getProject() == null;
        if (noProjectSelected) return false;

        boolean noTopLevelClassDefined = MethodNameUtils.getStaticOrTopLevelClass(file, editor) == null;
        if (noTopLevelClassDefined) return false;

        final List<PsiFieldMember> targetFields = collectFields(file, editor);
        return targetFields != null && !targetFields.isEmpty();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
        final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        final Document currentDocument = psiDocumentManager.getDocument(file);
        if (currentDocument == null) return;

        psiDocumentManager.commitDocument(currentDocument);

        boolean canWrite = FileDocumentManager.getInstance()
                                        .requestWriting(editor.getDocument(), project);
        if (!canWrite) return;

        final List<PsiFieldMember> existingFields = collectFields(file, editor);
        if (existingFields == null || existingFields.isEmpty()) return;

        final List<PsiFieldMember> selectedFields = show(existingFields, project);
        if (selectedFields == null || selectedFields.isEmpty()) return;

        CodeGenerator.generate(project, editor, file, selectedFields);
    }

}
