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
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.StringConverter;
import tf.helper.javafx.CellUtils;
import tf.ownnote.ui.helper.FormatHelper;

/**
 * Add TextFieldTreeCell functionality to CheckBoxTreeCell
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 *
 * @author thomas
 */
public class TagCheckBoxTreeCell extends CheckBoxTreeCell<TagInfo> {
    private final TreeView<TagInfo> myTreeView;
    private TextField textField;
    private HBox hbox;

    final static StringConverter<TreeItem<TagInfo>> treeItemConverter = new StringConverter<TreeItem<TagInfo>>() {
        @Override
        public String toString(TreeItem<TagInfo> item) {
            return item.getValue().getName();
        }

        @Override
        public TreeItem<TagInfo> fromString(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

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

    public TagCheckBoxTreeCell(final TreeView<TagInfo> treeView) {
        super();
        
        myTreeView = treeView;
    }
    
    public TagCheckBoxTreeCell(final TreeView<TagInfo> treeView, final Callback<TreeItem<TagInfo>, ObservableValue<Boolean>> clbck) {
        super(clbck, treeItemConverter);
        
        myTreeView = treeView;
    }

    public TagCheckBoxTreeCell(final TreeView<TagInfo> treeView, final Callback<TreeItem<TagInfo>, ObservableValue<Boolean>> clbck, final StringConverter<TreeItem<TagInfo>> sc) {
        super(clbck, sc);
        
        myTreeView = treeView;
    }
    
    @Override
    public void updateItem(TagInfo item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null && !empty) {
            final TreeItem<TagInfo> treeItem = getTreeItem();
            
            final String colorName = treeItem.getValue().getColorName();
            if (colorName != null && !colorName.isEmpty()) {
                // happy for any hint how this look can be achieved with less effort...
                final HBox holder = new HBox();
                holder.setAlignment(Pos.CENTER);
                
                final Label spacer = new Label("");
                spacer.setGraphic(getGraphic());

                final Label graphic = new Label("   ");
                graphic.setStyle("-fx-background-color: " + colorName + ";");
                
                holder.getChildren().addAll(spacer, graphic);
                setGraphic(holder);
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
        }
    }            

    @Override
    public void startEdit() {
        if (!isEditable() || !getTreeView().isEditable()) {
            return;
        }
        
        // check if item is fixed (means no edit)
        final TreeItem<TagInfo> treeItem = getTreeItem();
        if (treeItem != null && treeItem.getValue().isFixed()) {
            return;
        }
        
        super.startEdit();

        if (isEditing()) {
            if (textField == null) {
                textField = CellUtils.createTextField(this, tagInfoConverter);

                if (myTreeView instanceof TagsTreeView) {
                    // set textformatter that checks against existing tags and disables duplicates
                    FormatHelper.getInstance().initTagNameTextField(textField, (t) -> {
                        return !((TagsTreeView) myTreeView).isTagNameElsewhereInTreeView(t, treeItem);
                    });
                }
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
}
