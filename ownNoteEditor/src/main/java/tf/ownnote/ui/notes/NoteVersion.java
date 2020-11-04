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
package tf.ownnote.ui.notes;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author thomas
 */
public class NoteVersion {
    private static final String DATA_SEP = "@";

    private String versionAuthor;
    private LocalDateTime versionDate;
    
    private NoteVersion() {
    }
    
    public NoteVersion(final String author, final LocalDateTime date) {
        versionAuthor = author;
        versionDate = date;
    }

    public String getAuthor() {
        return versionAuthor;
    }

    public void setAuthor(final String author) {
        versionAuthor = author;
    }

    public LocalDateTime getDate() {
        return versionDate;
    }

    public void setDate(final LocalDateTime date) {
       versionDate = date;
    }
    
    public static NoteVersion fromHtmlString(final String htmlString) {
        final NoteVersion result = new NoteVersion("", LocalDateTime.now());
        
        String [] data = htmlString.split(DATA_SEP);
        if (data.length == 2) {
            result.setAuthor(data[0]);
            
            try {
                result.setDate(LocalDateTime.parse(data[1], OwnNoteEditor.DATE_TIME_FORMATTER));
            } catch (DateTimeException ex) {
                Logger.getLogger(NoteVersion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }

    public static String toHtmlString(final NoteVersion data) {
        if (data == null) {
            return "";
        }

        return data.getAuthor() + DATA_SEP + OwnNoteEditor.DATE_TIME_FORMATTER.format(data.getDate());
    }
}
