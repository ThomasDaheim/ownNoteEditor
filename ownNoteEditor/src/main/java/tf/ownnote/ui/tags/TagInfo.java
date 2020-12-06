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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Helper class to fill TagsTable / TagsTree with info per tag.
 * 
 * @author thomas
 */
public class TagInfo {
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
    private final StringProperty name = new SimpleStringProperty();
    private final ObservableList<TagInfo> children = FXCollections.<TagInfo>observableArrayList();
    private final StringProperty iconName = new SimpleStringProperty();

    public TagInfo() {
        this("");
    }

    public TagInfo(final String na) {
        this(na, new ArrayList<>());
    }

    public TagInfo(final String na, final List<TagInfo> childs) {
        this(false, na, childs);
    }

    public TagInfo(final boolean sel, final String na, final List<TagInfo> childs) {
        selectedProperty.setValue(sel);
        name.set(na);
        children.setAll(FXCollections.<TagInfo>observableArrayList(childs));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TagInfo))
            return false;
        TagInfo other = (TagInfo)o;
        // we can't have two tags with same name...
        return this.name.get().equals(other.name.get());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.name.get());
        return hash;
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

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(final String na) {
        name.set(na);
    }

    public StringProperty iconNameProperty() {
        return iconName;
    }

    public String getIconName() {
        return iconName.get();
    }

    public void setIconName(final String na) {
        iconName.set(na);
    }

    public ObservableList<TagInfo> getChildren() {
        return children;
    }

    public void setChildren(final List<TagInfo> childs) {
        children.setAll(FXCollections.<TagInfo>observableArrayList(childs));
    }
    
    // method to get flat stream of taginfo + all its child tags
    // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
    public Stream<TagInfo> flattened() {
        return Stream.concat(Stream.of(this),
                children.stream().flatMap(TagInfo::flattened));
    }
}
