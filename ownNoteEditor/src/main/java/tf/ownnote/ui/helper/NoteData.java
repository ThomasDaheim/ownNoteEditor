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
package tf.ownnote.ui.helper;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class NoteData extends HashMap<String,String> {
    // to reference the columns for notes table
    private static final String[] notesMapKeys = { "noteName", "noteModified", "noteDelete", "groupName" };

    public NoteData() {
        super();
    }
    
    public NoteData(final NoteData noteData) {
        super(noteData);
    }
    
    public NoteData(final Map<String,String> noteData) {
        super(noteData);
    }
    
    public Map<String,String> getAsMap() {
        return this;
    }
    
    public static String getNoteDataName(final int i) {
        return notesMapKeys[i];
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
        return get(notesMapKeys[0]);
    }

    public void setNoteName(final String noteName) {
        put(notesMapKeys[0], noteName);
    }

    public String getNoteModified() {
        return get(notesMapKeys[1]);
    }

    public void setNoteModified(final String noteModified) {
        put(notesMapKeys[1], noteModified);
    }

    public String getNoteDelete() {
        return get(notesMapKeys[2]);
    }

    public void setNoteDelete(final String noteDelete) {
        put(notesMapKeys[2], noteDelete);
    }

    public String getGroupName() {
        return get(notesMapKeys[3]);
    }

    public void setGroupName(final String groupName) {
        put(notesMapKeys[3], groupName);
    }
}
