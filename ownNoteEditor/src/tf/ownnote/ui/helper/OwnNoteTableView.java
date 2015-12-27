/*
 * Copyright (c) 2014 Thomas Feuster
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
package tf.ownnote.ui.helper;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTableView implements IGroupListContainer {
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
    
    private TableView<Map<String, String>> myTableView = null;
    
    private TableType myTableType = null;

    public static enum TableType {
        groupsTable,
        notesTable
    }
    
    private OwnNoteTableView() {
        super();
    }
            
    public OwnNoteTableView(final TableView<Map<String, String>> tableView) {
        super();
        myTableView = tableView;
        
        initTableView();
    }

    @Override
    public void setEditor(final OwnNoteEditor editor) {
        myEditor = editor;
    }

    @Override
    public void setGroups(final ObservableList<Map<String, String>> groupsList, final boolean updateOnly) {
        if (!updateOnly) {
            myTableView.setItems(null);
            myTableView.layout();
        }
        
        final List<String> listNames = new LinkedList<String>();
        if (myTableView.getItems() != null) {
            listNames.addAll(
                myTableView.getItems().stream().
                    map(s -> {
                        return s.get(GroupData.groupsMapKeys[0]);
                    }).
                    collect(Collectors.toList()));
        }
        
        final ObservableList<Map<String, String>> newGroups = FXCollections.observableArrayList();
        for (Map<String, String> group: groupsList) {
           final String groupName = (new GroupData(group)).getGroupName();

           if (!updateOnly || !listNames.contains(groupName)) {
               newGroups.add(group);
           }
        }
        if (myTableView.getItems() != null) {
            myTableView.getItems().addAll(newGroups);
        } else {
            myTableView.setItems(newGroups);
        }
        selectRow(0);
    }
    
    @Override
    public GroupData getCurrentGroup() {
        return new GroupData(myTableView.getSelectionModel().getSelectedItem());
    }
    
    public void setTableType(final TableType newTableType) {
        if (!newTableType.equals(this.myTableType)) {
            myTableType = newTableType;
            initTableView();
        }
    }
    
    public TableView<Map<String, String>> getTableView() {
        return myTableView;
    }
    
    public void selectRow(final int rownum) {
        myTableView.getSelectionModel().clearAndSelect(rownum);
    }

    public void selectAndFocusRow(final int rownum) {
        selectRow(rownum);
        myTableView.getFocusModel().focus(rownum);
    }

    @SuppressWarnings("unchecked")
    private void initTableView() {
        myTableView.setPlaceholder(new Text(""));
        myTableView.getSelectionModel().setCellSelectionEnabled(false);
        myTableView.setDisable(false);
        
        // TODO: add support for drag & drop
        // https://stackoverflow.com/questions/28051589/tableview-drag-and-drop-a-song-in-a-row-to-a-playlist-button-on-the-side-bar-jav
        // https://rterp.wordpress.com/2013/09/19/drag-and-drop-with-custom-components-in-javafx/
        
        if (myTableType != null) {
            if (TableType.notesTable.equals(myTableType)) {
                myTableView.setRowFactory((TableView<Map<String, String>> tableView) -> {
                    final TableRow<Map<String, String>> row = new TableRow<>();
                    final ContextMenu contextMenu = new ContextMenu();
                    final MenuItem newNote = new MenuItem("New Note");
                    newNote.setOnAction((ActionEvent event) -> {
                        final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());
                        final String newNoteName = "New Note " + myTableView.getItems().size();

                        // TODO: what group to use if "All" group is selected?
                        if (myEditor.createNoteWrapper(curNote.getGroupName(), newNoteName)) {
                            myEditor.initFromDirectory(false);
                        }
                    });
                    final MenuItem renameNote = new MenuItem("Rename Note");
                    renameNote.setDisable(true);
                    renameNote.setOnAction((ActionEvent event) -> {
                        final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());

                        // TODO: how to edit in-line from the code
                    });
                    final MenuItem deleteNote = new MenuItem("Delete Note");
                    deleteNote.setOnAction((ActionEvent event) -> {
                        final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());

                        if(myEditor.deleteNoteWrapper(curNote)) {
                            // TODO: maintain current group selected
                            myEditor.initFromDirectory(false);
                        }
                    });
                    contextMenu.getItems().addAll(newNote, renameNote, deleteNote);
                    // Set context menu on row, but use a binding to make it only show for non-empty rows:
                    row.contextMenuProperty().bind(
                            Bindings.when(row.emptyProperty())
                                    .then((ContextMenu)null)
                                    .otherwise(contextMenu)
                    );
                    
                    // support for dragging
                    row.setOnDragDetected((MouseEvent event) -> {
                        final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());
                        
                        /* allow any transfer mode */
                        Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                        
                        /* put a string on dragboard */
                        ClipboardContent content = new ClipboardContent();
                        content.putHtml("notesTable");
                        content.putString(curNote.toString());
                        db.setContent(content);
                        
                        // use note ext as image
                        StackPane dragStagePane = new StackPane();
                        dragStagePane.setStyle("-fx-background-color:#DDDDDD;");
                        dragStagePane.setPadding(new Insets(5));
                        Text dragText = new Text(curNote.getNoteName());
                        StackPane.setAlignment(dragText, Pos.CENTER);
                        dragStagePane.getChildren().add(dragText);

                        // https://stackoverflow.com/questions/13015698/how-to-calculate-the-pixel-width-of-a-string-in-javafx
                        Scene dragScene = new Scene(dragStagePane);
                        dragText.applyCss(); 

                        // https://stackoverflow.com/questions/26515326/create-a-image-from-text-with-background-and-wordwrap
                        WritableImage img =
                                new WritableImage((int) dragText.getLayoutBounds().getWidth()+10, (int) dragText.getLayoutBounds().getHeight()+10);
                        dragScene.snapshot(img);
                        
                        db.setDragView(img);
                        
                        event.consume();
                    });
                    row.setOnDragDone((DragEvent event) -> {
                        if (event.getTransferMode() == TransferMode.MOVE) {
                            // TODO: remove row from this table
                        }
                        
                        event.consume();
                    });

                    return row ;  
                });              
            } else {
                myTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null && !newSelection.equals(oldSelection)) {
                        // select matching notes for group
                        final String groupName = new GroupData(myTableView.getSelectionModel().getSelectedItem()).getGroupName();

                        myEditor.setFilterPredicate(groupName);
                    }
                });        
                
                // TODO add support for context menu to groups table as well
                
                // TODO: add support for drag end
            }
            
        }
    }
    
    /* Required getter and setter methods are forwarded to internal TableView */

    public void setEditable(final boolean b) {
        myTableView.setEditable(b);
    }

    public Scene getScene() {
        return myTableView.getScene();
    }

    @Override
    public void setStyle(final String style) {
        myTableView.setStyle(style);
    }

    public ObservableList<Map<String, String>> getItems() {
        return myTableView.getItems();
    }

    public void setNotes(final ObservableList<Map<String, String>> items) {
        myTableView.setItems(null);
        myTableView.layout();
        myTableView.setItems(items);
    }

    @Override
    public void setDisable(final boolean b) {
        myTableView.setDisable(b);
    }

    @Override
    public void setVisible(final boolean b) {
        myTableView.setVisible(b);
    }

    public ReadOnlyObjectProperty<Comparator<Map<String, String>>> comparatorProperty() {
        return myTableView.comparatorProperty();
    }

}
