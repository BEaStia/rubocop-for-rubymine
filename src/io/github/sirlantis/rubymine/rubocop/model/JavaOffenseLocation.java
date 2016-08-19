package io.github.sirlantis.rubymine.rubocop.model;

import com.google.gson.stream.JsonReader;

import java.io.IOException;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaOffenseLocation {
    public int line, column, length;

    public JavaOffenseLocation(int line, int column, int length) {
        this.line = line;
        this.column = column;
        this.length = length;
    }

    public static JavaOffenseLocation zero(){
        return new JavaOffenseLocation(0, 0, 0);
    }

    public static JavaOffenseLocation readFromJsonReader(JsonReader reader){
        try {
            reader.beginObject();

        int line = 0;
        int column = 0;
        int length = 0;

        while (reader.hasNext()) {
            String attrName = reader.nextName();

            switch (attrName) {
                case "line":
                    line = reader.nextInt();
                    break;
                case "column":
                    column = reader.nextInt();
                    break;
                case "length":
                    length = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return new JavaOffenseLocation(line, column, length);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
