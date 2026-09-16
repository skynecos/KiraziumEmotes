package com.kirazium.emotes.integration;

public record IntegrationStatus(
        IntegrationType type,
        boolean installed,
        String version
) {
}
