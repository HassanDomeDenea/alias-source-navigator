package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class GeneratedAliasGotoDeclarationHandler implements GotoDeclarationHandler {
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

        PsiFile resolvedDeclarationFile = resolveDeclarationFile(sourceElement);
        if (
            resolvedDeclarationFile != null
                && !GeneratedAliasResolver.isDeclarationFile(resolvedDeclarationFile)
        ) {
            return null;
        }

        Collection<PsiFile> declarationFiles = resolvedDeclarationFile != null
            ? List.of(resolvedDeclarationFile)
            : GeneratedAliasResolver.findGeneratedDeclarationFiles(sourceElement.getProject());

        for (PsiFile declarationFile : declarationFiles) {
            GeneratedAliasParser.AliasTarget alias = GeneratedAliasParser.find(
                declarationFile.getText(),
                symbolName
            );
            if (alias == null) {
                continue;
            }

            PsiElement target = GeneratedAliasResolver.resolveAliasTarget(declarationFile, alias);
            if (target != null) {
                return new PsiElement[]{target};
            }
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

    private static @Nullable PsiFile resolveDeclarationFile(PsiElement sourceElement) {
        PsiElement current = sourceElement;

        for (int depth = 0; current != null && depth < 6; depth++, current = current.getParent()) {
            for (PsiReference reference : current.getReferences()) {
                PsiElement resolved = reference.resolve();
                if (resolved != null && resolved.getContainingFile() != null) {
                    return resolved.getContainingFile();
                }
            }
        }

        return null;
    }

}
