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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.helper.OwnNoteFileManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class TestNoteData {
    private final Map<String, NoteGroup> groupsList = new LinkedHashMap<>();
    private final Map<String, Note> notesList = new LinkedHashMap<>();

    public TestNoteData() {
        // init groupsList for know groups - ALL & NOT_GROUPED
        NoteGroup groupRow = new NoteGroup();
        groupRow.setGroupName(NoteGroup.ALL_GROUPS);
        groupRow.setGroupDelete(null);
        groupRow.setGroupCount("0");
        groupsList.put("All", new NoteGroup(groupRow));
        
        groupRow.clear();
        groupRow.setGroupName(NoteGroup.NOT_GROUPED);
        groupRow.setGroupDelete(null);
        groupRow.setGroupCount("0");
        groupsList.put("Not grouped", new NoteGroup(groupRow));
    }
    
    public void copyTestFiles(final Path testpath) throws Throwable {
        final File notespath = new File("src/test/resources/LookAndFeel");

        final File [] files = notespath.listFiles();
        for (File htmlfile : files) {
            // get file name from path + file
            String filename = htmlfile.getName();
            // System.out.println("copying: " + filename);
            
            // create new filename
            Path notename = Paths.get(testpath.toAbsolutePath() + "/" + filename);
            // System.out.println("to: " + notename.toString());
            
            // copy
            Files.copy(htmlfile.toPath(), notename);
            
            // and now add to our internal groupsList if a htm file
            if (FilenameUtils.isExtension(htmlfile.getName(), "htm")) {
                updateLists(htmlfile);
            }
        }        
    }
    
    public boolean createTestFile(final Path testpath, final String filename) {
        boolean result = true;
        
        // create new filename
        Path notename = Paths.get(testpath.toAbsolutePath() + "/" + filename);
        
        try {
            Files.createFile(notename);
        } catch (IOException e) {
            System.err.println("already exists: " + e.getMessage());
            result = false;
        }        
        
        return result;
    }
    
    public boolean deleteTestFile(final Path testpath, final String filename) {
        boolean result = true;
        
        // create new filename
        Path notename = Paths.get(testpath.toAbsolutePath() + "/" + filename);
        
        try {
            result = Files.deleteIfExists(notename);
        } catch (IOException e) {
            System.err.println("already exists: " + e.getMessage());
            result = false;
        }        
        
        return result;
    }
    
    // same logic as done in OwnNoteFileManager::initOwnNotePath
    private void updateLists(final File file) {
        final String filename = file.getName();

        // extract info from file and fill maps accordingly
        final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(file.lastModified())).toInstant(), ZoneId.systemDefault());
                
        String noteName = "";
        String groupName = "";
        // split filename to notes & group names
        if (filename.startsWith("[")) {
            groupName = filename.substring(1, filename.indexOf("]"));
            // see pull request #44
            noteName = filename.substring(filename.indexOf("]")+2, filename.lastIndexOf("."));
        } else {
            groupName = NoteGroup.NOT_GROUPED;
            // see pull request #44
            noteName = filename.substring(0, filename.lastIndexOf("."));
        }
        
        NoteGroup groupRow = new NoteGroup();
        if (groupsList.containsKey(groupName)) {
            // group already exists - increase counter
            groupRow = groupsList.get(groupName);
            groupRow.setGroupCount(Integer.toString(Integer.valueOf(groupRow.getGroupCount())+1));
            groupsList.replace(groupName, new NoteGroup(groupRow));
        } else {                    // new group - add to list
            groupRow.clear();
            groupRow.setGroupName(groupName);
            groupRow.setGroupDelete(OwnNoteFileManager.deleteString);
            groupRow.setGroupCount("1");
            groupsList.put(groupName, new NoteGroup(groupRow));
        }
        // allways increment count for all :-)
        groupRow = groupsList.get(NoteGroup.ALL_GROUPS);
        groupRow.setGroupCount(Integer.toString(Integer.valueOf(groupRow.getGroupCount())+1));
        groupsList.replace(NoteGroup.ALL_GROUPS, new NoteGroup(groupRow));
        
        final Note noteRow = new Note(groupName, noteName);
        noteRow.setNoteModified(FormatHelper.getInstance().formatFileTime(filetime));
        noteRow.setNoteDelete(OwnNoteFileManager.deleteString);
        // use filename and not notename since duplicate note names can exist in diffeent groups
        notesList.put(filename, noteRow);
    }
    
    public Collection<NoteGroup> getGroupsList() {
        return groupsList.values();
    }
    
    public Collection<Note> getNotesList() {
        return notesList.values();
    }
    
    public int getNotesCountForGroup(final String groupName) {
        int result = 0;
        
        if (groupsList.containsKey(groupName)) {
            result = Integer.parseInt(groupsList.get(groupName).getGroupCount());
        }
        
        return result;
    }
    
    public int getNotesCountForName(final String noteName) {
        int result = 0;

        for (Note note : notesList.values()) {
            if (note.getNoteName().contains(noteName)) {
                result++;
            }
        }
        
        return result;
    }
    
    public String getCodeContent() {
        final String result = "body {\n" +
            "    margin: 0 !important;\n" +
            "    padding: 0 !important;\n" +
            "    /* border: 1px solid red; */\n" + 
            "}";
        
        return result;
    }
    
    public File getDragFile() {
        final File result = new File("src/test/resources/dummy.txt");
        
        return result;
    }
}
