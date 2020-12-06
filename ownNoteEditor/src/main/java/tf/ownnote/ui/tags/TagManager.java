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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import tf.helper.general.ObjectsHelper;
import tf.helper.xstreamfx.FXConverters;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;

/**
 * Load & save tag info to XML stored along with notes.
 * // https://nullbeans.com/configuring-xstream-to-convert-java-objects-to-and-from-xml/
 * 
 * @author thomas
 */
public class TagManager implements IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static TagManager INSTANCE = new TagManager();
    
    private final static String TAG_FILE = File.separator + "MetaData" + File.separator + "tag_info.xml";
    
    private ObservableList<TagInfo> tagList = null;
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
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
    
    public ObservableList<TagInfo> getTagList() {
        if (tagList == null) {
            tagList = FXCollections.<TagInfo>observableArrayList();
            // lazy loading
            loadTags();
        }
        
        // you can use but not change
        return tagList;
    }
    
    public void resetTagList() {
        tagList = null;
    }
    
    public void loadTags() {
        final String fileName = OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            // load from xml AND from current metadata
            final XStream xstream = new XStream(new DomDriver("ISO-8859-1"));
            xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
            XStream.setupDefaultSecurity(xstream);
            final Class<?>[] classes = new Class[] { 
                TagInfo.class, 
                ObservableListWrapper.class, 
                ObservableSetWrapper.class, 
                SimpleBooleanProperty.class, 
                SimpleStringProperty.class };
            xstream.allowTypes(classes);

            FXConverters.configure(xstream);

            xstream.alias("taginfo", TagInfo.class);
            xstream.alias("listProperty", ObservableListWrapper.class);
            xstream.alias("setProperty", ObservableSetWrapper.class);
            xstream.alias("booleanProperty", SimpleBooleanProperty.class);
            xstream.alias("stringProperty", SimpleStringProperty.class);

            try (
                BufferedInputStream stdin = new BufferedInputStream(new FileInputStream(fileName));
                Reader reader = new InputStreamReader(stdin, "ISO-8859-1");
            ) {
                tagList = ObjectsHelper.uncheckedCast(xstream.fromXML(reader));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // xstream.fromXML can return null without an exception...
        if (tagList == null) {
            tagList = FXCollections.<TagInfo>observableArrayList();
        }

        // current metadata tags
//        final List<Note> noteList = OwnNoteFileManager.getInstance().getNotesList();
//        final Set<String> noteTags = new HashSet<>();
//        for (Note note : noteList) {
//            noteTags.addAll(note.getMetaData().getTags());
//        }
        
//        // add if not yet in list
//        // flatten tagslist to set
//        // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
//        // https://stackoverflow.com/a/31992391
//        final Set<String> tagNames = tagList.stream().map((t) -> {
//            return t.flattened().map(TagInfo::getName);
//        }).flatMap(Function.identity()).collect(Collectors.toSet());
//        
//        for (String noteTag : noteTags) {
//            if (!tagNames.contains(noteTag)) {
//                tagList.add(new TagInfo(noteTag));
//            }
//        }
    }

    public void saveTags() {
        if (tagList == null) {
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
            SimpleStringProperty.class };
        xstream.allowTypes(classes);

        FXConverters.configure(xstream);

        xstream.alias("taginfo", TagInfo.class);
        xstream.alias("listProperty", ObservableListWrapper.class);
        xstream.alias("setProperty", ObservableSetWrapper.class);
        xstream.alias("booleanProperty", SimpleBooleanProperty.class);
        xstream.alias("stringProperty", SimpleStringProperty.class);

        try (
            BufferedOutputStream stdout = new BufferedOutputStream(new FileOutputStream(OwnNoteFileManager.getInstance().getNotesPath() + TAG_FILE));
            Writer writer = new OutputStreamWriter(stdout, "ISO-8859-1");
        ) {
            PrettyPrintWriter printer = new PrettyPrintWriter(writer, new char[]{'\t'});
            writer.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + System.getProperty("line.separator"));
        
            xstream.marshal(tagList, printer);
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
                if (!note.getMetaData().getTags().contains(groupInfo)) {
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

        // make sure with the set...
        getTagList();
        
        // flatten tagslist to set
        // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
        // https://stackoverflow.com/a/31992391
        final Set<TagInfo> flatTags = tagList.stream().map((t) -> {
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
                tagList.add(newTag);
                result.add(newTag);
            }
        }
        
        return result;
    }
}
