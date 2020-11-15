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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.helper.javafx.AbstractStage;
import static tf.helper.javafx.AbstractStage.INSET_SMALL;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;
import tf.helper.javafx.EnumHelper;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.main.OwnNoteEditorManager;
import tf.ownnote.ui.notes.NoteData;

/**
 *
 * @author thomas
 */
public class TagManager extends AbstractStage {
    private final static TagManager INSTANCE = new TagManager();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private WorkMode myWorkMode;
    private NoteData myWorkNote;
    
    public enum BulkAction {
        BulkAction,
        Delete;
    }
    
    public enum WorkMode {
        FULL_EDIT,
        ONLY_SELECT;
    }
    
    public enum SelectedMode {
        CHECK_ACTION,
        IGNORE_ACTION;
    }
    
    private final ChoiceBox<BulkAction> bulkActionChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(BulkAction.class, BulkAction.BulkAction);
    private final Button applyBulkActionBtn = new Button("Apply");
    private final TagsTable tagsTable = new TagsTable();
    private final Button saveBtn = new Button("Save");

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

        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(OwnNoteEditorManager.class.getResource("/css/ownnote.min.css").toExternalForm());

        int rowNum = 0;
        // selection for mass action (delete), "Apply" button
        getGridPane().add(bulkActionChoiceBox, 0, rowNum, 1, 1);
        GridPane.setMargin(bulkActionChoiceBox, INSET_TOP);

        applyBulkActionBtn.setOnAction((ActionEvent arg0) -> {
            doBulkAction(bulkActionChoiceBox.getSelectionModel().getSelectedItem());
        });
        bulkActionChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                applyBulkActionBtn.setDisable(BulkAction.BulkAction.equals(newValue));
            } else {
                applyBulkActionBtn.setDisable(true);
            }
        });
        applyBulkActionBtn.setDisable(true);
        getGridPane().add(applyBulkActionBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(applyBulkActionBtn, INSET_TOP);
        
        rowNum++;
        // table to hold tags
        tagsTable.setRenameFunction(this::doRenameTag);
        getGridPane().add(tagsTable, 0, rowNum, 2, 1);
        GridPane.setMargin(tagsTable, INSET_TOP);
        GridPane.setVgrow(tagsTable, Priority.ALWAYS);
        
        // buttons OK, Cancel
        final HBox buttonBox = new HBox();
        
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
    
    public boolean editTags(final WorkMode workMode, final NoteData workNote) {
        assert workMode != null;
        if (WorkMode.ONLY_SELECT.equals(workMode)) {
            assert workNote != null;
        }
        
        myWorkMode = workMode;
        myWorkNote = workNote;
        
        initTags();
        
        showAndWait();

        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    public List<String> getSelectedTags(final SelectedMode checkAction) {
        if (ButtonPressed.ACTION_BUTTON.equals(getButtonPressed()) || SelectedMode.IGNORE_ACTION.equals(checkAction)) {
            return tagsTable.getItems().stream().filter((t) -> {
                return t.isSelected();
            }).map((t) -> {
                return t.getName();
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
    
    private void initTags() {
        bulkActionChoiceBox.getSelectionModel().select(BulkAction.BulkAction);
        applyBulkActionBtn.setDisable(true);

        switch (myWorkMode) {
            case FULL_EDIT:
                bulkActionChoiceBox.setManaged(true);
                bulkActionChoiceBox.setDisable(false);
                applyBulkActionBtn.setManaged(true);
                saveBtn.setText("Save");

                // fill table with existing tags
                tagsTable.fillTableView(new ArrayList<>());
                break;
            case ONLY_SELECT:
                bulkActionChoiceBox.setManaged(false);
                bulkActionChoiceBox.setDisable(true);
                applyBulkActionBtn.setManaged(false);
                saveBtn.setText("Add");

                // fill table with unset tags only
                tagsTable.fillTableView(myWorkNote.getMetaData().getTags());
                break;
        }
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
    
    public void doRenameTag(final String oldName, final String newName) {
        assert oldName != null;
        
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
