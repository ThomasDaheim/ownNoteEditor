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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import tf.helper.javafx.RecursiveTreeItem;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteGroup;

/**
 * A TreeView for TagInfo.
 * 
 * It supports checking / unchecking items for e.g. bulk actions done in the TagManager.
 * But since it should be working with RecursiveTreeItem to support a hierarchy of tags it 
 * can't be a using CheckboxTreeItem - you can't have a generic class for the whole TreeItem hierarchy...
 * Also, we wan't to be able to edit the Tag name.
 * 
 * So we need to merge the functionality of CheckBoxTreeCell with TextFieldTreeCell :-)
 * And we need to do something similar to CheckTreeView from controlsfx that keeps track of the selected items.

* @author thomas
 */
public class TagsTreeView extends TreeView<TagInfoWrapper> implements IGroupListContainer {
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;

    private boolean inPropgateUpwardsAction = false;
    private boolean inInitTreeView = false;

    public enum WorkMode {
        EDIT_MODE,
        SELECT_MODE,
        LIST_MODE
    }
    private final BooleanProperty allowReorder = new SimpleBooleanProperty(true);
    
    private WorkMode myWorkMode = WorkMode.EDIT_MODE;
    
    private BiConsumer<String, String> renameFunction;
    
    private final ObservableSet<TagInfo> selectedItems = FXCollections.<TagInfo>observableSet(new HashSet<>());
    
    private final Set<TagInfo> initialTags = new HashSet<>();
    
    private NoteGroup currentGroup;
    
    public TagsTreeView() {
        initTreeView();
    }

    public TagsTreeView(final OwnNoteEditor editor) {
        assert editor != null;
        myEditor = editor;
        
        initTreeView();
    }

    public void setCallback(final OwnNoteEditor editor) {
        assert editor != null;
        myEditor = editor;
    }
    
    private void initTreeView() {
        getStyleClass().add("tagsTreeView");
        
        setEditable(true);
        setShowRoot(false);
        
        setOnEditCommit((t) -> {
            if (!t.getNewValue().getTagInfo().getName().equals(t.getOldValue().getTagInfo().getName())) {
                assert renameFunction != null;
                renameFunction.accept(t.getOldValue().getTagInfo().getName(), t.getNewValue().getTagInfo().getName());
                
                t.getOldValue().getTagInfo().setName(t.getNewValue().getTagInfo().getName());
            }
        });
        
        getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends TreeItem<TagInfoWrapper>> change) -> {
            // getSelectionModel().getSelectedItem() can be null even if getSelectionModel().getSelectedItems() isn't empty
            // list is updated first before property? and therefore ListChangeListener runs before selected item gets set?
            if (!getSelectionModel().getSelectedItems().isEmpty() && WorkMode.LIST_MODE.equals(myWorkMode)) {
                final TreeItem<TagInfoWrapper> item = getSelectionModel().getSelectedItems().get(0);
                if (item != null) {
                    final TagInfo tag = item.getValue().getTagInfo();
                    
                    if (TagManager.isGroupsTag(tag)) {
                        // "Groups" selected => similar to "All" in tabs
                        currentGroup = OwnNoteFileManager.getInstance().getNoteGroup(NoteGroup.ALL_GROUPS);
                        myEditor.setGroupNameFilter(NoteGroup.ALL_GROUPS);
                    } else if (TagManager.isAnyGroupTag(tag)) {
                        // a tag under "Groups" selected => similar to select a tab
                        currentGroup = OwnNoteFileManager.getInstance().getNoteGroup(tag.getName());
                        myEditor.setGroupNameFilter(tag.getName());
                    } else {
                        // any "normal" tag has been selected - set filter on all its and childrens notes
                        final Set<Note> tagNotes = tag.flattened().map((t) -> {
                            return t.getLinkedNotes();
                        }).flatMap(List::stream).collect(Collectors.toSet());
                        
                        myEditor.setTagFilter(tag);
                    }

                    myEditor.selectFirstOrCurrentNote();
                }
            }
        });
    }
    
    private void initWorkMode() {
        // reset so that change listener gets triggered
        allowReorder.set(true);

        switch (myWorkMode) {
        case EDIT_MODE:
            getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            
            // allow drag/drop & add/remove
            allowReorder.set(true);
            break;
        case SELECT_MODE:
            getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // disable drag/drop & add/remove
            allowReorder.set(false);
            break;
        case LIST_MODE:
            getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            // disable drag/drop & add/remove
            allowReorder.set(false);
            break;
        }
        
        currentGroup = OwnNoteFileManager.getInstance().getNoteGroup(NoteGroup.ALL_GROUPS);
    }
    
    public BooleanProperty allowReorderProperty() {
        return allowReorder;
    }
    
    public ObservableSet<TagInfo> getSelectedItems() {
        return selectedItems;
    }
    
    public Set<TagInfo> getSelectedLeafItems() {
        return selectedItems.stream().filter((t) -> {
            return t.getChildren().isEmpty();
        }).collect(Collectors.toSet());
    }
    
    // callback to do the work
    public void setRenameFunction(final BiConsumer<String, String> funct) {
        renameFunction = funct;
    }
    
    public void fillTreeView(final WorkMode workMode, final Set<TagInfo> tags) {
        assert myEditor != null;
        
        myWorkMode = workMode;
        
        initialTags.clear();
        if (tags != null) {
            initialTags.addAll(tags);
        }

        // choose tree cell type based on workmode
        TagTreeCellFactory.TreeCellType cellType;
        if (!WorkMode.LIST_MODE.equals(myWorkMode)) {
            cellType = TagTreeCellFactory.TreeCellType.CHECKBOX;
        } else {
            cellType = TagTreeCellFactory.TreeCellType.TEXTFIELD;
        }
        setCellFactory(new TagTreeCellFactory(cellType, myEditor));
        
        selectedItems.clear();

        inInitTreeView = true;
        // for SELECT_MODE we don't show Groups - since its handled implicitly & is connected to the group name of the notes file
        setRoot(new RecursiveTreeItem<>(new TagInfoWrapper(TagManager.getInstance().getRootTag()), this::newItemConsumer, (item) -> null, TagInfoWrapper::getChildren, true, (item) -> {
            return !(WorkMode.SELECT_MODE.equals(myWorkMode) && TagManager.isGroupsTag(item.getTagInfo()));
        }));
        inInitTreeView = false;
        
        // set property after filling list :-)
        initWorkMode();
    }
    
    private void newItemConsumer(final TreeItem<TagInfoWrapper> newItem) {
        assert myEditor != null;

        newItem.getValue().selectedProperty().addListener((obs, oldValue, newValue) -> {
            changeAction(newItem, oldValue, newValue);
        });
        
        newItem.getValue().getTagInfo().nameProperty().addListener((obs, oldValue, newValue) -> {
            refresh();
        });

        final TagInfoWrapper tag = newItem.getValue();
        if (tag != null) {
            if (initialTags.contains(tag.getTagInfo())) {
                tag.setSelected(true);
                selectedItems.add(tag.getTagInfo());
            } else {
                tag.setSelected(false);
                selectedItems.remove(tag.getTagInfo());
            }
        }
    }
    
    private void changeAction(final TreeItem<TagInfoWrapper> item, final Boolean oldValue, final Boolean newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!inPropgateUpwardsAction && !inInitTreeView) {
                item.getValue().getChildren().forEach(child -> child.setSelected(newValue));
            }

            if (item.getParent() != null) {
                // see, if we need to change the parents
                // recursion is done automatically through this listener
                propagateUpwards(item, newValue);

                if (newValue) {
                    selectedItems.add(item.getValue().getTagInfo());
                } else {
                    selectedItems.remove(item.getValue().getTagInfo());
                }
            }
        }
    }
    
    private void propagateUpwards(final TreeItem<TagInfoWrapper> item, final boolean newValue) {
        if (!item.isLeaf()) {
            // root is artificial
            return;
        }

        // check / uncheck based on value of all child items of items parent
        // one unchecked <-> root unchecked
        // see bulk vs. single in TagsTable
        inPropgateUpwardsAction = true;
        
        final TreeItem<TagInfoWrapper> parent = item.getParent();
        
        boolean unSelectedFlag = false;
        for (TreeItem<TagInfoWrapper> child : parent.getChildren()) {
            if (!child.getValue().isSelected()) {
                unSelectedFlag = true;
                break;
            }
        }
        parent.getValue().setSelected(!unSelectedFlag);

        inPropgateUpwardsAction = false;
    }
    
    @Override
    public void selectGroupForNote(final Note note) {
        assert note != null;
        
        final TreeItem<TagInfoWrapper> groupItem = getTreeViewItem(getRoot(), note.getGroupName());
        if (groupItem != null) {
            getSelectionModel().select(groupItem);
        }
    }
    
    public boolean isTagNameInTreeView(final String tag) {
        return isTagNameElsewhereInTreeView(tag, null);
    }

    public boolean isTagNameElsewhereInTreeView(final String tag, final TreeItem<TagInfoWrapper> thisItem) {
        final TreeItem<TagInfoWrapper> otherItem = getTreeViewItem(getRoot(), tag);
        return (otherItem != null && !otherItem.equals(thisItem));
    }

    // TODO make a generic helper method out of it with TreeItem<T>, Function<TreeItem<T>, S> and S value
    public static TreeItem<TagInfoWrapper> getTreeViewItem(TreeItem<TagInfoWrapper> item , String value) {
        if (item != null) {
            if (item.getValue().getTagInfo().getName().equals(value)) {
                // found it!
                return item;
            }

            // check children and below
            for (TreeItem<TagInfoWrapper> child : item.getChildren()){
                TreeItem<TagInfoWrapper> s = getTreeViewItem(child, value);
                if (s != null) {
                    return s;
                }
            }
        }
        
        return null;
    }

    @Override
    public void setGroups(ObservableList<NoteGroup> groupsList, boolean updateOnly) {
        myEditor.selectFirstOrCurrentNote();
    }

    @Override
    public NoteGroup getCurrentGroup() {
        return currentGroup;
    }

    @Override
    public void setBackgroundColor(String style) {
    }
}
