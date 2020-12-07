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

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.StringConverter;
import tf.helper.javafx.CellUtils;
import tf.ownnote.ui.helper.FormatHelper;

/**
 * TextFieldTreeCell functionality for tags
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 *
 * @author thomas
 */
public class TagTextFieldTreeCell extends TextFieldTreeCell<TagInfo> {
    private final TreeView<TagInfo> myTreeView;
    private TextField textField;

    final static StringConverter<TagInfo> tagInfoConverter = new StringConverter<TagInfo>() {
        @Override
        public String toString(TagInfo item) {
            return item.getName();
        }

        @Override
        public TagInfo fromString(String string) {
            return new TagInfo(string);
        }
    };

    public TagTextFieldTreeCell(final TreeView<TagInfo> treeView) {
        super(tagInfoConverter);
        
        myTreeView = treeView;
    }
    
    public TagTextFieldTreeCell(final TreeView<TagInfo> treeView, final StringConverter<TagInfo> sc) {
        super(sc);
        
        myTreeView = treeView;
    }
    
    @Override
    public void updateItem(TagInfo item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null && !empty) {
            final TreeItem<TagInfo> treeItem = getTreeItem();

            final String colorName = treeItem.getValue().getColorName();
            if (colorName != null && !colorName.isEmpty()) {
                setStyle("-fx-background-color: " + colorName + ";");
            } else {
                setStyle(null);
            }

            final ContextMenu contextMenu = new ContextMenu();

            final MenuItem newChildItem = new MenuItem("New child");
            newChildItem.setOnAction((ActionEvent event) -> {
                // act on tag lists - RecursiveTreeItem will take care of the rest
                getTreeItem().getValue().getChildren().add(new TagInfo("New child tag"));
            });

            if (treeItem.getParent() != null) {
                final MenuItem newSilblingItem = new MenuItem("New sibling");
                newSilblingItem.setOnAction((ActionEvent event) -> {
                    // act on tag lists - RecursiveTreeItem will take care of the rest
                    getTreeItem().getParent().getValue().getChildren().add(new TagInfo("New sibling tag"));
                });

                final MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setOnAction((ActionEvent event) -> {
                    // act on tag lists - RecursiveTreeItem will take care of the rest
                    getTreeItem().getParent().getValue().getChildren().remove(getTreeItem().getValue());
                });

                contextMenu.getItems().addAll(newSilblingItem, newChildItem, deleteItem);
            } else {
                contextMenu.getItems().addAll(newChildItem);
            }

            if (myTreeView instanceof TagsTreeView) {
                contextMenuProperty().bind(
                        Bindings.when(((TagsTreeView) myTreeView).allowReorderProperty()).
                                then(contextMenu).otherwise((ContextMenu)null));
            } else {
                setContextMenu(contextMenu);
            }
        } else {
            setStyle(null);
        }
    }            

    @Override
    public void startEdit() {
        if (! isEditable() || ! getTreeView().isEditable()) {
            return;
        }
        super.startEdit();

        if (isEditing()) {
            if (textField == null) {
                textField = CellUtils.createTextField(this, tagInfoConverter);

                if (myTreeView instanceof TagsTreeView) {
                    // set textformatter that checks against existing tags and disables duplicates
                    FormatHelper.getInstance().initTagNameTextField(textField, (t) -> {
                        return !((TagsTreeView) myTreeView).isTagNameInTreeView(t);
                    });
                }
            }

            CellUtils.startEdit(this, getConverter(), null, null, textField);
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        CellUtils.cancelEdit(this, getConverter(), null);
    }
}
