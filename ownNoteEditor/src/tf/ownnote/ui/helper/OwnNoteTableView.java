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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
    
    private List<TableColumn<Map<String, String>,?>> mySortOrder;
    
    // store selected group before changing the group lists for later re-select
    private String selectedGroupName = GroupData.ALL_GROUPS;

    public static enum TableType {
        groupsTable,
        notesTable
    }
    
    private OwnNoteTableView() {
        super();
    }
            
    public OwnNoteTableView(final TableView<Map<String, String>> tableView, final OwnNoteEditor editor) {
        super();
        myTableView = tableView;
        myEditor = editor;
        
        // TF, 20160627: select tabletype based on passed TableView - safer than having a setTableType method
        if (tableView.getId().equals("notesTableFXML")) {
            myTableType = TableType.notesTable;
        }
        if (tableView.getId().equals("groupsTableFXML")) {
            myTableType = TableType.groupsTable;
        }

        // stop if we haven't been passed a correct TableView
        assert (myTableType != null);
        
        initTableView();
    }

    @Override
    public void setGroups(final ObservableList<Map<String, String>> groupsList, final boolean updateOnly) {
        assert (TableType.groupsTable.equals(myTableType));
        
        // remember the current group
        storeSelectedGroup();

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
        this.restoreSortOrder();
        
        // try to restore the current group if its still there
        restoreSelectedGroup();
    }
    
    @Override
    public GroupData getCurrentGroup() {
        return new GroupData(myTableView.getSelectionModel().getSelectedItem());
    }
    
    @Override
    public int getNotesCount() {
        return myTableView.getItems().size();
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
        
        if (myTableType != null) {
            if (TableType.notesTable.equals(myTableType)) {
                final ContextMenu newMenu = new ContextMenu();
                final MenuItem newNote2 = new MenuItem("New Note");
                newNote2.setOnAction((ActionEvent event) -> {
                    // no note selected - above empty part of the table
                    String newGroupName = (String) getTableView().getUserData();
                    // TF, 20160524: group name could be "All" - thats to be changed to "Not grouped"
                    if (newGroupName.equals(GroupData.ALL_GROUPS)) {
                        newGroupName = GroupData.NEW_GROUP;
                    }
                    final String newNoteName = myEditor.uniqueNewNoteNameForGroup(newGroupName);
                    
                    this.createNoteWrapper(newGroupName, newNoteName);
                });
                newMenu.getItems().addAll(newNote2);
                myTableView.setContextMenu(newMenu);
                
                myTableView.setRowFactory((TableView<Map<String, String>> tableView) -> {
                    final TableRow<Map<String, String>> row = new TableRow<Map<String, String>>() {
                        @Override
                        protected void updateItem(Map<String, String> item, boolean empty) {
                            super.updateItem(item, empty);
                            
                            // issue #36 - but only for "oneNote" look & feel
                            if (OwnNoteEditorParameters.LookAndFeel.oneNote.equals(myEditor.getCurrentLookAndFeel())) {
                                if (item == null) {
                                    // reset background to default
                                    setStyle("-fx-background-color: none");
                                } else {
                                    // TF, 20160627: add support for issue #36 using ideas from
                                    // https://rterp.wordpress.com/2015/04/11/atlas-trader-test/

                                    // get tab color for notes group name
                                    assert (item instanceof NoteData);
                                    final String groupName = ((NoteData) item).getGroupName();

                                    final String groupColor = myEditor.getGroupColor(groupName);
                                    setStyle("-fx-background-color: " + groupColor);
                                }
                            }
                        }
                    };
                    
                    final ContextMenu fullMenu = new ContextMenu();
                    
                    final MenuItem newNote1 = new MenuItem("New Note");
                    // issue #41 - but only in oneNote look...
                    if (OwnNoteEditorParameters.LookAndFeel.oneNote.equals(myEditor.getCurrentLookAndFeel())) {
                        newNote1.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
                    }
                    newNote1.setOnAction((ActionEvent event) -> {
                        if (myTableView.getSelectionModel().getSelectedItem() != null) {
                            final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());
                            final String newNoteName = myEditor.uniqueNewNoteNameForGroup(curNote.getGroupName());
                    
                            this.createNoteWrapper(curNote.getGroupName(), newNoteName);
                        }
                    });
                    final MenuItem renameNote = new MenuItem("Rename Note");
                    // issue #41 - but only in oneNote look...
                    if (OwnNoteEditorParameters.LookAndFeel.oneNote.equals(myEditor.getCurrentLookAndFeel())) {
                        renameNote.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
                    }
                    renameNote.setOnAction((ActionEvent event) -> {
                        if (myTableView.getSelectionModel().getSelectedItem() != null) {
                            final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());
                            
                            this.startEditingName(myTableView.getSelectionModel().getSelectedIndex());
                        }
                    });
                    final MenuItem deleteNote = new MenuItem("Delete Note");
                    // issue #41 - no accelarator for delete...
                    deleteNote.setOnAction((ActionEvent event) -> {
                        if (myTableView.getSelectionModel().getSelectedItem() != null) {
                            final NoteData curNote = new NoteData(myTableView.getSelectionModel().getSelectedItem());

                            if(myEditor.deleteNoteWrapper(curNote)) {
                                storeSelectedGroup();
                                myEditor.initFromDirectory(false);
                                restoreSelectedGroup();
                            }
                        }
                    });
                    fullMenu.getItems().addAll(newNote1, renameNote, deleteNote);
                    
                    // Set context menu on row, but use a binding to make it only show for non-empty rows:
                    row.contextMenuProperty().bind(
                            Bindings.when(row.emptyProperty())
                                    .then((ContextMenu) null)
                                    .otherwise(fullMenu)
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
                        dragStagePane.setPadding(new Insets(10));
                        Text dragText = new Text(curNote.getNoteName());
                        StackPane.setAlignment(dragText, Pos.CENTER);
                        dragStagePane.getChildren().add(dragText);

                        // https://stackoverflow.com/questions/13015698/how-to-calculate-the-pixel-width-of-a-string-in-javafx
                        Scene dragScene = new Scene(dragStagePane);
                        dragText.applyCss(); 

                        // https://stackoverflow.com/questions/26515326/create-a-image-from-text-with-background-and-wordwrap
                        WritableImage img =
                                new WritableImage((int) dragText.getLayoutBounds().getWidth()+20, (int) dragText.getLayoutBounds().getHeight()+20);
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

                myTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null && !newSelection.equals(oldSelection)) {
                        // start editing new note
                        assert (newSelection instanceof NoteData);
                        if (myEditor.editNote((NoteData) newSelection)) {
                            // rescan diretory - also group name counters need to be updated...
                            myEditor.initFromDirectory(false);
                        }
                    }
                });        
            } else {
                myTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null && !newSelection.equals(oldSelection)) {
                        // select matching notes for group
                        final String groupName = new GroupData(myTableView.getSelectionModel().getSelectedItem()).getGroupName();

                        myEditor.setFilterPredicate(groupName);
                    }
                });        
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createNoteWrapper(final String newGroupName, final String newNoteName) {
        assert (TableType.notesTable.equals(myTableType));
        
        if (myEditor.createNoteWrapper(newGroupName, newNoteName)) {
            myEditor.initFromDirectory(true);
            
            // issue 39: start editing note name
            // https://stackoverflow.com/questions/28456215/tableview-edit-focused-cell

            // find & select new entry based on note name and group name
            int selectIndex = -1;
            int i = 0;
            NoteData noteData;
            for (Map<String, String> note : this.getItems()) {
                noteData = new NoteData(note);
                
                if (newNoteName.equals(noteData.getNoteName()) && newGroupName.equals(noteData.getGroupName())) {
                    selectIndex = i;
                    break;
                }
                i++;
            }
            
            this.startEditingName(selectIndex);
        }
    }
    
    private void startEditingName(final int selectIndex) {
        if (selectIndex != -1) {
            // need to run layout first, otherwise edit() doesn't do anything
            myTableView.layout();

            this.selectAndFocusRow(selectIndex);

            // use selected row and always first column
            myTableView.edit(selectIndex, myTableView.getColumns().get(0));
        }
    }

    private void storeSelectedGroup() {
        assert (TableType.groupsTable.equals(myTableType));
        
        if (myTableView.getSelectionModel().getSelectedItem() != null) {
            selectedGroupName = getCurrentGroup().getGroupName();
        } else {
            selectedGroupName = GroupData.ALL_GROUPS;
        }
    }

    private void restoreSelectedGroup() {
        assert (TableType.groupsTable.equals(myTableType));
        
        int selectIndex = 0;
        int i = 0;
        GroupData groupData;
        for (Map<String, String> note : this.getItems()) {
            groupData = new GroupData(note);

            if (selectedGroupName.equals(groupData.getGroupName())) {
                selectIndex = i;
                break;
            }
            i++;
        }

        selectRow(selectIndex);
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
        assert (TableType.notesTable.equals(myTableType));
        
        // that removes the sort order!!!
        myTableView.setItems(null);
        myTableView.layout();
        myTableView.setItems(items);
        this.restoreSortOrder();
    }

    @Override
    public void setDisable(final boolean b) {
        myTableView.setDisable(b);
    }

    @Override
    public void setVisible(final boolean b) {
        myTableView.setVisible(b);
    }
    
    public TableSortHelper getSortOrder() {
        return new TableSortHelper(myTableView.getSortOrder());
    }
    
    public void setSortOrder(final TableSortHelper sortOrder) {
        mySortOrder = sortOrder.toTableColumnList(myTableView.getColumns());
        
        this.restoreSortOrder();
    }

    private void restoreSortOrder() {
        myTableView.getSortOrder().clear();
        myTableView.getSortOrder().addAll(mySortOrder);
        myTableView.sort();  
    }

    public ReadOnlyObjectProperty<Comparator<Map<String, String>>> comparatorProperty() {
        return myTableView.comparatorProperty();
    }

}
