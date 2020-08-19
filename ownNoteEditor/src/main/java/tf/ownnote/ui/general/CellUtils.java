/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.general;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import tf.helper.general.ObjectsHelper;

/**
 * Copy of CellUtils from openjdk/jfx to make access public...
 * 
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class CellUtils {

    static int TREE_VIEW_HBOX_GRAPHIC_PADDING = 3;

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final static StringConverter<?> defaultStringConverter = new StringConverter<Object>() {
        @Override public String toString(Object t) {
            return t == null ? null : t.toString();
        }

        @Override public Object fromString(String string) {
            return (Object) string;
        }
    };

    private final static StringConverter<?> defaultTreeItemStringConverter =
        new StringConverter<TreeItem<?>>() {
            @Override public String toString(TreeItem<?> treeItem) {
                return (treeItem == null || treeItem.getValue() == null) ?
                        "" : treeItem.getValue().toString();
            }

            @Override public TreeItem<?> fromString(String string) {
                return new TreeItem<>(string);
            }
        };

    /***************************************************************************
     *                                                                         *
     * General convenience                                                     *
     *                                                                         *
     **************************************************************************/

    /*
     * Simple method to provide a StringConverter implementation in various cell
     * implementations.
     */
    public static <T> StringConverter<T> defaultStringConverter() {
        return ObjectsHelper.uncheckedCast(defaultStringConverter);
    }

    /*
     * Simple method to provide a TreeItem-specific StringConverter
     * implementation in various cell implementations.
     */
    public static <T> StringConverter<TreeItem<T>> defaultTreeItemStringConverter() {
        return ObjectsHelper.uncheckedCast(defaultTreeItemStringConverter);
    }

    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
            cell.getItem() == null ? "" : cell.getItem().toString() :
            converter.toString(cell.getItem());
    }


    public static Node getGraphic(TreeItem<?> treeItem) {
        return treeItem == null ? null : treeItem.getGraphic();
    }



    /***************************************************************************
     *                                                                         *
     * ChoiceBox convenience                                                   *
     *                                                                         *
     **************************************************************************/

    public static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final ChoiceBox<T> choiceBox) {
        updateItem(cell, converter, null, null, choiceBox);
    }

    public static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final HBox hbox,
                               final Node graphic,
                               final ChoiceBox<T> choiceBox) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (choiceBox != null) {
                    choiceBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);

                if (graphic != null) {
                    hbox.getChildren().setAll(graphic, choiceBox);
                    cell.setGraphic(hbox);
                } else {
                    cell.setGraphic(choiceBox);
                }
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    };

    public static <T> ChoiceBox<T> createChoiceBox(
            final Cell<T> cell,
            final ObservableList<T> items,
            final ObjectProperty<StringConverter<T>> converter) {
        ChoiceBox<T> choiceBox = new ChoiceBox<T>(items);
        choiceBox.setMaxWidth(Double.MAX_VALUE);
        choiceBox.converterProperty().bind(converter);
        choiceBox.showingProperty().addListener(o -> {
            if (!choiceBox.isShowing()) {
                cell.commitEdit(choiceBox.getSelectionModel().getSelectedItem());
            }
        });
        return choiceBox;
    }



    /***************************************************************************
     *                                                                         *
     * TextField convenience                                                   *
     *                                                                         *
     **************************************************************************/

    public static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final TextField textField) {
        updateItem(cell, converter, null, null, textField);
    }

    public static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final HBox hbox,
                               final Node graphic,
                               final TextField textField) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(cell, converter));
                }
                cell.setText(null);

                if (graphic != null) {
                    hbox.getChildren().setAll(graphic, textField);
                    cell.setGraphic(hbox);
                } else {
                    cell.setGraphic(textField);
                }
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    }

    public static <T> void startEdit(final Cell<T> cell,
                              final StringConverter<T> converter,
                              final HBox hbox,
                              final Node graphic,
                              final TextField textField) {
        if (textField != null) {
            textField.setText(getItemText(cell, converter));
        }
        cell.setText(null);

        if (graphic != null) {
            hbox.getChildren().setAll(graphic, textField);
            cell.setGraphic(hbox);
        } else {
            cell.setGraphic(textField);
        }

        textField.selectAll();

        // requesting focus so that key input can immediately go into the
        // TextField (see RT-28132)
        textField.requestFocus();
    }

    public static <T> void cancelEdit(Cell<T> cell, final StringConverter<T> converter, Node graphic) {
        cell.setText(getItemText(cell, converter));
        cell.setGraphic(graphic);
    }

    public static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
        final TextField textField = new TextField(getItemText(cell, converter));

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction(event -> {
            if (converter == null) {
                throw new IllegalStateException(
                        "Attempting to convert text input into Object, but provided "
                                + "StringConverter is null. Be sure to set a StringConverter "
                                + "in your cell factory.");
            }
            cell.commitEdit(converter.fromString(textField.getText()));
            event.consume();
        });
        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        return textField;
    }



    /***************************************************************************
     *                                                                         *
     * ComboBox convenience                                                   *
     *                                                                         *
     **************************************************************************/

    public static <T> void updateItem(Cell<T> cell, StringConverter<T> converter, ComboBox<T> comboBox) {
        updateItem(cell, converter, null, null, comboBox);
    }

    public static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final HBox hbox,
                               final Node graphic,
                               final ComboBox<T> comboBox) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (comboBox != null) {
                    comboBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);

                if (graphic != null) {
                    hbox.getChildren().setAll(graphic, comboBox);
                    cell.setGraphic(hbox);
                } else {
                    cell.setGraphic(comboBox);
                }
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    };

    public static <T> ComboBox<T> createComboBox(final Cell<T> cell,
                                          final ObservableList<T> items,
                                          final ObjectProperty<StringConverter<T>> converter) {
        ComboBox<T> comboBox = new ComboBox<T>(items);
        comboBox.converterProperty().bind(converter);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        // setup listeners to properly commit any changes back into the data model.
        // First listener attempts to commit or cancel when the ENTER or ESC keys are released.
        // This is applicable in cases where the ComboBox is editable, and the user has
        // typed some input, and also when the ComboBox popup is showing.
        comboBox.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                tryComboBoxCommit(comboBox, cell);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
            }
        });

        // Second listener attempts to commit when the user is in the editor of
        // the ComboBox, and moves focus away.
        comboBox.getEditor().focusedProperty().addListener(o -> {
            if (!comboBox.isFocused()) {
                tryComboBoxCommit(comboBox, cell);
            }
        });

        // Third listener makes an assumption about the skin being used, and attempts to add
        // a listener to the ListView within it, such that when the user mouse clicks on a
        // on an item, that is immediately committed and the cell exits the editing mode.
        boolean success = listenToComboBoxSkin(comboBox, cell);
        if (!success) {
            comboBox.skinProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    boolean successInListener = listenToComboBoxSkin(comboBox, cell);
                    if (successInListener) {
                        comboBox.skinProperty().removeListener(this);
                    }
                }
            });
        }

        return comboBox;
    }

    private static <T> void tryComboBoxCommit(ComboBox<T> comboBox, Cell<T> cell) {
        StringConverter<T> sc = comboBox.getConverter();
        if (comboBox.isEditable() && sc != null) {
            T value = sc.fromString(comboBox.getEditor().getText());
            cell.commitEdit(value);
        } else {
            cell.commitEdit(comboBox.getValue());
        }
    }

    private static <T> boolean listenToComboBoxSkin(final ComboBox<T> comboBox, final Cell<T> cell) {
        Skin<?> skin = comboBox.getSkin();
        if (skin != null && skin instanceof ComboBoxListViewSkin) {
            ComboBoxListViewSkin cbSkin = (ComboBoxListViewSkin) skin;
            Node popupContent = cbSkin.getPopupContent();
            if (popupContent != null && popupContent instanceof ListView) {
                popupContent.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> cell.commitEdit(comboBox.getValue()));
                return true;
            }
        }
        return false;
    }
}