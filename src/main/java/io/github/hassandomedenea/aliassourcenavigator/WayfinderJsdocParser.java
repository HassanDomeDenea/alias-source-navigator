package io.github.hassandomedenea.aliassourcenavigator;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WayfinderJsdocParser {
    private static final Pattern FQN_SEE = Pattern.compile(
        "@see\\s+\\\\([A-Za-z_\\\\][A-Za-z0-9_\\\\]*)::([A-Za-z_][A-Za-z0-9_]*)"
    );
    private static final Pattern PATH_SEE = Pattern.compile(
        "@see\\s+\\\\?([A-Za-z]:\\\\[^\\s*]+?\\.php):(\\d+)"
    );
    private static final Pattern PATH_SEE_UNIX = Pattern.compile(
        "@see\\s+(/[^\\s*]+?\\.php):(\\d+)"
    );
    private static final Pattern BLOCK_COMMENT = Pattern.compile(
        "/\\*\\*.*?\\*/",
        Pattern.DOTALL
    );
    /**
     * Text that may appear between a Wayfinder JSDoc block and the exported symbol name offset.
     * When the offset is the identifier itself, this is often only {@code export const}
     * (the name is at the offset, so it is not part of the "between" span).
     */
    private static final Pattern ALLOWED_BETWEEN_DOC_AND_NAME = Pattern.compile(
        "export\\s+(?:const|function|let|var)(?:\\s+[A-Za-z_$][\\w$]*)?"
    );

    private WayfinderJsdocParser() {
    }

    static @Nullable PhpTarget parse(@Nullable String docComment) {
        if (docComment == null || docComment.isBlank()) {
            return null;
        }

        String classFqn = null;
        String methodName = null;
        String filePath = null;
        Integer line = null;

        Matcher fqnMatcher = FQN_SEE.matcher(docComment);
        if (fqnMatcher.find()) {
            classFqn = "\\" + fqnMatcher.group(1);
            methodName = fqnMatcher.group(2);
        }

        Matcher pathMatcher = PATH_SEE.matcher(docComment);
        if (pathMatcher.find()) {
            filePath = pathMatcher.group(1);
            line = Integer.parseInt(pathMatcher.group(2));
        } else {
            Matcher unixMatcher = PATH_SEE_UNIX.matcher(docComment);
            if (unixMatcher.find()) {
                filePath = unixMatcher.group(1);
                line = Integer.parseInt(unixMatcher.group(2));
            }
        }

        if (classFqn == null && filePath == null) {
            return null;
        }

        return new PhpTarget(classFqn, methodName, filePath, line);
    }

    static @Nullable String findDocCommentBefore(String fileText, int offset) {
        if (fileText == null || offset <= 0 || offset > fileText.length()) {
            return null;
        }

        int cursor = offset;
        while (cursor > 0 && Character.isWhitespace(fileText.charAt(cursor - 1))) {
            cursor--;
        }

        String prefix = fileText.substring(0, cursor);
        Matcher matcher = BLOCK_COMMENT.matcher(prefix);
        String lastComment = null;
        while (matcher.find()) {
            lastComment = matcher.group();
        }

        if (lastComment == null) {
            return null;
        }

        int commentStart = prefix.lastIndexOf(lastComment);
        String between = prefix.substring(commentStart + lastComment.length());
        if (!isAllowedBetweenDocAndName(between)) {
            return null;
        }

        return lastComment;
    }

    private static boolean isAllowedBetweenDocAndName(String between) {
        if (between == null || between.isBlank()) {
            return true;
        }

        // Offset is often the exported identifier (autocomplete), not the "export" keyword.
        // Accept only the generated declaration prefix so unrelated code still blocks the match.
        return ALLOWED_BETWEEN_DOC_AND_NAME.matcher(between.strip()).matches();
    }

    record PhpTarget(
        @Nullable String classFqn,
        @Nullable String methodName,
        @Nullable String filePath,
        @Nullable Integer line
    ) {
    }
}
