package com.github.flerro.ddbmapping;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.github.flerro.ddbmapping.MethodNameUtils.capitalize;

public class CodeGenerator implements Runnable {

    private final Project project;
    private final PsiFile file;
    private final Editor editor;
    private final List<PsiFieldMember> fields;
    private final PsiElementFactory elementFactory;

    public static void generate(Project project, Editor editor, PsiFile file, List<PsiFieldMember> fields) {
        final Runnable builderGenerator = new CodeGenerator(project, file, editor, fields);
        ApplicationManager.getApplication().runWriteAction(builderGenerator);
    }

    private CodeGenerator(Project project, PsiFile file, Editor editor, List<PsiFieldMember> fields) {
        this.project = project;
        this.file = file;
        this.editor = editor;
        this.fields = fields;
        this.elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
        final PsiClass targetClass = MethodNameUtils.getStaticOrTopLevelClass(file, editor);
        if (targetClass == null) return;

        Set<CodeGenerationOption> options = FieldSelectionDialog.selectedOptions();

        String targetClassName = Objects.requireNonNull(targetClass.getName());
        PsiType targetClassType = typeFromText(targetClassName, null);

        PsiMethod marshallMethod = buildMarshallMethod(targetClassType);
        addOrReplaceMethod(targetClass, marshallMethod, "marshall");

        PsiMethod unmarshallMethod = buildUnmarshallMethod(targetClassType, options);
        addOrReplaceMethod(targetClass, unmarshallMethod, "unmarshall");

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        CodeStyleManager.getInstance(project).reformat(targetClass);
    }

    private void addOrReplaceMethod(PsiClass targetClass, PsiMethod newMethod, String methodName) {
        PsiMethod[] methods = targetClass.findMethodsByName(methodName, false);
        if (methods.length > 0) {
            methods[0].replace(newMethod);
        } else {
            targetClass.add(newMethod);
        }
    }

    private PsiMethod buildUnmarshallMethod(PsiType targetClassType, Set<CodeGenerationOption> options) {
        String targetClassName = targetClassType.getPresentableText();
        final PsiType returnType = typeFromText(targetClassName, null);
        final PsiMethod method = createMethod("unmarshall", returnType);
        method.getModifierList().setModifierProperty("static", true);

        final PsiType parameterType = typeFromText("Map<String, AttributeValue>", null);
        final PsiParameter parameter = createParameter("item", parameterType);
        method.getParameterList().add(parameter);

        final PsiCodeBlock methodBody = method.getBody();
        if (methodBody == null) return method;

        boolean useInnerBuilder = options.contains(CodeGenerationOption.USE_INNER_BUILDER);

        if (useInnerBuilder) {
            String tpl = "return %s.builder()%n";
            String line = String.format(tpl, targetClassName);
            StringBuilder stmts = new StringBuilder(line);

            for (PsiFieldMember member : fields) {
                PsiField field = member.getElement();
                tpl = ".with%s(%s)%n";
                line = String.format(tpl, capitalize(field.getName()), unmarshallStmtText(field));
                stmts.append(line);
            }

            stmts.append(";");
            methodBody.add(stmtFromText(stmts.toString(), method));

        } else {

            String tpl = "%s obj = new %1$s();%n";
            String line = String.format(tpl, targetClassName);
            methodBody.add(stmtFromText(line, method));

            for (final PsiFieldMember member : fields) {
                final PsiField field = member.getElement();
                tpl = "obj.set%s(%s);";
                line = String.format(tpl, capitalize(field.getName()), unmarshallStmtText(field));
                methodBody.add(stmtFromText(line, method));
            }

            line = "return obj;";
            methodBody.add(stmtFromText(line, method));
        }

        return method;
    }

    private String unmarshallStmtText(PsiField field) {
        String typeTpl = "item.get(\"%s\").%s()";
        String delegateTpl = "%s.unmarshall(item)";

        switch (field.getType().getPresentableText()) {
            case "Double":
            case "double":
            case "Float":
            case "float":
            case "Integer":
            case "int":
                return String.format(typeTpl, field.getName(), "n");
            case "String":
                return String.format(typeTpl, field.getName(), "s");
            default:
                return String.format(delegateTpl, capitalize(field.getName()));
        }
    }

    private String marshallStmtText(PsiField field) {
        String typeTpl = "AttributeValue.builder().%s(obj.get%s()).build()";
        String delegateTpl = "%s.marshall(obj.get%1$s())";
        String capitalizedName = capitalize(field.getName());

        switch (field.getType().getPresentableText()) {
            case "Double":
            case "double":
            case "Float":
            case "float":
            case "Integer":
            case "int":
                return String.format(typeTpl,  "n", capitalizedName);
            case "String":
                return String.format(typeTpl, "s", capitalizedName);
            default:
                return String.format(delegateTpl, capitalizedName);
        }
    }


    private PsiMethod buildMarshallMethod(PsiType targetClassType) {
        final PsiType returnType = typeFromText("Map<String, AttributeValue>", null);
        final PsiMethod method = createMethod("marshall", returnType);
        method.getModifierList().setModifierProperty("static", true);

        final PsiParameter parameter = createParameter("obj", targetClassType);
        method.getParameterList().add(parameter);

        final PsiCodeBlock methodBody = method.getBody();
        if (methodBody == null) return method;

        String line = "Map<String, AttributeValue> item = new HashMap<>();";
        PsiStatement newStmt = stmtFromText(line, method);
        methodBody.add(newStmt);

        for (final PsiFieldMember member : fields) {
            final PsiField field = member.getElement();
            String tpl = "item.put(\"%s\", %s);";
            line = String.format(tpl, field.getName(), marshallStmtText(field));
            final PsiStatement assignStatement = stmtFromText(line, method);
            methodBody.add(assignStatement);
        }

        line = "return item;";
        final PsiStatement retutnStmt = stmtFromText(line, method);
        methodBody.add(retutnStmt);

        return method;
    }

    @NotNull
    public PsiType typeFromText(@NotNull String s, @Nullable PsiElement psiElement) throws IncorrectOperationException {
        return elementFactory.createTypeFromText(s, psiElement);
    }

    @NotNull
    public PsiStatement stmtFromText(@NotNull String s, @Nullable PsiElement psiElement) throws IncorrectOperationException {
        return elementFactory.createStatementFromText(s, psiElement);
    }

    @NotNull
    public PsiMethod createMethod(@NotNull String s, PsiType psiType) throws IncorrectOperationException {
        return elementFactory.createMethod(s, psiType);
    }

    public PsiParameter createParameter(@NotNull String s, PsiType psiType) throws IncorrectOperationException {
        return elementFactory.createParameter(s, psiType);
    }
}
