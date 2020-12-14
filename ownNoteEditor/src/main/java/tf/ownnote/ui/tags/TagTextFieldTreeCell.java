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

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.StringConverter;

/**
 * TextFieldTreeCell functionality for tags
 * 
 * Throw drag & drop  + copy & paste support into the mix :-)
 * Based on https://github.com/cerebrosoft/treeview-dnd-example/blob/master/treedrag/TaskCellFactory.java and GPXEditor
 *
 * @author thomas
 */
public class TagTextFieldTreeCell extends TextFieldTreeCell<TagInfo> implements ITagTreeCell {
    private final TreeView<TagInfo> myTreeView;

    public TagTextFieldTreeCell(final TreeView<TagInfo> treeView) {
        super(TagTreeCellBase.tagInfoConverter);
        
        myTreeView = treeView;
    }
    
    public TagTextFieldTreeCell(final TreeView<TagInfo> treeView, final StringConverter<TagInfo> sc) {
        super(sc);
        
        myTreeView = treeView;
    }
    
    @Override
    public void updateItem(TagInfo item, boolean empty) {
        super.updateItem(item, empty);
        TagTreeCellBase.updateItem(this, item, empty);
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
        TagTreeCellBase.startEdit(this);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        TagTreeCellBase.cancelEdit(this);
    }

    @Override
    public StringConverter<TagInfo> getTextConverter() {
        return getConverter();
    }

    @Override
    public TreeCell<TagInfo> getTreeCell() {
        return this;
    }
}
