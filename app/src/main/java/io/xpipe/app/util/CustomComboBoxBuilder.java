package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class CustomComboBoxBuilder<T> {

    private final Property<T> selected;
    private final Function<T, Node> nodeFunction;
    private ObservableValue<String> emptyAccessibilityName = AppI18n.observable("none");
    private Function<T, String> accessibleNameFunction;
    private Function<T, Node> selectedDisplayNodeFunction;
    private final Map<Node, T> nodeMap = new HashMap<>();
    private final Map<Node, Runnable> actionsMap = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();
    private final Set<Node> disabledNodes = new HashSet<>();
    private final Node emptyNode;
    private final Predicate<T> veto;
    private final Property<String> filterString = new SimpleStringProperty();
    private final List<T> filterable = new ArrayList<>();
    private BiPredicate<T, String> filterPredicate;
    private Node filterNode;
    private Function<T, Node> unknownNode;

    public CustomComboBoxBuilder(
            Property<T> selected, Function<T, Node> nodeFunction, Node emptyNode, Predicate<T> veto) {
        this.selected = selected;
        this.nodeFunction = nodeFunction;
        this.selectedDisplayNodeFunction = nodeFunction;
        this.emptyNode = emptyNode;
        this.veto = veto;
    }

    public void setSelectedDisplay(Function<T, Node> nodeFunction) {
        selectedDisplayNodeFunction = nodeFunction;
    }

    public void setAccessibleNames(Function<T, String> function) {
        accessibleNameFunction = function;
    }

    public void setEmptyAccessibilityName(ObservableValue<String> n) {
        emptyAccessibilityName = n;
    }

    public void addAction(Node node, Runnable run) {
        nodes.add(node);
        actionsMap.put(node, run);
    }

    public void disable(Node node) {
        disabledNodes.add(node);
    }

    public void setUnknownNode(Function<T, Node> node) {
        unknownNode = node;
    }

    public Node add(T val) {
        var node = nodeFunction.apply(val);
        nodeMap.put(node, val);
        nodes.add(node);
        if (filterPredicate != null) {
            filterable.add(val);
        }
        return node;
    }

    public void addSeparator() {
        var sep = new Separator(Orientation.HORIZONTAL);
        nodes.add(sep);
        disabledNodes.add(sep);
    }

    public void addHeader(String name) {
        var spacer = new Region();
        spacer.setPrefHeight(10);
        var header = new Label(name);
        header.setAlignment(Pos.CENTER);
        var v = new VBox(spacer, header, new Separator(Orientation.HORIZONTAL));
        v.setAccessibleText(name);
        v.setAlignment(Pos.CENTER);
        nodes.add(v);
        disabledNodes.add(v);
    }

    public void addFilter(BiPredicate<T, String> filterPredicate) {
        this.filterPredicate = filterPredicate;

        var spacer = new Region();
        spacer.setPrefHeight(10);
        var header = new FilterComp(filterString).createStructure();
        var v = new VBox(header.get());
        v.setAlignment(Pos.CENTER);
        nodes.add(v);
        filterNode = header.getText();
    }

    public ComboBox<Node> build() {
        var cb = new ComboBox<Node>();
        cb.getItems().addAll(nodes);

        cb.setCellFactory((lv) -> {
            return new Cell();
        });
        cb.setButtonCell(new SelectedCell());
        SimpleChangeListener.apply(selected, c -> {
            var item = nodeMap.entrySet().stream()
                    .filter(e -> Objects.equals(c, e.getValue()))
                    .map(e -> e.getKey())
                    .findAny()
                    .orElse(c == null || unknownNode == null ? emptyNode : unknownNode.apply(c));
            cb.setValue(item);
        });
        cb.valueProperty().addListener((c, o, n) -> {
            if (nodeMap.containsKey(n)) {
                if (veto != null && !veto.test(nodeMap.get(n))) {
                    return;
                }
                selected.setValue(nodeMap.get(n));
            }

            if (actionsMap.containsKey(n)) {
                cb.setValue(o);
                actionsMap.get(n).run();
            }
        });

        if (filterPredicate != null) {
            SimpleChangeListener.apply(filterString, c -> {
                var filteredNodes = nodes.stream()
                        .filter(e -> e.equals(cb.getValue())
                                || !(nodeMap.get(e) != null
                                        && (filterable.contains(nodeMap.get(e))
                                                && filterString.getValue() != null
                                                && !filterPredicate.test(nodeMap.get(e), c))))
                        .toList();
                cb.setItems(FXCollections.observableList(filteredNodes));
            });

            filterNode.sceneProperty().addListener((c, o, n) -> {
                if (n != null) {
                    n.getWindow().focusedProperty().addListener((c2, o2, n2) -> {
                        Platform.runLater(() -> {
                            filterNode.requestFocus();
                        });
                    });
                }
                Platform.runLater(() -> {
                    filterNode.requestFocus();
                });
            });
        }

        if (emptyNode != null) {
            emptyNode.setAccessibleText(emptyAccessibilityName.getValue());
        }
        if (accessibleNameFunction != null) {
            nodes.forEach(node -> node.setAccessibleText(accessibleNameFunction.apply(nodeMap.get(node))));
        }

        return cb;
    }

    private class SelectedCell extends ListCell<Node> {

        @Override
        protected void updateItem(Node item, boolean empty) {
            super.updateItem(item, empty);

            accessibleTextProperty().unbind();
            if (empty || item.equals(emptyNode)) {
                if (emptyAccessibilityName != null) {
                    accessibleTextProperty().bind(emptyAccessibilityName);
                } else {
                    setAccessibleText(null);
                }
            }

            if (empty) {
                return;
            }

            if (item.equals(emptyNode)) {
                setGraphic(item);
                return;
            }

            // Case for dynamically created unknown nodes
            if (!nodeMap.containsKey(item)) {
                setGraphic(item);
                // Don't expect the accessible name function to properly map this item
                setAccessibleText(null);
                return;
            }

            var val = nodeMap.get(item);
            var newNode = selectedDisplayNodeFunction.apply(val);
            setGraphic(newNode);
            setAccessibleText(newNode.getAccessibleText());
        }
    }

    private class Cell extends ListCell<Node> {

        public Cell() {
            addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!nodeMap.containsKey(getItem())) {
                    event.consume();
                }
            });
            addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER && !nodeMap.containsKey(getItem())) {
                    event.consume();
                }
            });
        }

        @Override
        protected void updateItem(Node item, boolean empty) {
            setGraphic(item);
            if (getItem() == item) {
                return;
            }

            super.updateItem(item, empty);
            if (item == null) {
                return;
            }

            setGraphic(item);
            if (disabledNodes.contains(item)) {
                this.setDisable(true);
                this.setFocusTraversable(false);
                //                 this.setPadding(Insets.EMPTY);
            } else {
                this.setDisable(false);
                this.setFocusTraversable(true);
                setAccessibleText(item.getAccessibleText());
            }
        }
    }
}
