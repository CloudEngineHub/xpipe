package io.xpipe.ext.jdbc.mysql;

import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcGuiHelper;
import io.xpipe.ext.jdbc.JdbcStoreProvider;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.ModuleInstall;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.ChoicePaneComp;
import io.xpipe.extension.fxcomps.impl.TabPaneComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public class MysqlStoreProvider extends JdbcStoreProvider {

    public static final String PROTOCOL = "mysql";
    public static final int DEFAULT_PORT = 3306;
    public static final String DEFAULT_USERNAME = "admin";

    public MysqlStoreProvider() {
        super("com.mysql.cj.jdbc.Driver");
    }

    @Override
    public boolean init() throws Exception {
        super.init();
        return false;
    }

    @Override
    public ModuleInstall getRequiredAdditionalInstallation() {
        return new MysqlInstall();
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var wizVal = new SimpleValidator();
        var wizValue = new SimpleObjectProperty<DataStore>(
                store.getValue() instanceof MysqlSimpleStore ? store.getValue() : null);
        var wizard = new TabPaneComp.Entry(I18n.observable("jdbc.connectionWizard"), null, wizard(wizValue, wizVal));

        var urlVal = new SimpleValidator();
        var urlValue = new SimpleObjectProperty<>(store.getValue() instanceof MysqlUrlStore ? store.getValue() : null);
        var url = new TabPaneComp.Entry(I18n.observable("jdbc.connectionUrl"), null, url(urlValue, urlVal));

        var stringVal = new SimpleValidator();
        var stringValue = new SimpleObjectProperty<>(store.getValue());
        var string =
                new TabPaneComp.Entry(I18n.observable("jdbc.connectionString"), null, string(stringValue, stringVal));

        var selected = new SimpleObjectProperty<>(store.getValue() instanceof MysqlUrlStore ? url : wizard);

        var map = Map.of(
                wizard, wizVal,
                url, urlVal,
                string, stringVal);
        var orVal = new ExclusiveValidator<>(map, selected);

        var propMap = Map.of(
                wizard, wizValue,
                url, urlValue,
                string, stringValue);
        PropertiesHelper.bindExclusive(selected, propMap, store);

        var pane = new TabPaneComp(selected, List.of(wizard, url));
        return new GuiDialog(pane, orVal);
    }

    private Comp<?> string(Property<DataStore> store, Validator val) {
        return Comp.of(() -> new Region());
    }

    private Comp<?> url(Property<DataStore> store, Validator val) {
        return JdbcGuiHelper.url(PROTOCOL, MysqlUrlStore.class, store, val);
    }

    private Comp<?> wizard(Property<DataStore> store, Validator val) {
        MysqlSimpleStore st = (MysqlSimpleStore) store.getValue();
        Property<JdbcBasicAddress> addrProp =
                new SimpleObjectProperty<>(st != null ? (JdbcBasicAddress) st.getAddress() : null);

        var host = new SimpleStringProperty(
                addrProp.getValue() != null ? addrProp.getValue().getHostname() : null);
        var port = new SimpleObjectProperty<>(
                addrProp.getValue() != null ? addrProp.getValue().getPort() : null);
        var addrQ = new DynamicOptionsBuilder(I18n.observable("jdbc.connection"))
                .addString(I18n.observable("jdbc.host"), host)
                .nonNull(val)
                .addInteger(I18n.observable("jdbc.port"), port)
                .bind(
                        () -> {
                            return JdbcBasicAddress.builder()
                                    .hostname(host.get())
                                    .port(port.get())
                                    .build();
                        },
                        addrProp)
                .buildComp();

        Property<AuthMethod> authProp = new SimpleObjectProperty<>(st != null ? st.getAuth() : null);
        Property<SimpleAuthMethod> passwordAuthProp = new SimpleObjectProperty<>(
                authProp.getValue() instanceof SimpleAuthMethod ? (SimpleAuthMethod) authProp.getValue() : null);
        var passwordAuthQ = Comp.of(() -> {
            var user = new SimpleStringProperty(
                    passwordAuthProp.getValue() != null
                            ? passwordAuthProp.getValue().getUsername()
                            : DEFAULT_USERNAME);
            var pass = new SimpleObjectProperty<>(
                    passwordAuthProp.getValue() != null
                            ? passwordAuthProp.getValue().getPassword()
                            : null);
            return new DynamicOptionsBuilder(false)
                    .addString(I18n.observable("jdbc.username"), user)
                    .nonNull(val)
                    .addSecret(I18n.observable("jdbc.password"), pass)
                    .nonNull(val)
                    .bind(
                            () -> {
                                return new SimpleAuthMethod(user.get(), pass.get());
                            },
                            passwordAuthProp)
                    .build();
        });

        Comp<?> authChoice;
        var passwordEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.passwordAuth"), passwordAuthQ);
        var windowsEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.windowsAuth"), Comp.of(Region::new));
        var entries = List.of(passwordEntry, windowsEntry);
        var authSelected = new SimpleObjectProperty<ChoicePaneComp.Entry>(
                authProp.getValue() == null || authProp.getValue() instanceof SimpleAuthMethod
                        ? passwordEntry
                        : windowsEntry);
        var check = Validator.nonNull(val, I18n.observable("jdbc.authentication"), authSelected);
        authChoice = new ChoicePaneComp(entries, authSelected).apply(s -> check.decorates(s.get()));
        var authQ = new DynamicOptionsBuilder(I18n.observable("jdbc.authentication"))
                .addComp((ObservableValue<String>) null, authChoice, authSelected)
                .bindChoice(
                        () -> {
                            if (entries.indexOf(authSelected.get()) == 0) {
                                return passwordAuthProp;
                            }
                            if (entries.indexOf(authSelected.get()) == 1) {
                                return new SimpleObjectProperty<AuthMethod>(new WindowsAuth());
                            }
                            return null;
                        },
                        authProp)
                .buildComp();

        store.bind(Bindings.createObjectBinding(
                () -> {
                    return MysqlSimpleStore.builder()
                            .address(addrProp.getValue())
                            .auth(authProp.getValue())
                            .build();
                },
                addrProp,
                authProp));

        return new VerticalComp(List.of(addrQ, authQ));
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("mysql");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MysqlSimpleStore.class, MysqlUrlStore.class);
    }

    @Override
    public String getDisplayIconFileName() {
        return "jdbc:mysql_icon.svg";
    }

    @Override
    public DataStore defaultStore() {
        return MysqlSimpleStore.builder()
                .address(JdbcBasicAddress.builder()
                        .hostname("localhost")
                        .port(DEFAULT_PORT)
                        .build())
                .auth(new SimpleAuthMethod(DEFAULT_USERNAME, null))
                .build();
    }
}
