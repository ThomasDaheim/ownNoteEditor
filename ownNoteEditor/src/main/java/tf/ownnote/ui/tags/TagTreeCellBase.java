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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import tf.helper.javafx.CellUtils;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.notes.NoteGroup;

/**
 * Base class for some common functionality of all tag treeview cells.
 * 
 * @author thomas
 */
public class TagTreeCellBase {
    public final static StringConverter<TreeItem<TagDataWrapper>> treeItemConverter = new StringConverter<TreeItem<TagDataWrapper>>() {
        @Override
        public String toString(TreeItem<TagDataWrapper> item) {
            return item.getValue().getTagInfo().getName();
        }

        @Override
        public TreeItem<TagDataWrapper> fromString(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    public final static StringConverter<TagDataWrapper> tagInfoConverter = new StringConverter<TagDataWrapper>() {
        @Override
        public String toString(TagDataWrapper item) {
            return item.getTagInfo().getName();
        }

        @Override
        public TagDataWrapper fromString(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    
    public static void updateItem(ITagTreeCell cell, TagDataWrapper item, boolean empty) {
        if (item != null && !empty) {
            final TreeCell<TagDataWrapper> treeCell = cell.getTreeCell();
            final TagData tag = item.getTagInfo();
            
            final String colorName = tag.getColorName();
            if (colorName != null && !colorName.isEmpty()) {
                // happy for any hint how this look can be achieved with less effort...
                final HBox holder = new HBox();
                holder.setAlignment(Pos.CENTER);
                
                final Label spacer = new Label("");
                spacer.setGraphic(treeCell.getGraphic());

                final Label graphic = new Label("   ");
                graphic.setStyle("-fx-background-color: " + colorName + ";");
                
                holder.getChildren().addAll(spacer, graphic);
                treeCell.setGraphic(holder);
            }

            final ContextMenu contextMenu = new ContextMenu();

            final String sibling = TagManager.isGroupsChildTag(tag) ? NoteGroup.NEW_GROUP : "New sibling";
            final MenuItem newSilblingItem = new MenuItem(sibling);
            newSilblingItem.setOnAction((ActionEvent event) -> {
                // act on tag lists - RecursiveTreeItem will take care of the rest
                tag.getParent().getChildren().add(TagManager.getInstance().createTag(sibling, TagManager.isGroupsChildTag(tag)));
            });

            // only if allowed
            final String child = TagManager.isGroupsTag(tag) ? NoteGroup.NEW_GROUP : "New child";
            final MenuItem newChildItem = new MenuItem(child);
            newChildItem.setOnAction((ActionEvent event) -> {
                // act on tag lists - RecursiveTreeItem will take care of the rest
                tag.getChildren().add(TagManager.getInstance().createTag(child, TagManager.isGroupsTag(tag)));
            });

            // only if allowed
            final MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction((ActionEvent event) -> {
                TagManager.getInstance().deleteTag(tag);
            });

            if (tag.getParent() != null) {
                // no siblings for root
                contextMenu.getItems().add(newSilblingItem);
            }
            if (TagManager.childTagsAllowed(tag)) {
                contextMenu.getItems().add(newChildItem);
            }
            if (!TagManager.isFixedTag(tag)) {
                contextMenu.getItems().add(deleteItem);
            }

            if (treeCell.getTreeView() instanceof TagsTreeView) {
                treeCell.contextMenuProperty().bind(
                        Bindings.when(((TagsTreeView) treeCell.getTreeView()).allowReorderProperty()).
                                then(contextMenu).otherwise((ContextMenu)null));
            } else {
                treeCell.setContextMenu(contextMenu);
            }
            
            // name needs to be unique, so we can also use it as id - makes life easier in 
            treeCell.setId(tag.getName());
        }
    }            

    public static void startEdit(ITagTreeCell cell) {
        final TreeCell<TagDataWrapper> treeCell = cell.getTreeCell();

        if (treeCell.isEditing()) {
            final TextField textField = createTextField(treeCell, cell.getTextConverter());

            if (treeCell.getTreeView() instanceof TagsTreeView) {
                // set textformatter that checks against existing tags and disables duplicates
                FormatHelper.getInstance().initTagNameTextField(textField, (t) -> {
                    return !((TagsTreeView) treeCell.getTreeView()).isTagNameElsewhereInTreeView(t, treeCell.getTreeItem());
                });
            }
            final HBox hbox = new HBox(CellUtils.TREE_VIEW_HBOX_GRAPHIC_PADDING);

            CellUtils.startEdit(treeCell, cell.getTextConverter(), hbox, getTreeItemGraphic(treeCell), textField);
        }
    }

    public static void cancelEdit(ITagTreeCell cell) {
        final TreeCell<TagDataWrapper> treeCell = cell.getTreeCell();

        CellUtils.cancelEdit(treeCell, cell.getTextConverter(), getTreeItemGraphic(treeCell));
    }

    private static Node getTreeItemGraphic(TreeCell<TagDataWrapper> cell) {
        TreeItem<TagDataWrapper> treeItem = cell.getTreeItem();
        return treeItem == null ? null : treeItem.getGraphic();
    }

    public static TextField createTextField(final Cell<TagDataWrapper> cell, final StringConverter<TagDataWrapper> converter) {
        final TextField textField = new TextField(CellUtils.getItemText(cell, converter));

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction(event -> {
            if (converter == null) {
                throw new IllegalStateException(
                        "Attempting to convert text input into Object, but provided "
                                + "StringConverter is null. Be sure to set a StringConverter "
                                + "in your cell factory.");
            }
            cell.getItem().getTagInfo().setName(textField.getText());
            cell.commitEdit(cell.getItem());
            event.consume();
        });
        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        return textField;
    }
}
