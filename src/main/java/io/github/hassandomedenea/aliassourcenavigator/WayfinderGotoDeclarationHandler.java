package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class WayfinderGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][\\w$]*");

    @Override
    public @Nullable PsiElement[] getGotoDeclarationTargets(
        @Nullable PsiElement sourceElement,
        int offset,
        Editor editor
    ) {
        if (sourceElement == null) {
            return null;
        }

        String symbolName = identifierAt(sourceElement, offset);
        if (symbolName == null) {
            return null;
        }

        PsiElement resolved = resolveReference(sourceElement);
        if (resolved == null) {
            return null;
        }

        PsiFile resolvedFile = resolved.getContainingFile();
        if (!WayfinderGeneratedDetector.isWayfinderFile(resolvedFile)) {
            return null;
        }

        PsiElement namedExport = WayfinderTsExportResolver.resolveNamedExport(resolved, symbolName);
        if (namedExport == null) {
            return null;
        }

        PsiElement phpTarget = resolvePhpTarget(namedExport);
        if (phpTarget != null) {
            return new PsiElement[]{phpTarget, namedExport};
        }

        // Still improve TS landing even when PHP cannot be resolved.
        if (!isSameNavigationTarget(resolved, namedExport)) {
            return new PsiElement[]{namedExport};
        }

        return null;
    }

    private static @Nullable PsiElement resolvePhpTarget(PsiElement namedExport) {
        PsiFile file = namedExport.getContainingFile();
        if (file == null) {
            return null;
        }

        String fileText = file.getText();
        String methodName = exportName(namedExport);
        if (fileText == null || methodName == null) {
            return null;
        }

        Integer exportOffset = WayfinderTsExportResolver.namedExportOffset(fileText, methodName);
        if (exportOffset == null) {
            exportOffset = namedExport.getTextOffset();
        }

        String docComment = WayfinderJsdocParser.findDocCommentBefore(fileText, exportOffset);
        WayfinderJsdocParser.PhpTarget target = WayfinderJsdocParser.parse(docComment);
        if (target == null) {
            return null;
        }

        return WayfinderPhpMethodResolver.resolve(namedExport.getProject(), target);
    }

    private static @Nullable String exportName(PsiElement namedExport) {
        if (namedExport instanceof com.intellij.lang.javascript.psi.JSNamedElement named
            && named.getName() != null
        ) {
            return named.getName();
        }

        String text = namedExport.getText();
        if (text != null && IDENTIFIER.matcher(text).matches()) {
            return text;
        }

        return null;
    }

    private static @Nullable String identifierAt(PsiElement sourceElement, int offset) {
        PsiFile sourceFile = sourceElement.getContainingFile();
        PsiElement leaf = sourceFile != null ? sourceFile.findElementAt(offset) : null;

        if (leaf != null && IDENTIFIER.matcher(leaf.getText()).matches()) {
            return leaf.getText();
        }

        if (IDENTIFIER.matcher(sourceElement.getText()).matches()) {
            return sourceElement.getText();
        }

        return null;
    }

    private static @Nullable PsiElement resolveReference(PsiElement sourceElement) {
        PsiElement current = sourceElement;

        for (int depth = 0; current != null && depth < 8; depth++, current = current.getParent()) {
            for (PsiReference reference : current.getReferences()) {
                PsiElement resolved = reference.resolve();
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }

    private static boolean isSameNavigationTarget(PsiElement first, PsiElement second) {
        if (first == null || second == null) {
            return false;
        }

        PsiElement left = first.getNavigationElement();
        PsiElement right = second.getNavigationElement();
        return left.equals(right) || first.equals(second);
    }
}
