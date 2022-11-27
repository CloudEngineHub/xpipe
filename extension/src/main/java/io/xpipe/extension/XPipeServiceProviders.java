package io.xpipe.extension;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.xpipe.beacon.NamedFunction;
import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.prefs.PrefsProviders;

public class XPipeServiceProviders {

    public static void load(ModuleLayer layer) {
        LocalProcessControlProvider.init(layer);

        TrackEvent.info("Loading extension providers ...");
        DataSourceProviders.init(layer);
        for (DataSourceProvider<?> p : DataSourceProviders.getAll()) {
            TrackEvent.trace("Loaded data source provider " + p.getId());
            JacksonMapper.configure(objectMapper -> {
                objectMapper.registerSubtypes(new NamedType(p.getSourceClass()));
            });
        }

        DataStoreProviders.init(layer);
        for (DataStoreProvider p : DataStoreProviders.getAll()) {
            TrackEvent.trace("Loaded data store provider " + p.getId());
            JacksonMapper.configure(objectMapper -> {
                for (Class<?> storeClass : p.getStoreClasses()) {
                    objectMapper.registerSubtypes(new NamedType(storeClass));
                }
            });
        }

        DataStoreActionProvider.init(layer);
        DataSourceActionProvider.init(layer);

        SupportedApplicationProviders.loadAll(layer);
        PrefsProviders.init(layer);
        NamedFunction.init(layer);
        TrackEvent.info("Finished loading extension providers");
    }
}
