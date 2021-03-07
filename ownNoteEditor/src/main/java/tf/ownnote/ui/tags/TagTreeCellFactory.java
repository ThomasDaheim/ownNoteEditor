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
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AppClipboard;
import tf.ownnote.ui.helper.OwnNoteTableView;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteGroup;

/**
 * Add TextFieldTreeCell functionality to CheckBoxTreeCell
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 * 
 * @author thomas
 */
public class TagTreeCellFactory implements Callback<TreeView<TagDataWrapper>, TreeCell<TagDataWrapper>> {
    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/ownnoteeditor-treetableview-dnd");
    public static final DataFormat COPY_AND_PASTE = new DataFormat("application/ownnoteeditor-treetableview-cnp");
    
    public enum TreeCellType {
        CHECKBOX,
        TEXTFIELD
    }
    
    private final TreeCellType myType;

    private enum DropPosition{
        FULL(0, 0.2, "-fx-background-color: #eea82f;"),
        TOP(0, 0.2, "-fx-border-color: #eea82f; -fx-border-width: 2 0 0 0;"),
        CENTER(0.2, 0.8, "-fx-border-color: #eea82f; -fx-border-width: 0 2 0 0;"),
        BOTTOM(0.8, 1.0, "-fx-border-color: #eea82f; -fx-border-width: 0 0 2 0;");
        
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
    private TreeCell<TagDataWrapper> dropZone;

    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;

    private TagTreeCellFactory() {
        super();
        myType = null;
    }

    public TagTreeCellFactory(final TreeCellType cellType, final OwnNoteEditor editor) {
        super();
        
        myType = cellType;
        myEditor = editor;
    }

    public void setCallback(final OwnNoteEditor editor) {
        assert editor != null;
        myEditor = editor;
    }

    // see https://stackoverflow.com/a/25444841
    private final Callback<TreeItem<TagDataWrapper>, ObservableValue<Boolean>> getSelectedProperty = ((p) -> { 
        return p.getValue().selectedProperty();
    }); 

    @Override
    public TreeCell<TagDataWrapper> call(TreeView<TagDataWrapper> treeView) {
        TreeCell<TagDataWrapper> cell;
        
        if (TreeCellType.CHECKBOX.equals(myType)) {
            cell = new TagCheckBoxTreeCell(treeView, getSelectedProperty, myEditor);
        } else {
            cell = new TagTextFieldTreeCell(treeView, myEditor);
        }

        if (treeView instanceof TagsTreeView) {
            cell.setOnDragDetected((MouseEvent event) -> dragDetected(event, cell, treeView, ((TagsTreeView) treeView).allowReorderProperty().get()));
        } else {
            cell.setOnDragDetected((MouseEvent event) -> dragDetected(event, cell, treeView, true));
        }
        cell.setOnDragOver((DragEvent event) -> dragOver(event, cell, treeView));
        cell.setOnDragDropped((DragEvent event) -> drop(event, cell, treeView));
        cell.setOnDragExited((DragEvent event) -> clearDropLocation());
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
    
    private void dragDetected(
            final MouseEvent event, 
            final TreeCell<TagDataWrapper> treeCell, 
            final TreeView<TagDataWrapper> treeView, 
            final boolean allowReorder) {
        if (treeCell.getItem() == null || TagManager.isFixedTag(treeCell.getItem().getTagInfo()) || !allowReorder) return;
        
        final TreeItem<TagDataWrapper> draggedItem = treeCell.getTreeItem();

        // root can't be dragged
        if (draggedItem.getParent() == null) return;
        AppClipboard.getInstance().addContent(DRAG_AND_DROP, treeCell);

        final Dragboard db = treeCell.startDragAndDrop(TransferMode.MOVE);
        final ClipboardContent content = new ClipboardContent();
        // store key pressed info in dragboard - no key events during drag & drop!
        // only if child tag is allowed...
        content.put(DRAG_AND_DROP, String.valueOf(event.isShiftDown() && TagManager.childTagsAllowed(treeCell.getItem().getTagInfo())));
        db.setContent(content);
        db.setDragView(treeCell.snapshot(null, null));
        
        // reset pressed pseudo-class
        treeCell.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);

        // TODO: find a way to have mouseDragOver in drag-and-drop
//        treeCell.startFullDrag();
//        treeCell.setMouseTransparent(true);
        
        event.consume();
    }

    private void dragOver(final DragEvent event, final TreeCell<TagDataWrapper> treeCell, final TreeView<TagDataWrapper> treeView) {
        if (treeCell.getItem() == null) return;
        
        if (event.getDragboard().hasContent(DRAG_AND_DROP)) {
            if (TagManager.isFixedTag(treeCell.getItem().getTagInfo())) return;
            
            final TreeItem<TagDataWrapper> thisItem = treeCell.getTreeItem();

            final TreeCell<TagDataWrapper> dragCell = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(DRAG_AND_DROP));
            final TreeItem<TagDataWrapper> draggedItem = dragCell.getTreeItem();
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
                final boolean createChild = Boolean.valueOf(ObjectsHelper.uncheckedCast(event.getDragboard().getContent(DRAG_AND_DROP)));
                if (!createChild) {
                    dropPosition = DropPosition.getPositionForPercentage(event.getY() / dropZone.getHeight());
                } else {
                    dropPosition = DropPosition.CENTER;
                }

                dropZone.setStyle(dropZone.getStyle() + dropPosition.dropHint);
            }
        } else if (event.getDragboard().hasContent(OwnNoteTableView.DRAG_AND_DROP)) {
            final Note dragNote = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(OwnNoteTableView.DRAG_AND_DROP));
            
            // note is dragged here - only accept if it hasn't this tag / group already
            final TagData thisTag = treeCell.getTreeItem().getValue().getTagInfo();
            boolean dropAllowed = true;
            if (TagManager.isAnyGroupTag(thisTag)) {
                // you can't drop on your own group, on "Groups" or "All" tags
                // how about other tags that aren't leafs?
                dropAllowed = !thisTag.getName().equals(dragNote.getGroupName()) && 
                        !TagManager.isGroupsTag(thisTag) && 
                        !NoteGroup.ALL_GROUPS.equals(thisTag.getName());
            } else {
                dropAllowed = !dragNote.getMetaData().getTags().contains(thisTag);
            }
            
            if (dropAllowed) {
                event.acceptTransferModes(TransferMode.MOVE);
                if (!Objects.equals(dropZone, treeCell)) {
                    clearDropLocation();
                    dropZone = treeCell;
                    dropZone.setStyle(dropZone.getStyle() + DropPosition.FULL.dropHint);
                }
            }
        }
    }

    private void drop(final DragEvent event, final TreeCell<TagDataWrapper> treeCell, final TreeView<TagDataWrapper> treeView) {
        if (treeCell.getItem() == null || TagManager.isFixedTag(treeCell.getItem().getTagInfo())) return;
        
        boolean success = true;
        if (event.getDragboard().hasContent(DRAG_AND_DROP)) {
            final TreeItem<TagDataWrapper> thisItem = treeCell.getTreeItem();
            final TreeItem<TagDataWrapper> thisItemParent = thisItem.getParent();
            final TreeCell<TagDataWrapper> dragCell = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(DRAG_AND_DROP));
            final TreeItem<TagDataWrapper> draggedItem = dragCell.getTreeItem();
            final TreeItem<TagDataWrapper> draggedItemParent = draggedItem.getParent();
            
            final TagData draggedTag = draggedItem.getValue().getTagInfo();

            // remove from previous location - but only if not same parent!
            if (!thisItemParent.equals(draggedItemParent)) {
                // act on tag lists - RecursiveTreeItem will take care of the rest
                draggedItemParent.getValue().getTagInfo().getChildren().remove(draggedTag);
            }

            // dropping on parent node makes it the first child
            if (Objects.equals(draggedItemParent, thisItem)) {
                thisItem.getValue().getChildren().add(0, draggedItem.getValue());
                treeView.getSelectionModel().select(draggedItem);
            } else {
                // add to new location
                final boolean createChild = Boolean.valueOf(ObjectsHelper.uncheckedCast(event.getDragboard().getContent(DRAG_AND_DROP)));
                if (!createChild) {
                    final int indexInParent = thisItem.getParent().getChildren().indexOf(thisItem);
                    int oldInParent = thisItem.getParent().getChildren().indexOf(draggedItem);
                    // act on tag lists - RecursiveTreeItem will take care of the rest
                    if (DropPosition.TOP.equals(dropPosition.dropHint)) {
                        thisItemParent.getValue().getTagInfo().getChildren().add(indexInParent, draggedTag);
                        if (oldInParent > indexInParent) {
                            oldInParent++;
                        }
                    } else {
                        thisItemParent.getValue().getTagInfo().getChildren().add(indexInParent + 1, draggedTag);
                        if (oldInParent > indexInParent+1) {
                            oldInParent++;
                        }
                    }
                    // now remove old version of dragged tag
                    if (oldInParent > 0) {
                        thisItemParent.getValue().getTagInfo().getChildren().remove(oldInParent);
                    }
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
        } else if (event.getDragboard().hasContent(OwnNoteTableView.DRAG_AND_DROP)) {
            assert myEditor != null;
            
            final Note dragNote = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(OwnNoteTableView.DRAG_AND_DROP));
            final TagData thisTag = treeCell.getTreeItem().getValue().getTagInfo();
            if (TagManager.isAnyGroupTag(thisTag)) {
                // if group was also a tag -> remove & add
                final TagData groupTag = TagManager.getInstance().tagForGroupName(dragNote.getGroupName(), false);
                if (groupTag == null) {
                    System.err.println("Something is wrong here! Tried to drag to group that doesn't have a tag?!?!?!");
                }

                // move note to group
                final String oldGroupName = dragNote.getGroupName();
                if (myEditor.moveNote(dragNote, thisTag.getName())) {
                }
            } else {
                // add tag to note
                dragNote.getMetaData().getTags().add(thisTag);
            }
            
            AppClipboard.getInstance().clearContent(OwnNoteTableView.DRAG_AND_DROP);
            event.setDropCompleted(success);
        }
        
//        dragCell.setMouseTransparent(false);
    }

    private void clearDropLocation() {
        if (dropZone != null) {
            dropZone.setStyle(
                    dropZone.getStyle().
                            replace(DropPosition.FULL.dropHint, "").
                            replace(DropPosition.CENTER.dropHint, "").
                            replace(DropPosition.BOTTOM.dropHint, "").
                            replace(DropPosition.TOP.dropHint, ""));
            
            dropZone = null;
        }
    }
}
