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
package tf.ownnote.ui;

import java.io.File;
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
import tf.ownnote.ui.helper.GroupData;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteFileManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class Testdata {
    private final Map<String, GroupData> groupsList = new LinkedHashMap<>();
    private final Map<String, NoteData> notesList = new LinkedHashMap<>();

    public Testdata() {
        // init groupsList for know groups - ALL & NOT_GROUPED
        GroupData groupRow = new GroupData();
        groupRow.setGroupName(GroupData.ALL_GROUPS);
        groupRow.setGroupDelete(null);
        groupRow.setGroupCount("0");
        groupsList.put("All", new GroupData(groupRow));
        
        groupRow.clear();
        groupRow.setGroupName(GroupData.NOT_GROUPED);
        groupRow.setGroupDelete(null);
        groupRow.setGroupCount("0");
        groupsList.put("Not grouped", new GroupData(groupRow));
    }
    
    public void copyTestFiles(final Path testpath) throws Throwable {
        final File notespath = new File("src/test/resources");

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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
            groupName = GroupData.NOT_GROUPED;
            // see pull request #44
            noteName = filename.substring(0, filename.lastIndexOf("."));
        }
        
        GroupData groupRow = new GroupData();
        if (groupsList.containsKey(groupName)) {
            // group already exists - increase counter
            groupRow = groupsList.get(groupName);
            groupRow.setGroupCount(Integer.toString(Integer.valueOf(groupRow.getGroupCount())+1));
            groupsList.replace(groupName, new GroupData(groupRow));
        } else {                    // new group - add to list
            groupRow.clear();
            groupRow.setGroupName(groupName);
            groupRow.setGroupDelete(OwnNoteFileManager.deleteString);
            groupRow.setGroupCount("1");
            groupsList.put(groupName, new GroupData(groupRow));
        }
        // allways increment count for all :-)
        groupRow = groupsList.get(GroupData.ALL_GROUPS);
        groupRow.setGroupCount(Integer.toString(Integer.valueOf(groupRow.getGroupCount())+1));
        groupsList.replace(GroupData.ALL_GROUPS, new GroupData(groupRow));
        
        final NoteData noteRow = new NoteData();
        noteRow.setNoteName(noteName);
        noteRow.setNoteModified(FormatHelper.getInstance().formatFileTime(filetime));
        noteRow.setNoteDelete(OwnNoteFileManager.deleteString);
        noteRow.setGroupName(groupName);
        // use filename and not notename since duplicate note names can exist in diffeent groups
        notesList.put(filename, new NoteData(noteRow));
    }
    
    public Collection<GroupData> getGroupsList() {
        return groupsList.values();
    }
    
    public Collection<NoteData> getNotesList() {
        return notesList.values();
    }
    
    public int getNotesCountForGroup(final String groupName) {
        int result = 0;
        
        if (groupsList.containsKey(groupName)) {
            result = Integer.parseInt(groupsList.get(groupName).getGroupCount());
        }
        
        return result;
    }
}
