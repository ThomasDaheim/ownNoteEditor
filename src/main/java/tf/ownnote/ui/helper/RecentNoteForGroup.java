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
package tf.ownnote.ui.helper;

import java.util.HashMap;
import java.util.stream.Collectors;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagManager;

/**
 * Map that keeps track of the most recent edited note for each group.
 * 
 * Supports storing to / reading from preferences.
 * 
 * @author thomas
 */
public class RecentNoteForGroup extends HashMap<String, Note> {
    private final static RecentNoteForGroup INSTANCE = new RecentNoteForGroup();
    
    private RecentNoteForGroup() {
        super();
    }
    
    public static RecentNoteForGroup getInstance() {
        return INSTANCE;
    }

    public String toPreferenceString() {
        return EditorPreferences.PREF_STRING_PREFIX + 
                entrySet().stream().map((t) -> {
                    if (t.getValue() != null) {
                        return t.getKey() + EditorPreferences.PREF_DATA_SEP + t.getValue().getNoteName();
                    } else {
                        return null;
                    }
                }).collect( Collectors.joining(EditorPreferences.PREF_STRING_SEP)) +
                EditorPreferences.PREF_STRING_SUFFIX;
    }
    
    public void fromPreferenceString(final String prefString) {
        // not long enough to be a valid preference string
        if (prefString.length() < (EditorPreferences.PREF_STRING_PREFIX + EditorPreferences.PREF_STRING_SUFFIX).length()) {
            return;
        }
        if (!prefString.startsWith(EditorPreferences.PREF_STRING_PREFIX)) {
            return;
        }
        if (!prefString.endsWith(EditorPreferences.PREF_STRING_SUFFIX)) {
            return;
        }
        
        // list of key - value pairs (as single string) for the map
        final String [] prefs = prefString.substring(EditorPreferences.PREF_STRING_PREFIX.length(), prefString.length()-EditorPreferences.PREF_STRING_SUFFIX.length()).
                    strip().split(EditorPreferences.PREF_STRING_SEP); 
        
        for (String pref : prefs) {
            // no two elements in preference string
            if (pref.split(EditorPreferences.PREF_DATA_SEP).length != 2) {
                continue;
            }
            
            final String[] recentNote = pref.split(EditorPreferences.PREF_DATA_SEP);
            put(recentNote[0], FileManager.getInstance().getNote(TagManager.getInstance().groupForExternalName(recentNote[0], false), recentNote[1]));
        }
    }
}
