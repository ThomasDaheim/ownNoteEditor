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
package tf.ownnote.ui.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import tf.helper.javafx.AbstractStage;
import static tf.helper.javafx.AbstractStage.INSET_SMALL;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;
import tf.helper.javafx.EnumHelper;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.NoteData;

/**
 *
 * @author thomas
 */
public class TagManager extends AbstractStage {
    private final static TagManager INSTANCE = new TagManager();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    public enum BulkAction {
        BulkAction,
        Delete;
    }
    
    // interal helper class to store tag info and to fill tableview
    private class TagInfo {
        private boolean selected;
        private String name;
        
        public TagInfo(final boolean sel, final String na) {
            selected = sel;
            name = na;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(final boolean sel) {
            selected = sel;
        }

        public String getName() {
            return name;
        }

        public void setName(final String na) {
            name = na;
        }
    }
    
    private final ChoiceBox<BulkAction> bulkActionChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(BulkAction.class, BulkAction.BulkAction);
    private TableView<TagInfo> tagsTable = new TableView<>();

    private TagManager() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static TagManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;
    }
    
    private void initViewer() {
        setTitle("Tags");
        initModality(Modality.APPLICATION_MODAL); 
        getGridPane().getChildren().clear();
        setHeight(600.0);
        setWidth(800.0);
        getGridPane().setPrefWidth(800);

        int rowNum = 0;
        // selection for mass action (delete), "Apply" button
        getGridPane().add(bulkActionChoiceBox, 0, rowNum, 1, 1);
        GridPane.setMargin(bulkActionChoiceBox, INSET_TOP);

        final Button applyBtn = new Button("Apply");
        applyBtn.setOnAction((ActionEvent arg0) -> {
            doBulkAction(bulkActionChoiceBox.getSelectionModel().getSelectedItem());
        });
        bulkActionChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                applyBtn.setDisable(BulkAction.BulkAction.equals(newValue));
            } else {
                applyBtn.setDisable(true);
            }
        });
        applyBtn.setDisable(true);
        getGridPane().add(applyBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(applyBtn, INSET_TOP);
        
        rowNum++;
        // table to hold tags
        // columns: action checkbox, tagname (editable), icon (combobox of symbols), notes count
        // https://github.com/SaiPradeepDandem/javafx-demos/blob/master/src/main/java/com/ezest/javafx/demogallery/tableviews/TableViewCheckBoxColumnDemo.java
        tagsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tagsTable.setEditable(true);

        // selected: boolean
        final TableColumn<TagInfo, Boolean> selectedCol = new TableColumn<>();
        selectedCol.setText("");
        selectedCol.setCellValueFactory((TableColumn.CellDataFeatures<TagInfo, Boolean> p) -> {
            final SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(p.getValue().isSelected());
            booleanProp.addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                p.getValue().setSelected(newValue);
            });
            return booleanProp;
        });
        selectedCol.setCellFactory((TableColumn<TagInfo, Boolean> p) -> {
            CheckBoxTableCell<TagInfo, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        selectedCol.setEditable(true);

        // name: string
        final TableColumn<TagInfo, String> nameCol = new TableColumn<>();
        nameCol.setText("Tag");
        nameCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<TagInfo, String> p) -> new SimpleStringProperty(p.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit((TableColumn.CellEditEvent<TagInfo, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                doRenameTag(t.getOldValue(), t.getNewValue());

                t.getRowValue().setName(t.getNewValue());
            }
        });
        // we can even change the name - since we use a key for the preference store
        nameCol.setEditable(true);
        
        // addAll() leads to unchecked cast - and we don't want that
        tagsTable.getColumns().add(selectedCol);
        tagsTable.getColumns().add(nameCol);
        
        getGridPane().add(tagsTable, 0, rowNum, 2, 1);
        GridPane.setMargin(tagsTable, INSET_TOP);
        GridPane.setVgrow(tagsTable, Priority.ALWAYS);
        
        // buttons OK, Cancel
        final HBox buttonBox = new HBox();
        
        final Button saveBtn = new Button("Save");
        saveBtn.setOnAction((ActionEvent arg0) -> {
            saveTags();

            close();
        });
        setActionAccelerator(saveBtn);
        buttonBox.getChildren().add(saveBtn);
        HBox.setMargin(saveBtn, INSET_SMALL);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        setCancelAccelerator(cancelBtn);
        buttonBox.getChildren().add(cancelBtn);
        HBox.setMargin(cancelBtn, INSET_SMALL);
        
        // TFE, 20200619: not part of grid but separately below - to have scrolling with fixed buttons
        getRootPane().getChildren().add(buttonBox);
        VBox.setMargin(buttonBox, INSET_TOP_BOTTOM);
    }
    
    public boolean editTags() {
        initTags();

        showAndWait();
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    private void initTags() {
        bulkActionChoiceBox.getSelectionModel().select(BulkAction.BulkAction);

        // fill table with existing tags
        final List<NoteData> notesList = OwnNoteFileManager.getInstance().getNotesList();
        final Set<String> tagsList = new HashSet<>();
        for (NoteData note : notesList) {
            tagsList.addAll(note.getMetaData().getTags());
        }
        
        final List<TagInfo> itemList = new ArrayList<>();
        for (String tag : tagsList) {
            itemList.add(new TagInfo(false, tag));
        }
        
        tagsTable.getItems().clear();
        tagsTable.getItems().addAll(itemList);
    }
    
    private void saveTags() {
        // save tag icons to preferences
    }
   
    private void doBulkAction(final BulkAction action) {
        for (TagInfo tagInfo : tagsTable.getItems()) {
            if (tagInfo.isSelected()) {
                switch (action) {
                    case Delete:
                        doRenameTag(tagInfo.getName(), null);
                        break;
                }
            }
        }
        
        // a lot might have changed - start from scratch
        initTags();
    }
    
    private void doRenameTag(final String oldName, final String newName) {
        final List<NoteData> notesList = OwnNoteFileManager.getInstance().getNotesList();
        for (NoteData note : notesList) {
            if (note.getMetaData().getTags().contains(oldName)) {
                final boolean inEditor = note.equals(myEditor.getEditedNote());
                if (!inEditor) {
                    // read note - only if not currently in editor!
                    OwnNoteFileManager.getInstance().readNote(note);
                }
                
                if (newName != null) {
                    Collections.replaceAll(note.getMetaData().getTags(), oldName, newName);
                } else {
                    note.getMetaData().getTags().remove(oldName);
                }

                if (!inEditor) {
                    // save new metadata - only if not currently in editor!
                    OwnNoteFileManager.getInstance().saveNote(note);
                }
            }
        }
    }
}
