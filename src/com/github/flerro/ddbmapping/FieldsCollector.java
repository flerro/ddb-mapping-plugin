package com.github.flerro.ddbmapping;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.text.StringUtil.hasLowerCaseChar;
import static com.intellij.psi.util.TypeConversionUtil.getSuperClassSubstitutor;

public final class FieldsCollector {

    @Nullable
    public static List<PsiFieldMember> collectFields(final PsiFile file, final Editor editor) {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement element = file.findElementAt(offset);
        if (element == null) return new ArrayList<>();

        final PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (clazz == null || clazz.hasModifierProperty(PsiModifier.ABSTRACT)) return new ArrayList<>();

        final List<PsiFieldMember> allFields = new ArrayList<>();

        PsiClass target = clazz;
        while (target != null) {
            allFields.addAll(collectFieldsInClass(element, clazz, target));
            target = target.getSuperClass();
        }

        return allFields;
    }

    private static List<PsiFieldMember> collectFieldsInClass(final PsiElement element,
                                                             final PsiClass accessObjectClass,
                                                             final PsiClass clazz) {

        final List<PsiFieldMember> classFieldMembers = new ArrayList<>();
        final PsiResolveHelper helper = JavaPsiFacade.getInstance(clazz.getProject()).getResolveHelper();

        Set<String> setters = Arrays.stream(clazz.getAllMethods())
                                    .map(PsiMethod::getName)
                                    .filter(n -> n.startsWith("set"))
                                    .map(s -> s.substring(3, 4).toLowerCase() + s.substring(4))
                                    .collect(Collectors.toSet());

        for (final PsiField field : clazz.getFields()) {

            boolean isFieldAccessible = helper.isAccessible(field, clazz, accessObjectClass)
                                                             || setters.contains(field.getName());
            boolean isFromAncestor = PsiTreeUtil.isAncestor(field, element, false);

            if (!isFieldAccessible || isFromAncestor) continue;

            boolean isStatic = field.hasModifierProperty(PsiModifier.STATIC) ||
                                                     !hasLowerCaseChar(field.getName());
            if (isStatic) continue;

            boolean isLogger = field.getType().getCanonicalText().contains("Logger");
            if (isLogger) continue;

            final PsiClass containingClass = field.getContainingClass();
            if (containingClass != null) {
                classFieldMembers.add(buildFieldMember(field, containingClass, clazz));
            }

        }

        return classFieldMembers;
    }

    private static PsiFieldMember buildFieldMember(final PsiField field,
                                                   final PsiClass containingClass,
                                                   final PsiClass clazz) {
        return new PsiFieldMember(field, getSuperClassSubstitutor(containingClass, clazz, PsiSubstitutor.EMPTY));
    }
}
