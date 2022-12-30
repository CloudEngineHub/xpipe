package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents a reference to an XPipe data source.
 * This reference consists out of a collection name and an entry name to allow for better organisation.
 * <p>
 * To allow for a simple usage of data source ids, the collection and entry names are trimmed and
 * converted to lower case names when creating them.
 * The two names are separated by a colon and are therefore not allowed to contain colons themselves.
 * <p>
 * A missing collection name indicates that the data source exists only temporarily.
 *
 * @see #create(String, String)
 * @see #fromString(String)
 */
@EqualsAndHashCode
@Getter
public class DataSourceId {

    public static final char SEPARATOR = ':';

    private final String collectionName;
    private final String entryName;

    @JsonCreator
    private DataSourceId(String collectionName, String entryName) {
        this.collectionName = collectionName;
        this.entryName = entryName;
    }

    public static String cleanString(String input) {
        var replaced = input.replaceAll(":", "");
        return replaced.length() == 0 ? "-" : replaced;
    }

    /**
     * Creates a new data source id from a collection name and an entry name.
     *
     * @param collectionName the collection name, which may be null but not an empty string
     * @param entryName      the entry name, which must be not null and not empty
     * @throws IllegalArgumentException if any name is not valid
     */
    public static DataSourceId create(String collectionName, String entryName) {
        if (collectionName != null && collectionName.trim().length() == 0) {
            throw new IllegalArgumentException("Trimmed collection name is empty");
        }
        if (collectionName != null && collectionName.contains("" + SEPARATOR)) {
            throw new IllegalArgumentException(
                    "Separator character " + SEPARATOR + " is not allowed in the collection name");
        }

        if (entryName == null) {
            throw new IllegalArgumentException("Collection name is null");
        }
        if (entryName.trim().length() == 0) {
            throw new IllegalArgumentException("Trimmed entry name is empty");
        }
        if (entryName.contains("" + SEPARATOR)) {
            throw new IllegalArgumentException(
                    "Separator character " + SEPARATOR + " is not allowed in the entry name");
        }

        return new DataSourceId(collectionName, entryName);
    }

    /**
     * Creates a new data source id from a string representation.
     * The string must contain exactly one colon and non-empty names.
     *
     * @param s the string representation, must be not null and fulfill certain requirements
     * @throws IllegalArgumentException if the string is not valid
     */
    public static DataSourceId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String is null");
        }

        var split = s.split(String.valueOf(SEPARATOR));
        if (split.length != 2) {
            throw new IllegalArgumentException("Data source id must contain exactly one " + SEPARATOR);
        }

        var cn = split[0].trim().toLowerCase();
        var en = split[1].trim().toLowerCase();
        cn = cn.isEmpty() ? null : cn;
        if (en.length() == 0) {
            throw new IllegalArgumentException("Entry name must not be empty");
        }

        return new DataSourceId(cn, en);
    }

    @Override
    public String toString() {
        return (collectionName != null ? collectionName.toLowerCase() : "") + SEPARATOR + entryName;
    }
}
