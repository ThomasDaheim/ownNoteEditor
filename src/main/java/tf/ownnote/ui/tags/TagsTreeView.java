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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import tf.helper.javafx.RecursiveTreeItem;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;

/**
 * A TreeView for TagInfo.
 * 
 * It supports checking / unchecking items for e.g. bulk actions done in the TagManager.
 * But since it should be working with RecursiveTreeItem to support a hierarchy of tags it 
 * can't be a using CheckboxTreeItem - you can't have a generic class for the whole TreeItem hierarchy...
 * Also, we want to be able to edit the Tag name.
 * 
 * So we need to merge the functionality of CheckBoxTreeCell with TextFieldTreeCell :-)
 * And we need to do something similar to CheckTreeView from controlsfx that keeps track of the selected items.

* @author thomas
 */
public class TagsTreeView extends TreeView<TagDataWrapper> implements IGroupListContainer {
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
    
    private final ObservableSet<TagData> selectedItems = FXCollections.<TagData>observableSet(new HashSet<>());
    
    private final Set<TagData> initialTags = new HashSet<>();
    
    private TagData currentGroup;

    private ListChangeListener<TagData> tagListener;
    
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
            // TODO: verify that this works!
            if (!t.getNewValue().getTagData().getName().equals(t.getOldValue().getTagData().getName())) {
                TagManager.getInstance().renameTag(t.getOldValue().getTagData(), t.getNewValue().getTagData().getName());
                t.getNewValue().getTagData().setName(t.getNewValue().getTagData().getName());
            }
        });
        
        getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends TreeItem<TagDataWrapper>> change) -> {
            // getSelectionModel().getSelectedItem() can be null even if getSelectionModel().getSelectedItems() isn't empty
            // list is updated first before property? and therefore ListChangeListener runs before selected item gets set?
            if (!getSelectionModel().getSelectedItems().isEmpty() && WorkMode.LIST_MODE.equals(myWorkMode)) {
                final TreeItem<TagDataWrapper> item = getSelectionModel().getSelectedItems().get(0);
                if (item != null) {
                    final TagData tag = item.getValue().getTagData();
                    
                    if (TagManager.isGroupsRootTag(tag)) {
                        // "Groups" selected => similar to "All" in tabs
                        currentGroup = TagManager.ReservedTag.All.getTag();
                        myEditor.setGroupFilter(currentGroup);
                    } else if (tag.isGroup()) {
                        // a tag under "Groups" selected => similar to select a tab
                        currentGroup = tag;
                        myEditor.setGroupFilter(currentGroup);
                    } else {
                        // any "normal" tag has been selected - set filter on all its and childrens notes
                        final Set<Note> tagNotes = tag.flattened().map((t) -> {
                            return t.getLinkedNotes();
                        }).flatMap(Set::stream).collect(Collectors.toSet());
                        
                        myEditor.setTagFilter(tag);
                    }

                    myEditor.selectFirstOrCurrentNote();
                }
            }
        });
        
        // TFE, 20210331: listener to react to changes to any tags properties
        tagListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        // nothing to do - handled by recursivetreeitem
                    }
                    if (change.wasAdded()) {
                        // nothing to do - handled by recursivetreeitem
                    }
                    if (change.wasUpdated()) {
                        // refresh / rebuild the list
                        refresh();
                    }
                }
            }
        };
        
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
        
        currentGroup = TagManager.ReservedTag.All.getTag();
    }
    
    public BooleanProperty allowReorderProperty() {
        return allowReorder;
    }
    
    public ObservableSet<TagData> getSelectedItems() {
        return selectedItems;
    }
    
    public Set<TagData> getSelectedLeafItems() {
        return selectedItems.stream().filter((t) -> {
            return t.getChildren().isEmpty();
        }).collect(Collectors.toSet());
    }
    
    public void fillTreeView(final WorkMode workMode, final Set<TagData> tags) {
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
        setRoot(new RecursiveTreeItem<>(new TagDataWrapper(TagManager.getInstance().getRootTag()), this::newItemConsumer, (item) -> null, TagDataWrapper::getChildren, true, (item) -> {
            return !(WorkMode.SELECT_MODE.equals(myWorkMode) && TagManager.isGroupsRootTag(item.getTagData()));
        }));
        inInitTreeView = false;
        
        // set property after filling list :-)
        initWorkMode();
        
        // add listener to tags list to get notified on case of 
        TagManager.getInstance().removeListener(tagListener);
        TagManager.getInstance().addListChangeListener(tagListener);
    }
    
    private void newItemConsumer(final TreeItem<TagDataWrapper> newItem) {
        assert myEditor != null;
        
        final TagDataWrapper wrapper = newItem.getValue();
        final TagData tag = wrapper.getTagData();

        wrapper.selectedProperty().addListener((obs, oldValue, newValue) -> {
            changeAction(newItem, oldValue, newValue);
        });
        
        wrapper.getTagData().nameProperty().addListener((obs, oldValue, newValue) -> {
            refresh();
        });

        // refresh on change of linked notes - is part of the group name...
        wrapper.getTagData().getLinkedNotes().addListener((SetChangeListener.Change<? extends Note> change) -> {
            refresh();
        });

        if (initialTags.contains(tag)) {
            wrapper.setSelected(true);
            selectedItems.add(tag);
        } else {
            wrapper.setSelected(false);
            selectedItems.remove(tag);
        }
        
        // TFE, 20220601: don't expand tags under archive
        // TODO: keep track of expand/colapse state per tag and store as metadata
        newItem.setExpanded(!TagManager.isArchiveRootTag(tag));
    }
    
    private void changeAction(final TreeItem<TagDataWrapper> item, final Boolean oldValue, final Boolean newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!inPropgateUpwardsAction && !inInitTreeView) {
                item.getValue().getChildren().forEach(child -> child.setSelected(newValue));
            }

            if (item.getParent() != null) {
                // see, if we need to change the parents
                // recursion is done automatically through this listener
                propagateUpwards(item, newValue);

                if (newValue) {
                    selectedItems.add(item.getValue().getTagData());
                } else {
                    selectedItems.remove(item.getValue().getTagData());
                }
            }
        }
    }
    
    private void propagateUpwards(final TreeItem<TagDataWrapper> item, final boolean newValue) {
        if (!item.isLeaf()) {
            // root is artificial
            return;
        }

        // check / uncheck based on value of all child items of items parent
        // one unchecked <-> root unchecked
        // see bulk vs. single in TagsTable
        inPropgateUpwardsAction = true;
        
        final TreeItem<TagDataWrapper> parent = item.getParent();
        
        boolean unSelectedFlag = false;
        for (TreeItem<TagDataWrapper> child : parent.getChildren()) {
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
        
        TreeItem<TagDataWrapper> searchItem = getRoot();
        // TFE, 20221228: with hierarchies in groups we need to find starting from the root of the groups @ the note!
        for (TagData tag : TagManager.getTagHierarchyAsList(note.getGroup())) {
            searchItem = getTreeViewItem(searchItem, tag.getName());
            
            if (searchItem == null) {
                // wedon't know this group
                break;
            }
        }
        if (searchItem != null) {
            getSelectionModel().select(searchItem);
        }
    }
    
    // TODO make a generic helper method out of it with TreeItem<T>, Function<TreeItem<T>, S> and S value
    private static TreeItem<TagDataWrapper> getTreeViewItem(TreeItem<TagDataWrapper> item , String value) {
        if (item != null) {
            if (item.getValue().getTagData().getName().equals(value)) {
                // found it!
                return item;
            }

            // check children and below
            for (TreeItem<TagDataWrapper> child : item.getChildren()){
                TreeItem<TagDataWrapper> s = getTreeViewItem(child, value);
                if (s != null) {
                    return s;
                }
            }
        }
        
        return null;
    }

    @Override
    public void setGroups(ObservableList<TagData> groupsList, boolean updateOnly) {
        myEditor.selectFirstOrCurrentNote();
    }

    @Override
    public TagData getCurrentGroup() {
        return currentGroup;
    }

    @Override
    public void setBackgroundColor(String style) {
    }
}
