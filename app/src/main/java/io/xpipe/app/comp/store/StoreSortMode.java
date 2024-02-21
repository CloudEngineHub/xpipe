package io.xpipe.app.comp.store;

import io.xpipe.app.storage.DataStoreEntry;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public interface StoreSortMode {

    StoreSortMode ALPHABETICAL_DESC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            return s;
        }

        @Override
        public String getId() {
            return "alphabetical-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparing(
                    e -> e.getWrapper().nameProperty().getValue().toLowerCase(Locale.ROOT));
        }
    };
    StoreSortMode ALPHABETICAL_ASC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            return s;
        }

        @Override
        public String getId() {
            return "alphabetical-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, String>comparing(
                            e -> e.getWrapper().nameProperty().getValue().toLowerCase(Locale.ROOT))
                    .reversed();
        }
    };
    StoreSortMode DATE_DESC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            var c = comparator();
            return Stream.of(
                            s.getShownChildren().stream()
                                    .max((o1, o2) -> {
                                        return c.compare(representative(o1), representative(o2));
                                    })
                                    .orElse(s),
                            s)
                    .max(c)
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparing(e -> {
                return flatten(e)
                        .map(entry -> entry.getLastAccess())
                        .max(Comparator.naturalOrder())
                        .orElseThrow();
            });
        }
    };
    StoreSortMode DATE_ASC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            var c = comparator();
            return Stream.of(
                            s.getShownChildren().stream()
                                    .min((o1, o2) -> {
                                        return c.compare(representative(o1), representative(o2));
                                    })
                                    .orElse(s),
                            s)
                    .min(c)
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, Instant>comparing(e -> {
                        return flatten(e)
                                .map(entry -> entry.getLastAccess())
                                .max(Comparator.naturalOrder())
                                .orElseThrow();
                    })
                    .reversed();
        }
    };
    List<StoreSortMode> ALL = List.of(ALPHABETICAL_DESC, ALPHABETICAL_ASC, DATE_DESC, DATE_ASC);

    static Stream<DataStoreEntry> flatten(StoreSection section) {
        return Stream.concat(
                Stream.of(section.getWrapper().getEntry()),
                section.getAllChildren().stream().flatMap(section1 -> flatten(section1)));
    }

    static Optional<StoreSortMode> fromId(String id) {
        return ALL.stream()
                .filter(storeSortMode -> storeSortMode.getId().equals(id))
                .findFirst();
    }

    StoreSection representative(StoreSection s);

    String getId();

    Comparator<StoreSection> comparator();
}
