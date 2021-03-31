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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Wrapper for TagData that adds selected property as is required in TagsTreeView.
 * 
 * @author thomas
 */
public class TagDataWrapper {
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
    private TagData myTag;
    private final ObservableList<TagDataWrapper> children = FXCollections.<TagDataWrapper>observableArrayList();
    
    private TagDataWrapper() {
        this(null);
    }

    public TagDataWrapper(final TagData tag) {
        this(tag, false);
    }

    public TagDataWrapper(final TagData tag, final boolean sel) {
        myTag = tag;
        selectedProperty.setValue(sel);
        
        initChildren();
    }
    
    private void initChildren() {
        if (myTag != null) {
            createChildren();
        }

        // and now we need to keep track of changes to the wrapped list
        myTag.getChildren().addListener((ListChangeListener.Change<? extends TagData> change) -> {
            createChildren();
        });
    }
    
    private void createChildren() {
        children.clear();
        for (TagData child : myTag.getChildren()) {
            children.add(new TagDataWrapper(child));
        }
    }
    
    public TagData getTagInfo() {
        return myTag;
    }
    
    public void setTagInfo(final TagData tag) {
        myTag = tag;
    }
    
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(final boolean sel) {
        selectedProperty.set(sel);
    }

    public ObservableList<TagDataWrapper> getChildren() {
        return children;
    }
}
