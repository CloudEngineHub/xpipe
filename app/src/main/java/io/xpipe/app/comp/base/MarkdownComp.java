package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.MarkdownHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

public class MarkdownComp extends Comp<CompStructure<StackPane>> {

    private final ObservableValue<String> markdown;
    private final UnaryOperator<String> htmlTransformation;

    public MarkdownComp(String markdown, UnaryOperator<String> htmlTransformation) {
        this.markdown = new SimpleStringProperty(markdown);
        this.htmlTransformation = htmlTransformation;
    }

    public MarkdownComp(ObservableValue<String> markdown, UnaryOperator<String> htmlTransformation) {
        this.markdown = markdown;
        this.htmlTransformation = htmlTransformation;
    }

    private String getHtml() {
        return MarkdownHelper.toHtml(markdown.getValue(), s -> s, htmlTransformation);
    }

    @SneakyThrows
    private WebView createWebView() {
        var wv = new WebView();
        wv.getEngine().setJavaScriptEnabled(false);
        wv.setContextMenuEnabled(false);
        wv.getEngine()
                .setUserDataDirectory(
                        AppProperties.get().getDataDir().resolve("webview").toFile());
        wv.setPageFill(Color.TRANSPARENT);
        var theme = AppPrefs.get() != null && AppPrefs.get().theme.getValue().isDark()
                ? "misc/github-markdown-dark.css"
                : "misc/github-markdown-light.css";
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, theme).orElseThrow();
        wv.getEngine().setUserStyleSheetLocation(url.toString());

        PlatformThread.sync(markdown).subscribe(val -> {
            // Workaround for https://bugs.openjdk.org/browse/JDK-8199014
            try {
                var file = Files.createTempFile(null, ".html");
                Files.writeString(file, getHtml());
                var contentUrl = file.toUri();
                wv.getEngine().load(contentUrl.toString());
            } catch (IOException e) {
                // Any possible IO errors can occur here
                ErrorEvent.fromThrowable(e).expected().handle();
            }
        });

        wv.getStyleClass().add("markdown-comp");
        addLinkHandler(wv.getEngine());
        return wv;
    }

    private void addLinkHandler(WebEngine engine) {
        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
                    String toBeopen = engine.getLoadWorker().getMessage().trim().replace("Loading ", "");
                    if (toBeopen.contains("http://") || toBeopen.contains("https://")) {
                        engine.getLoadWorker().cancel();
                        Hyperlinks.open(toBeopen);
                    }
                }));
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var sp = new StackPane(createWebView());
        sp.setPadding(Insets.EMPTY);
        return new SimpleCompStructure<>(sp);
    }
}
