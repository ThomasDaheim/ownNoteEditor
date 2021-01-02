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

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.util.Callback;
import javafx.util.StringConverter;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * Add TextFieldTreeCell functionality to CheckBoxTreeCell
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 *
 * @author thomas
 */
public class TagCheckBoxTreeCell extends CheckBoxTreeCell<TagInfoWrapper> implements ITagTreeCell {
    // callback to OwnNoteEditor
    private final OwnNoteEditor myEditor;
    private final TreeView<TagInfoWrapper> myTreeView;

    private TagCheckBoxTreeCell() {
        myEditor = null;
        myTreeView = null;
    }

    public TagCheckBoxTreeCell(final TreeView<TagInfoWrapper> treeView, final OwnNoteEditor editor) {
        this(treeView, null, TagTreeCellBase.treeItemConverter, editor);
    }
    
    public TagCheckBoxTreeCell(final TreeView<TagInfoWrapper> treeView, final Callback<TreeItem<TagInfoWrapper>, ObservableValue<Boolean>> clbck, final OwnNoteEditor editor) {
        this(treeView, clbck, TagTreeCellBase.treeItemConverter, editor);
    }

    public TagCheckBoxTreeCell(final TreeView<TagInfoWrapper> treeView, final Callback<TreeItem<TagInfoWrapper>, ObservableValue<Boolean>> clbck, final StringConverter<TreeItem<TagInfoWrapper>> sc, final OwnNoteEditor editor) {
        super(clbck, sc);
        
        myEditor = editor;
        myTreeView = treeView;
    }
    
    @Override
    public void updateItem(TagInfoWrapper item, boolean empty) {
        super.updateItem(item, empty);
        TagTreeCellBase.updateItem(this, item, empty);
    }            

    @Override
    public void startEdit() {
        if (!isEditable() || !getTreeView().isEditable()) {
            return;
        }
        
        // check if item can be edited
        final TreeItem<TagInfoWrapper> treeItem = getTreeItem();
        if ((treeItem != null) && TagManager.isEditableTag(treeItem.getValue().getTagInfo())) {
            return;
        }
        
        super.startEdit();
        TagTreeCellBase.startEdit(this);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        TagTreeCellBase.cancelEdit(this);
    }

    @Override
    public OwnNoteEditor getEditor() {
        return myEditor;
    }

    @Override
    public StringConverter<TagInfoWrapper> getTextConverter() {
        return TagTreeCellBase.tagInfoConverter;
    }

    @Override
    public TreeCell<TagInfoWrapper> getTreeCell() {
        return this;
    }
}
