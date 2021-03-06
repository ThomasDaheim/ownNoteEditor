/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.notes;

import java.util.HashMap;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.unbescape.html.HtmlEscape;
import tf.ownnote.ui.helper.OwnNoteFileManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class Note extends HashMap<String, String> {
    // to reference the columns for notes table
    private enum NoteMapKey {
        noteName,
        noteModified,
        noteDelete,
        groupName,
        // TFE, 20200814: content as in file
        // can be used for diff in FileContentChangeType event handling
        noteFileContent,
        // TFE, 20200814: content as in editor
        noteEditorContent;
    }
    
    // TFE, 20201022: store additional metadata, e.g. tags, author, ...
    private NoteMetaData myMetaData;
    
    // TFE, 20210201: know you own change status
    private final BooleanProperty hasUnsavedNoteChanges = new SimpleBooleanProperty(false);
    // TFE, 20210219: be more user friendly
    private final BooleanProperty hasUnsavedChanges = new SimpleBooleanProperty(false);
    private BooleanBinding bindingHelper;

    private Note() {
        super();
        
        setMetaData(new NoteMetaData());
    }
    
    public Note(final String groupName, final String noteName) {
        super();
        
        setGroupName(groupName);
        setNoteName(noteName);
        setMetaData(new NoteMetaData());
    }
    
    public Note(final Note note) {
        super(note);
        
        myMetaData = note.myMetaData;
        initNoteHasChanged();
    }
    
    private void initNoteHasChanged() {
        bindingHelper = Bindings.or(hasUnsavedNoteChanges, myMetaData.hasUnsavedChangesProperty());
        hasUnsavedChanges.bind(bindingHelper);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(getGroupName()) + Objects.hashCode(getNoteName());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Note other = (Note) obj;
        if (!NoteGroup.isSameGroup(this.getGroupName(), other.getGroupName()) || !Objects.equals(this.getNoteName(), other.getNoteName())) {
            return false;
        }
        return true;
    }
    
    public static String getNoteValueName(final int i) {
        return NoteMapKey.values()[i].name();
    }
    
    public String getNoteName() {
        return get(NoteMapKey.noteName.name());
    }

    public void setNoteName(final String noteName) {
        put(NoteMapKey.noteName.name(), noteName);
    }

    public String getNoteModified() {
        return get(NoteMapKey.noteModified.name());
    }

    public void setNoteModified(final String noteModified) {
        put(NoteMapKey.noteModified.name(), noteModified);
    }

    public String getNoteDelete() {
        return get(NoteMapKey.noteDelete.name());
    }

    public void setNoteDelete(final String noteDelete) {
        put(NoteMapKey.noteDelete.name(), noteDelete);
    }

    public String getGroupName() {
        return get(NoteMapKey.groupName.name());
    }

    public void setGroupName(final String groupName) {
        put(NoteMapKey.groupName.name(), groupName);
    }

    public String getNoteFileContent() {
        return get(NoteMapKey.noteFileContent.name());
    }

    public void setNoteFileContent(final String content) {
        // TFE, 20201024: extract line with metadata - if any
        put(NoteMapKey.noteFileContent.name(), NoteMetaData.removeMetaDataContent(content));
        
        // set meta data - was done intially but might now have changed during editing
        if (NoteMetaData.hasMetaDataContent(content)) {
            setMetaData(NoteMetaData.fromHtmlComment(content));
        }
    }

    public String getNoteEditorContent() {
        return get(NoteMapKey.noteEditorContent.name());
    }

    public void setNoteEditorContent(final String content) {
        put(NoteMapKey.noteEditorContent.name(), content);
        hasUnsavedNoteChanges.set(!HtmlEscape.unescapeHtml(getNoteFileContent()).equals(HtmlEscape.unescapeHtml(getNoteEditorContent())));
    }

    public NoteMetaData getMetaData() {
        return myMetaData;
    }

    public void setMetaData(final NoteMetaData metaData) {
        myMetaData = metaData;
        
        myMetaData.setNote(this);
        initNoteHasChanged();
    }
    
    public String getNoteFileName() {
        return OwnNoteFileManager.getInstance().buildNoteName(this);
    }
    
    public BooleanProperty hasUnsavedChangesProperty() {
        return hasUnsavedChanges;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges.getValue();
    }
    public void setUnsavedChanges(final boolean changed) {
        hasUnsavedNoteChanges.setValue(changed);
        myMetaData.setUnsavedChanges(changed);
    }
}
