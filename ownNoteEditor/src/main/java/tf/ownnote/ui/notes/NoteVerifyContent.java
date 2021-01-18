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

/**
 * Helper class to find & fix common issues in note content, e.g. from previous bugs :-)
 * 
 * @author thomas
 */
public class NoteVerifyContent {
    private final static NoteVerifyContent INSTANCE = new NoteVerifyContent();
    
    private enum ContentIssues {
        REPEATED_METADATA("Repeated Metadata", "--><!--"),
        ADDITIONAL_CHECKBOX_ATTRIBUTES("Additional checkbox attributes", "<input name=\"zutat\" type=\"checkbox\" value=\"salami\" checked=\"checked\">");
        
        private final String issueName;
        private final String issuePattern;
        
        private ContentIssues(final String name, final String pattern) {
            issueName = name;
            issuePattern = pattern;
        }
        
        @Override
        public String toString() {
            return issueName;
        }
        
        public String getIssueName() {
            return issueName;
        }
        
        public String getIssuePattern() {
            return issuePattern;
        }
    }
    
    private NoteVerifyContent() {
        super();
    }
        
    public static NoteVerifyContent getInstance() {
        return INSTANCE;
    }
    
    public boolean verifyNoteFileContent(final Note note) {
        return verifyNoteContent(note.getNoteFileContent());
    }

    
    public boolean verifyNoteEditorContent(final Note note) {
        return verifyNoteContent(note.getNoteEditorContent());
    }
    
    public boolean verifyNoteContent(final String content) {
        boolean result = true;
        
        // TODO: go through all pattern and check for occurence
        
        return result;
    }
}
