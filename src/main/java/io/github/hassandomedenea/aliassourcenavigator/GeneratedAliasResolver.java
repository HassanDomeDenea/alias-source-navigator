package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class GeneratedAliasResolver {
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

    private GeneratedAliasResolver() {
    }

    static boolean isDeclarationFile(PsiFile file) {
        return file.getName().matches(".+\\.d\\.(?:c|m)?ts");
    }

    static Collection<PsiFile> findGeneratedDeclarationFiles(Project project) {
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

    static @Nullable PsiElement resolveAliasTarget(
        PsiFile declarationFile,
        GeneratedAliasParser.AliasTarget alias
    ) {
        VirtualFile targetVirtualFile = resolveAliasTargetFile(declarationFile, alias);
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

    static @Nullable JSNamedElement resolveAliasDeclaration(
        PsiFile declarationFile,
        GeneratedAliasParser.AliasTarget alias
    ) {
        PsiElement element = declarationFile.findElementAt(alias.symbolOffset());

        for (int depth = 0; element != null && depth < 8; depth++, element = element.getParent()) {
            if (
                element instanceof JSNamedElement namedElement
                    && alias.symbolName().equals(namedElement.getName())
            ) {
                return namedElement;
            }
        }

        return null;
    }

    private static @Nullable VirtualFile resolveAliasTargetFile(
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

        return findModuleFile(declarationVirtualFile.getParent(), alias.modulePath());
    }

    private static @Nullable VirtualFile findModuleFile(
        VirtualFile baseDirectory,
        String modulePath
    ) {
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
