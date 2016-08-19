package io.github.sirlantis.rubymine.rubocop;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaRubocopInspection extends LocalInspectionTool implements BatchSuppressableTool, UnfairLocalInspectionTool {

    public static String INSPECTION_SHORT_NAME = "RubocopInspection";

    public static Key<JavaRubocopInspection> KEY() {
        return Key.create(INSPECTION_SHORT_NAME);
    }

    public static Logger LOG() {
        return Logger.getInstance(JavaRubocopBundle.LOG_ID);
    }

    public String getStaticDescription() {
        return "Uses RuboCop for linting.<br>Make sure Rubocop gem is installed.<br><br><b>Note:</b> Selected color doesn't have an effect!";
    }

    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        return ExternalAnnotatorInspectionVisitor.checkFileWithExternalAnnotator(file, manager, isOnTheFly, JavaRubocopAnnotator.getInstance());
    }

    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
    {
        return new ExternalAnnotatorInspectionVisitor(holder, JavaRubocopAnnotator.getInstance(), isOnTheFly);
    }

    @NotNull
    @Pattern("[a-zA-Z_0-9.-]+")
    public String getID(){
        return "Settings.Ruby.Linters.Rubocop";
    }

    public boolean isSuppressedFor(@NotNull PsiElement element) {
        return false;
    }

    public boolean runForWholeFile() {
        return true;
    }

    @NotNull
    public SuppressQuickFix[] getBatchSuppressActions(PsiElement element) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
