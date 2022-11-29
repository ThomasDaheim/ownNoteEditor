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
package tf.ownnote.ui.links;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import javafx.application.Platform;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagData;

/**
 * Manager for processes involving note links.
 * 
 * @author thomas
 */
public class LinkManager implements INoteCRMDS, IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static LinkManager INSTANCE = new LinkManager();

    private boolean inFileChange = false;
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private LinkManager() {
        super();
    }

    public static LinkManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        // now we can register everywhere
        OwnNoteFileManager.getInstance().subscribe(INSTANCE);
        myEditor.getNoteEditor().subscribe(INSTANCE);
    }
    
    private boolean updateExistingLinks(final String oldNoteName, final String newNoteName) {
        // TODO update existing links

        return true;
    }

    private boolean invalidateExistingLinks(final String noteName) {
        // TODO invalidate existing links

        return true;
    }

    @Override
    public boolean createNote(TagData newGroup, String newNoteName) {
        // nothing to do
        return true;
    }

    @Override
    public boolean renameNote(Note curNote, String newValue) {
        return updateExistingLinks(curNote.getNoteFileName(), newValue);
    }

    @Override
    public boolean moveNote(Note curNote, TagData newGroup) {
        // what is the name of the note after moving to the new group?
        final String newValue = OwnNoteFileManager.getInstance().buildNoteName(newGroup, curNote.getNoteName());

        return updateExistingLinks(curNote.getNoteFileName(), newValue);
    }

    @Override
    public boolean deleteNote(Note curNote) {
        // TODO invalidate existing links
        return invalidateExistingLinks(curNote.getNoteFileName());
    }

    @Override
    public boolean saveNote(Note note) {
        // nothing to do
        return true;
    }

    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        if (inFileChange) {
            return true;
        }

        final Note curNote = myEditor.getEditedNote();
        if (curNote != null && OwnNoteFileManager.getInstance().buildNoteName(curNote).equals(filePath.getFileName().toString())) {
            return true;
        }
        
        Platform.runLater(() -> {
            inFileChange = true;
            // only act for files not currently shown - that will come via FileContentChange...
            if (StandardWatchEventKinds.ENTRY_DELETE.equals(eventKind) || StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                // file deleted -> remove any links
                invalidateExistingLinks(filePath.getFileName().toString());
            }
            // modify is delete + add :-)

            inFileChange = false;
        });
        
        return true;
    }

    @Override
    public boolean processFileContentChange(FileContentChangeType changeType, Note note, String oldContent, String newContent) {
        // TODO not sure what we would need to do here?
        return true;
    }
}
