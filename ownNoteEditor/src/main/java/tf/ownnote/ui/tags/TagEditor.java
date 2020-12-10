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

import javafx.collections.SetChangeListener;
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
import tf.ownnote.ui.notes.Note;

/**
 * Edit tags & tag structure in a treeview.
 * 
 * @author thomas
 */
public class TagEditor extends AbstractStage {
    private final static TagEditor INSTANCE = new TagEditor();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private Note myWorkNote;
    
    public enum BulkAction {
        None,
        Delete;
    }
    
    public enum SelectedMode {
        CHECK_ACTION,
        IGNORE_ACTION;
    }
    
    private final ChoiceBox<BulkAction> bulkActionChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(BulkAction.class, BulkAction.None);
    private final Button applyBulkActionBtn = new Button("Apply");
    private final TagsTreeView tagsTreeView = new TagsTreeView();
    private final Button saveBtn = new Button("Save");

    private TagEditor() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static TagEditor getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        tagsTreeView.setCallback(editor);
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
                applyBulkActionBtn.setDisable(BulkAction.None.equals(newValue) || tagsTreeView.getSelectedItems().isEmpty());
            } else {
                applyBulkActionBtn.setDisable(true);
            }
        });
        tagsTreeView.getSelectedItems().addListener((SetChangeListener.Change<? extends TagInfo> c) -> {
            applyBulkActionBtn.setDisable(
                    BulkAction.None.equals(bulkActionChoiceBox.getSelectionModel().getSelectedItem()) || 
                    tagsTreeView.getSelectedItems().isEmpty());
        });
        applyBulkActionBtn.setDisable(true);
        getGridPane().add(applyBulkActionBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(applyBulkActionBtn, INSET_TOP);
        
        rowNum++;
        // table to hold tags
        tagsTreeView.setRenameFunction(TagManager.getInstance()::doRenameTag);
        getGridPane().add(tagsTreeView, 0, rowNum, 2, 1);
        GridPane.setMargin(tagsTreeView, INSET_TOP);
        GridPane.setVgrow(tagsTreeView, Priority.ALWAYS);
        
        // buttons OK, Cancel
        final HBox buttonBox = new HBox();
        
        saveBtn.setOnAction((ActionEvent arg0) -> {
            // save tags to file
            if (myWorkNote == null) {
                TagManager.getInstance().getTagList().setAll(tagsTreeView.getRoot().getValue().getChildren());
                TagManager.getInstance().saveTags();
            } else {
                myWorkNote.getMetaData().setTags(tagsTreeView.getSelectedLeafItems());
            }

            close();
        });
        setActionAccelerator(saveBtn);
        buttonBox.getChildren().add(saveBtn);
        HBox.setMargin(saveBtn, INSET_SMALL);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            // reload tags from file
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
    
    public boolean editTags(final Note workNote) {
        assert myEditor != null;
        
        myWorkNote = workNote;
        
        initTags();
        
        showAndWait();

        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    private void initTags() {
        bulkActionChoiceBox.getSelectionModel().select(BulkAction.None);
        applyBulkActionBtn.setDisable(true);

        if (myWorkNote == null) {
            bulkActionChoiceBox.setManaged(true);
            bulkActionChoiceBox.setDisable(false);
            applyBulkActionBtn.setManaged(true);
            saveBtn.setText("Save");
            tagsTreeView.fillTreeView(TagsTreeView.WorkMode.EDIT_MODE, null);
        } else {
            bulkActionChoiceBox.setManaged(false);
            bulkActionChoiceBox.setDisable(true);
            applyBulkActionBtn.setManaged(false);
            saveBtn.setText("Set");
            tagsTreeView.fillTreeView(TagsTreeView.WorkMode.SELECT_MODE, myWorkNote.getMetaData().getTags());
        }
    }
    
    private void doBulkAction(final BulkAction action) {
        tagsTreeView.getSelectedItems().stream().forEach((t) -> {
            switch (action) {
                case Delete:
                    TagManager.getInstance().doRenameTag(t.getName(), null);
                    TagManager.getInstance().deleteTag(t);
                    break;
            }
        });
        
        // a lot might have changed - start from scratch
        initTags();
    }
}
