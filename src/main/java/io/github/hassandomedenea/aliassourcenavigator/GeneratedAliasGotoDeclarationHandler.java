package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class GeneratedAliasGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][\\w$]*");
    private static final Set<String> GENERATED_DECLARATION_FILES = Set.of(
        "auto-imports.d.ts",
        "imports.d.ts",
        "components.d.ts"
    );
    private static final List<String> MODULE_SUFFIXES = List.of(
        "",
        ".ts",
        ".tsx",
        ".js",
        ".jsx",
        ".mts",
        ".cts",
        ".vue",
        "/index.ts",
        "/index.tsx",
        "/index.js",
        "/index.vue"
    );

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
        if (resolvedDeclarationFile != null && !isDeclarationFile(resolvedDeclarationFile)) {
            return null;
        }

        Collection<PsiFile> declarationFiles = resolvedDeclarationFile != null
            ? List.of(resolvedDeclarationFile)
            : findGeneratedDeclarationFiles(sourceElement.getProject());

        for (PsiFile declarationFile : declarationFiles) {
            GeneratedAliasParser.AliasTarget alias = GeneratedAliasParser.find(
                declarationFile.getText(),
                symbolName
            );
            if (alias == null) {
                continue;
            }

            PsiElement target = resolveAliasTarget(declarationFile, alias);
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

    private static boolean isDeclarationFile(PsiFile file) {
        return file.getName().matches(".+\\.d\\.(?:c|m)?ts");
    }

    private static Collection<PsiFile> findGeneratedDeclarationFiles(Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        PsiManager psiManager = PsiManager.getInstance(project);
        List<PsiFile> files = new ArrayList<>();

        for (String fileName : GENERATED_DECLARATION_FILES) {
            for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName(fileName, scope)) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    files.add(psiFile);
                }
            }
        }

        return files;
    }

    private static @Nullable PsiElement resolveAliasTarget(
        PsiFile declarationFile,
        GeneratedAliasParser.AliasTarget alias
    ) {
        VirtualFile declarationVirtualFile = declarationFile.getVirtualFile();
        if (declarationVirtualFile == null || declarationVirtualFile.getParent() == null) {
            return null;
        }

        if (!alias.modulePath().startsWith(".")) {
            return null;
        }

        VirtualFile targetVirtualFile = findModuleFile(
            declarationVirtualFile.getParent(),
            alias.modulePath()
        );
        if (targetVirtualFile == null) {
            return null;
        }

        PsiFile targetFile = PsiManager.getInstance(declarationFile.getProject())
            .findFile(targetVirtualFile);
        if (targetFile == null) {
            return null;
        }

        if ("default".equals(alias.exportName())) {
            return targetFile;
        }

        Collection<JSNamedElement> namedElements = PsiTreeUtil.findChildrenOfType(
            targetFile,
            JSNamedElement.class
        );
        for (JSNamedElement namedElement : namedElements) {
            if (alias.exportName().equals(namedElement.getName())) {
                return namedElement.getNavigationElement();
            }
        }

        return targetFile;
    }

    private static @Nullable VirtualFile findModuleFile(VirtualFile baseDirectory, String modulePath) {
        String normalizedPath = modulePath.replace('\\', '/');

        for (String suffix : MODULE_SUFFIXES) {
            VirtualFile candidate = VfsUtil.findRelativeFile(normalizedPath + suffix, baseDirectory);
            if (candidate != null && !candidate.isDirectory()) {
                return candidate;
            }
        }

        return null;
    }
}
