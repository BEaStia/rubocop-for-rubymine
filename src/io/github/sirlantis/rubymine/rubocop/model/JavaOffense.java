package io.github.sirlantis.rubymine.rubocop.model;

import com.google.gson.stream.JsonReader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaOffense {
    public String severity;
    public String cop;
    public String message;
    public JavaOffenseLocation location;

    public JavaOffense(String severity, String cop, String message, JavaOffenseLocation location) {
        this.severity = severity;
        this.cop = cop;
        this.message = message;
        this.location = location;
    }

    @Nullable
    public static JavaOffense readFromJsonReader(JsonReader reader) {
        try {
            reader.beginObject();

            JavaOffenseLocation location = JavaOffenseLocation.zero();
            String message = "(no message)";
            String cop = "Style/UnknownCop";
            String severity = "unknown";

            while (reader.hasNext()) {
                String attrName = reader.nextName();

                switch (attrName) {
                    case "message":
                        message = reader.nextString();
                        break;
                    case "cop_name":
                        cop = reader.nextString();
                        break;
                    case "severity":
                        severity = reader.nextString();
                        break;
                    case "location":
                        location = JavaOffenseLocation.readFromJsonReader(reader);
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            reader.endObject();
            return new JavaOffense(severity, cop, message, location);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
