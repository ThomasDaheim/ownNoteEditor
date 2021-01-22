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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tf.ownnote.ui.notes.Note;

/**
 * Helper class to find & fix common issues in note content, e.g. from previous bugs :-)
 * 
 * @author thomas
 */
public class VerifyNoteContent {
    private final static VerifyNoteContent INSTANCE = new VerifyNoteContent();
    
    private enum ContentIssues {
        REPEATED_METADATA("Repeated Metadata", "--><!--", VerifyNoteContent.getInstance()::findMatches),
        ADDITIONAL_CHECKBOX_ATTRIBUTES("Additional checkbox attributes", "<input[^>]*(?<!type|checked)=\"([^\"]*)\"[^>]*>", VerifyNoteContent.getInstance()::findMatches),
        DUPLICATE_METADATA_ID("Duplicate Metadata Id", "<!-- id=\"([^\"]*)\"", VerifyNoteContent.getInstance()::findDuplicateValues);
        
        private final String issueName;
        private final String issuePatternString;
        private final Pattern issuePattern;
        private final BiFunction<ContentIssues, String, Map<Integer, String>> issueChecker;
        
        private ContentIssues(final String name, final String pattern, final BiFunction<ContentIssues, String, Map<Integer, String>> checker) {
            issueName = name;
            issuePatternString = pattern;
            issuePattern = Pattern.compile(issuePatternString);
            issueChecker = checker;
        }
        
        @Override
        public String toString() {
            return issueName;
        }
        
        public String getIssueName() {
            return issueName;
        }
        
        public String getIssuePatternString() {
            return issuePatternString;
        }
        
        public Pattern getIssuePattern() {
            return issuePattern;
        }
        
        public boolean checkContent(final String content) {
            final Map<Integer, String> result =  issueChecker.apply(this, content);
            if (!result.isEmpty()) {
                String errorString = "";
                final Note checkNote = VerifyNoteContent.getInstance().getCheckNote();
                if (checkNote != null) {
                    errorString = checkNote.getNoteFileName() + ": ";
                }
                errorString += "Checking of " + issueName + " failed!";
                System.err.println(errorString);
                for (Map.Entry<Integer, String> entry : result.entrySet()) {
                    System.err.println("  Found " + entry.getValue() + " @" + entry.getKey());
                }
            }
            
            return result.isEmpty();
        }
    }
    
    private Note checkNote = null;
    
    private VerifyNoteContent() {
        super();
    }
        
    public static VerifyNoteContent getInstance() {
        return INSTANCE;
    }
    
    private Note getCheckNote() {
        return checkNote;
    }
    
    public boolean verifyNoteFileContent(final Note note) {
        checkNote = note;
        return doVerify(note.getNoteFileContent());
    }

    
    public boolean verifyNoteEditorContent(final Note note) {
        checkNote = note;
        return doVerify(note.getNoteEditorContent());
    }
    
    public boolean verifyNoteContent(final String content) {
        checkNote = null;
        return doVerify(content);
    }
    
    private boolean doVerify(final String content) {
        // go through all pattern and check for occurence
        for (ContentIssues issue: ContentIssues.values()) {
            if (!issue.checkContent(content)) {
                return false;
            }
        }
        
        return true;
    }
    
    private Map<Integer, String> findMatches(final ContentIssues issue, final String content) {
        final Map<Integer, String> result = new HashMap<>();
        
        // if we find the pattern, we're doomed
        final Matcher matcher = issue.getIssuePattern().matcher(content);
        while (matcher.find()) {
            result.put(matcher.start(), matcher.group());
        }
        
        return result;
    }
    
    private Map<Integer, String> findDuplicateValues(final ContentIssues issue, final String content) {
        final Map<Integer, String> result = new HashMap<>();
        
        // if we find the variable more than once, we're doomed
        final List<String> patternValues = new ArrayList<>();
        final Matcher matcher = issue.getIssuePattern().matcher(content);
        while (matcher.find()) {
            // use 0 or first group - if any
            final int groupPos = Math.min(matcher.groupCount(), 1);
            final int checkPos = matcher.start(groupPos);
            final String checkString = matcher.group(groupPos);
            if (patternValues.contains(checkString)) {
                result.put(checkPos, checkString);
            }
            patternValues.add(checkString);
        }
        
        return result;
    }
}
