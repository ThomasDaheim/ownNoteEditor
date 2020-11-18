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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import tf.ownnote.ui.general.CellUtils;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.NoteData;

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
public class TagsTreeView extends TreeView<TagInfo> {
    private boolean inPropgateUpwardsAction = false;
    
    private BiConsumer<String, String> renameFunction;
    
    private ObservableSet<TagInfo> selectedItems = FXCollections.observableSet(new HashSet<>());
    
    public TagsTreeView() {
        initTreeView();
    }

    private void initTreeView() {
        setEditable(true);

        final double treeViewHeight = 300.0;
        setPrefHeight(treeViewHeight);
        setMinHeight(treeViewHeight);
        setMaxHeight(treeViewHeight);

        // see https://stackoverflow.com/a/25444841
        final Callback<TreeItem<TagInfo>, ObservableValue<Boolean>> getSelectedProperty = ((p) -> { return p.getValue().selectedProperty(); }); 
        final StringConverter<TreeItem<TagInfo>> treeItemConverter = new StringConverter<TreeItem<TagInfo>>() {
            @Override
            public String toString(TreeItem<TagInfo> item) {
                return item.getValue().getName();
            }

            @Override
            public TreeItem<TagInfo> fromString(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        final StringConverter<TagInfo> tagInfoConverter = new StringConverter<TagInfo>() {
            @Override
            public String toString(TagInfo item) {
                return item.getName();
            }

            @Override
            public TagInfo fromString(String string) {
                return new TagInfo(string);
            }
        };
        
        // mix CheckBoxTreeCell with TextFieldTreeCell...
        setCellFactory((tree -> new CheckBoxTreeCell<TagInfo>(getSelectedProperty, treeItemConverter){
            private TextField textField;
            private HBox hbox;

            @Override
            public void startEdit() {
                if (! isEditable() || ! getTreeView().isEditable()) {
                    return;
                }
                super.startEdit();

                if (isEditing()) {
                    if (textField == null) {
                        textField = CellUtils.createTextField(this, tagInfoConverter);
                    }
                    if (hbox == null) {
                        hbox = new HBox(CellUtils.TREE_VIEW_HBOX_GRAPHIC_PADDING);
                    }

                    CellUtils.startEdit(this, tagInfoConverter, hbox, getTreeItemGraphic(), textField);
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                CellUtils.cancelEdit(this, tagInfoConverter, getTreeItemGraphic());
            }

            private Node getTreeItemGraphic() {
                TreeItem<TagInfo> treeItem = getTreeItem();
                return treeItem == null ? null : treeItem.getGraphic();
            }
        }));
        
        setOnEditCommit((t) -> {
            if (!t.getNewValue().getName().equals(t.getOldValue().getName())) {
                assert renameFunction != null;
                renameFunction.accept(t.getOldValue().getName(), t.getNewValue().getName());
                
                t.getOldValue().setName(t.getNewValue().getName());
            }
        });

        final TagInfo rootItem = new TagInfo("Tags");
        final TreeItem<TagInfo> root = new TreeItem<>(rootItem);
        rootItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
            changeAction(root, oldValue, newValue);
        });
        root.setExpanded(true);
        setRoot(root);
    }
    
    public ObservableSet<TagInfo> getSelectedItems() {
        return selectedItems;
    }
    
    // callback to do the work
    public void setRenameFunction(final BiConsumer<String, String> funct) {
        renameFunction = funct;
    }
    
    public void fillTreeView(final List<String> excludeTags) {
        // TODO: use with RecursiveTreeItem once tags handling is clever enough

        final List<NoteData> notesList = OwnNoteFileManager.getInstance().getNotesList();
        final Set<String> tagsList = new HashSet<>();
        for (NoteData note : notesList) {
            tagsList.addAll(note.getMetaData().getTags());
        }
        
        final List<TagInfo> itemList = new ArrayList<>();
        for (String tag : tagsList) {
            if (!excludeTags.contains(tag)) {
                itemList.add(new TagInfo(tag));
            }
        }
        
        selectedItems.clear();

        getRoot().getValue().setChildTags(itemList);

        getRoot().getChildren().clear();
        getRoot().getChildren().addAll(itemList.stream().map((t) -> {
            // since we don't use a CheckBoxTreeItem we have do handle selection change ourselves
            // https://stackoverflow.com/a/29991507
            final TreeItem<TagInfo> info = new TreeItem<>(t);
            t.selectedProperty().addListener((obs, oldValue, newValue) -> {
                changeAction(info, oldValue, newValue);
            });
            return info;
        }).collect(Collectors.toList()));
    }
    
    private void changeAction(final TreeItem<TagInfo> item, final Boolean oldValue, final Boolean newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!inPropgateUpwardsAction) {
                item.getValue().childTagsProperty().forEach(child -> child.setSelected(newValue));
            }

            if (item.isLeaf()) {
                // see, if we need to change the parents
                // recursion is done automatically through this listener
                propagateUpwards(item, newValue);

                if (newValue) {
                    selectedItems.add(item.getValue());
                } else {
                    selectedItems.remove(item.getValue());
                }
            }
        }
    }
    
    private void propagateUpwards(final TreeItem<TagInfo> item, final boolean newValue) {
        if (!item.isLeaf()) {
            // root is artificial
            return;
        }

        // check / uncheck based on value of all child items of items parent
        // one unchecked <-> root unchecked
        // see bulk vs. single in TagsTable
        inPropgateUpwardsAction = true;
        
        final TreeItem<TagInfo> parent = item.getParent();
        
        boolean unSelectedFlag = false;
        for (TreeItem<TagInfo> child : parent.getChildren()) {
            if (!child.getValue().isSelected()) {
                unSelectedFlag = true;
                break;
            }
        }
        parent.getValue().setSelected(!unSelectedFlag);

        inPropgateUpwardsAction = false;
    }
}
