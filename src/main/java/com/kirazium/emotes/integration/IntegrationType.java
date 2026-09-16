package com.kirazium.emotes.integration;

public enum IntegrationType {
    MODEL_ENGINE("ModelEngine"),
    NEXO("Nexo"),
    ITEMS_ADDER("ItemsAdder"),
    ORAXEN("Oraxen");

    private final String pluginName;

    IntegrationType(String pluginName) {
        this.pluginName = pluginName;
    }

    public String pluginName() {
        return pluginName;
    }
}
