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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.apache.commons.lang3.EnumUtils;
import tf.helper.general.ObjectsHelper;
import tf.helper.xstreamfx.FXConverters;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteGroup;

/**
 * Load & save tag info to XML stored along with notes.
 * // https://nullbeans.com/configuring-xstream-to-convert-java-objects-to-and-from-xml/
 * 
 * @author thomas
 */
public class TagManager implements IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static TagManager INSTANCE = new TagManager();
    
    private final static String TAG_FILE = File.separator + "MetaData" + File.separator + "tag_info.xml";
    
    // reserved names for tags - can't be moved or edited
    public enum ReservedTagNames {
        Groups
    }
    
    private final static Set<String> reservedTagNames = new HashSet<>(EnumUtils.getEnumList(ReservedTagNames.class).stream().map((t) -> {
        return t.name();
    }).collect(Collectors.toSet()));
    private final static Set<TagInfo> reservedTags = new HashSet<>();
    
    private final static String ROOT_TAG_NAME = "Tags";
    // root of all tags - not saved or loaded
    private final static TagInfo ROOT_TAG = new TagInfo(ROOT_TAG_NAME);
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private boolean tagsLoaded = false;
    
    private TagManager() {
        super();
        // Exists only to defeat instantiation.
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
    
    public TagInfo getRootTag() {
        if (!tagsLoaded) {
            // lazy loading
            loadTags();
        }
        
        // you can use but not change
        return ROOT_TAG;
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
                TagInfo.class, 
                ObservableListWrapper.class, 
                ObservableSetWrapper.class, 
                SimpleBooleanProperty.class, 
                SimpleStringProperty.class,
                SimpleObjectProperty.class};
            xstream.allowTypes(classes);

            FXConverters.configure(xstream);

            xstream.alias("taginfo", TagInfo.class);
            xstream.alias("listProperty", ObservableListWrapper.class);
            xstream.alias("setProperty", ObservableSetWrapper.class);
            xstream.alias("booleanProperty", SimpleBooleanProperty.class);
            xstream.alias("stringProperty", SimpleStringProperty.class);
            xstream.alias("objectProperty", SimpleObjectProperty.class);

            xstream.aliasField("name", TagInfo.class, "nameProperty");
            xstream.aliasField("iconName", TagInfo.class, "iconNameProperty");
            xstream.aliasField("colorName", TagInfo.class, "colorNameProperty");
            
            xstream.omitField(TagInfo.class, "linkedNotes");
            xstream.omitField(TagInfo.class, "parentProperty");
            // TFE, 20201220: we had that in for a while
            xstream.omitField(TagInfo.class, "fixedProperty");

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
            TagInfo tag = tagForName(tagName);
            if (tag == null) {
                tag = new TagInfo(tagName);
                ROOT_TAG.getChildren().add(tag);
                
                System.out.println("No tag for reserved name " + tagName + " found. Initializing...");
            }
            
            if (TagManager.ReservedTagNames.Groups.name().equals(tag.getName())) {
                boolean hasAllGroups = false;
                boolean hasNotGrouped = false;
                
                // color my children
                for (TagInfo tagChild : tag.getChildren()) {
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
                    final TagInfo allGroups = new TagInfo(NoteGroup.ALL_GROUPS);
                    allGroups.setColorName(OwnNoteEditor.getGroupColor(NoteGroup.ALL_GROUPS));
                    
                    tag.getChildren().add(0, allGroups);
                }
                if (!hasNotGrouped) {
                    // all groups is always the second entry
                    final TagInfo notGrouped = new TagInfo(NoteGroup.NOT_GROUPED);
                    notGrouped.setColorName(OwnNoteEditor.getGroupColor(NoteGroup.NOT_GROUPED));
                    
                    tag.getChildren().add(1, notGrouped);
                }
            }
            
            reservedTags.add(tag);
        }

        // backlink all parents
        final Set<TagInfo> flatTags = getRootTag().getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toSet());

        for (TagInfo tag: flatTags) {
            for (TagInfo childTag : tag.getChildren()) {
                childTag.setParent(tag);
            }
        }
    }

    public void saveTags() {
        if (ROOT_TAG.getChildren().isEmpty()) {
            // list hasn't been initialized yet - so don't try to save
            return;
        }
        
        // save to xml
        final XStream xstream = new XStream(new DomDriver("ISO-8859-1"));
        xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
        XStream.setupDefaultSecurity(xstream);
        final Class<?>[] classes = new Class[] { 
            TagInfo.class, 
            ObservableListWrapper.class, 
            ObservableSetWrapper.class, 
            SimpleBooleanProperty.class, 
            SimpleStringProperty.class,
            SimpleObjectProperty.class};
        xstream.allowTypes(classes);

        FXConverters.configure(xstream);

        xstream.alias("taginfo", TagInfo.class);
        xstream.alias("listProperty", ObservableListWrapper.class);
        xstream.alias("setProperty", ObservableSetWrapper.class);
        xstream.alias("booleanProperty", SimpleBooleanProperty.class);
        xstream.alias("stringProperty", SimpleStringProperty.class);
        xstream.alias("objectProperty", SimpleObjectProperty.class);

        xstream.aliasField("name", TagInfo.class, "nameProperty");
        xstream.aliasField("iconName", TagInfo.class, "iconNameProperty");
        xstream.aliasField("colorName", TagInfo.class, "colorNameProperty");
        
        xstream.omitField(TagInfo.class, "linkedNotes");
        xstream.omitField(TagInfo.class, "parentProperty");

        try (
            BufferedOutputStream stdout = new BufferedOutputStream(new FileOutputStream(OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE));
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
        // probaly only used once for "migration" to tag metadata
        for (Note note : OwnNoteFileManager.getInstance().getNotesList()) {
            if (!note.getGroupName().isEmpty()) {
                final TagInfo groupInfo = tagForName(note.getGroupName());
                if (note.getMetaData().getTags().contains(groupInfo)) {
                    System.out.println("Removing tag " + note.getGroupName() + " from note " + note.getNoteName());
                    OwnNoteFileManager.getInstance().readNote(note);
                    note.getMetaData().getTags().remove(groupInfo);
                    OwnNoteFileManager.getInstance().saveNote(note);
                } else {
                    System.out.println("Adding tag " + note.getGroupName() + " to note " + note.getNoteName());
                    OwnNoteFileManager.getInstance().readNote(note);
                    note.getMetaData().getTags().add(groupInfo);
                    OwnNoteFileManager.getInstance().saveNote(note);
                }
            }
        }
    }
    
    public TagInfo tagForName(final String tagName) {
        return tagsForNames(new HashSet<>(Arrays.asList(tagName))).iterator().next();
    }

    public Set<TagInfo> tagsForNames(final Set<String> tagNames) {
        final Set<TagInfo> result = new HashSet<>();

        // flatten tagslist to set
        // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
        // https://stackoverflow.com/a/31992391
        final Set<TagInfo> flatTags = getRootTag().getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toSet());

        for (String tagName : tagNames) {
            final Optional<TagInfo> tag = flatTags.stream().filter((t) -> {
                return t.getName().equals(tagName);
            }).findFirst();
            
            if (tag.isPresent()) {
                result.add(tag.get());
            } else {
                // we didn't run into that one before...
                final TagInfo newTag = new TagInfo(tagName);
                getRootTag().getChildren().add(newTag);
                result.add(newTag);
            }
        }
        
        return result;
    }
    
    public void doRenameTag(final String oldName, final String newName) {
        assert oldName != null;
        
        final TagInfo oldTag = TagManager.getInstance().tagForName(oldName);
        final boolean groupTag = isGroupsChildTag(oldTag);
        
        final List<Note> notesList = OwnNoteFileManager.getInstance().getNotesList();
        for (Note note : notesList) {
            if (note.getMetaData().getTags().contains(oldTag)) {
                final boolean inEditor = note.equals(myEditor.getEditedNote());
                if (!inEditor) {
                    // read note - only if not currently in editor!
                    OwnNoteFileManager.getInstance().readNote(note);
                }
                
                note.getMetaData().getTags().remove(oldTag);
                if (newName != null) {
                    note.getMetaData().getTags().add(TagManager.getInstance().tagForName(newName));
                }

                if (!inEditor) {
                    // save new metadata - only if not currently in editor!
                    OwnNoteFileManager.getInstance().saveNote(note);
                }
            }
        }
        
        // if groupTag rename group (and with it note file) as well
        if (groupTag) {
            myEditor.renameGroupWrapper(oldName, newName);
        }
    }
    
    public TagInfo createTag(final String name, final boolean isGroup) {
        final TagInfo result = new TagInfo(name);
        if (isGroup) {
            TagManager.initGroupTag(result);
        }
        return result;
    }
    
    public void deleteTag(final TagInfo tag) {
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
    
    public static void initGroupTag(final TagInfo tag) {
        tag.setColorName(OwnNoteEditor.getGroupColor(tag.getName()));
    }
    
    public static boolean isAnyGroupTag(final TagInfo tag) {
        // a group tag is the "Groups" itself and everything below it
        return (isGroupsTag(tag) || isGroupsChildTag(tag));
    }
    
    public static boolean isGroupsTag(final TagInfo tag) {
        // a group tag is the "Groups" itself and everything below it
        return TagManager.ReservedTagNames.Groups.name().equals(tag.getName());
    }
    
    public static boolean isGroupsChildTag(final TagInfo tag) {
        // a group tag is the "Groups" itself and everything below it
        return (tag.getParent() != null) && TagManager.ReservedTagNames.Groups.name().equals(tag.getParent().getName());
    }
    
    public static boolean isEditableTag(final TagInfo tag) {
        // editable: currently same as fixed
        return isFixedTag(tag);
    }

    public static boolean isFixedTag(final TagInfo tag) {
        // fixed: "Groups", "All", "Not grouped" tags
        return reservedTags.contains(tag) ||
                NoteGroup.ALL_GROUPS.equals(tag.getName()) ||
                NoteGroup.NOT_GROUPED.equals(tag.getName());
    }
    
    public static boolean childTagsAllowed(final TagInfo tag) {
        // no child tags: anything below "Groups" tag
        return !isGroupsChildTag(tag);
    }
}
