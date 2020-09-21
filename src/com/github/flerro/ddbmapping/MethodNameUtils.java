package com.github.flerro.ddbmapping;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MethodNameUtils {
    private MethodNameUtils() { }

    public static String capitalize(String str) {
        if (str.length() == 1) return str.toUpperCase();
        return hasOneLetterPrefix(str) ?
                Character.toUpperCase(str.charAt(1)) + str.substring(2) :
                Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static boolean hasOneLetterPrefix(String str) {
        return Character.isLowerCase(str.charAt(0))
                && Character.isUpperCase(str.charAt(1));
    }

    @Nullable
    public static PsiClass getStaticOrTopLevelClass(PsiFile file, Editor editor) {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement element = file.findElementAt(offset);
        if (element == null) return null;

        PsiClass topLevelClass = PsiUtil.getTopLevelClass(element);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) return null;

        boolean equivalentTypes = psiClass.getManager().areElementsEquivalent(psiClass, topLevelClass);
        boolean hasStaticModifier = psiClass.hasModifierProperty(PsiModifier.STATIC);
        return  (hasStaticModifier || equivalentTypes) ? psiClass : null;
    }

}
