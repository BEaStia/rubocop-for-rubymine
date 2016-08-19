package io.github.sirlantis.rubymine.rubocop;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.github.sirlantis.rubymine.rubocop.model.JavaFileResult;
import io.github.sirlantis.rubymine.rubocop.model.JavaOffense;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igorpavlov on 17.08.16.
 */
public class JavaRubocopAnnotator extends ExternalAnnotator<JavaRubocopAnnotator.Input, JavaRubocopAnnotator.Result> {

    public class Input {
        Module module;
        PsiFile file;
        String content;
        EditorColorsScheme colorScheme;

        public Input(Module module, PsiFile file, String content, EditorColorsScheme colorScheme) {
            this.module = module;
            this.file = file;
            this.content = content;
            this.colorScheme = colorScheme;
        }
    }

    public class Result {
        Input input;
        JavaFileResult result;
        ArrayList<String> warnings;

        public Result(Input input, JavaFileResult result, ArrayList<String> warnings) {
            this.input = input;
            this.result = result;
            this.warnings = warnings;
        }
    }

    private HighlightDisplayKey inspectionKey = null;

    private HighlightDisplayKey getInspectionKey() {
        String id = "Rubocop";
        if (inspectionKey == null) {
            HighlightDisplayKey key = HighlightDisplayKey.find(id);
            if (key == null)
                inspectionKey = new HighlightDisplayKey(id, id);
            else
                inspectionKey = key;
        }
        return inspectionKey;
    }

    public JavaRubocopAnnotator.Input collectInformation(PsiFile file, Editor editor, Boolean hasErrors) {
        return collectInformation(file, editor);
    }

    public JavaRubocopAnnotator.Input collectInformation(PsiFile file){
        return collectInformation(file, null);
    }

    @Nullable
    private Input collectInformation(PsiFile file, Editor editor) {
        if (file.getContext() != null || !isRubyFile(file)) {
            return null;
        }

        VirtualFile virtualFile = file.getVirtualFile();

        if (!virtualFile.isInLocalFileSystem()) {
            return null;
        }

        Project project = file.getProject();
        Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
        if (module == null)
            return null;

        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null)
            return null;

        return new Input(module, file, document.getText(), editor.getColorsScheme());
    }

    public Boolean isRubyFile(PsiFile file) {
        return file.getFileType().getName().equals("Ruby");
    }

    public void apply(PsiFile file, Result annotationResult, AnnotationHolder holder) {

        if (annotationResult == null) {
            return;
        }

        //showWarnings(annotationResult);

        JavaFileResult result = annotationResult.result;
        if (result == null) {
            return;
        }

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null)
            return;

        List<JavaOffense> offenses = result.offenses;

        for(JavaOffense offense: offenses) {
            HighlightSeverity severity = severityForOffense(offense);
            createAnnotation(holder, document, offense, "RuboCop: ", severity, false);
        }
    }

    private HighlightSeverity severityForOffense(JavaOffense offense) {
        String severity = offense.severity;
        if (severity.equals("error") || severity.equals("fatal")) {
            return HighlightSeverity.ERROR;
        }

        if (severity.equals("warning")) return HighlightSeverity.WARNING;
        if (severity.equals("convention")) return HighlightSeverity.WEAK_WARNING;

        if (severity.equals("factor"))
            return HighlightSeverity.INFORMATION;
        return HighlightSeverity.INFORMATION;
    }


    private Annotation createAnnotation(AnnotationHolder holder, Document document,
                                        JavaOffense offense,
                                        String prefix, HighlightSeverity severity,
                                        Boolean showErrorOnWholeLine) {

        int offenseLine = clamp(0, document.getLineCount() - 1, offense.location.line - 1);

        int lineEndOffset = document.getLineEndOffset(offenseLine);
        int lineStartOffset = document.getLineStartOffset(offenseLine);

        TextRange range;

        if (showErrorOnWholeLine || offense.location.length <= 0) {
            range = new TextRange(lineStartOffset, lineEndOffset);
        } else {
            int length = offense.location.length;
            int start = lineStartOffset + (offense.location.column - 1);
            range = new TextRange(start, start + length);
        }

        String message = prefix + offense.message.trim() + " (" + offense.cop + ")";
        return holder.createAnnotation(severity, range, message);
    }

    public static <T extends Comparable<T>> T clamp(T val, T min, T max) {
        if (val.compareTo(min) < 0) return min;
        else if (val.compareTo(max) > 0) return max;
        else return val;
    }

    private static JavaRubocopAnnotator instance = null;

    public static JavaRubocopAnnotator getInstance() {
        if (instance == null)
            instance = new JavaRubocopAnnotator();
        return instance;
    }

    public Result doAnnotate(Input collectedInfo) {
        if (collectedInfo == null) {
            return null;
        }

        JavaRubocopTask task = new JavaRubocopTask(collectedInfo.module.getProject(), collectedInfo.file.getVirtualFile());

        task.run();

        return new Result(collectedInfo, task.result.fileResults.get(0), (ArrayList<String>) task.result.warnings);
    }

}

