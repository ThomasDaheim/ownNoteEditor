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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteFileManager {
    // callback to OwnNoteEditor required for e.g. delete & rename
    private final OwnNoteEditor myEditor;
    
    // monitor for changes using java Watcher service
    private final OwnNoteDirectoryMonitor myDirMonitor;

    public static final String deleteString = "";
    
    private String ownNotePath;
    
    private final Map<String, GroupData> groupsList = new LinkedHashMap<>();
    private final Map<String, NoteData> notesList = new LinkedHashMap<>();
    
    private OwnNoteFileManager() {
        super();

        myEditor = null;
        myDirMonitor = null;
    }

    public OwnNoteFileManager(final OwnNoteEditor editor) {
        super();
        
        myEditor = editor;
        myDirMonitor = new OwnNoteDirectoryMonitor(myEditor);
    }
    
    // forward to monitor to shut down things
    public void stop() {
        myDirMonitor.stop();
    }
    
    public void initOwnNotePath(final String ownNotePath) {
        assert ownNotePath != null;
        
        this.ownNotePath = ownNotePath;
        
        // scan directory for files and build groups & notes maps
        groupsList.clear();
        notesList.clear();

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
        
        // iterate over all files from directory
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(Paths.get(this.ownNotePath), "*.htm");
            
            for (Path path: stream) {
                final File file = path.toFile();
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
                //System.out.println("Added note " + noteName + " for group " + groupName + " from filename " + filename);
            }
        } catch (IOException | DirectoryIteratorException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // fix #14
        // monitor directory for changes
        myDirMonitor.setDirectoryToMonitor(ownNotePath);   
    }

    public String getOwnNotePath() {
        return ownNotePath;
    }
    
    public ObservableList<Map<String, String>> getGroupsList() {
        return FXCollections.observableArrayList(groupsList.values());
    }

    public ObservableList<Map<String, String>> getNotesList() {
        return FXCollections.observableArrayList(notesList.values());
    }

    public boolean deleteNote(String groupName, String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        boolean result = true;
        initFilesInProgress();
        
        final String noteFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.delete(Paths.get(ownNotePath, noteFileName));
            
            final GroupData groupRow = groupsList.get(groupName);
            groupRow.setGroupCount(Integer.toString(Integer.valueOf(groupRow.getGroupCount())-1));
            groupsList.replace(groupName, groupRow);
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        resetFilesInProgress();
        return result;
    }

    public String buildNoteName(String groupName, String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        return buildGroupName(groupName) + noteName + ".htm";
    }

    private String buildGroupName(String groupName) {
        assert groupName != null;
        
        String result = null;
        
        if (groupName.equals(GroupData.NOT_GROUPED) || groupName.isEmpty() || groupName.equals(GroupData.ALL_GROUPS)) {
            // only the note name
            result = "";
        } else {
            // group name upfront
            result = "[" + groupName + "] ";
        }
        
        return result;
    }

    public boolean createNewNote(final String groupName, final String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String newFileName = buildNoteName(groupName, noteName);
        
        try {
            Path newPath = Files.createFile(Paths.get(this.ownNotePath, newFileName));
            
            // TF, 20151129
            // update notesList as well
            final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(newPath.toFile().lastModified())).toInstant(), ZoneId.systemDefault());

            final NoteData noteRow = new NoteData();
            noteRow.setNoteName(noteName);
            noteRow.setNoteModified(FormatHelper.getInstance().formatFileTime(filetime));
            noteRow.setNoteDelete(OwnNoteFileManager.deleteString);
            noteRow.setGroupName(groupName);
            // use filename and not notename since duplicate note names can exist in diffeent groups
            notesList.put(newFileName, new NoteData(noteRow));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        resetFilesInProgress();
        return result;
    }

    public String readNote(final NoteData curNote) {
        assert curNote != null;
        
        String result = "";
        
        final String noteFileName = buildNoteName(curNote.getGroupName(), curNote.getNoteName());
        
        try {
            result = new String(Files.readAllBytes(Paths.get(ownNotePath, noteFileName)));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = "";
        }
        
        return result;
    }

    public boolean saveNote(final String groupName, final String noteName, final String htmlText) {
        assert groupName != null;
        assert noteName != null;
        assert htmlText != null;
        
        boolean result = true;
        initFilesInProgress();

        final String newFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.write(Paths.get(this.ownNotePath, newFileName), htmlText.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        resetFilesInProgress();
        return result;
    }
    
    public boolean renameNote(final String groupName, final String oldNoteName, final String newNoteName) {
        assert groupName != null;
        assert oldNoteName != null;
        assert newNoteName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String oldFileName = buildNoteName(groupName, oldNoteName);
        final Path oldFile = Paths.get(this.ownNotePath, oldFileName);
        final String newFileName = buildNoteName(groupName, newNoteName);
        final Path newFile = Paths.get(this.ownNotePath, newFileName);
        
        // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
        if (Files.exists(newFile)) {
            result = false;
        } else {
            try {
                Files.move(oldFile, newFile);

                final NoteData dataRow = notesList.remove(oldFileName);
                dataRow.setNoteName(newNoteName);
                notesList.put(newFileName, dataRow);
            } catch (IOException ex) {
                Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }

        resetFilesInProgress();
        return result;
    }
    
    public boolean moveNote(final String oldGroupName, final String noteName, final String newGroupName) {
        assert oldGroupName != null;
        assert noteName != null;
        assert newGroupName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String oldFileName = buildNoteName(oldGroupName, noteName);
        final Path oldFile = Paths.get(this.ownNotePath, oldFileName);
        final String newFileName = buildNoteName(newGroupName, noteName);
        final Path newFile = Paths.get(this.ownNotePath, newFileName);
        
        // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
        if (Files.exists(newFile)) {
            result = false;
        } else {
            try {
                // System.out.printf("Time %s: Added files\n", getCurrentTimeStamp());
                Files.move(oldFile, newFile);

                final NoteData dataRow = notesList.remove(oldFileName);
                dataRow.setGroupName(newGroupName);
                notesList.put(newFileName, dataRow);
            } catch (IOException ex) {
                Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }
        
        if (result) {
            // change count on groups
            final GroupData oldGroupRow = groupsList.get(oldGroupName);
            final GroupData newGroupRow = groupsList.get(newGroupName);
            oldGroupRow.setGroupCount(Integer.toString(Integer.valueOf(newGroupRow.getGroupCount())-1));
            newGroupRow.setGroupCount(Integer.toString(Integer.valueOf(newGroupRow.getGroupCount())+1));
            groupsList.replace(oldGroupName, oldGroupRow);
            groupsList.replace(newGroupName, newGroupRow);
        }
        
        resetFilesInProgress();
        return result;
    }

    public boolean createNewGroup(final String groupName) {
        assert groupName != null;
        
        boolean result = true;

        if (!groupsList.containsKey(groupName)) {
            // creating a group is only adding it to the groupsList
            final GroupData groupRow = new GroupData();
            groupRow.setGroupName(groupName);
            groupRow.setGroupDelete(OwnNoteFileManager.deleteString);
            groupRow.setGroupCount("0");
            groupsList.put(groupName, new GroupData(groupRow));
        } else {
            result = false;
        }
        
        return result;
    }

    public boolean renameGroup(final String oldGroupName, final String newGroupName) {
        assert oldGroupName != null;
        assert newGroupName != null;
        
        Boolean result = true;
        initFilesInProgress();
        
        // old and new part of note name
        final String oldNoteNamePrefix = buildGroupName(oldGroupName);
        final String newNoteNamePrefix = buildGroupName(newGroupName);

        // [ and ] are special chars in glob syntax...
        final String escapedNoteNamePrefix = oldNoteNamePrefix.replace("[", "\\[").replace("]", "\\]");
        //System.out.println("Searching for -" + oldNoteNamePrefix + "- as prefix");
        
        // renaming a group means renaming all notes in the group to the new group name BUT only if no note with same new filename already exists
        DirectoryStream<Path> notesForGroup = null;
        try {
            // 1. get all note names for group
            notesForGroup = Files.newDirectoryStream(Paths.get(this.ownNotePath), escapedNoteNamePrefix + "*.htm");

            // 2. check all note names against new group name and fail if one already existing
            for (Path path: notesForGroup) {
                final File file = path.toFile();
                final String filename = file.getName();
                //System.out.println("Checking " + filename);
                
                final String newFileName = newNoteNamePrefix + filename.substring(oldNoteNamePrefix.length());
                
                if (Files.exists(Paths.get(this.ownNotePath, newFileName))) {
                    result = false;
                    break;
                }
           }
        } catch (IOException | DirectoryIteratorException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        // 3. rename all notes
        NoteData noteRow = null;
        if (result) {
            try {
                // need to re-read since iterator can only be used once - don't ask
                // https://stackoverflow.com/questions/25089294/java-lang-illegalstateexception-iterator-already-obtained
                notesForGroup = Files.newDirectoryStream(Paths.get(this.ownNotePath), escapedNoteNamePrefix + "*.htm");

                for (Path path: notesForGroup) {
                    final File file = path.toFile();
                    final String filename = file.getName();

                    final String newFileName = newNoteNamePrefix + filename.substring(oldNoteNamePrefix.length());

                    try {
                        Files.move(Paths.get(this.ownNotePath, filename), Paths.get(this.ownNotePath, newFileName));
                        
                        // TF, 20151129
                        // update notelist as well
                        noteRow = notesList.remove(filename);
                        noteRow.setGroupName(newGroupName);
                        notesList.put(newFileName, noteRow);

                    } catch (IOException ex) {
                        // and now we're royaly screwed since we would need to do a rollback for the already renamed files...
                        Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                        result = false;
                        break;
                   }
               }
            } catch (IOException | DirectoryIteratorException ex) {
                Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }
        
        // TF, 20151129
        // 4. update grouplist as well
        // TF, 20151215: if new group already exists only increase note count!
        final GroupData oldGroupRow = groupsList.remove(oldGroupName);
        final GroupData newGroupRow = groupsList.get(newGroupName);
        if (newGroupRow == null) {
            oldGroupRow.setGroupName(newGroupName);
            groupsList.put(newGroupName, oldGroupRow);
        } else {
            newGroupRow.setGroupCount(Integer.toString(Integer.valueOf(newGroupRow.getGroupCount())+Integer.valueOf(oldGroupRow.getGroupCount())));
            groupsList.replace(newGroupName, newGroupRow);
        }

        resetFilesInProgress();
        return result;
    }

    public boolean deleteGroup(final String groupName) {
        assert groupName != null;
        
        // deleting a group is removing the group name from the note name
        return renameGroup(groupName, GroupData.NOT_GROUPED);
    }
    
    public boolean noteExists(final String groupName, final String noteName) {
        final String fileName = buildNoteName(groupName, noteName);

        return Files.exists(Paths.get(this.ownNotePath, fileName));
    }

    private void initFilesInProgress() {
        // disable watcher
        myDirMonitor.disableMonitor();
    }
    
    private void resetFilesInProgress() {
        // enable watcher
        myDirMonitor.enableMonitor();
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
}
