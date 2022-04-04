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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AppClipboard;
import tf.helper.javafx.StyleHelper;
import tf.helper.javafx.TableMenuUtils;
import tf.helper.javafx.TableViewPreferences;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTableView implements IPreferencesHolder {
    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/ownnoteeditor-ownnotetableview-dnd");

    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
    
    private TableView<Map<String, String>> myTableView = null;
    private FilteredList<Note> filteredData = null;
    
    // Issue #59: filter group names and note names
    private String groupNameFilter;
    private String noteSearchText;
    // default: don't search in files
    private Boolean noteSearchMode = false;
    private Set<Note> noteSearchNotes;

    // TFE, 20201208: tag support to show only notes linked to tags
    private TagData tagFilter;
    
    private String backgroundColor = "white";
    
    private List<TableColumn<Map<String, String>,?>> mySortOrder;
    
    // store selected group before changing the group lists for later re-select
    private String selectedGroupName = TagManager.ALL_GROUPS;

    private OwnNoteTableView() {
        super();
    }
            
    public OwnNoteTableView(final TableView<Map<String, String>> tableView, final OwnNoteEditor editor) {
        super();
        myTableView = tableView;
        myEditor = editor;
        
        initTableView();
    }

    @Override
    public void loadPreferences(final IPreferencesStore store) {
//            setSortOrder(TableSortHelper.fromString(store.get(OwnNoteEditorPreferences.RECENT_NOTESTABLE_SORTORDER, "")));
        TableViewPreferences.loadTableViewPreferences(myTableView, OwnNoteEditorPreferences.RECENT_NOTESTABLE_SETTINGS.getAsType(), store);
        setSortOrder();
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
//            store.put(OwnNoteEditorPreferences.RECENT_NOTESTABLE_SORTORDER, TableSortHelper.toString(getSortOrder()));
        TableViewPreferences.saveTableViewPreferences(myTableView, OwnNoteEditorPreferences.RECENT_NOTESTABLE_SETTINGS.getAsType(), store);
    }
    
    private TableView<Map<String, String>> getTableView() {
        return myTableView;
    }
    
    public void selectRow(final int rownum) {
        myTableView.getSelectionModel().clearAndSelect(rownum);
    }

    public void selectAndFocusRow(final int rownum) {
        selectRow(rownum);
        myTableView.getFocusModel().focus(rownum);
    }
    
    public void selectNote(final Note note) {
        myTableView.getSelectionModel().select(note);
    }

    private void initTableView() {
        myTableView.applyCss();
        myTableView.setPlaceholder(new Text(""));
        myTableView.getSelectionModel().setCellSelectionEnabled(false);
        myTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        myTableView.setDisable(false);
        myTableView.setFocusTraversable(false);
        myTableView.setCache(true);
        myTableView.setCacheHint(CacheHint.SPEED);
        
        Platform.runLater(() -> {
            TableMenuUtils.addCustomTableViewMenu(myTableView);
        });

        final ContextMenu newMenu = new ContextMenu();
        final MenuItem newNote2 = new MenuItem("New Note");
        newNote2.setOnAction((ActionEvent event) -> {
            // no note selected - above empty part of the table
            String newGroupName = (String) getTableView().getUserData();
            // TF, 20160524: group name could be "All" - thats to be changed to "Not grouped"
            if (newGroupName == null || newGroupName.equals(TagManager.ALL_GROUPS)) {
                newGroupName = "";
            }
            final String newNoteName = myEditor.uniqueNewNoteNameForGroup(newGroupName);

            createNoteWrapper(newGroupName, newNoteName);
        });
        newMenu.getItems().addAll(newNote2);
        myTableView.setContextMenu(newMenu);

        myTableView.setRowFactory((TableView<Map<String, String>> tableView) -> {
            final BooleanProperty changeValue = new SimpleBooleanProperty();

            final TableRow<Map<String, String>> row = new TableRow<Map<String, String>>() {
                @Override
                protected void updateItem(Map<String, String> item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        getStyleClass().removeAll("hasUnsavedChanges");
                        // need to unbind as well to avoid affecting mutliple rows...
                        changeValue.unbind();
                    } else {
                        assert (item instanceof Note);

                        if (((Note) item).hasUnsavedChanges()) {
                            getStyleClass().add("hasUnsavedChanges");
                        } else {
                            getStyleClass().removeAll("hasUnsavedChanges");
                        }

                        // we get a booleanbinding and need to listen to its changes...
                        changeValue.bind(((Note) item).hasUnsavedChangesProperty());
                        changeValue.addListener((ov, oldValue, newValue) -> {
                            if (newValue != null && !newValue.equals(oldValue)) {
                                if (newValue) {
                                    getStyleClass().add("hasUnsavedChanges");
                                } else {
                                    getStyleClass().removeAll("hasUnsavedChanges");
                                }
                            }
                        });
                    }

                    // issue #36 - but only for "groupTabs" look & feel
                    if (OwnNoteEditorParameters.LookAndFeel.groupTabs.equals(myEditor.getCurrentLookAndFeel())) {
                        if (empty) {
                            // reset background to default
                            setStyle(OwnNoteEditor.GROUP_COLOR_CSS + ": none");
                        } else {
                            // TF, 20160627: add support for issue #36 using ideas from
                            // https://rterp.wordpress.com/2015/04/11/atlas-trader-test/

                            // get tab color for notes group name
                            assert (item instanceof Note);

                            // get the color for the groupname
                            final String groupColor = TagManager.getInstance().tagForGroupName(((Note) item).getGroupName(), false).getColorName();
                            setStyle(OwnNoteEditor.GROUP_COLOR_CSS + ": " + groupColor);
                            //System.out.println("updateItem - groupName, groupColor: " + groupName + ", " + groupColor);
                        }
                    }
                }
            };

            final ContextMenu fullMenu = new ContextMenu();

            final MenuItem newNote1 = new MenuItem("New Note");
            // issue #41 - but only in groupTabs look...
            if (OwnNoteEditorParameters.LookAndFeel.groupTabs.equals(myEditor.getCurrentLookAndFeel())) {
                newNote1.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
            }
            newNote1.setOnAction((ActionEvent event) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
                    final Note curNote = new Note(ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem()));
                    final String newNoteName = myEditor.uniqueNewNoteNameForGroup(curNote.getGroupName());

                    createNoteWrapper(curNote.getGroupName(), newNoteName);
                }
            });
            final MenuItem renameNote = new MenuItem("Rename Note");
            // issue #41 - but only in groupTabs look...
            renameNote.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
            renameNote.setOnAction((ActionEvent event) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
                    final Note curNote = ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem());

                    startEditingName(myTableView.getSelectionModel().getSelectedIndex());
                }
            });
            final MenuItem deleteNote = new MenuItem("Delete Note");
            // issue #41 - no accelarator for delete...
            deleteNote.setOnAction((ActionEvent event) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
                    final Note curNote = ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem());

                    if(myEditor.deleteNote(curNote)) {
                        myEditor.initFromDirectory(false, false);
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
                // TFE, 20201227: don't use copy of note but the real one
                final Note curNote = ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem());

                AppClipboard.getInstance().addContent(DRAG_AND_DROP, curNote);

                /* allow any transfer mode */
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);

                /* put a string on dragboard */
                ClipboardContent content = new ClipboardContent();
                content.put(DRAG_AND_DROP, curNote.toString());
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
                assert (newSelection instanceof Note);
                if (myEditor.editNote((Note) newSelection)) {
                    // rescan directory - also group name counters need to be updated...
                    myEditor.initFromDirectory(false, false);
                }
            }
        });        
    }

    private void createNoteWrapper(final String newGroupName, final String newNoteName) {
        if (myEditor.createNote(newGroupName, newNoteName)) {
            myEditor.initFromDirectory(true, false);
            
            // issue 39: start editing note name
            // https://stackoverflow.com/questions/28456215/tableview-edit-focused-cell

            // find & select new entry based on note name and group name
            int selectIndex = -1;
            int i = 0;
            Note note;
            for (Map<String, String> noteData : getItems()) {
                note = ObjectsHelper.uncheckedCast(noteData);
                
                if (newNoteName.equals(note.getNoteName()) && TagManager.isSameGroup(newGroupName, note.getGroupName())) {
                    selectIndex = i;
                    break;
                }
                i++;
            }
            
            startEditingName(selectIndex);
        }
    }
    
    private void startEditingName(final int selectIndex) {
        if (selectIndex != -1) {
            // need to run layout first, otherwise edit() doesn't do anything
            myTableView.layout();

            selectAndFocusRow(selectIndex);

            // use selected row and always first column
            myTableView.edit(selectIndex, myTableView.getColumns().get(0));
        }
    }

    /* Required getter and setter methods are forwarded to internal TableView */

    public void setEditable(final boolean b) {
        myTableView.setEditable(b);
    }

    public Scene getScene() {
        return myTableView.getScene();
    }

    public void setBackgroundColor(final String color) {
        myTableView.setStyle(StyleHelper.addAndRemoveStyles(
                myTableView, 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, color), 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, backgroundColor)));
        backgroundColor = color;
    }

    public ObservableList<Map<String, String>> getItems() {
        return myTableView.getItems();
    }
    
    public void setNotes(final ObservableList<Note> items) {
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(items, p -> true);
        // re-apply filter predicate when already set
        final String curGroupName = (String) getTableView().getUserData();
        if (curGroupName != null) {
            setGroupNameFilter(curGroupName);
        } else {
            // only set & apply predicate to new list
            setFilterPredicate();
        }

        // 2. Create sorted list
        SortedList<Map<String, String>> sortedData = new SortedList<>(filteredData);

        // 3. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(comparatorProperty());
        
        // 4. update item list in table filter
        // that removes the sort order!!!
        myTableView.setItems(null);
        myTableView.layout();
        myTableView.setItems(sortedData);
        restoreSortOrder();
    }
    
    public void setGroupNameFilter(final String filterValue) {
        groupNameFilter = filterValue;
        tagFilter = null;

        // force re-run of filtering since refilter() is a private method...
        setFilterPredicate();
    }
    
    public void setTagFilter(final TagData filterValue) {
        groupNameFilter = null;
        tagFilter = filterValue;

        // force re-run of filtering since refilter() is a private method...
        setFilterPredicate();
    }

    public void setNoteFilterText(final String filterValue) {
        noteSearchText = filterValue;
        if(noteSearchMode) {
            noteSearchNotes = myEditor.getNotesWithText(noteSearchText);
        } else {
            noteSearchNotes = null;
        }

        // force re-run of filtering since refilter() is a private method...
        setFilterPredicate();
    }
    
    public void setNoteFilterMode(final Boolean findInFiles) {
        noteSearchMode = findInFiles;
        if(noteSearchMode) {
            noteSearchNotes = myEditor.getNotesWithText(noteSearchText);
        } else {
            noteSearchNotes = null;
        }
        // force re-run of filtering since refilter() is a private method...
        setFilterPredicate();
    }
    
    public void setFilterPredicate() {
        getTableView().setUserData(groupNameFilter);
        
        filteredData.setPredicate((Note note) -> {
            // 1. If group filter text is empty or "All": no need to check
            if (groupNameFilter != null && !groupNameFilter.isEmpty() && !groupNameFilter.equals(TagManager.ALL_GROUPS) ) {
                // TFE, 20220404: we now have hierarchy in groups BUT groupTabs can't handle that
                // so we need to show all notes including the ones in hierarchical groups below

                // Compare group name to group filter text
                // check hierarchy as well - let manager do this
                if (!TagManager.getInstance().isSameGroupOrChildGroup(
                        note.getGroupName(), 
                        groupNameFilter,
                        OwnNoteEditorParameters.LookAndFeel.groupTabs.equals(myEditor.getCurrentLookAndFeel()))) {
                    return false;
                }
            }
            
            // 2. If tag filter text is empty: no need to check
            if (tagFilter != null) {
                if (!note.getMetaData().getTags().contains(tagFilter)) {
                    return false;
                }
            }
            
            // TFE, 20181028: and now also check for note names
            return filterNotesForSearchText(note);
        });
    }
    private boolean filterNotesForSearchText(Note note) {
        boolean result = true;
        
        if (noteSearchMode) {
            // we find in files, so use name list
            if (noteSearchNotes != null) {
                // compare note name & group name against list of matches
                result = noteSearchNotes.contains(note);

                result = (noteSearchNotes.stream().filter((t) -> {
                    return (t.getNoteName().equals(note.getNoteName()) && t.getGroupName().equals(note.getGroupName()));
                }).count() > 0);
            }
        } else {
            // If name filter text is empty, display all notes.
            if (noteSearchText == null || noteSearchText.isEmpty()) {
                result = true;
            } else {
                // Compare note name to note filter text
                result = note.getNoteName().contains(noteSearchText); 
            }
        }
        
        return result;
    }

    public void setDisable(final boolean b) {
        myTableView.setDisable(b);
    }

    public void setVisible(final boolean b) {
        myTableView.setVisible(b);
    }
    
    private void setSortOrder() {
        mySortOrder = myTableView.getSortOrder();
        
        restoreSortOrder();
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
