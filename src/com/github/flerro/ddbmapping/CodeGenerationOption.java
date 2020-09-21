package com.github.flerro.ddbmapping;

enum CodeGenerationOption {

    USE_INNER_BUILDER("Unmarshall with inner builder",
            "Use fluent builder pattern in un-marshalling operation.");

    private final String label;
    private final String tooltip;

    CodeGenerationOption(String label, String tooltip) {
        this.label = label;
        this.tooltip = tooltip;
    }

    public String getLabel() {
        return label;
    }

    public String getToolip() {
        return tooltip;
    }

    public String getProperty() {
        return this.name().toLowerCase();
    }
}
