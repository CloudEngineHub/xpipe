package io.xpipe.ext.csv;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.NamedCharacter;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvSourceProvider implements SimpleFileDataSourceProvider<CsvSource> {

    @Override
    public boolean supportsConversion(CsvSource in, DataSourceType t) {
        return t == DataSourceType.TEXT || SimpleFileDataSourceProvider.super.supportsConversion(in, t);
    }

    @Override
    public DataSource<?> convert(CsvSource in, DataSourceType t) throws Exception {
        return t == DataSourceType.TEXT
                ? TextSource.builder()
                        .store(in.getStore())
                        .charset(in.getCharset())
                        .newLine(in.getNewLine())
                        .build()
                : SimpleFileDataSourceProvider.super.convert(in, t);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("csv"));
    }

    @Override
    public Region configGui(Property<CsvSource> source, boolean preferQuiet) {
        var s = source.getValue();
        var charset = new SimpleObjectProperty<>(s.getCharset());
        var newLine = new SimpleObjectProperty<NewLine>(s.getNewLine());

        var headerState = new SimpleObjectProperty<CsvHeaderState>(s.getHeaderState());
        var headerStateNames = new LinkedHashMap<CsvHeaderState, ObservableValue<String>>();
        headerStateNames.put(CsvHeaderState.INCLUDED, I18n.observable("csv.included"));
        headerStateNames.put(CsvHeaderState.OMITTED, I18n.observable("csv.omitted"));

        var delimiter = new SimpleObjectProperty<Character>(s.getDelimiter());
        var delimiterNames = new LinkedHashMap<Character, ObservableValue<String>>();
        CsvDelimiter.ALL.forEach(d -> {
            delimiterNames.put(
                    d.getNamedCharacter().getCharacter(),
                    I18n.observable(d.getNamedCharacter().getTranslationKey()));
        });
        ObservableValue<String> delimiterCustom = I18n.observable("csv.custom");

        var quote = new SimpleObjectProperty<Character>(s.getQuote());
        var quoteNames = new LinkedHashMap<Character, ObservableValue<String>>();
        CsvQuoteChar.CHARS.forEach(d -> {
            quoteNames.put(d.getCharacter(), I18n.observable(d.getTranslationKey()));
        });

        return new DynamicOptionsBuilder()
                .addCharset(charset)
                .addNewLine(newLine)
                .addToggle(headerState, I18n.observable("csv.header"), headerStateNames)
                .addCharacter(delimiter, I18n.observable("csv.delimiter"), delimiterNames, delimiterCustom)
                .addCharacter(quote, I18n.observable("csv.quote"), quoteNames)
                .bind(
                        () -> {
                            return CsvSource.builder()
                                    .store(s.getStore())
                                    .charset(charset.get())
                                    .newLine(newLine.getValue())
                                    .delimiter(delimiter.get())
                                    .quote(quote.get())
                                    .headerState(headerState.get())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("csv", ".csv");
    }

    public Dialog configDialog(CsvSource source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        var headerQ = Dialog.skipIf(
                Dialog.choice(
                        "Header",
                        (CsvHeaderState h) -> h == CsvHeaderState.INCLUDED ? "Included" : "Omitted",
                        true,
                        false,
                        source.getHeaderState(),
                        CsvHeaderState.values()),
                () -> source.getHeaderState() != null && !all);
        var quoteQ = DialogHelper.query(
                "Quote", source.getQuote(), false, NamedCharacter.converter(CsvQuoteChar.CHARS, false), all);
        var delimiterQ = DialogHelper.query(
                "Delimiter",
                source.getDelimiter(),
                false,
                NamedCharacter.converter(
                        CsvDelimiter.ALL.stream()
                                .map(CsvDelimiter::getNamedCharacter)
                                .toList(),
                        true),
                all);
        return Dialog.chain(cs, nl, headerQ, quoteQ, delimiterQ).evaluateTo(() -> CsvSource.builder()
                .store(source.getStore())
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .delimiter(delimiterQ.getResult())
                .quote(quoteQ.getResult())
                .headerState(headerQ.getResult())
                .build());
    }

    @Override
    public CsvSource createDefaultSource(DataStore input) throws Exception {
        var stream = (StreamDataStore) input;
        return CsvDetector.detect(stream, 100);
    }

    @Override
    public Class<CsvSource> getSourceClass() {
        return CsvSource.class;
    }
}