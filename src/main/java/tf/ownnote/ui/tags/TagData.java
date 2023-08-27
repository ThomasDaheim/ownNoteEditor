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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Label;
import tf.ownnote.ui.notes.Note;

/**
 * Helper class to fill TagsTable / TagsTree with info per tag.
 * 
 * @author thomas
 */
public class TagData {
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final ObservableList<TagData> children = 
            FXCollections.<TagData>observableArrayList(p -> new Observable[]{p.nameProperty(), p.iconNameProperty(), p.colorNameProperty(), p.parentProperty()});
    private final ObjectProperty<TagData> parentProperty = new SimpleObjectProperty<>(null);
    private final StringProperty iconNameProperty = new SimpleStringProperty();
    private final StringProperty colorNameProperty = new SimpleStringProperty("");
    // TFE, 20220404: allow hierarchical group tags - now we need to keep track of our position in the hierarchy
    private final IntegerProperty levelProperty = new SimpleIntegerProperty(0);
    
    // link to notes with this tag - transient, will be re-created on startup
    private final ObservableSet<Note> linkedNotes = FXCollections.<Note>observableSet();
    
    // we don't do Group as subclass of Tag but as attribute
    private final BooleanProperty isGroupProperty = new SimpleBooleanProperty(false);
    // lets keep track if we're aprt of the Archive special group - to avoid permanent walkthroughs of the tag tree
    private final BooleanProperty isArchivedGroupProperty = new SimpleBooleanProperty(false);

    // TFE, 20201230: initialized here to always have a value but can be overwritten from parsed noteContent
    private String myId = RandomStringUtils.random(12, "0123456789abcdef"); 
    
    private TagData() {
        this("", false, false);
    }

    protected TagData(final String na, final boolean isGroup, final boolean isArchiveGroup) {
        nameProperty.set(na);
        isGroupProperty.set(isGroup);
        isArchivedGroupProperty.set(isArchiveGroup);
        
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

        linkedNotes.addListener((SetChangeListener.Change<? extends Note> change) -> {
            if (change.wasAdded()) {
//                        System.out.println("Setting linked note " + change.getElementAdded().getNoteName() + " for tag " + getName() + ", " + this + ", linked notes count " + linkedNotes.size());
            }

            if (change.wasRemoved()) {
//                        System.out.println("Removing linked note " + change.getElementRemoved().getNoteName() + " for tag " + getName() + ", " + this + ", linked notes count " + linkedNotes.size());
            }
        });
        
        // TFE, 20220509: I can't find a way to have this outside of TagData and only in TagManager...
        // 1) ListChangeListener with extractor doesn't give you the old value of a name change
        // 2) a generic ChangeListener in TagManager for all TagData.nameProperty doesn't give you the tag where the name chane occured
        // ?!?!?!?!?!?!?
        nameProperty.addListener((ov, oldValue, newValue) -> {
            TagManager.getInstance().processTagNameChange(this, oldValue, newValue);
        });
    }
    
    // TFE, 20220427: not true anymore!
    // we're back to comparing instance identities now
    // logic has been moved to TagManager.compareGroups
//    @Override
//    public boolean equals(Object o) {
//        if (o == this)
//            return true;
//        if (!(o instanceof TagData))
//            return false;
//        TagData other = (TagData)o;
//        // we can't have two tags with same nameProperty...
//        return this.nameProperty.get().equals(other.nameProperty.get());
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 7;
//        hash = 11 * hash + Objects.hashCode(this.nameProperty.get());
//        return hash;
//    }
    
    protected TagData cloneMe() {
        final TagData clone = new TagData(getName(), isGroup(), isArchivedGroup());
        clone.myId = myId;
        clone.colorNameProperty.set(colorNameProperty.get());
        clone.iconNameProperty.set(iconNameProperty.get());
        // list copied directly
        clone.linkedNotes.clear();
        clone.linkedNotes.addAll(linkedNotes);
        // TFE, 20220427: additional attributes? no - all of them are transient anyways - but what the heck...
        clone.parentProperty.set(parentProperty.get());
        clone.isGroupProperty.set(isGroupProperty.get());
        clone.isArchivedGroupProperty.set(isArchivedGroupProperty.get());
        
        // clone childs recursively
        for (TagData child : children) {
            clone.children.add(child.cloneMe());
        }
        
        return clone;
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

    public String getExternalName() {
        return TagManager.getExternalName(this);
    }
    
    public Boolean isGroup() {
        return isGroupProperty.get();
    }
    
    public void setIsGroup(final boolean group) {
        isGroupProperty.set(group);
    }
    
    public BooleanProperty isGroupProperty() {
        return isGroupProperty;
    }

    public Boolean isArchivedGroup() {
        return isArchivedGroupProperty.get();
    }
    
    public void setIsArchivedGroup(final boolean group) {
        isArchivedGroupProperty.set(group);
    }
    
    public BooleanProperty isArchivedGroupProperty() {
        return isArchivedGroupProperty;
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
        return TagManager.getIconForName(iconNameProperty.get(), TagManager.IconSize.NORMAL);
    }

    public StringProperty colorNameProperty() {
        return colorNameProperty;
    }

    public String getColorName() {
        if ((colorNameProperty.get() != null) && !colorNameProperty.get().isEmpty()) {
            return colorNameProperty.get();
        } else {
            return null;
        }
    }

    public void setColorName(final String col) {
        colorNameProperty.set(col);
    }

    public IntegerProperty levelProperty() {
        return levelProperty;
    }

    public Integer getLevel() {
        return levelProperty.get();
    }

    public void setLevel(final int level) {
        levelProperty.set(level);
    }

    public ObservableSet<Note> getLinkedNotes() {
        return linkedNotes;
    }
    
    private ObservableSet<Note> getLinkedNotes(final boolean includeHierarchy) {
        if (!includeHierarchy) {
            return linkedNotes;
        } else {
            final ObservableSet<Note> notes = FXCollections.<Note>observableSet();
            notes.addAll(linkedNotes);
            
            for (TagData child : getChildren()) {
                notes.addAll(child.getLinkedNotes(includeHierarchy));
            }
            
            return notes;
        }
    }

    public void setLinkedNotes(final Set<Note> notes) {
        linkedNotes.clear();
        linkedNotes.addAll(notes);
    }
    
    public int getLinkesNoteCount(final boolean includeHierarchy) {
        if (TagManager.isGroupsRootTag(this)) {
            return 0;
        }

        return getLinkedNotes(includeHierarchy).size();
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

    protected void setParent(final TagData parent) {
        // conveniance in case of e.g. test cases that trigger multiple setParent with the same values
        if (Objects.equals(this.getParent(), parent)) {
            // nothing to do here...
            return;
        }
        
//        if (parent != null) {
//            System.out.println("Setting parent to " + parent.getName() + " for tag " + getName());
//        } else {
//            System.out.println("Setting parent to 'null' for tag " + getName());
//        }
        parentProperty.set(parent);
    }
    
    // method to get flat stream of taginfo + all its child tags
    // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
    public Stream<TagData> flattened() {
        return Stream.concat(Stream.of(this),
                children.stream().flatMap(TagData::flattened));
    }
}
