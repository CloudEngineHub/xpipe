package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconCache;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.icon.SystemIconSource;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class IconsCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "icons";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("customIcons")
                .sub(new OptionsBuilder()
                        .nameAndDescription("iconSources")
                        .addComp(createOverview())
                )
                .buildComp();
    }

    private Comp<?> createOverview() {
        var sources = FXCollections.<SystemIconSource>observableArrayList();
        AppPrefs.get().getIconSources().subscribe((newValue) -> {
            sources.setAll(newValue);
        });
        var box = new ListBoxViewComp<>(sources, sources, s -> createSourceEntry(s), true);

        var busy = new SimpleBooleanProperty(false);
        var refreshButton = new TileButtonComp("refreshSources", "refreshSourcesDescription", "mdi2r-refresh", e -> {
            ThreadHelper.runFailableAsync(() -> {
                try (var ignored = new BooleanScope(busy).start()) {
                    SystemIconManager.reload();
                }
            });
            e.consume();
        });
        refreshButton.disable(PlatformThread.sync(busy.or(Bindings.isEmpty(sources))));
        refreshButton.grow(true, false);

        var addGitButton = new TileButtonComp("addGitIconSource", "addGitIconSourceDescription", "mdi2a-access-point-plus", e -> {
            var remote = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "repositoryUrl",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(remote);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (remote.get() == null || remote.get().isBlank()) {
                    return;
                }

                var source = SystemIconSource.GitRepository.builder().remote(remote.get()).id(UUID.randomUUID().toString()).build();
                sources.add(source);
            });
            modal.show();
            e.consume();
        });
        addGitButton.grow(true, false);

        var addDirectoryButton = new TileButtonComp("addDirectoryIconSource", "addDirectoryIconSourceDescription", "mdi2f-folder-plus", e -> {
            var dir = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "iconDirectory",
                    new ContextualFileReferenceChoiceComp(new SimpleObjectProperty<>(DataStorage.get().local().ref()),dir,null,List.of()).prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (dir.get() == null || dir.get().isBlank()) {
                    return;
                }

                var source = SystemIconSource.Directory.builder().path(Path.of(dir.get())).id(UUID.randomUUID().toString()).build();
                sources.add(source);
            });
            modal.show();
            e.consume();
        });
        addDirectoryButton.grow(true, false);

        var vbox = new VerticalComp(List.of(Comp.vspacer(10), box, Comp.separator(), refreshButton,  Comp.separator(), addDirectoryButton, addGitButton));
        vbox.spacing(10);
        return vbox;
    }

    private Comp<?> createSourceEntry(SystemIconSource source) {
        var delete = new IconButtonComp(new LabelGraphic.IconGraphic("mdal-delete_outline"), () -> {
            if (!AppDialog.confirm("iconSourceDeletion")) {
                return;
            }

        });
        var buttons = new HorizontalComp(List.of(delete));
        buttons.spacing(5);
        var tile = new TileButtonComp(
                new SimpleStringProperty(source.getId()),
                new SimpleStringProperty(source.getDescription()),
                new SimpleObjectProperty<>(source.getIcon()),
                actionEvent -> {
                    ThreadHelper.runFailableAsync(() -> {
                        source.open();
                    });
                });
        tile.setRight(buttons);
        tile.setIconSize(1.0);
        tile.grow(true, false);
        tile.apply(struc -> AppFont.medium(struc.get()));
        return tile;
    }
}
