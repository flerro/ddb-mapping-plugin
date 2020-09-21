package com.github.flerro.ddbmapping;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.NonFocusableCheckBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class FieldSelectionDialog {

    @Nullable
    public static List<PsiFieldMember> show(final List<PsiFieldMember> members, final Project project) {

        if (members == null || members.isEmpty()) return new ArrayList<>();

        // Generate options
        final PropertiesComponent props = PropertiesComponent.getInstance();
        final List<JCheckBox> optionsCheckBoxes = new ArrayList<>();
        for (CodeGenerationOption option : CodeGenerationOption.values()) {
            optionsCheckBoxes.add(buildOptionCheckBox(props, option));
        }

        // Create Dialog
        final PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);
        boolean allowEmptySelection = false;
        boolean allowMultiSelection = true;
        final MemberChooser<PsiFieldMember> chooser = new MemberChooser<>(memberArray,
                    allowEmptySelection, allowMultiSelection, project,
                    null, optionsCheckBoxes.toArray(new JCheckBox[0]));

        chooser.setTitle("Select Fields:");
        chooser.selectElements(memberArray);
        return chooser.showAndGet() ? chooser.getSelectedElements() : null;
    }


    private static JCheckBox buildOptionCheckBox(final PropertiesComponent propertiesComponent, final CodeGenerationOption option) {
        final JCheckBox optionCheckBox = new NonFocusableCheckBox(option.getLabel());
        optionCheckBox.setToolTipText(option.getToolip());

        String currentOption = option.getProperty();
        optionCheckBox.setSelected(propertiesComponent.isTrueValue(currentOption));
        optionCheckBox.addItemListener(event -> propertiesComponent.setValue(currentOption,
                                                    Boolean.toString(optionCheckBox.isSelected())));
        return optionCheckBox;
    }

    public static Set<CodeGenerationOption> selectedOptions() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final Predicate<CodeGenerationOption> isSelected = o -> propertiesComponent.getBoolean(o.getProperty(), false);
        return Arrays.stream(CodeGenerationOption.values()).filter(isSelected).collect(Collectors.toSet());
    }
}

