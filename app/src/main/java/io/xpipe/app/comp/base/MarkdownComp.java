package io.xpipe.app.comp.base;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.xpipe.app.core.AppResources;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

public class MarkdownComp extends Comp<CompStructure<StackPane>> {

    private final String markdown;
    private final UnaryOperator<String> transformation;

    public MarkdownComp(String markdown, UnaryOperator<String> transformation) {
        this.markdown = markdown;
        this.transformation = transformation;
    }

    private String getHtml() {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Document document = parser.parse(markdown);
        var html = renderer.render(document);
        var result = transformation.apply(html);
        return "<article class=\"markdown-body\">" + result + "</article>";
    }

    @SneakyThrows
    private WebView createWebView() {
        var wv = new WebView();
        wv.setPageFill(Color.valueOf("#EEE"));
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, "web/github-markdown.css")
                .orElseThrow();
        wv.getEngine().setUserStyleSheetLocation(url.toString());

        // Work around for https://bugs.openjdk.org/browse/JDK-8199014
        try {
            var file = Files.createTempFile(null, ".html");
            Files.writeString(file, getHtml());
            var contentUrl = file.toUri();
            wv.getEngine().load(contentUrl.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
                        try {
                            Desktop.getDesktop().browse(new URL(toBeopen).toURI());
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).omit().handle();
                        }
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
