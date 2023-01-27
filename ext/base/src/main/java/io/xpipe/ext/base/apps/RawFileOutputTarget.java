package io.xpipe.ext.base.apps;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class RawFileOutputTarget implements DataSourceTarget {

    @Override
    public String getId() {
        return "rawFileOutput";
    }

    @Override
    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var target = new SimpleObjectProperty<StreamDataStore>();

        var storeChoice = XPipeDaemon.getInstance()
                .namedStoreChooser(
                        new SimpleObjectProperty<>(store -> store instanceof StreamDataStore
                                && (store.getFlow().hasOutput())),
                        target,
                        DataStoreProvider.Category.STREAM);
        storeChoice
                .apply(GrowAugment.create(true, true))
                .apply(struc -> GridPane.setVgrow(struc.get(), Priority.ALWAYS));
        var layout = new DynamicOptionsBuilder(false)
                .addTitle("destination")
                .addComp(storeChoice, target)
                .build();

        return new InstructionsDisplay(
                layout,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StreamDataStore inputStore = (StreamDataStore) source.getStore();
                            try (var in = inputStore.openInput()) {
                                try (var out = target.get().openOutput()) {
                                    in.transferTo(out);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                storeChoice.getValidator());
    }

    @Override
    public boolean isApplicable(DataSource<?> source) {
        return source.getStore() instanceof StreamDataStore;
    }

    @Override
    public ObservableValue<String> getName() {
        return I18n.observable("base.rawFileOutput");
    }

    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.ACTIVE;
    }

    @Override
    public String getSetupGuideURL() {
        return null;
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2s-subtitles-outline";
    }
}
