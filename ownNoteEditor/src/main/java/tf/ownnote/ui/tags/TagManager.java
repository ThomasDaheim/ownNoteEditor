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

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.NoteData;

/**
 * Load & save tag info to XML stored along with notes.
 * // https://nullbeans.com/configuring-xstream-to-convert-java-objects-to-and-from-xml/
 * 
 * @author thomas
 */
public class TagManager implements IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static TagManager INSTANCE = new TagManager();
    
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
            tagList = FXCollections.observableArrayList();
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
        // load from xml AND from current metadata

        // current metadata tags
        final List<NoteData> noteList = OwnNoteFileManager.getInstance().getNotesList();
        final Set<String> noteTags = new HashSet<>();
        for (NoteData note : noteList) {
            noteTags.addAll(note.getMetaData().getTags());
        }
        
        // add if not yet in list
        // flatten tagslist to set
        // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
        // https://stackoverflow.com/a/31992391
        final Set<String> tagNames = tagList.stream().map((t) -> {
            return t.flattened().map(TagInfo::getName);
        }).flatMap(Function.identity()).collect(Collectors.toSet());
        
        for (String noteTag : noteTags) {
            if (!tagNames.contains(noteTag)) {
                tagList.add(new TagInfo(noteTag));
            }
        }
    }

    public void saveTags() {
        // save to xml
    }

    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final NoteData note, final String oldContent, final String newContent) {
        return true;
    }
}
