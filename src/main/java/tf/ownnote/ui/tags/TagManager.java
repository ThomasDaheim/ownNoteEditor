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

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;
import tf.helper.general.ObjectsHelper;
import tf.helper.xstreamfx.FXConverters;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;

/**
 * Load & save tag info to XML stored along with notes.
 * // https://nullbeans.com/configuring-xstream-to-convert-java-objects-to-and-from-xml/
 * 
 * @author thomas
 */
public class TagManager implements IFileChangeSubscriber, IFileContentChangeSubscriber, INoteCRMDS {
    private final static TagManager INSTANCE = new TagManager();
    
    private final static String TAG_DIR = File.separator + "MetaData";
    private final static String TAG_FILE = TAG_DIR + File.separator + "tag_info.xml";

    public static final String ALL_GROUPS = "All";
    public static final String NOT_GROUPED = "Not grouped";
    public static final String NEW_GROUP = "New group";

    // available colors for tabs to rotate through
    // issue #36 - have "All" without color
    // TF, 20170122: use colors similar to OneNote - a bit less bright
    //private static final String[] groupColors = { "darkseagreen", "cornflowerblue", "lightsalmon", "gold", "orchid", "cadetblue", "goldenrod", "darkorange", "MediumVioletRed", "lightpink", "skyblue" };
    private static final String[] groupColors = { "#F4A6A6", "#99D0DF", "#F1B87F", "#F2A8D1", "#9FB2E1", "#B4AFDF", "#D4B298", "#C6DA82", "#A2D07F", "#F1B5B5", "#ffb6c1", "#87ceeb" };
    
    // reserved names for tags - can't be moved or edited
    public enum ReservedTagName {
        Groups
    }
    
    public enum IconSize {
        NORMAL("1.166667em"),
        LARGE("28px");
        
        private final String iconSize;
        
        private IconSize(final String size) {
            iconSize = size;
        }
        
        public final String getSize() {
            return iconSize;
        }
    }
    
    private final static Map<ReservedTagName, TagData> reservedTags = new HashMap<>();
    
    protected final static String ROOT_TAG_NAME = "Tags";
    // root of all tags - not saved or loaded
    private final static TagData ROOT_TAG = new TagData(ROOT_TAG_NAME);
    
    // we need to keep track of changes in the tag tree to update the group tag list....
    private final ListChangeListener<TagData> tagChildrenListener;
    // TFE, 20210405: hold list of group tags with an extractor - to be able to listen to changes of tag content
    private final ObservableList<TagData> groupTags = 
            FXCollections.<TagData>observableArrayList(p -> new Observable[]{p.nameProperty(), p.iconNameProperty(), p.colorNameProperty()});
    
    // know thy listeners - to able to add / remove from added / removed tags
    private final List<ListChangeListener<? super TagData>> changeListeners = new ArrayList<>();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private boolean tagsLoaded = false;
    
    private TagManager() {
        super();
        // Exists only to defeat instantiation.
        
        tagChildrenListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (TagData tag : change.getRemoved()) {
                            // can't check for isGroupsChildTag(tag) anymore since it has already been removed from the parent...
//                            System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was removed from group tags");
                            groupTags.remove(tag);
                        }
                    }
                    if (change.wasAdded()) {
                        for (TagData tag : change.getAddedSubList()) {
                            if (isGroupsChildTag(tag) && !groupTags.contains(tag)) {
//                                System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was added to group tags");
                                groupTags.add(tag);
                            }
                        }
                    }
                    if (change.wasUpdated()) {
                        for (TagData tag : change.getList().subList(change.getFrom(), change.getTo())) {
//                            System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was updated");
                        }
                    }
                }
            }
        };
        
        changeListeners.add(tagChildrenListener);
    }

    public static TagManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        // now we can register everywhere
        OwnNoteFileManager.getInstance().subscribe(INSTANCE);
        myEditor.getNoteEditor().subscribe(INSTANCE);
    }

    private void initTags() {
        if (!tagsLoaded) {
            // lazy loading
            loadTags();

            // we want to listen to everything as well
            doAddAllListener(ROOT_TAG);
        }
    }
    
    public final TagData getRootTag() {
        initTags();
        // you can use but not change
        return ROOT_TAG;
    }

    public ObservableList<TagData> getGroupTags() {
        initTags();
        return groupTags;
    }
    
    // helper methods to check for All, Not Grouped 
    public static boolean isSpecialGroup(final String groupName) {
        return (isNotGrouped(groupName) || ALL_GROUPS.equals(groupName));
    }
    
    public static boolean isNotGrouped(final String groupName) {
        // isEmpty() happens for new notes, otherwise, group names are NOT_GROUPED from OwnNoteFileManager.initNotesPath()
        return (groupName.isEmpty() || NOT_GROUPED.equals(groupName));
    }
    
    public static boolean isSameGroup(final String groupName1, final String groupName2) {
        assert groupName1 != null;
        assert groupName2 != null;
        // either both are equal or both are part of "Not grouped"
        return (groupName1.equals(groupName2) || isNotGrouped(groupName1) && isNotGrouped(groupName2));
    }    
    
    public void resetTagList() {
        if (tagsLoaded) {
            doRemoveAllListener(ROOT_TAG);
            ROOT_TAG.getChildren().clear();
            tagsLoaded = false;
        }
    }
    
    private void loadTags() {
        final TagData xstreamRoot = new TagData("xstream-root");
        
        final String fileName = OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            // load from xml AND from current metadata
            final XStream xstream = new XStream(new PureJavaReflectionProvider(), new DomDriver("UTF-8"));
            xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
            XStream.setupDefaultSecurity(xstream);
            final Class<?>[] classes = new Class[] { 
                TagData.class, 
                ObservableListWrapper.class, 
                ObservableSetWrapper.class, 
                SimpleBooleanProperty.class, 
                SimpleStringProperty.class,
                SimpleObjectProperty.class};
            xstream.allowTypes(classes);

            FXConverters.configure(xstream);

            xstream.alias("taginfo", TagData.class);
            xstream.alias("listProperty", ObservableListWrapper.class);
            xstream.alias("setProperty", ObservableSetWrapper.class);
            xstream.alias("booleanProperty", SimpleBooleanProperty.class);
            xstream.alias("stringProperty", SimpleStringProperty.class);
            xstream.alias("objectProperty", SimpleObjectProperty.class);

            xstream.aliasField("id", TagData.class, "myId");
            xstream.aliasField("name", TagData.class, "nameProperty");
            xstream.aliasField("iconName", TagData.class, "iconNameProperty");
            xstream.aliasField("colorName", TagData.class, "colorNameProperty");
            
            xstream.omitField(TagData.class, "linkedNotes");
            xstream.omitField(TagData.class, "parentProperty");
            // TFE, 20201220: we had that in for a while
            xstream.omitField(TagData.class, "fixedProperty");

            try (
                BufferedInputStream stdin = new BufferedInputStream(new FileInputStream(fileName));
                Reader reader = new InputStreamReader(stdin, "ISO-8859-1");
            ) {
                // TFE, 20210507: can't read into real root tag since xstream deserialization messes something up with the properties
                // and that stops change events from being registered and provided via property extractors
                xstreamRoot.setChildren(ObjectsHelper.uncheckedCast(xstream.fromXML(reader)));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // deep copy tags over to real root
        ROOT_TAG.setChildren(xstreamRoot.cloneMe().getChildren());
        tagsLoaded = true;
        
        // ensure reserved names are fixed
        for (ReservedTagName reservedTag : ReservedTagName.values()) {
            final String tagName = reservedTag.name();
            TagData tag = tagForName(tagName, ROOT_TAG, false);
            if (tag == null) {
                tag = new TagData(tagName);
                ROOT_TAG.getChildren().add(tag);
                
                System.out.println("No tag for reserved name " + tagName + " found. Initializing...");
            }
            
            // TFE, 20220122: need to put before testing for missing tags...
            reservedTags.put(reservedTag, tag);

            if (TagManager.ReservedTagName.Groups.name().equals(tag.getName())) {
                // store for later use
                groupTags.setAll(tag.getChildren());
                
                boolean hasAllGroups = false;
                boolean hasNotGrouped = false;
                
                // color my children
                for (TagData tagChild : tag.getChildren()) {
                    final String tagChildName = tagChild.getName();
                    
                    initGroupTag(tagChild);
                    
                    if (ALL_GROUPS.equals(tagChildName)) {
                        hasAllGroups = true;
                    }
                    if (NOT_GROUPED.equals(tagChildName)) {
                        hasNotGrouped = true;
                    }
                }
                
                if (!hasAllGroups) {
                    // all groups is always the first entry
                    System.out.println("No tag for group " + ALL_GROUPS + " found. Adding...");
                    createTagAtPosition(ALL_GROUPS, true, 0);
                }
                if (!hasNotGrouped) {
                    // all groups is always the second entry
                    System.out.println("No tag for group " + NOT_GROUPED + " found. Adding...");
                    createTagAtPosition(NOT_GROUPED, true, 1);
                }
            }
        }

        // backlink all parents recursively
        backlinkParent(ROOT_TAG);
    }
    private void backlinkParent(final TagData tag) {
        for (TagData childTag : tag.getChildren()) {
            childTag.setParent(tag);
            backlinkParent(childTag);
        }
    }
    
    public void addListener(ListChangeListener<? super TagData> ll) {
        changeListeners.add(ll);
        doAddListener(getRootTag(), ll);
    }
    private void doAddListener(final TagData tagRoot, ListChangeListener<? super TagData> ll) {
        // add listener to my children and to the children of my children
//        System.out.println("Adding listener " + ll + " to tag " + tagRoot.getName());
        tagRoot.getChildren().addListener(ll);
        for (TagData tag : tagRoot.getChildren()) {
            doAddListener(tag, ll);
        }
    }
    private void doAddAllListener(final TagData tagRoot) {
        // add all listeners to the children
        for (ListChangeListener<? super TagData> ll : changeListeners) {
            doAddListener(tagRoot, ll);
        }
    }
    
    public void removeListener(ListChangeListener<? super TagData> ll) {
        changeListeners.remove(ll);
        doRemoveListener(getRootTag(), ll);
    }
    private void doRemoveListener(final TagData tagRoot, ListChangeListener<? super TagData> ll) {
        // remove listener from my children and from the children of my children
//        System.out.println("Removing listener " + ll + " from tag " + tagRoot.getName());
        tagRoot.getChildren().removeListener(ll);
        for (TagData tag : tagRoot.getChildren()) {
            doRemoveListener(tag, ll);
        }
    }
    private void doRemoveAllListener(final TagData tagRoot) {
        // add all listeners to the children
        for (ListChangeListener<? super TagData> ll : changeListeners) {
            doRemoveListener(tagRoot, ll);
        }
    }

    public void saveTags() {
        if (ROOT_TAG.getChildren().isEmpty()) {
            // list hasn't been initialized yet - so don't try to save
            return;
        }

        try {
            FileUtils.forceMkdir(new File(OwnNoteFileManager.getInstance().getNotesPath() + TAG_DIR));
        } catch (IOException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        final String fileName = OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && (file.isDirectory() || !file.canWrite())) {
            return;
        }
        
        // save to xml
        final XStream xstream = new XStream(new DomDriver("UTF-8"));
        xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
        XStream.setupDefaultSecurity(xstream);
        final Class<?>[] classes = new Class[] { 
            TagData.class, 
            ObservableListWrapper.class, 
            ObservableSetWrapper.class, 
            SimpleBooleanProperty.class, 
            SimpleStringProperty.class,
            SimpleObjectProperty.class};
        xstream.allowTypes(classes);

        FXConverters.configure(xstream);

        xstream.alias("taginfo", TagData.class);
        xstream.alias("listProperty", ObservableListWrapper.class);
        xstream.alias("setProperty", ObservableSetWrapper.class);
        xstream.alias("booleanProperty", SimpleBooleanProperty.class);
        xstream.alias("stringProperty", SimpleStringProperty.class);
        xstream.alias("objectProperty", SimpleObjectProperty.class);

        xstream.aliasField("id", TagData.class, "myId");
        xstream.aliasField("name", TagData.class, "nameProperty");
        xstream.aliasField("iconName", TagData.class, "iconNameProperty");
        xstream.aliasField("colorName", TagData.class, "colorNameProperty");
        
        xstream.omitField(TagData.class, "linkedNotes");
        xstream.omitField(TagData.class, "parentProperty");

        try (
            BufferedOutputStream stdout = new BufferedOutputStream(new FileOutputStream(fileName));
            Writer writer = new OutputStreamWriter(stdout, "ISO-8859-1");
        ) {
            PrettyPrintWriter printer = new PrettyPrintWriter(writer, new char[]{'\t'});
            writer.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + System.getProperty("line.separator"));
        
            xstream.marshal(ROOT_TAG.getChildren(), printer);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final Note note, final String oldContent, final String newContent) {
        return true;
    }
    
    public void groupsToTags() {
        // check all notes if they already have a tag with their group name
        // if not add such a tag
        // probably only used once for "migration" to tag metadata
        for (Note note : OwnNoteFileManager.getInstance().getNotesList()) {
            // lets not store a "Not grouped" tag...
            if (!isNotGrouped(note.getGroupName())) {
                final TagData groupInfo = tagForGroupName(note.getGroupName(), true);
                if (note.getMetaData().getTags().contains(groupInfo)) {
                    System.out.println("Removing tag " + note.getGroupName() + " from note " + note.getNoteName());
                    OwnNoteFileManager.getInstance().readNote(note, false);
                    note.getMetaData().getTags().remove(groupInfo);
                    OwnNoteFileManager.getInstance().saveNote(note);
                } else {
                    System.out.println("Adding tag " + note.getGroupName() + " to note " + note.getNoteName());
                    OwnNoteFileManager.getInstance().readNote(note, false);
                    note.getMetaData().getTags().add(groupInfo);
                    OwnNoteFileManager.getInstance().saveNote(note);
                }
            }
        }
    }
    
    public TagData tagForGroupName(final String groupName, final boolean createIfNotFound) {
        initTags();
        // start searching under groups only
        return tagForName(groupName.isEmpty() ? NOT_GROUPED : groupName, reservedTags.get(ReservedTagName.Groups), createIfNotFound);
    }

    public TagData tagForName(final String tagName, final TagData startTag, final boolean createIfNotFound) {
        final Set<TagData> tags = tagsForNames(new HashSet<>(Arrays.asList(tagName)), startTag, createIfNotFound);
        
        if (tags.isEmpty()) {
            return null;
        } else {
            return tags.iterator().next();
        }
    }

    public Set<TagData> tagsForNames(final Set<String> tagNames, final TagData startTag, final boolean createIfNotFound) {
        initTags();
        final Set<TagData> result = new HashSet<>();
        
        final TagData realStartTag = (startTag != null) ? startTag : getRootTag();

        // flatten tagslist to set
        // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
        // https://stackoverflow.com/a/31992391
        final Set<TagData> flatTags = realStartTag.getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toSet());

        for (String tagName : tagNames) {
            final Optional<TagData> tag = flatTags.stream().filter((t) -> {
                return t.getName().equals(tagName);
            }).findFirst();
            
            if (tag.isPresent()) {
                result.add(tag.get());
            } else {
                // we didn't run into that one before...
                if (createIfNotFound) {
                    final TagData newTag = createTag(tagName, isGroupsChildTag(realStartTag));
                    realStartTag.getChildren().add(newTag);
                    result.add(newTag);
                }
            }
        }
        
        return result;
    }
    
    public boolean renameTag(final TagData tag, final String newName) {
        assert tag != null;
        
        if (tag.getName().equals(newName)) {
            // nothing to do
            return true;
        }
        // can't rename a group to ALL or NOT_GROUPED
        if (isGroupsChildTag(tag) && isSpecialGroup(tag.getName())) {
            return false;
        }

        boolean result = true;
        final String oldName = tag.getName();

        // if groupTag rename group (and with it note file) as well
        if (isGroupsChildTag(tag)) {
            final String newGroupName = newName != null ? newName : NOT_GROUPED;
            result = OwnNoteFileManager.getInstance().renameGroup(oldName, newGroupName);

            if (result) {
                //check if we just moved the current note in the editor...
                if (myEditor != null && myEditor.getNoteEditor().getEditedNote() != null && myEditor.getNoteEditor().getEditedNote().getGroupName().equals(oldName)) {
                    myEditor.getNoteEditor().doNameChange(oldName, newGroupName, 
                            myEditor.getNoteEditor().getEditedNote().getNoteName(), myEditor.getNoteEditor().getEditedNote().getNoteName());
                }
            } else {
                if (newName != null) {
                    // error message - most likely note in new group with same name already exists
                    myEditor.showAlert(Alert.AlertType.ERROR, "Error Dialog", "An error occured while renaming the group.", "A file in the new group has the same name as a file in the old.");
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    myEditor.showAlert(Alert.AlertType.ERROR, "Error Dialog", "An error occured while deleting the group.", "An ungrouped file has the same name as a file in this group.");
                }

                return false;
            }
        }
        
        final List<Note> notesList = OwnNoteFileManager.getInstance().getNotesList();
        final List<Note> changedNotes = new ArrayList<>();
        for (Note note : notesList) {
            if (note.getMetaData().getTags().contains(tag)) {
                if (!note.equals(myEditor.getEditedNote())) {
                    // read note - only if not currently in editor!
                    OwnNoteFileManager.getInstance().readNote(note, false);
                }
                changedNotes.add(note);
            }
        }
        
        // now we have loaded all notes, time to rename/remove the tag...
        if (newName == null) {
            for (Note note : changedNotes) {
                note.getMetaData().getTags().remove(tag);
            }
            // go through tag tree an delete tag
            if (tag.getParent() != null) {
                tag.getParent().getChildren().remove(tag);

                // remove all listeners from the children
                doRemoveAllListener(tag);
            }
        } else {
            tag.setName(newName);
        }
        // save the notes (except for the one currently in the editor
        for (Note note : changedNotes) {
            if (!note.equals(myEditor.getEditedNote())) {
                OwnNoteFileManager.getInstance().saveNote(note);
            } else {
                // tell the world, the note metadata has changed (implicitly)
                // so that any interesting party can listen to the change
                note.getMetaData().setUnsavedChanges(true);
            }
        }
        
        return result;
    }
    
    // helper to insert group at certain position in list
    private TagData createTagAtPosition(final String name, final boolean isGroup, final int position) {
        // does it already exist?
        TagData result = tagForName(name, null, false);
        
        if (result != null && (isGroupsChildTag(result) != isGroup)) {
            // tag is there but not what shoud be created!
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, "Trying to create tag {0} that already exists but of wrong type: {1}", new Object[]{name, !isGroup});
            return null;
        }
        if (result == null) {
            result = new TagData(name);
            // add all listeners to the children
            doAddAllListener(result);

            if (isGroup) {
                // TFE, 20210307: add it to the list as well - dummy
                if (position != -1) {
                    reservedTags.get(ReservedTagName.Groups).getChildren().add(position, result);
                }
                initGroupTag(result);
            }
        }

        return result;
    }

    protected TagData createTag(final String name, final boolean isGroup) {
        return createTagAtPosition(name, isGroup, -1);
    }
    
    public boolean deleteTag(final TagData tag) {
        assert tag != null;
        
        // reuse existing rename method
        return renameTag(tag, null);
    }
    
    private void initGroupTag(final TagData tag) {
        if (tag.getColorName() == null || tag.getColorName().isEmpty()) {
            tag.setColorName(getGroupColor(tag));
        }
    }

    // TF, 20160703: to support coloring of notes table view for individual notes
    // TF, 20170528: determine color from groupname for new colors
    private String getGroupColor(final TagData groupTag) {
        String groupColor = "";

        if (groupTag != null) {
            // for tagTree the color comes from the tag color - if any
            groupColor = groupTag.getColorName();
        }
        
        if (groupColor == null || groupColor.isEmpty()) {
            final int groupIndex = getGroupTags().indexOf(groupTag);

            // TF, 20170122: "All" & "Not grouped" have their own colors ("darkgrey", "lightgrey"), rest uses list of colors
            switch (groupIndex) {
                case 0: 
                    groupColor = "darkgrey";
                    break;
                case 1: 
                    groupColor = "lightgrey";
                    break;
                case -1:
                    // no color found via tag or group - must be new
                    groupColor = groupColors[getGroupTags().size() % groupColors.length];
                    break;
                default: 
                    groupColor = groupColors[groupIndex % groupColors.length];
                    break;
            }
//                System.out.println("Found group: " + groupTag + " as number: " + groupIndex + " color: " + groupColor);
        }
        
        return groupColor;
    }
    
    public static boolean isAnyGroupTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        return (isGroupsTag(tag) || isGroupsChildTag(tag));
    }
    
    public static boolean isGroupsTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        return TagManager.ReservedTagName.Groups.name().equals(tag.getName());
    }
    
    public static boolean isGroupsChildTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        // don't use groupTags here since the list might not yet have been updated...
        final Set<TagData> flatTags = reservedTags.get(ReservedTagName.Groups).getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toSet());

        final Optional<TagData> groupTag = flatTags.stream().filter((t) -> {
            return t.getName().equals(tag.getName());
        }).findFirst();

        return groupTag.isPresent();
    }
    
    public static boolean isEditableTag(final TagData tag) {
        // editable: currently same as fixed
        return isFixedTag(tag);
    }

    public static boolean isFixedTag(final TagData tag) {
        // fixed: "Groups", "All", "Not grouped" tags
        return reservedTags.containsValue(tag) ||
                ALL_GROUPS.equals(tag.getName()) ||
                NOT_GROUPED.equals(tag.getName());
    }
    
    public static boolean childTagsAllowed(final TagData tag) {
        // no child tags: anything below "Groups" tag
        return !isGroupsChildTag(tag);
    }

    @Override
    public boolean createNote(String newGroupName, String newNoteName) {
        final Note newNote = OwnNoteFileManager.getInstance().getNote(newGroupName, newNoteName);
        tagForGroupName(newNote.getGroupName(), false).getLinkedNotes().add(newNote);
        tagForGroupName(ALL_GROUPS, false).getLinkedNotes().add(newNote);

        return true;
    }

    @Override
    public boolean renameNote(Note curNote, String newValue) {
        return true;
    }

    @Override
    public boolean moveNote(Note curNote, String newGroupName) {
        // we have been called after the fact... so the linkedNotes have already been updated with the new group name
        final Note movedNote = OwnNoteFileManager.getInstance().getNote(newGroupName, curNote.getNoteName());

        // TFE, 20201227: allign tags and their note links as well...
        final TagData oldGroupTag = TagManager.getInstance().tagForGroupName(curNote.getGroupName(), false);
        final TagData newGroupTag = TagManager.getInstance().tagForGroupName(newGroupName, false);
        if (movedNote.getMetaData().getTags().contains(oldGroupTag)) {
            movedNote.getMetaData().getTags().remove(oldGroupTag);
            movedNote.getMetaData().getTags().add(newGroupTag);
        } else {
            // need to manually update the linkt tag <-> note - both variants, just to be sure...
            oldGroupTag.getLinkedNotes().remove(curNote);
            oldGroupTag.getLinkedNotes().remove(movedNote);
            newGroupTag.getLinkedNotes().add(movedNote);
        }

        return true;
    }

    @Override
    public boolean deleteNote(Note curNote) {
        tagForGroupName(curNote.getGroupName(), false).getLinkedNotes().remove(curNote);
        tagForGroupName(ALL_GROUPS, false).getLinkedNotes().remove(curNote);

        return true;
    }

    @Override
    public boolean saveNote(Note note) {
        return true;
    }
    
    public static Label getIconForName(final String iconName, final IconSize size) {
        Label result; 
        try {
            // TFE, 20210316: can't use GlyphsDude.createIcon since styling of text color isn't working
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.valueOf(iconName), "", size.getSize(), "0px", ContentDisplay.CENTER);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TagData.class.getName()).log(Level.SEVERE, null, ex);
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.BUG, "", size.getSize(), "0px", ContentDisplay.CENTER);
        }
        return result;
    }
    
    // ===========================================================================
    
//    public void checkLinkedNotesCount(final String tagId, final int count) {
//        Optional<TagData> tag = getRootTag().getChildren().stream().map((t) -> {
//            return t.flattened();
//        }).flatMap(Function.identity()).filter((t) -> {
//            return t.getId().equals(tagId);
//        }).findFirst();
//        
//        if (tag.isEmpty()) {
//            System.out.println("Bummer, tag with id " + tagId + " not found!");
//        } else {
//            final int size = tag.get().getLinkedNotes().size();
//            if (size != count) {
//                System.out.println("Bummer, tag with id " + tagId + " has " + size + " linked notes instead of " + count);
//            } else {
//                System.out.println("Bingo, tag with id " + tagId + " has " + count + " linked notes!");
//            }
//        }
//    }
}
