package com.construction.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuilderPropertiesTest {

    @Test
    @DisplayName("Valid non-blank activation code passes startup validation")
    void validate_ValidCode_Success() {
        BuilderProperties properties = new BuilderProperties();
        properties.setActivationCode("EXPLICIT-SECURE-SECRET-2026");

        assertDoesNotThrow(properties::validate);
    }

    @Test
    @DisplayName("Null activation code fails startup validation with IllegalStateException")
    void validate_NullCode_ThrowsIllegalStateException() {
        BuilderProperties properties = new BuilderProperties();
        properties.setActivationCode(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(ex.getMessage().contains("BUILDER_ACTIVATION_CODE"));
        assertTrue(ex.getMessage().contains("Application startup aborted"));
    }

    @Test
    @DisplayName("Blank/whitespace activation code fails startup validation with IllegalStateException")
    void validate_BlankCode_ThrowsIllegalStateException() {
        BuilderProperties properties = new BuilderProperties();
        properties.setActivationCode("   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(ex.getMessage().contains("BUILDER_ACTIVATION_CODE"));
    }
}
