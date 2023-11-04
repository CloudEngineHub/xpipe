package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.util.ResourceBundleService;
import io.xpipe.app.core.AppI18n;
import lombok.NonNull;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class QuietResourceBundleService extends ResourceBundleService {

    public QuietResourceBundleService() {
        super(new ResourceBundle() {
            @Override
            protected Object handleGetObject(@NonNull String key) {
                return null;
            }

            @Override
            public @NonNull Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        });
    }

    @Override
    public String translate(String key) {
        return AppI18n.get(key);
    }
}
