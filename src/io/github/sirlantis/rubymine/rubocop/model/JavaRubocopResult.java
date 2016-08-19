package io.github.sirlantis.rubymine.rubocop.model;

import com.google.gson.stream.JsonReader;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.sirlantis.rubymine.rubocop.JavaRubocopTask;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaRubocopResult extends ArrayList<JavaFileResult> {

    public List<JavaFileResult> fileResults;
    public List<String> warnings;

    public JavaRubocopResult(List<JavaFileResult> fileResults, List<String> warnings) {
        this.fileResults = fileResults;
        this.warnings = warnings;
    }

    public static JavaRubocopResult readFromFile(VirtualFile file) {
        try {
            return readFromReader(new FileReader(file.getPath()), null);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static JavaRubocopResult readFromReader(InputStreamReader reader, InputStreamReader stderrReader) {
        return readFromJsonReader(new JsonReader(reader), stderrReader);
    }

    public static JavaRubocopResult readFromJsonReader(JsonReader reader, InputStreamReader stderrReader) {
        // var timestamp = null
        LinkedList<JavaFileResult> fileResults = new LinkedList<JavaFileResult>();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String attrName = reader.nextName();

                if (attrName.equals("files")) {
                    reader.beginArray();

                    while (reader.hasNext()) {
                        fileResults.add(JavaFileResult.readFromJsonReader(reader));
                    }

                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();

            return new JavaRubocopResult(fileResults, extractWarnings(stderrReader));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }

    private static List<String> extractWarnings(InputStreamReader stderrReader){
        if (stderrReader == null) {
            return new ArrayList<>();
        }


        Pattern pattern = Pattern.compile("Warning: (unrecognized cop .*)$", Pattern.MULTILINE);
        //Matcher matcher = pattern.matcher(JavaRubocopTask.convertStreamToString(stderrReader.);
        LinkedList<String> list = new LinkedList<>();

        /*while (matcher.find()) {
            list.add(matcher.group(1));
        }*/

        return list;
    }
}
