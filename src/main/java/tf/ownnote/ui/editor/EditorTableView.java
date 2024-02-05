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
package tf.ownnote.ui.editor;

import java.util.Comparator;
import java.util.List;
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
import javafx.scene.input.Dragboard;
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
import tf.ownnote.ui.helper.CmdLineParameters;
import tf.ownnote.ui.helper.EditorPreferences;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class EditorTableView implements IPreferencesHolder {
    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/ownnoteeditor-ownnotetableview-dnd");

    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
    
    private TableView<Note> myTableView = null;
    private FilteredList<Note> filteredData = null;
    
    // Issue #59: filter group names and note names
    private TagData groupFilter;
    private String noteSearchText;
    // default: don't search in files
    private Boolean noteSearchMode = false;
    private Set<Note> noteSearchNotes;

    // TFE, 20201208: tag support to show only notes linked to tags
    private TagData tagFilter;
    
    private String backgroundColor = "white";
    
    private List<TableColumn<Note,?>> mySortOrder;

    private EditorTableView() {
        super();
    }
            
    public EditorTableView(final TableView<Note> tableView, final OwnNoteEditor editor) {
        super();
        myTableView = tableView;
        myEditor = editor;
        
        initTableView();
    }

    @Override
    public void loadPreferences(final IPreferencesStore store) {
//            setSortOrder(TableSortHelper.fromString(store.get(EditorPreferences.RECENT_NOTESTABLE_SORTORDER, "")));
        TableViewPreferences.loadTableViewPreferences(myTableView, EditorPreferences.RECENT_NOTESTABLE_SETTINGS.getAsType(), store);
        setSortOrder();
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
//            store.put(EditorPreferences.RECENT_NOTESTABLE_SORTORDER, TableSortHelper.toString(getSortOrder()));
        TableViewPreferences.saveTableViewPreferences(myTableView, EditorPreferences.RECENT_NOTESTABLE_SETTINGS.getAsType(), store);
    }
    
    private TableView<Note> getTableView() {
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
            TagData newGroup = (TagData) getTableView().getUserData();
            // TF, 20160524: group name could be "All" - thats to be changed to "Not grouped"
            if (newGroup == null || newGroup.equals(TagManager.ReservedTag.All.getTag())) {
                newGroup = TagManager.ReservedTag.NotGrouped.getTag();
            }
            final String newNoteName = myEditor.uniqueNewNoteNameForGroup(newGroup);

            createNoteWrapper(newGroup, newNoteName);
        });
        newMenu.getItems().addAll(newNote2);
        myTableView.setContextMenu(newMenu);

        myTableView.setRowFactory((p) -> {
            final BooleanProperty changeValue = new SimpleBooleanProperty();

            final TableRow<Note> row = new TableRow<>() {
                @Override
                protected void updateItem(Note item, boolean empty) {
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
                    if (CmdLineParameters.LookAndFeel.groupTabs.equals(myEditor.getCurrentLookAndFeel())) {
                        if (empty) {
                            // reset background to default
                            setStyle(OwnNoteEditor.GROUP_COLOR_CSS + ": none");
                        } else {
                            // TF, 20160627: add support for issue #36 using ideas from
                            // https://rterp.wordpress.com/2015/04/11/atlas-trader-test/

                            // get tab color for notes group name
                            assert (item instanceof Note);

                            // get the color for the groupname
                            final String groupColor = ((Note) item).getGroup().getColorName();
                            setStyle(OwnNoteEditor.GROUP_COLOR_CSS + ": " + groupColor);
                            //System.out.println("updateItem - groupName, groupColor: " + groupName + ", " + groupColor);
                        }
                    }
                }
            };

            final ContextMenu fullMenu = new ContextMenu();

            final MenuItem newNote1 = new MenuItem("New Note");
            // issue #41 - but only in groupTabs look...
            newNote1.setOnAction((ActionEvent event) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
                    final Note curNote = ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem());
                    final String newNoteName = myEditor.uniqueNewNoteNameForGroup(curNote.getGroup());

                    createNoteWrapper(curNote.getGroup(), newNoteName);
                }
            });
            final MenuItem renameNote = new MenuItem("Rename Note");
            // issue #41 - but only in groupTabs look...
            renameNote.setOnAction((ActionEvent event) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
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
            final MenuItem archiveNote = new MenuItem("Archive Note");
            archiveNote.setOnAction((t) -> {
                if (myTableView.getSelectionModel().getSelectedItem() != null) {
                    final Note curNote = ObjectsHelper.uncheckedCast(myTableView.getSelectionModel().getSelectedItem());

                    if(myEditor.doArchiveRestoreNote(curNote)) {
                    }
                }
            });
            row.itemProperty().addListener((ov, t, t1) -> {
                if (t1 != null && t1.getGroup().isArchivedGroup()) {
                    archiveNote.setText("Restore Note");
                } else {
                    archiveNote.setText("Archive Note");
                }
            });

            fullMenu.getItems().addAll(newNote1, renameNote, deleteNote, archiveNote);
            
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

            row.setOnDragDone((event) -> {
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

    private void createNoteWrapper(final TagData newGroup, final String newNoteName) {
        if (myEditor.createNote(newGroup, newNoteName)) {
            myEditor.initFromDirectory(true, false);
            
            // issue 39: start editing note name
            // https://stackoverflow.com/questions/28456215/tableview-edit-focused-cell

            // find & select new entry based on note name and group name


            int selectIndex = -1;
            int i = 0;
            for (Note note : getItems()) {
                if (newNoteName.equals(note.getNoteName()) && TagManager.isSameGroup(newGroup, note.getGroup())) {
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
            myTableView.refresh();

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

    public ObservableList<Note> getItems() {
        return myTableView.getItems();
    }
    
    public void setNotes(final ObservableList<Note> items) {
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(items, p -> true);
        // re-apply filter predicate when already set
        final TagData curGroup = (TagData) getTableView().getUserData();
        if (curGroup != null) {
            setGroupFilter(curGroup);
        } else {
            setGroupFilter(TagManager.ReservedTag.All.getTag());
        }

        // 2. Create sorted list
        SortedList<Note> sortedData = new SortedList<>(filteredData);

        // 3. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(comparatorProperty());
        
        // 4. update item list in table filter
        // that removes the sort order!!!
//        myTableView.setItems(null);
//        myTableView.layout();
        myTableView.setItems(sortedData);
        restoreSortOrder();
    }
    
    public void setGroupFilter(final TagData group) {
        groupFilter = group;
        tagFilter = null;

        // force re-run of filtering since refilter() is a private method...
        setFilterPredicate();
    }
    
    public void setTagFilter(final TagData filterValue) {
        groupFilter = null;
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
        getTableView().setUserData(groupFilter);
        
        if (filteredData == null) {
            return;
        }
        
        filteredData.setPredicate((Note note) -> {
            // 1. If group filter text is empty or "All": no need to check
            if (groupFilter != null && !groupFilter.equals(TagManager.ReservedTag.All.getTag()) ) {
                // TFE, 20220404: we now have hierarchy in groups BUT groupTabs can't handle that
                // so we need to show all notes including the ones in hierarchical groups below

                // Compare group name to group filter text
                // check hierarchy as well - let manager do this
                // TFE, 20230423: use preference to include notes from sub groups
                if (!TagManager.getInstance().compareTagsHierarchy(
                        groupFilter, 
                        note.getGroup(), 
                        TagManager.TagCompare.BY_IDENTITY,  
                        EditorPreferences.SHOW_NOTES_FROM_SUB_GROUPS.getAsType())) {
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

    public ReadOnlyObjectProperty<Comparator<Note>> comparatorProperty() {
        return myTableView.comparatorProperty();
    }
}
