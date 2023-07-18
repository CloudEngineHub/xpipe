package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.DataStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A store that refers to another store in the XPipe storage.
 * The referenced store has to be resolved by the caller manually, as this class does not act as a resolver.
 */
@JsonTypeName("named")
@SuperBuilder
@Jacksonized
public final class NamedStore implements DataStore {

    @Getter
    private final String name;

    @Override
    public void validate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <DS extends DataStore> DS asNeeded() {
        throw new UnsupportedOperationException();
    }

}
