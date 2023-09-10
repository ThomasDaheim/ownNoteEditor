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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.unbescape.html.HtmlEscape;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.FileManager;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.links.LinkManager;
import tf.ownnote.ui.tags.TagData;

/**
 * Base class for a note shown in the editor. 
 * 
 * It holds the basic information of a note:
 *
 * name
 * group
 * modified date
 * file content
 * current editor content
 * 
 * Add. data (as stored in the notes comment section) is stored as NoteMetaData.
 * 
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class Note {
    // TFE, 20220412: good bye, my map... welcome properties!
    private final StringProperty noteNameProperty = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> noteModifiedProperty = new SimpleObjectProperty<>();
    private final StringProperty noteFileContentProperty = new SimpleStringProperty();
    private final StringProperty noteEditorContentProperty = new SimpleStringProperty();
    private final ObjectProperty<TagData> groupProperty = new SimpleObjectProperty<>();
    
    // TFE, 20201022: store additional metadata, e.g. tags, author, ...
    private NoteMetaData myMetaData;
    
    // TFE, 20210201: know you own change status
    private final BooleanProperty hasUnsavedNoteChangesProperty = new SimpleBooleanProperty(false);
    // TFE, 20210219: be more user friendly
    private final BooleanProperty hasUnsavedChangesProperty = new SimpleBooleanProperty(false);
    private BooleanBinding bindingHelper;

    private Note() {
        super();
        
        setMetaData(new NoteMetaData(this));
    }
    
    public Note(final TagData group, final String noteName) {
        super();
        
        setGroup(group);
        setNoteName(noteName);
        setMetaData(new NoteMetaData(this));
    }
    
    protected Note(final String groupName, final String noteName) {
        super();
        
        setNoteName(noteName);
        setMetaData(new NoteMetaData(this));
    }
    
    public Note(final Note note) {
        super();
        
        // TFE, 20220427
        // no map anymore - need to clone attributes manually...
        noteNameProperty.set(note.noteNameProperty.get());
        noteModifiedProperty.set(LocalDateTime.now());
        noteFileContentProperty.set(note.noteFileContentProperty.get());
        noteEditorContentProperty.set(note.noteEditorContentProperty.get());
        groupProperty.set(note.groupProperty.get());
        
        myMetaData = note.myMetaData;
        initNoteHasChanged();
    }
    
    private void initNoteHasChanged() {
        bindingHelper = Bindings.or(hasUnsavedNoteChangesProperty, myMetaData.hasUnsavedChangesProperty());
        hasUnsavedChangesProperty.bind(bindingHelper);
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
        return (Objects.equals(this.getGroupName(), other.getGroupName()) && Objects.equals(this.getNoteName(), other.getNoteName()));
    }
    
    public String getNoteName() {
        return noteNameProperty.get();
    }

    public void setNoteName(final String noteName) {
        noteNameProperty.set(noteName);
    }
    
    public StringProperty noteNameProperty() {
        return noteNameProperty;
    }

    public LocalDateTime getNoteModified() {
        return noteModifiedProperty.get();
    }
    
    public String getNoteModifiedFormatted() {
        return FormatHelper.getInstance().formatFileTime(getNoteModified());
    }

    public void setNoteModified(final LocalDateTime noteModified) {
        noteModifiedProperty.set(noteModified);
    }
    
    public ObjectProperty<LocalDateTime> noteModifiedProperty() {
        return noteModifiedProperty;
    }
    
    public TagData getGroup() {
        return groupProperty.get();
    }
    
    public void setGroup(final TagData group) {
        groupProperty.set(group);
    }
    
    public ObjectProperty<TagData> groupProperty() {
        return groupProperty;
    }

    public String getGroupName() {
        return getGroup().getName();
    }

    public String getNoteFileContent() {
        return noteFileContentProperty.get();
    }

    public void setNoteFileContent(final String content) {
        if (NoteMetaData.hasMetaDataContent(content)) {
            final Set<Note> linkedNotes = myMetaData.getLinkedNotes();
            final Set<Note> linkingNotes = myMetaData.getLinkingNotes();

            setMetaDataFromHtmlComment(content);
            
            myMetaData.setLinkedNotes(linkedNotes);
            myMetaData.setLinkingNotes(linkingNotes);
        }

        final String oldContent = getNoteFileContent();
        final String newContent = NoteMetaData.removeMetaDataContent(content);
        
        if (oldContent == null || !oldContent.equals(newContent)) {
            // only set in case of changes
            noteFileContentProperty.set(newContent);

            // TODO: call notefilecontentchange listeners properly in case of changes
            // TFE, 20230110: links need to be initialized in any case explicitly here until above TODO is done
            // YES, I know that this is an ugly hack...
            LinkManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, this, oldContent, newContent);
        }
        
    }
    
    public StringProperty noteFileContentProperty() {
        return noteFileContentProperty;
    }

    public String getNoteEditorContent() {
        return noteEditorContentProperty.get();
    }

    public void setNoteEditorContent(final String content) {
        noteEditorContentProperty.set(content);
        
        if (getNoteFileContent() != null && getNoteEditorContent() != null) {
            hasUnsavedNoteChangesProperty.set(!HtmlEscape.unescapeHtml(getNoteFileContent()).equals(HtmlEscape.unescapeHtml(getNoteEditorContent())));
        } else {
            hasUnsavedNoteChangesProperty.set((getNoteFileContent() != null) || (getNoteEditorContent() != null));
        }
    }
    
    public StringProperty noteEditorContentProperty() {
        return noteEditorContentProperty;
    }

    public NoteMetaData getMetaData() {
        return myMetaData;
    }

    public String getMetaDataAsHtmlComment() {
        return NoteMetaData.toHtmlComment(myMetaData);
    }

    private void setMetaData(final NoteMetaData metaData) {
        myMetaData = metaData;
        
        myMetaData.setNote(this);
        initNoteHasChanged();
    }
    
    public void setMetaDataFromHtmlComment(final String comment) {
        setMetaData(NoteMetaData.fromHtmlComment(this, comment));
    }
    
    public String getNoteFileName() {
        return FileManager.getInstance().buildNoteName(this);
    }
    
    public BooleanProperty hasUnsavedChangesProperty() {
        return hasUnsavedChangesProperty;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChangesProperty.getValue();
    }
    public void setUnsavedChanges(final boolean changed) {
        hasUnsavedNoteChangesProperty.setValue(changed);
        myMetaData.setUnsavedChanges(changed);
    }
}
