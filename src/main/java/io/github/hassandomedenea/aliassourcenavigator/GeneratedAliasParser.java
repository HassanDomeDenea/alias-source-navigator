package io.github.hassandomedenea.aliassourcenavigator;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GeneratedAliasParser {
    private GeneratedAliasParser() {
    }

    static @Nullable AliasTarget find(String declarationText, String symbolName) {
        String identifier = "(?<![\\w$])" + Pattern.quote(symbolName) + "(?![\\w$])";
        Pattern pattern = Pattern.compile(
            identifier
                + "\\s*:\\s*typeof\\s+import\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
                + "\\s*(?:\\.\\s*([A-Za-z_$][\\w$]*)|\\[\\s*['\"]([^'\"]+)['\"]\\s*])"
        );
        Matcher matcher = pattern.matcher(declarationText);

        if (!matcher.find()) {
            return null;
        }

        String exportName = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);

        return new AliasTarget(matcher.group(1), exportName);
    }

    record AliasTarget(String modulePath, String exportName) {
    }
}
