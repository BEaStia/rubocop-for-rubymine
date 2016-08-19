package io.github.sirlantis.rubymine.rubocop;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.sirlantis.rubymine.rubocop.model.JavaRubocopResult;
import io.github.sirlantis.rubymine.rubocop.utils.JavaNotifyUtil;
import org.apache.sanselan.util.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.gem.util.BundlerUtil;
import org.jetbrains.plugins.ruby.ruby.run.CmdlinePreprocessor;
import org.jetbrains.plugins.ruby.ruby.run.RubyCommandLine;
import org.jetbrains.plugins.ruby.ruby.run.Runner;
import org.jetbrains.plugins.ruby.ruby.run.RunnerUtil;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by igorpavlov on 17.08.16.
 */
/*public class JavaRubocopTask(Module module, ArrayList<String> paths) extends Backgroundable(module.project, "Running RuboCop", true) {

}*/

public class JavaRubocopTask extends Task.Backgroundable {

    public JavaRubocopResult result = null;
    private Module module = null;
    private VirtualFile virtualFile = null;
    private String workDirectory = "";
    private Logger logger = null;


    private Sdk getSdk(Module module) {
        return ModuleRootManager.getInstance(module).getSdk();
    }

    public JavaRubocopTask(@Nullable Project project, VirtualFile vFile) {
        super(project, "RubocopTask");
        virtualFile = vFile;
        Module[] modules = ModuleManager.getInstance(this.getProject()).getModules();
        logger = Logger.getInstance(JavaRubocopBundle.LOG_ID);
        for (Module module1 : modules) {
            String moduleSdkName = ModuleRootManager.getInstance(module1).getSdk().getSdkType().getName();
            logger.warn(moduleSdkName);
            if (moduleSdkName.equals("RUBY_SDK"))
                module = module1;
            logger.warn(module1.getName());
        }
        workDirectory = getWorkDirectory();
    }

    public JavaRubocopTask(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, "RubocopTask", canBeCancelled);
    }

    public JavaRubocopTask(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled, @Nullable PerformInBackgroundOption backgroundOption) {
        super(project, "RubocopTask", canBeCancelled, backgroundOption);
    }

    @Contract(pure = true)
    private Application getApp() {
        return ApplicationManager.getApplication();
    }

    @NotNull
    private String getWorkDirectory() {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        try {
            if (roots.length > 0)
                return roots[0].getCanonicalPath();
            else
                return getProject().getProjectFilePath();
        } catch (Exception e) {
            return "";
        }
    }

    private String getSdkRoot() {
        return getSdk(module).getHomeDirectory().getParent().getCanonicalPath();
    }

    public void run() {
        logger.warn("Hello, world!");
        if (module != null) {
            Runner runner = RunnerUtil.getRunner(getSdk(module), module);
            Boolean sudo = false;

            RubyCommandLine setupCmdLine = runner.createAndSetupCmdLine(workDirectory, null, true, "rubocop", getSdk(module), sudo, "--format", "json", virtualFile.getCanonicalPath());
            CmdlinePreprocessor preprocessor = BundlerUtil.createBundlerPreprocessor(module, getSdk(module));
            preprocessor.preprocess(setupCmdLine);
            logger.debug("Executing RuboCop (SDK=" + getSdkRoot() + ")", setupCmdLine.getCommandLineString());


            try {
                parseProcessOutput(setupCmdLine.createProcess());
            } catch (Exception e) {
                logger.error(e);
            }
        }

        /*val command = commandLineList.removeFirst();
        val args = commandLineList.toTypedArray();
        val sudo = false;

        val commandLine = runner.createAndSetupCmdLine(workDirectory.canonicalPath!!, null, true, command, sdk, sudo, *args);
        CmdlinePreprocessor preprocessor = BundlerUtil.createBundlerPreprocessor(module, sdk);
        preprocessor.preprocess(commandLine);

        logger.debug("Executing RuboCop (SDK=%s)".format(sdkRoot), commandLine.commandLineString);*/

        //parseProcessOutput { commandLine.createProcess() }
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        run();
    }

    private void parseProcessOutput(Process start) {

        int bufferSize = 5 * 1024 * 1024;

        BufferedInputStream stdoutStream = new BufferedInputStream(start.getInputStream(), bufferSize);
        InputStreamReader stdoutReader = new InputStreamReader(stdoutStream);

        BufferedInputStream stderrStream = new BufferedInputStream(start.getErrorStream(), bufferSize);
        InputStreamReader stderrReader = new InputStreamReader(stderrStream);

        try {
            result = JavaRubocopResult.readFromReader(stdoutReader, stderrReader);
        } catch (Exception e) {
            logger.warn("Failed to parse RuboCop output", e);
            logParseFailure(stderrStream, stdoutStream);
        }

        Boolean exited = false;

        try {
            start.waitFor();
            exited = true;
        } catch (Exception e) {
            logger.error("Interrupted while waiting for RuboCop", e);
        }

        try {
            stdoutStream.close();
        } catch (Exception e) {
            logger.warn("Exception is on closing stdOut");
        }

        try {
            stderrStream.close();
        } catch (Exception e) {
            logger.warn("Exception is on closing stdErr");
        }

        if (exited) {
            if (start.exitValue() == 0 || start.exitValue() == 1) {
                logger.warn("RuboCop exited with " + start.exitValue());
            } else {

                logger.info("RuboCop exited with " + start.exitValue());
            }
        }

        if (result != null) {
            logger.warn(String.valueOf(result));
        }
    }

    private void logParseFailure(BufferedInputStream stderrStream, BufferedInputStream stdoutStream) {
        String stdout;
        String stderr;
        try {
            stdout = readStreamToString(stdoutStream, true);
            stderr = readStreamToString(stderrStream, true);
        } catch (IOException e) {
            e.printStackTrace();
            JavaNotifyUtil.notifyError(getProject(), "Failed to parse RuboCop output", e.getMessage());
            return;
        }


        logger.warn("=== RuboCop STDOUT START ===\n%s\n=== RuboCop STDOUT END ===".format(stdout));
        logger.warn("=== RuboCop STDERR START ===\n%s\n=== RuboCop STDERR END ===".format(stderr));

        StringBuilder errorBuilder = new StringBuilder("Please make sure that:");
        errorBuilder.append("<ul>");

        errorBuilder.append("<li>you installed RuboCop for this Ruby version</li>");
        errorBuilder.append("<li>you did run <code>bundle install</code> successfully (if you use Bundler)</li>");
        errorBuilder.append("<li>your RuboCop version isn't ancient</li>");
        errorBuilder.append("</ul>");

        errorBuilder.append("<pre><code>");
        errorBuilder.append(stderr);
        errorBuilder.append("</code></pre>");

        JavaNotifyUtil.notifyError(getProject(), "Failed to parse RuboCop output", errorBuilder.toString());
    }

    private String readStreamToString(BufferedInputStream stream, boolean reset) throws IOException {
        if (reset) {
            try {
                stream.reset();
            } catch (Exception e) {
                logger.debug("Couldn't reset stream - probably because it's empty", e);
                return "";
            }
        }

        return convertStreamToString(stream);
    }

    public static String convertStreamToString(BufferedInputStream stream) throws IOException{
        ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            resultBuffer.write(buffer, 0, length);
        }
        return resultBuffer.toString("UTF-8");
    }
}