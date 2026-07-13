package io.github.hassandomedenea.aliassourcenavigator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class GeneratedAliasParserTest {
    @Test
    void parsesAutoImportedComposable() {
        GeneratedAliasParser.AliasTarget target = GeneratedAliasParser.find(
            "const useAuth: typeof import('../modules/auth/authStore').useAuth",
            "useAuth"
        );

        assertEquals("../modules/auth/authStore", target.modulePath());
        assertEquals("useAuth", target.exportName());
    }

    @Test
    void parsesAutoImportedComponent() {
        GeneratedAliasParser.AliasTarget target = GeneratedAliasParser.find(
            "LoginForm: typeof import('./../modules/auth/LoginForm.vue')['default']",
            "LoginForm"
        );

        assertEquals("./../modules/auth/LoginForm.vue", target.modulePath());
        assertEquals("default", target.exportName());
    }

    @Test
    void ignoresDifferentSymbols() {
        GeneratedAliasParser.AliasTarget target = GeneratedAliasParser.find(
            "const useAuth: typeof import('../modules/auth/authStore').useAuth",
            "useTestStore"
        );

        assertNull(target);
    }
}
