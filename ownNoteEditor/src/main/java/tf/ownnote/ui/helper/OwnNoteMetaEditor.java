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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import tf.helper.javafx.AbstractStage;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.NoteData;
import tf.ownnote.ui.tasks.TaskCount;
import tf.ownnote.ui.tasks.TaskManager;

/**
 * Show and edit metadata of a note:
 * 1) Authors: show only, last one selected 
 * 2) Tags: show & edit in a fancy way
 *    - show each tag as label with cross to remove
 *    - show label "add or create" that open small dialog with the list of known tags and a create option
 * 3) Tasks: show counts (open / closed / total)
 * @author thomas
 */
public class OwnNoteMetaEditor {
    private HBox myHBox;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
    
    private ComboBox<String> authors = new ComboBox<>();
    private Label taskstxt;
    private FlowPane tagsBox;
    
    private NoteData editorNote;
    private boolean hasChanged = false;

    private OwnNoteMetaEditor() {
        super();
    }
    
    public OwnNoteMetaEditor(final HBox hBox, final OwnNoteEditor editor) {
        super();

        myEditor = editor;
        
        myHBox = hBox;
        
        initEditor();
    }
    
    private void initEditor() {
//        myHBox.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        myHBox.setFillHeight(true);
        myHBox.setAlignment(Pos.CENTER_LEFT);
        
        final Label authorsLbl = new Label("Authors:");
        HBox.setMargin(authorsLbl, AbstractStage.INSET_SMALL);
        
        // https://stackoverflow.com/a/32373721
        authors.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item);
                    setDisable(true);
                }
            }
        });

        final Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);

        final Label tagsLbl = new Label("Tags:");
        HBox.setMargin(tagsLbl, AbstractStage.INSET_SMALL);
        
        tagsBox = new FlowPane();
        tagsBox.getStyleClass().add("tagsBox");
        tagsBox.setMinWidth(100.0);
        
        final Button tagsButton = new Button("+");

        final Label tasksLbl = new Label("Tasks:");
        HBox.setMargin(tasksLbl, AbstractStage.INSET_SMALL);

        Tooltip t = new Tooltip("Open / Closed / Total");
        taskstxt = new Label("");
        taskstxt.setTooltip(t);
        HBox.setMargin(taskstxt, new Insets(0, 8, 0, 0));
        
        myHBox.getChildren().addAll(authorsLbl, authors, tagsLbl, tagsBox, tagsButton, region1, tasksLbl, taskstxt);
    }
    
    public void editNote(final NoteData note) {
        editorNote = note;
        hasChanged = false;
        
        authors.getItems().clear();
        // set labels & fields with note data
        if (editorNote != null) {
            if (editorNote.getMetaData().getAuthors().isEmpty()) {
                editorNote.getMetaData().addAuthor(System.getProperty("user.name"));
            }
            authors.getItems().addAll(editorNote.getMetaData().getAuthors());
            authors.getSelectionModel().selectLast();
        }
        
        final TaskCount taskCount = TaskManager.getInstance().getTaskCount(note);
        taskstxt.setText(taskCount.getCount(TaskCount.TaskType.OPEN) + " / " + taskCount.getCount(TaskCount.TaskType.CLOSED) + " / " + taskCount.getCount(TaskCount.TaskType.TOTAL));
    }
    public boolean hasChanged() {
        return hasChanged;
    }
    
    public void hasBeenSaved() {
        hasChanged = false;
    }
}
