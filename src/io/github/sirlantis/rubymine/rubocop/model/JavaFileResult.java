package io.github.sirlantis.rubymine.rubocop.model;

import com.google.gson.stream.JsonReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaFileResult {
    public String path;
    public List<JavaOffense> offenses;
    public JavaFileResult(String path, List<JavaOffense> offenses) {
        this.path = path;
        this.offenses = offenses;

        HashMap<Number, List<JavaOffense>> cache = new HashMap<>();

        for (JavaOffense offense: offenses) {
            int lineNumber = offense.location.line;
            List<JavaOffense> result;
            if (cache.containsKey(lineNumber))
                result = cache.get(lineNumber);
            else
            {
                result = new ArrayList<>();
                cache.put(lineNumber, result);
            }

            result.add(offense);
        }

        offenseCache = cache;
    }

    public static JavaFileResult readFromJsonReader(JsonReader reader) {
        ArrayList<JavaOffense> offenses = new ArrayList<>();
        String path = null;
        try {
            reader.beginObject();

            while (reader.hasNext()) {
                String attrName = reader.nextName();

                if (attrName.equals("offenses")) {
                    reader.beginArray();

                    while (reader.hasNext()) {
                        offenses.add(JavaOffense.readFromJsonReader(reader));
                    }

                    reader.endArray();
                } else if (attrName.equals("path")) {
                    path = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();
            return new JavaFileResult(path, offenses);
        } catch(Exception e) {
            return null;
        }
    }

    public HashMap<Number, List<JavaOffense>> offenseCache = new HashMap<>();

    public List<JavaOffense> getOffensesAt(int lineNumber) {
        List<JavaOffense> result = offenseCache.get(lineNumber);

        if (result != null)
            return result;
        else
            return new ArrayList<>();
    }
}
