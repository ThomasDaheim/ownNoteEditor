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
import java.util.function.BiConsumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import tf.helper.javafx.RecursiveTreeItem;

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
        
        setCellFactory(TagTreeCellFactory.getInstance());
        
        setOnEditCommit((t) -> {
            if (!t.getNewValue().getName().equals(t.getOldValue().getName())) {
                assert renameFunction != null;
                renameFunction.accept(t.getOldValue().getName(), t.getNewValue().getName());
                
                t.getOldValue().setName(t.getNewValue().getName());
            }
        });
    }
    
    public ObservableSet<TagInfo> getSelectedItems() {
        return selectedItems;
    }
    
    // callback to do the work
    public void setRenameFunction(final BiConsumer<String, String> funct) {
        renameFunction = funct;
    }
    
    public void fillTreeView() {
        selectedItems.clear();

        final TagInfo rootItem = new TagInfo("Tags");
        rootItem.setChildren(TagManager.getInstance().getTagList());
        
        setRoot(new RecursiveTreeItem<>(rootItem, this::newItemConsumer, (item) -> null, TagInfo::getChildren, true, (item) -> true));
    }
    
    public void newItemConsumer(final TreeItem<TagInfo> newItem) {
        newItem.getValue().selectedProperty().addListener((obs, oldValue, newValue) -> {
            changeAction(newItem, oldValue, newValue);
        });
    }
    
    private void changeAction(final TreeItem<TagInfo> item, final Boolean oldValue, final Boolean newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!inPropgateUpwardsAction) {
                item.getValue().getChildren().forEach(child -> child.setSelected(newValue));
            }

            if (item.getParent() != null) {
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
