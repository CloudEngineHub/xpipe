package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Node;
import javafx.scene.text.Font;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class AppFont {

    public static double em(double emSize) {
        return getPixelSize() * emSize;
    }

    public static double getPixelSize() {
        return getPixelSize(0);
    }

    public static double getPixelSize(int off) {
        var baseSize = OsType.getLocal() == OsType.LINUX ? 11 : 12;
        return new Size(baseSize + off, SizeUnits.PT).pixels();
    }

    public static void header(Node node) {
        setSize(node, +1);
    }

    public static void normal(Node node) {
        setSize(node, 0);
    }

    public static void medium(Node node) {
        setSize(node, -1);
    }

    public static void small(Node node) {
        setSize(node, -2);
    }

    public static void verySmall(Node node) {
        setSize(node, -3);
    }

    public static void setSize(Node node, int off) {
        if (node.getStyle().contains("-fx-font-size: ")) {
            return;
        }

        // Somehow the font is bigger on Linux
        var baseSize = OsType.getLocal() == OsType.LINUX ? 11 : 12;
        node.setStyle(node.getStyle() + "-fx-font-size: " + (baseSize + off) + "pt;");
    }

    public static void loadFonts() {
        TrackEvent.info("Loading fonts ...");
        AppResources.with(
                AppResources.XPIPE_MODULE,
                "fonts",
                path -> Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try (var in = Files.newInputStream(file)) {
                            Font.loadFont(in, 12);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }));
    }
}
