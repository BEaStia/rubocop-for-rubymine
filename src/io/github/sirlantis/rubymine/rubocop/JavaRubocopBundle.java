package io.github.sirlantis.rubymine.rubocop;

import com.intellij.CommonBundle;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaRubocopBundle {
    public static String BUNDLE = "io.github.sirlantis.rubymine.rubocop.RubocopBundle";
    public static String LOG_ID = "io.github.sirlantis.rubymine.rubocop";

    public static String message(String key, String... params) {
        return CommonBundle.message(instance, key, params);
    }

    private static Reference<ResourceBundle> ourBundle = null;

    private static ResourceBundle instance;

    public static ResourceBundle getInstance() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);

        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            ourBundle = new SoftReference<ResourceBundle>(bundle);
        }

        return instance;
    }
}

