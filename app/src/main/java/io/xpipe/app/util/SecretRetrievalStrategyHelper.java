package io.xpipe.app.util;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.App;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.List;

public class SecretRetrievalStrategyHelper {

    private static OptionsBuilder inPlace(Property<SecretRetrievalStrategy.InPlace> p) {
        var secretProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getValue() : null);
        return new OptionsBuilder()
                .name("password")
                .addComp(new SecretFieldComp(secretProperty), secretProperty)
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.InPlace(secretProperty.getValue());
                        },
                        p);
    }

    private static OptionsBuilder passwordManager(Property<SecretRetrievalStrategy.PasswordManager> p) {
        var keyProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getKey() : null);
        var content = new HorizontalComp(List.<Comp<?>>of(
                        new TextFieldComp(keyProperty).hgrow(),
                        new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                                    AppPrefs.get().selectCategory(3);
                                    App.getApp().getStage().requestFocus();
                                })
                                .grow(false, true)))
                .apply(struc -> struc.get().setSpacing(10));
        return new OptionsBuilder()
                .name("command")
                .addComp(content, keyProperty)
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.PasswordManager(keyProperty.getValue());
                        },
                        p);
    }

    private static OptionsBuilder customCommand(Property<SecretRetrievalStrategy.CustomCommand> p) {
        var cmdProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getCommand() : null);
        var content = new TextFieldComp(cmdProperty).apply(struc -> struc.get().setPromptText("Password key"));
        return new OptionsBuilder()
                .name("command")
                .addComp(content, cmdProperty)
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.CustomCommand(cmdProperty.getValue());
                        },
                        p);
    }

    public static OptionsBuilder comp(Property<SecretRetrievalStrategy> s) {
        SecretRetrievalStrategy strat = s.getValue();
        var inPlace = new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.InPlace i ? i : null);
        var passwordManager =
                new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.PasswordManager i ? i : null);
        var customCommand =
                new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.CustomCommand i ? i : null);
        var command = new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.CustomCommand c ? c : null);
        var map = new LinkedHashMap<String, OptionsBuilder>();
        map.put("none", new OptionsBuilder());
        map.put("password", inPlace(inPlace));
        map.put("passwordManager", passwordManager(passwordManager));
        map.put("customCommand", customCommand(customCommand));
        map.put("prompt", new OptionsBuilder());
        var selected = new SimpleIntegerProperty(
                strat instanceof SecretRetrievalStrategy.None
                        ? 0
                        : strat instanceof SecretRetrievalStrategy.InPlace
                                ? 1
                                : strat instanceof SecretRetrievalStrategy.PasswordManager
                                        ? 2
                                        : strat instanceof SecretRetrievalStrategy.CustomCommand ? 3 : strat instanceof SecretRetrievalStrategy.Prompt ? 4 : 0);
        return new OptionsBuilder()
                .choice(selected, map)
                .bindChoice(
                        () -> {
                            return switch (selected.get()) {
                                case 0 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.None());
                                case 1 -> inPlace;
                                case 2 -> passwordManager;
                                case 3 -> customCommand;
                                case 4 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.Prompt());
                                default -> new SimpleObjectProperty<>();
                            };
                        },
                        s);
    }
}
