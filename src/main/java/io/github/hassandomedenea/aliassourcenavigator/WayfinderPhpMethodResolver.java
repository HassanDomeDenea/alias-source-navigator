package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

final class WayfinderPhpMethodResolver {
    private WayfinderPhpMethodResolver() {
    }

    static @Nullable PsiElement resolve(Project project, WayfinderJsdocParser.PhpTarget target) {
        if (target == null) {
            return null;
        }

        PsiElement fromFqn = resolveByFqn(project, target.classFqn(), target.methodName());
        if (fromFqn != null) {
            return fromFqn;
        }

        return resolveByPath(project, target.filePath(), target.line());
    }

    private static @Nullable PsiElement resolveByFqn(
        Project project,
        @Nullable String classFqn,
        @Nullable String methodName
    ) {
        if (classFqn == null || methodName == null) {
            return null;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> classes = phpIndex.getClassesByFQN(classFqn);
        if (classes.isEmpty() && classFqn.startsWith("\\")) {
            classes = phpIndex.getClassesByFQN(classFqn.substring(1));
        }
        if (classes.isEmpty() && !classFqn.startsWith("\\")) {
            classes = phpIndex.getClassesByFQN("\\" + classFqn);
        }

        for (PhpClass phpClass : classes) {
            Method method = phpClass.findMethodByName(methodName);
            if (method != null) {
                return method.getNavigationElement() != null
                    ? method.getNavigationElement()
                    : method;
            }
        }

        return null;
    }

    private static @Nullable PsiElement resolveByPath(
        Project project,
        @Nullable String filePath,
        @Nullable Integer line
    ) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        VirtualFile virtualFile = findFile(filePath);
        if (virtualFile == null) {
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        if (line == null || line <= 0) {
            return psiFile;
        }

        CharSequence text = psiFile.getViewProvider().getContents();
        int offset = offsetOfLine(text, line);
        if (offset < 0) {
            return psiFile;
        }

        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? element : psiFile;
    }

    private static @Nullable VirtualFile findFile(String filePath) {
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile direct = fileSystem.findFileByPath(filePath.replace('\\', '/'));
        if (direct != null) {
            return direct;
        }

        return fileSystem.findFileByPath(filePath);
    }

    private static int offsetOfLine(CharSequence text, int line) {
        if (line <= 1) {
            return 0;
        }

        int currentLine = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                currentLine++;
                if (currentLine == line) {
                    return Math.min(i + 1, text.length());
                }
            }
        }

        return -1;
    }
}
