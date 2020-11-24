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

import java.util.Objects;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AppClipboard;
import tf.helper.javafx.CellUtils;

/**
 * Add TextFieldTreeCell functionality to CheckBoxTreeCell
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 * 
 * @author thomas
 */
public class TagTreeCellFactory implements Callback<TreeView<TagInfo>, TreeCell<TagInfo>> {
    private final static TagTreeCellFactory INSTANCE = new TagTreeCellFactory();

    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/ownnoteeditor-treetableview-dnd");
    public static final DataFormat COPY_AND_PASTE = new DataFormat("application/ownnoteeditor-treetableview-cnp");
    
    
    private enum DropPosition{
        TOP(0, 0.2, "-fx-border-color: #eea82f; -fx-border-width: 2 0 0 0"),
        CENTER(0.2, 0.8, "-fx-border-color: #eea82f; -fx-border-width: 0 2 0 0"),
        BOTTOM(0.8, 1.0, "-fx-border-color: #eea82f; -fx-border-width: 0 0 2 0");
        
        final double minPerc;
        final double maxPerc;
        final String dropHint;
        
        DropPosition(final double min, final double max, final String hint) {
            minPerc = min;
            maxPerc = max;
            dropHint = hint;
        }
        
        static DropPosition getPositionForPercentage(final double perc) {
            if (TOP.minPerc <= perc && TOP.maxPerc >= perc) {
                return TOP;
            } else if (BOTTOM.minPerc <= perc && BOTTOM.maxPerc >= perc) {
                return BOTTOM;
            } else {
                return CENTER;
            }
        }
    }
    private DropPosition dropPosition;
    private TreeCell<TagInfo> dropZone;

    private TagTreeCellFactory() {
        super();
    }

    public static TagTreeCellFactory getInstance() {
        return INSTANCE;
    }

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

    @Override
    public TreeCell<TagInfo> call(TreeView<TagInfo> treeView) {
        TreeCell<TagInfo> cell = new CheckBoxTreeCell<TagInfo>(getSelectedProperty, treeItemConverter) {
            private TextField textField;
            private HBox hbox;
            
            @Override
            public void updateItem(TagInfo item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item != null && !empty) {
                    final TreeItem<TagInfo> treeItem = getTreeItem();

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

                    setContextMenu(contextMenu);
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
        };

        cell.setOnDragDetected((MouseEvent event) -> dragDetected(event, cell, treeView));
        cell.setOnDragOver((DragEvent event) -> dragOver(event, cell, treeView));
        cell.setOnDragDropped((DragEvent event) -> drop(event, cell, treeView));
        cell.setOnDragDone((DragEvent event) -> clearDropLocation());
        
        // TODO: find a way to have mouseDragOver in drag-and-drop
        // simulate drag myself? https://stackoverflow.com/a/42890764
//        cell.setOnMouseDragEntered((MouseDragEvent event) -> mouseDragEntered(event, cell, treeView));
//        cell.setOnMouseDragExited((MouseDragEvent event) -> mouseDragExited(event, cell, treeView));
//        cell.setOnMouseDragOver((MouseDragEvent event) -> mouseDragOver(event, cell, treeView));
//        cell.setOnMouseDragReleased((MouseDragEvent event) -> mouseDragReleased(event, cell, treeView));
        
        return cell;
    }
    
//    private void mouseDragEntered(final MouseDragEvent event, final TreeCell<TagInfo> cell, final TreeView<TagInfo> treeView) {
//        if (!AppClipboard.getInstance().hasContent(DRAG_AND_DROP)) return;
//        System.out.println("mouseDragEntered");
//    }
//    private void mouseDragExited(final MouseDragEvent event, final TreeCell<TagInfo> cell, final TreeView<TagInfo> treeView) {
//        if (!AppClipboard.getInstance().hasContent(DRAG_AND_DROP)) return;
//        System.out.println("mouseDragExited");
//    }
//    private void mouseDragOver(final MouseDragEvent event, final TreeCell<TagInfo> cell, final TreeView<TagInfo> treeView) {
//        if (!AppClipboard.getInstance().hasContent(DRAG_AND_DROP)) return;
//        final String msg =
//            "(x: "       + event.getX()      + ", y: "       + event.getY()       + ") -- " +
//            "(sceneX: "  + event.getSceneX() + ", sceneY: "  + event.getSceneY()  + ") -- " +
//            "(screenX: " + event.getScreenX()+ ", screenY: " + event.getScreenY() + ")";
//        System.out.println(msg);
//    }
//    private void mouseDragReleased(final MouseDragEvent event, final TreeCell<TagInfo> cell, final TreeView<TagInfo> treeView) {
//        if (!AppClipboard.getInstance().hasContent(DRAG_AND_DROP)) return;
//        System.out.println("mouseDragReleased");
//    }
    
    private void dragDetected(final MouseEvent event, final TreeCell<TagInfo> treeCell, final TreeView<TagInfo> treeView) {
        final TreeItem<TagInfo> draggedItem = treeCell.getTreeItem();

        // root can't be dragged
        if (draggedItem.getParent() == null) return;
        AppClipboard.getInstance().addContent(DRAG_AND_DROP, treeCell);

        final Dragboard db = treeCell.startDragAndDrop(TransferMode.MOVE);
        final ClipboardContent content = new ClipboardContent();
        // store key pressed info in dragboard - no key events during drag & drop!
        content.put(DRAG_AND_DROP, String.valueOf(event.isShiftDown()));
        db.setContent(content);
        db.setDragView(treeCell.snapshot(null, null));
        
        // reset pressed pseudo-class
        treeCell.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);

        // TODO: find a way to have mouseDragOver in drag-and-drop
//        treeCell.startFullDrag();
//        treeCell.setMouseTransparent(true);
        
        event.consume();
    }

    private void dragOver(final DragEvent event, final TreeCell<TagInfo> treeCell, final TreeView<TagInfo> treeView) {
        if (!event.getDragboard().hasContent(DRAG_AND_DROP)) return;
        final TreeItem<TagInfo> thisItem = treeCell.getTreeItem();

        final TreeCell<TagInfo> dragCell = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(DRAG_AND_DROP));
        final TreeItem<TagInfo> draggedItem = dragCell.getTreeItem();
        // can't drop on itself
        if (draggedItem == null || thisItem == null || thisItem == draggedItem) return;
        // ignore if this is the root
        if (draggedItem.getParent() == null) {
            clearDropLocation();
            return;
        }

        event.acceptTransferModes(TransferMode.MOVE);
        if (!Objects.equals(dropZone, treeCell)) {
            clearDropLocation();
            dropZone = treeCell;
            
            // TFE, 20201119: figure out where on node we are and set style accordingly :-)
            final boolean shiftPressed = Boolean.valueOf(ObjectsHelper.uncheckedCast(event.getDragboard().getContent(DRAG_AND_DROP)));
            if (!shiftPressed) {
                dropPosition = DropPosition.getPositionForPercentage(event.getY() / dropZone.getHeight());
            } else {
                dropPosition = DropPosition.CENTER;
            }
            
            dropZone.setStyle(dropPosition.dropHint);
        }
    }

    private void drop(final DragEvent event, final TreeCell<TagInfo> treeCell, final TreeView<TagInfo> treeView) {
        boolean success = false;
        if (!event.getDragboard().hasContent(DRAG_AND_DROP)) return;

        final TreeItem<TagInfo> thisItem = treeCell.getTreeItem();
        final TreeCell<TagInfo> dragCell = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(DRAG_AND_DROP));
        final TreeItem<TagInfo> draggedItem = dragCell.getTreeItem();
        final TreeItem<TagInfo> draggedItemParent = draggedItem.getParent();

        // remove from previous location
        // act on tag lists - RecursiveTreeItem will take care of the rest
        draggedItemParent.getValue().getChildren().remove(draggedItem.getValue());

        // dropping on parent node makes it the first child
        if (Objects.equals(draggedItemParent, thisItem)) {
            thisItem.getValue().getChildren().add(0, draggedItem.getValue());
            treeView.getSelectionModel().select(draggedItem);
        } else {
            // add to new location
            final boolean shiftPressed = Boolean.valueOf(ObjectsHelper.uncheckedCast(event.getDragboard().getContent(DRAG_AND_DROP)));
            if (!shiftPressed) {
                int indexInParent = thisItem.getParent().getChildren().indexOf(thisItem);
                // act on tag lists - RecursiveTreeItem will take care of the rest
                thisItem.getParent().getValue().getChildren().add(indexInParent + 1, draggedItem.getValue());
            } else {
                // add as child to target
                // act on tag lists - RecursiveTreeItem will take care of the rest
                thisItem.getValue().getChildren().add(0, draggedItem.getValue());
                treeView.getSelectionModel().select(draggedItem);
            }
        }
        treeView.getSelectionModel().select(draggedItem);

        AppClipboard.getInstance().clearContent(DRAG_AND_DROP);
        event.setDropCompleted(success);
        
//        dragCell.setMouseTransparent(false);
    }

    private void clearDropLocation() {
        if (dropZone != null) dropZone.setStyle("");
    }
}
