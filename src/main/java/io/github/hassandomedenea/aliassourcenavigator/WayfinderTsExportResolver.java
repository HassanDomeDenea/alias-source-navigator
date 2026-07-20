package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WayfinderTsExportResolver {
    private WayfinderTsExportResolver() {
    }

    static @Nullable PsiElement resolveNamedExport(
        PsiElement resolvedElement,
        @Nullable String preferredName
    ) {
        if (resolvedElement == null) {
            return null;
        }

        PsiFile file = resolvedElement.getContainingFile();
        if (file == null) {
            return null;
        }

        String methodName = preferredName;
        if (methodName == null || methodName.isBlank()) {
            methodName = elementName(resolvedElement);
        }
        if (methodName == null || methodName.isBlank()) {
            return null;
        }

        if (isNamedExportFor(resolvedElement, methodName)) {
            return navigationTarget(resolvedElement);
        }

        return findNamedExport(file, methodName);
    }

    static @Nullable PsiElement findNamedExport(PsiFile file, String methodName) {
        String text = file.getText();
        if (text == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(
            "export\\s+(?:const|function|let|var)\\s+(" + Pattern.quote(methodName) + ")\\b"
        );
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        PsiElement leaf = file.findElementAt(matcher.start(1));
        if (leaf == null) {
            return null;
        }

        JSNamedElement named = PsiTreeUtil.getParentOfType(leaf, JSNamedElement.class, false);
        if (named != null && methodName.equals(named.getName())) {
            return navigationTarget(named);
        }

        return leaf;
    }

    static @Nullable Integer namedExportOffset(String fileText, String methodName) {
        if (fileText == null || methodName == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(
            "export\\s+(?:const|function|let|var)\\s+(" + Pattern.quote(methodName) + ")\\b"
        );
        Matcher matcher = pattern.matcher(fileText);
        if (!matcher.find()) {
            return null;
        }

        return matcher.start(1);
    }

    private static boolean isNamedExportFor(PsiElement element, String methodName) {
        JSNamedElement named = element instanceof JSNamedElement jsNamed
            ? jsNamed
            : PsiTreeUtil.getParentOfType(element, JSNamedElement.class, false);
        if (named == null || !methodName.equals(named.getName())) {
            return false;
        }

        PsiFile file = named.getContainingFile();
        if (file == null) {
            return false;
        }

        Integer exportOffset = namedExportOffset(file.getText(), methodName);
        if (exportOffset == null) {
            return false;
        }

        int elementOffset = named.getTextOffset();
        return Math.abs(elementOffset - exportOffset) <= methodName.length() + 16;
    }

    private static @Nullable String elementName(PsiElement element) {
        if (element instanceof JSNamedElement named && named.getName() != null) {
            return named.getName();
        }

        JSNamedElement parent = PsiTreeUtil.getParentOfType(element, JSNamedElement.class, false);
        if (parent != null && parent.getName() != null) {
            return parent.getName();
        }

        String text = element.getText();
        if (text != null && text.matches("[A-Za-z_$][\\w$]*")) {
            return text;
        }

        return null;
    }

    private static PsiElement navigationTarget(PsiElement element) {
        return element.getNavigationElement() != null
            ? element.getNavigationElement()
            : element;
    }
}
