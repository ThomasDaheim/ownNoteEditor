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

import java.util.List;
import java.util.Optional;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Wrapper for TagInfo tha adds selected property as is required in TagsTreeView.
 * 
 * @author thomas
 */
public class TagInfoWrapper {
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
    private final TagInfo myTag;
    private final ObservableList<TagInfoWrapper> children = FXCollections.<TagInfoWrapper>observableArrayList();
    
    private TagInfoWrapper() {
        this(null);
    }

    public TagInfoWrapper(final TagInfo tag) {
        this(tag, false);
    }

    public TagInfoWrapper(final TagInfo tag, final boolean sel) {
        myTag = tag;
        selectedProperty.setValue(sel);
        
        initChildren();
    }
    
    private void initChildren() {
        if (myTag != null) {
            createChildren();
        }

        // and now we need to keep track of changes to the wrapped list
        myTag.getChildren().addListener((ListChangeListener.Change<? extends TagInfo> change) -> {
            createChildren();
        });
    }
    
    private void createChildren() {
        children.clear();
        for (TagInfo child : myTag.getChildren()) {
            children.add(new TagInfoWrapper(child));
        }
    }
    
    public TagInfo getTagInfo() {
        return myTag;
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

    public ObservableList<TagInfoWrapper> getChildren() {
        return children;
    }
}
