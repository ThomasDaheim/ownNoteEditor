/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.tags;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.Optional;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;
import tf.helper.javafx.ColorConverter;
import tf.helper.javafx.GridComboBox;
import tf.helper.javafx.TooltipHelper;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * Show & edit tag data as a GridPane.
 * 
 * - name
 * - icon
 * - color
 * - group tag?
 * 
 * @author thomas
 */
public class TagDataEditor extends GridPane {
    private final static TagDataEditor INSTANCE = new TagDataEditor();

    private final static int SYMBOL_SIZE = 36;
    private final static int COLS_PER_ROW = 5;

    private TagData myTag = null;
    private OwnNoteEditor myEditor = null;
    
    private final TextField tagName = new TextField(); 
    private final GridComboBox<Label> tagIcon = new GridComboBox<>();
    private final ColorPicker tagColor = new ColorPicker();
    private final CheckBox groupTag = new CheckBox("");
    
    private TagDataEditor() {
        initCard();
    }
    
    public static TagDataEditor getInstance() {
        return INSTANCE;
    }

    private void initCard() {
        // needs to be done in case it becomes a stage...
        // (new JMetro(Style.LIGHT)).setScene(getScene());
        // getScene().getStylesheets().add(EditGPXWaypoint.class.getResource("/GPXEditor.css").toExternalForm());
        
        getStyleClass().add("tagEditor");

        final ColumnConstraints col1 = new ColumnConstraints();
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(100.0);
        col2.setMaxWidth(260.0);
        col2.setHgrow(Priority.ALWAYS);
        getGridPane().getColumnConstraints().addAll(col1,col2);

        int rowNum = 0;
        // description
        Tooltip t = new Tooltip("Tag name");
        final Label lblName = new Label("Name:");
        lblName.setTooltip(t);
        getGridPane().add(lblName, 0, rowNum, 1, 1);
        GridPane.setValignment(lblName, VPos.CENTER);
        GridPane.setMargin(lblName, INSET_TOP);
        
        getGridPane().add(tagName, 1, rowNum, 1, 1);
        GridPane.setMargin(tagName, INSET_TOP);

        rowNum++;
        // icon
        t = new Tooltip("Icon");
        final Label lblstatus = new Label("Icon:");
        lblstatus.setTooltip(t);
        getGridPane().add(lblstatus, 0, rowNum, 1, 1);
        GridPane.setValignment(lblstatus, VPos.CENTER);
        GridPane.setMargin(lblstatus, INSET_TOP);
        
        tagIcon.setEditable(false);
        tagIcon.setVisibleRowCount(8);
        tagIcon.setHgap(0.0);
        tagIcon.setVgap(0.0);
        tagIcon.setResizeContentColumn(false);
        tagIcon.setResizeContentRow(false);
        // handle non-string combobox content properly
        // https://stackoverflow.com/a/58286816
        tagIcon.setGridConverter(new StringConverter<Label>() {
            @Override
            public String toString(Label label) {
                if (label == null) {
                    return "";
                } else {
                    // icon
                    return label.getTooltip().getText();
                }
            }

            @Override
            public Label fromString(String string) {
                 return iconLabelForText(string);
            }
        });

        // add icons and group labels
        int gridRowNum = 0;
        int gridColNum = 0;
        
        for (FontAwesomeIcon icon : FontAwesomeIcon.values()) {
            final Label label = TagManager.getIconForName(icon.name(), TagManager.IconSize.LARGE);
            label.getStyleClass().add("icon-label");
            label.setGraphicTextGap(0.0);

            final Tooltip tooltip = new Tooltip(icon.name());
            tooltip.getStyleClass().add("nametooltip");
            TooltipHelper.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
            label.setTooltip(tooltip);

            tagIcon.add(label, gridColNum, gridRowNum, 1, 1);
            if (gridColNum + 1 < COLS_PER_ROW) {
                gridColNum++;
            } else {
                gridRowNum++;
                gridColNum = 0;
            }
        }
        // make sure things are laid out properly
        addColRowConstraints();
        // TFE, 20190721: filter while typing
        // TFE; 20200510: minor modification since we now show labels with images
        tagIcon.getEditor().textProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                setTagIcon(newValue);
            }
        });
        getGridPane().add(tagIcon, 1, rowNum);
        GridPane.setMargin(tagIcon, INSET_TOP);
  
        final Label symbolValue = new Label("");
        getGridPane().add(symbolValue, 2, rowNum);
        GridPane.setMargin(symbolValue, INSET_TOP);
  
        // update label for any changes of combobox selection
        tagIcon.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                symbolValue.setGraphic(TagManager.getIconForName(newValue, TagManager.IconSize.LARGE));
            }
        });

        // focus dropdown on selected item - hacking needed
        tagIcon.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                // set focus on selected item
                if (tagIcon.getSelectionModel().getSelectedIndex() > -1) {
                    tagIcon.scrollTo(iconLabelForText(tagIcon.getSelectionModel().getSelectedItem()));
                    // https://stackoverflow.com/a/36548310
                    // https://stackoverflow.com/a/47933342
//                    final ListView<Label> lv = ObjectsHelper.uncheckedCast(((ComboBoxListViewSkin) waypointSymTxt.getSkin()).getPopupContent());
//                    lv.scrollTo(waypointSymTxt.getSelectionModel().getSelectedIndex());
                }
            }
        });

        rowNum++;
        // color
        t = new Tooltip("Color");
        final Label lblCol = new Label("Color:");
        lblCol.setTooltip(t);
        getGridPane().add(lblCol, 0, rowNum, 1, 1);
        GridPane.setValignment(lblCol, VPos.CENTER);
        GridPane.setMargin(lblCol, INSET_TOP);
        
        getGridPane().add(tagColor, 1, rowNum, 1, 1);
        GridPane.setMargin(tagColor, INSET_TOP);

        rowNum++;
        // tag type
        t = new Tooltip("Group tag");
        final Label lbltype = new Label("Group tag:");
        lbltype.setTooltip(t);
        getGridPane().add(lbltype, 0, rowNum, 1, 1);
        GridPane.setValignment(lbltype, VPos.CENTER);
        GridPane.setMargin(lbltype, INSET_TOP);
        
        groupTag.setDisable(true);
        getGridPane().add(groupTag, 1, rowNum, 1, 1);
        GridPane.setMargin(groupTag, INSET_TOP);

        rowNum++;
        // save + cancel buttons
        final Button saveButton = new Button("Save");
        saveButton.setOnAction((ActionEvent event) -> {
            saveValues();
            // give parent node the oportunity to close
            fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, KeyCode.ACCEPT.toString(), KeyCode.ACCEPT.toString(), KeyCode.ACCEPT, false, false, false, false));
        });
        // not working since no scene yet...
//        getScene().getAccelerators().put(UsefulKeyCodes.CNTRL_S.getKeyCodeCombination(), () -> {
//            saveButton.fire();
//        });
        getGridPane().add(saveButton, 0, rowNum, 1, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, INSET_TOP_BOTTOM);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            initValues();
            // give parent node the oportunity to close
            fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, KeyCode.ESCAPE.toString(), KeyCode.ESCAPE.toString(), KeyCode.ESCAPE, false, false, false, false));
        });
        // not working since no scene yet...
//        getScene().getAccelerators().put(UsefulKeyCodes.ESCAPE.getKeyCodeCombination(), () -> {
//            cancelBtn.fire();
//        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setHalignment(cancelBtn, HPos.CENTER);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
    }

    private void addColRowConstraints() {
        for (int i = 0; i < tagIcon.getColumnCount(); i++) {
            final ColumnConstraints column = new ColumnConstraints(SYMBOL_SIZE);
            column.setFillWidth(true);
            column.setHgrow(Priority.ALWAYS);
            tagIcon.getColumnConstraints().add(column);
        }
        for (int i = 0; i < tagIcon.getRowCount(); i++) {
            final RowConstraints row = new RowConstraints(SYMBOL_SIZE);
            row.setFillHeight(true);
            tagIcon.getRowConstraints().add(row);
        }
    }
    
    private Label iconLabelForText(final String iconName) {
        if (iconName == null) {
            // default label is a BUG
            return iconLabelForText(FontAwesomeIcon.BUG.name());
        }
        
        final Optional<Label> label = tagIcon.getGridItems().stream().filter((t) -> {
            if (t.getTooltip() == null) {
                return false;
            } else {
                return t.getTooltip().getText().equals(iconName);
            }
        }).findFirst();
        
        if (label.isPresent()) {
            return label.get();
        } else {
            return null;
        }
    }
    
    private void setTagIcon(final String labelText) {
        final Label label = iconLabelForText(labelText);
        
        if (label != null) {
            tagIcon.getSelectionModel().select(label.getTooltip().getText());
            tagIcon.scrollTo(iconLabelForText(tagIcon.getSelectionModel().getSelectedItem()));
        }
    }

    public TagDataEditor editTag(final TagData tag, final OwnNoteEditor editor) {
        myTag = tag;
        myEditor = editor;
        
        initValues();
        
        return this;
    }
    
    private void initValues() {
        tagName.setText(myTag.getName());
        setTagIcon(myTag.getIconName());
        if (myTag.getColorName() != null && !myTag.getColorName().isEmpty()) {
            tagColor.setValue(Color.valueOf(myTag.getColorName()));
        } else{
            tagColor.setValue(Color.BLACK);
        }
        groupTag.setSelected(TagManager.isGroupsChildTag(myTag));
    }

    public void saveValues() {
        // in case of group name change we need to inform the rest of the world
        TagManager.getInstance().renameTag(myTag, tagName.getText());
        
        myTag.setIconName(tagIcon.getValue());
        myTag.setColorName(ColorConverter.JavaFXtoCSS(tagColor.getValue()));
    }

    // provision for future conversion into an AbstractStage - not very YAGNI
    private GridPane getGridPane() {
        return this;
    }
    
    public static boolean isCompleteCode(final KeyCode code) {
        return isSaveCode(code) || isCancelCode(code);
    }

    public static boolean isSaveCode(final KeyCode code) {
        return KeyCode.ACCEPT.equals(code);
    }

    public static boolean isCancelCode(final KeyCode code) {
        return KeyCode.ESCAPE.equals(code);
    }
}
