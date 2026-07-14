package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;

final class AliasSourcePsiReference implements PsiReference {
    private final PsiReference delegate;
    private final PsiElement sourceTarget;

    AliasSourcePsiReference(PsiReference delegate, PsiElement sourceTarget) {
        this.delegate = delegate;
        this.sourceTarget = sourceTarget;
    }

    @Override
    public PsiElement getElement() {
        return delegate.getElement();
    }

    @Override
    public TextRange getRangeInElement() {
        return delegate.getRangeInElement();
    }

    @Override
    public PsiElement resolve() {
        return sourceTarget;
    }

    @Override
    public String getCanonicalText() {
        return delegate.getCanonicalText();
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        return delegate.handleElementRename(newElementName);
    }

    @Override
    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
        return delegate.bindToElement(element);
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        return PsiManager.getInstance(element.getProject())
            .areElementsEquivalent(sourceTarget, element);
    }

    @Override
    public Object[] getVariants() {
        return delegate.getVariants();
    }

    @Override
    public boolean isSoft() {
        return delegate.isSoft();
    }
}
