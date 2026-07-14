package io.github.hassandomedenea.aliassourcenavigator;

import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;

import java.util.ArrayList;
import java.util.List;

public final class GeneratedAliasReferencesSearcher implements QueryExecutor<
    PsiReference,
    ReferencesSearch.SearchParameters
> {
    @Override
    public boolean execute(
        ReferencesSearch.SearchParameters parameters,
        Processor<? super PsiReference> consumer
    ) {
        List<AliasSearchTarget> searchTargets = ReadAction.computeCancellable(
            () -> findAliasSearchTargets(parameters)
        );

        for (AliasSearchTarget searchTarget : searchTargets) {
            boolean completed = ReferencesSearch.search(
                searchTarget.aliasDeclaration(),
                parameters.getScopeDeterminedByUser(),
                parameters.isIgnoreAccessScope()
            ).forEach((Processor<PsiReference>) reference -> consumer.process(
                new AliasSourcePsiReference(reference, searchTarget.sourceTarget())
            ));

            if (!completed) {
                return false;
            }
        }

        return true;
    }

    private static List<AliasSearchTarget> findAliasSearchTargets(
        ReferencesSearch.SearchParameters parameters
    ) {
        PsiElement sourceElement = parameters.getElementToSearch();
        JSNamedElement sourceDeclaration = namedElement(sourceElement);
        if (sourceDeclaration == null || sourceDeclaration.getName() == null) {
            return List.of();
        }

        PsiFile sourceFile = sourceDeclaration.getContainingFile();
        if (sourceFile == null || GeneratedAliasResolver.isDeclarationFile(sourceFile)) {
            return List.of();
        }

        PsiElement sourceTarget = sourceDeclaration.getNavigationElement();
        PsiManager psiManager = PsiManager.getInstance(sourceElement.getProject());
        List<AliasSearchTarget> searchTargets = new ArrayList<>();

        for (
            PsiFile declarationFile
            : GeneratedAliasResolver.findGeneratedDeclarationFiles(sourceElement.getProject())
        ) {
            List<GeneratedAliasParser.AliasTarget> aliases = GeneratedAliasParser.findByExportName(
                declarationFile.getText(),
                sourceDeclaration.getName()
            );

            for (GeneratedAliasParser.AliasTarget alias : aliases) {
                PsiElement aliasTarget = GeneratedAliasResolver.resolveAliasTarget(
                    declarationFile,
                    alias
                );
                if (!areEquivalent(psiManager, sourceTarget, aliasTarget)) {
                    continue;
                }

                JSNamedElement aliasDeclaration = GeneratedAliasResolver.resolveAliasDeclaration(
                    declarationFile,
                    alias
                );
                if (aliasDeclaration == null) {
                    continue;
                }

                searchTargets.add(new AliasSearchTarget(aliasDeclaration, sourceTarget));
            }
        }

        return searchTargets;
    }

    private static JSNamedElement namedElement(PsiElement element) {
        if (element instanceof JSNamedElement namedElement) {
            return namedElement;
        }

        return PsiTreeUtil.getParentOfType(element, JSNamedElement.class, false);
    }

    private static boolean areEquivalent(
        PsiManager psiManager,
        PsiElement sourceTarget,
        PsiElement aliasTarget
    ) {
        if (aliasTarget == null) {
            return false;
        }

        return psiManager.areElementsEquivalent(sourceTarget, aliasTarget)
            || psiManager.areElementsEquivalent(
                sourceTarget.getNavigationElement(),
                aliasTarget.getNavigationElement()
            );
    }

    private record AliasSearchTarget(
        JSNamedElement aliasDeclaration,
        PsiElement sourceTarget
    ) {
    }
}
