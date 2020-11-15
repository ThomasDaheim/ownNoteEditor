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

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class NoteData extends HashMap<String,String> {
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
    private NoteMetaData myMetaData = new NoteMetaData();

    public NoteData() {
        super();
    }
    
    public NoteData(final String groupName, final String noteName) {
        super();
        
        setGroupName(groupName);
        setNoteName(noteName);
    }
    
    public NoteData(final NoteData noteData) {
        super(noteData);
        
        myMetaData = noteData.myMetaData;
    }
    
    public static String getNoteDataName(final int i) {
        return NoteMapKey.values()[i].name();
    }
    
    public static NoteData fromString(final String groupString) {
        assert (groupString != null);
        assert (groupString.length() > 6);
        
        final NoteData data = new NoteData();
        final String[] dataStrings = groupString.substring(1, groupString.length()-1).split(", ");
        for (String mapString : dataStrings) {
            final String[] mapStrings = mapString.split("=");
            if (mapStrings.length == 2) {
                data.put( mapStrings[0], mapStrings[1] );
            } else {
                data.put( mapStrings[0], "" );
            }
        }
        return data;
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
            setMetaData(NoteMetaData.fromHtmlString(content));
        }
    }

    public String getNoteEditorContent() {
        return get(NoteMapKey.noteEditorContent.name());
    }

    public void setNoteEditorContent(final String content) {
        put(NoteMapKey.noteEditorContent.name(), content);
    }

    public NoteMetaData getMetaData() {
        return myMetaData;
    }

    public void setMetaData(final NoteMetaData metaData) {
        myMetaData = metaData;
    }
}
