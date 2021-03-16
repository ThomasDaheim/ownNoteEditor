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
 *  3. The nameProperty of the author may not be used to endorse or promote products
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

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import org.apache.commons.lang3.RandomStringUtils;
import tf.ownnote.ui.notes.Note;

/**
 * Helper class to fill TagsTable / TagsTree with info per tag.
 * 
 * @author thomas
 */
public class TagData {
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final ObservableList<TagData> children = FXCollections.<TagData>observableArrayList();
    private final ObjectProperty<TagData> parentProperty = new SimpleObjectProperty<>(null);
    private final StringProperty iconNameProperty = new SimpleStringProperty();
    private final StringProperty colorNameProperty = new SimpleStringProperty("");
    
    // link to notes with this tag - transient, will be re-created on startup
    private final ObservableList<Note> linkedNotes = FXCollections.<Note>observableArrayList();
//    private final ObservableSet<Note> linkedNotes = FXCollections.<Note>observableSet(new HashSet<>() {
//        @Override
//        public boolean remove(Object o) {
//            // TFE; 20201227: for some obscure reason the following doesn't work - don't ask
//            boolean result = super.remove(o);
//            if (!result) {
//                Iterator<Note> it = iterator();
//                while(it.hasNext()){
//                    if (it.next().equals(o)) {
//                        it.remove();
//                        result = true;
//                        break;
//                    }
//                }
//            }
//            return result;
//        }        
//    });

    // TFE, 20201230: initialized here to always have a value but can be overwritten from parsed noteContent
    private String myId = RandomStringUtils.random(12, "0123456789abcdef"); 
    
    public TagData() {
        this("");
    }

    public TagData(final String na) {
        this(na, new ArrayList<>());
    }

    public TagData(final String na, final List<TagData> childs) {
        nameProperty.set(na);
        
        readResolve();
        
        children.setAll(FXCollections.<TagData>observableArrayList(childs));
    }
    
    // required for deserialization by xstream
    private Object readResolve() {
        children.addListener((ListChangeListener.Change<? extends TagData> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (TagData tag: change.getAddedSubList()) {
//                        System.out.println("Setting tag parent: " + this.getName() + " for tag: " + tag.getName() + ", " + tag);
                        tag.setParent(this);
                    }
                }

                if (change.wasRemoved()) {
                    for (TagData tag: change.getRemoved()) {
                        // might already have been added to other tag...
                        if (this.equals(tag.getParent())) {
//                            System.out.println("Removing tag parent: " + this.getName() + " for tag: " + tag.getName() + ", " + tag);
                            tag.setParent(null);
                        }
                    }
                }
            }
        });

        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TagData))
            return false;
        TagData other = (TagData)o;
        // we can't have two tags with same nameProperty...
        return this.nameProperty.get().equals(other.nameProperty.get());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.nameProperty.get());
        return hash;
    }

    public String getId() {
        return myId;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public String getName() {
        return nameProperty.get();
    }

    public void setName(final String na) {
        nameProperty.set(na);
    }

    public StringProperty iconNameProperty() {
        return iconNameProperty;
    }

    public String getIconName() {
        return iconNameProperty.get();
    }

    public void setIconName(final String na) {
        iconNameProperty.set(na);
    }
    
    public Label getIcon() {
        Label result; 
        try {
            // TFE, 20210316: can't use GlyphsDude.createIcon since styling of text color isn't working
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.valueOf(iconNameProperty.get()), "", "1.166667em", "1.166667em", ContentDisplay.CENTER);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TagData.class.getName()).log(Level.SEVERE, null, ex);
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.BUG, "", "1.166667em", "1.166667em", ContentDisplay.CENTER);
        }
        return result;
    }

    public StringProperty colorNameProperty() {
        return colorNameProperty;
    }

    public String getColorName() {
        return colorNameProperty.get();
    }

    public void setColorName(final String col) {
        colorNameProperty.set(col);
    }

    public ObservableList<Note> getLinkedNotes() {
        return linkedNotes;
    }

    public void setLinkedNotes(final Set<Note> notes) {
        linkedNotes.clear();
        linkedNotes.addAll(notes);
    }
    
    public ObservableList<TagData> getChildren() {
        return children;
    }

    public void setChildren(final List<TagData> childs) {
        children.setAll(childs);
    }
    
    public ObjectProperty<TagData> parentProperty() {
        return parentProperty;
    }
    
    public TagData getParent() {
        return parentProperty.get();
    }

    public void setParent(final TagData parent) {
        parentProperty.set(parent);
    }
    
    // method to get flat stream of taginfo + all its child tags
    // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
    public Stream<TagData> flattened() {
        return Stream.concat(Stream.of(this),
                children.stream().flatMap(TagData::flattened));
    }
}
