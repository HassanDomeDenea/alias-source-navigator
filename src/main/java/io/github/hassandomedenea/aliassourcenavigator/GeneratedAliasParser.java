package io.github.hassandomedenea.aliassourcenavigator;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GeneratedAliasParser {
    private static final Pattern ALIAS_PATTERN = Pattern.compile(
        "(?<![\\w$])([A-Za-z_$][\\w$]*)"
            + "\\s*:\\s*typeof\\s+import\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
            + "\\s*(?:\\.\\s*([A-Za-z_$][\\w$]*)|\\[\\s*['\"]([^'\"]+)['\"]\\s*])"
    );

    private GeneratedAliasParser() {
    }

    static @Nullable AliasTarget find(String declarationText, String symbolName) {
        Matcher matcher = ALIAS_PATTERN.matcher(declarationText);

        while (matcher.find()) {
            if (symbolName.equals(matcher.group(1))) {
                return aliasTarget(matcher);
            }
        }

        return null;
    }

    static List<AliasTarget> findByExportName(String declarationText, String exportName) {
        Matcher matcher = ALIAS_PATTERN.matcher(declarationText);
        List<AliasTarget> aliases = new ArrayList<>();

        while (matcher.find()) {
            AliasTarget alias = aliasTarget(matcher);
            if (exportName.equals(alias.exportName())) {
                aliases.add(alias);
            }
        }

        return aliases;
    }

    private static AliasTarget aliasTarget(Matcher matcher) {
        String exportName = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);

        return new AliasTarget(
            matcher.group(1),
            matcher.group(2),
            exportName,
            matcher.start(1)
        );
    }

    record AliasTarget(
        String symbolName,
        String modulePath,
        String exportName,
        int symbolOffset
    ) {
    }
}
