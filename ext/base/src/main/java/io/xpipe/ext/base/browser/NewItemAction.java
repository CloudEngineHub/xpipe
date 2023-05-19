package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.fxcomps.Comp;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-plus-box-outline");
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "New";
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.size() == 1 && entries.get(0).getRawFileEntry().getPath().equals(model.getCurrentPath().get());
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public List<LeafAction> getBranchingActions() {
        return List.of(
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return "File";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var name = new SimpleStringProperty();
                        model.getOverlay().setValue(new ModalOverlayComp.OverlayContent("newFile", Comp.of(() -> {
                            var creationName = new TextField();
                            creationName.textProperty().bindBidirectional(name);
                            return creationName;
                        }), "finish", () -> {
                            model.createFileAsync(name.getValue());
                        }));
                    }

                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultFileIcon().createRegion();
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return "Directory";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var name = new SimpleStringProperty();
                        model.getOverlay().setValue(new ModalOverlayComp.OverlayContent("newDirectory", Comp.of(() -> {
                            var creationName = new TextField();
                            creationName.textProperty().bindBidirectional(name);
                            return creationName;
                        }), "finish", () -> {
                            model.createDirectoryAsync(name.getValue());
                        }));
                    }
                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultDirectoryIcon().createRegion();
                    }
                });
    }
}
