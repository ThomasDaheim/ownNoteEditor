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
import java.util.HashSet;
import java.util.List;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import tf.helper.general.ObjectsHelper;
import tf.helper.xstreamfx.FXConverters;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteGroup;

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

    // reserved names for tags - can't be moved or edited
    public enum ReservedTagNames {
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
    
    private final static Set<String> reservedTagNames = new HashSet<>(EnumUtils.getEnumList(ReservedTagNames.class).stream().map((t) -> {
        return t.name();
    }).collect(Collectors.toSet()));
    private final static Set<TagData> reservedTags = new HashSet<>();
    
    private final static String ROOT_TAG_NAME = "Tags";
    // root of all tags - not saved or loaded
    private final static TagData ROOT_TAG = new TagData(ROOT_TAG_NAME);
    
    // TFE, 20210330: also hold a flat set of tags with an extractor - to be able to listen to changes of tag content
    private final ObservableList<TagData> flatTags = 
            FXCollections.observableArrayList(p -> new Observable[]{p.nameProperty(), p.iconNameProperty(), p.colorNameProperty(), p.parentProperty(), p.getChildren()});
    // but then we need to keep track of changes in the tag tree to update the flattened tag list....
    private final ListChangeListener<TagData> tagChildrenListener;

    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private boolean tagsLoaded = false;
    
    private TagManager() {
        super();
        // Exists only to defeat instantiation.
        
        tagChildrenListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                boolean doFlatten = false;
                while (change.next() && !doFlatten) {
                    if (change.wasRemoved()) {
                        doFlatten = true;
                    }
                    if (change.wasAdded()) {
                        doFlatten = true;
                    }
                }
                
                if (doFlatten) {
                    removeTagChildrenListener();
                    flattenTags();
                    attachTagChildrenListener();
                }
            }
        };
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
    
    public TagData getRootTag() {
        if (!tagsLoaded) {
            // lazy loading
            loadTags();
        }
        
        // you can use but not change
        return ROOT_TAG;
    }
    
    public ObservableList<TagData> getFlatTagsList() {
        // not to be changed - only to be listened to
        return flatTags;
    }
    
    public void resetTagList() {
        ROOT_TAG.getChildren().clear();
        tagsLoaded = false;
    }
    
    public void loadTags() {
        final String fileName = OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            // load from xml AND from current metadata
            final XStream xstream = new XStream(new PureJavaReflectionProvider(), new DomDriver("ISO-8859-1"));
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
                ROOT_TAG.setChildren(ObjectsHelper.uncheckedCast(xstream.fromXML(reader)));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        tagsLoaded = true;
        
        // ensure reserved names are fixed
        for (String tagName : reservedTagNames) {
            TagData tag = tagForName(tagName, ROOT_TAG, false);
            if (tag == null) {
                tag = new TagData(tagName);
                ROOT_TAG.getChildren().add(tag);
                
                System.out.println("No tag for reserved name " + tagName + " found. Initializing...");
            }
            
            if (TagManager.ReservedTagNames.Groups.name().equals(tag.getName())) {
                boolean hasAllGroups = false;
                boolean hasNotGrouped = false;
                
                // add all groups from group names to tags - in case something new has come up...
                for (NoteGroup group : OwnNoteFileManager.getInstance().getGroupsList()) {
                    final String groupName = group.getGroupName();
                    // as usual "All" and "Not grouped" need special treatment
                    if (!NoteGroup.isSpecialGroup(groupName) && tagForName(groupName, tag, false) == null) {
                        System.out.println("No tag for group " + groupName + " found. Adding...");
                        tagForName(groupName, tag, true);
                    }
                }
                
                // color my children
                for (TagData tagChild : tag.getChildren()) {
                    final String tagChildName = tagChild.getName();
                    
                    initGroupTag(tagChild);
                    
                    if (NoteGroup.ALL_GROUPS.equals(tagChildName)) {
                        hasAllGroups = true;
                    }
                    if (NoteGroup.NOT_GROUPED.equals(tagChildName)) {
                        hasNotGrouped = true;
                    }
                }
                
                if (!hasAllGroups) {
                    // all groups is always the first entry
                    System.out.println("No tag for group " + NoteGroup.ALL_GROUPS + " found. Adding...");
                    final TagData allGroups = new TagData(NoteGroup.ALL_GROUPS);
                    allGroups.setColorName(myEditor.getGroupColor(NoteGroup.ALL_GROUPS));
                    
                    tag.getChildren().add(0, allGroups);
                }
                if (!hasNotGrouped) {
                    // all groups is always the second entry
                    System.out.println("No tag for group " + NoteGroup.NOT_GROUPED + " found. Adding...");
                    final TagData notGrouped = new TagData(NoteGroup.NOT_GROUPED);
                    notGrouped.setColorName(myEditor.getGroupColor(NoteGroup.NOT_GROUPED));
                    
                    tag.getChildren().add(1, notGrouped);
                }

                // link notes to group tags - notes might not have the tags explicitly...
                for (TagData tagChild : tag.getChildren()) {
                    tagChild.getLinkedNotes().addAll(OwnNoteFileManager.getInstance().getNotesForGroup(tagChild.getName()));
                }

            }
            
            reservedTags.add(tag);
        }

        // backlink all parents
        flattenTags();

        for (TagData tag: flatTags) {
            for (TagData childTag : tag.getChildren()) {
                childTag.setParent(tag);
            }
        }

        attachTagChildrenListener();
    }
    
    private void flattenTags() {
        // create a flat list of all tags
        flatTags.setAll(getRootTag().getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toList()));
    }
    
    private void attachTagChildrenListener() {
        getRootTag().getChildren().addListener(tagChildrenListener);
        for (TagData tag: flatTags) {
            tag.getChildren().addListener(tagChildrenListener);
        }
    }
    
    private void removeTagChildrenListener() {
        getRootTag().getChildren().removeListener(tagChildrenListener);
        for (TagData tag: flatTags) {
            tag.getChildren().removeListener(tagChildrenListener);
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
        final XStream xstream = new XStream(new DomDriver("ISO-8859-1"));
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
            if (!NoteGroup.isNotGrouped(note.getGroupName())) {
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
        return tagForName(groupName.isEmpty() ? NoteGroup.NOT_GROUPED : groupName, getRootTag(), createIfNotFound);
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
                    final TagData newTag = new TagData(tagName);
                    realStartTag.getChildren().add(newTag);
                    result.add(newTag);
                }
            }
        }
        
        return result;
    }
    
    public void renameTag(final TagData tag, final String newName) {
        assert tag != null;
        
        if (tag.getName().equals(newName)) {
            // nothing to do
            return;
        }
        final String oldName = tag.getName();

        final boolean groupTag = isGroupsChildTag(tag);
        
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
            doDeleteTag(tag);
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
        
        // if groupTag rename group (and with it note file) as well
        if (groupTag) {
            myEditor.renameGroupWrapper(oldName, newName);
        }
    }
    
    public TagData createTag(final String name, final boolean isGroup) {
        final TagData result = new TagData(name);
        if (isGroup) {
            initGroupTag(result);
            // TFE, 20210307: add it to the list as well - dummy
            OwnNoteFileManager.getInstance().createGroup(name);
        }
        return result;
    }
    
    public  void deleteTag(final TagData tag) {
        assert tag != null;
        
        // reuse existing rename method
        renameTag(tag, null);
    }
    
    private void doDeleteTag(final TagData tag) {
        // remove group as well - if it exists (= has notes)
        boolean doDelete = true;
        if (TagManager.isGroupsChildTag(tag) && OwnNoteFileManager.getInstance().getNoteGroup(tag.getName()) != null) {
            assert myEditor != null;
            doDelete = myEditor.deleteGroupWrapper(OwnNoteFileManager.getInstance().getNoteGroup(tag.getName()));
        }

        // delete for groups might fail! e.g. duplicate note names - in this case we can't delete the tag
        if (doDelete) {
            // go through tag tree an delete tag
            if (tag.getParent() != null) {
                tag.getParent().getChildren().remove(tag);
            }
        }
    }
    
    public void initGroupTag(final TagData tag) {
        if (tag.getColorName() == null || tag.getColorName().isEmpty()) {
            tag.setColorName(myEditor.getGroupColor(tag.getName()));
        }
    }
    
    public static boolean isAnyGroupTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        return (isGroupsTag(tag) || isGroupsChildTag(tag));
    }
    
    public static boolean isGroupsTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        return TagManager.ReservedTagNames.Groups.name().equals(tag.getName());
    }
    
    public static boolean isGroupsChildTag(final TagData tag) {
        // a group tag is the "Groups" itself and everything below it
        return (tag.getParent() != null) && TagManager.ReservedTagNames.Groups.name().equals(tag.getParent().getName());
    }
    
    public static boolean isEditableTag(final TagData tag) {
        // editable: currently same as fixed
        return isFixedTag(tag);
    }

    public static boolean isFixedTag(final TagData tag) {
        // fixed: "Groups", "All", "Not grouped" tags
        return reservedTags.contains(tag) ||
                NoteGroup.ALL_GROUPS.equals(tag.getName()) ||
                NoteGroup.NOT_GROUPED.equals(tag.getName());
    }
    
    public static boolean childTagsAllowed(final TagData tag) {
        // no child tags: anything below "Groups" tag
        return !isGroupsChildTag(tag);
    }

    @Override
    public boolean createNote(String newGroupName, String newNoteName) {
        final Note newNote = OwnNoteFileManager.getInstance().getNote(newGroupName, newNoteName);
        tagForGroupName(newNote.getGroupName(), false).getLinkedNotes().add(newNote);
        tagForGroupName(NoteGroup.ALL_GROUPS, false).getLinkedNotes().add(newNote);

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
        tagForGroupName(NoteGroup.ALL_GROUPS, false).getLinkedNotes().remove(curNote);

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
}
