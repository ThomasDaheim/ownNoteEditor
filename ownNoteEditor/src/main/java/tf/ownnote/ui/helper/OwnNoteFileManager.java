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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.GroupData;
import tf.ownnote.ui.notes.NoteData;
import tf.ownnote.ui.notes.NoteMetaData;
import tf.ownnote.ui.notes.NoteVersion;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteFileManager {
    private final static OwnNoteFileManager INSTANCE = new OwnNoteFileManager();
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor;
    
    // monitor for changes using java Watcher service
    private final OwnNoteDirectoryMonitor myDirMonitor = new OwnNoteDirectoryMonitor();

    public static final String deleteString = "";
    
    private String notesPath;
    
    private final Map<String, GroupData> groupsList = new LinkedHashMap<>();
    private final Map<String, NoteData> notesList = new LinkedHashMap<>();
    
    private OwnNoteFileManager() {
        super();

        myEditor = null;
    }

    public static OwnNoteFileManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;
        
        myDirMonitor.subscribe(editor);
    }

    // convinience to forward to OwnNoteDirectoryMonitor
    public void subscribe(final IFileChangeSubscriber subscriber) {
        myDirMonitor.subscribe(subscriber);
    }
    
    // convinience to forward to OwnNoteDirectoryMonitor
    public void unsubscribe(final IFileChangeSubscriber subscriber) {
        myDirMonitor.unsubscribe(subscriber);
    }
    
    // forward to monitor to shut down things
    public void stop() {
        myDirMonitor.stop();
    }
    
    public void initNotesPath(final String newPath) {
        assert newPath != null;
        
        notesPath = newPath;
        
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
            stream = Files.newDirectoryStream(Paths.get(this.notesPath), "*.htm");
            
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
                // TFE; 20201023: set note metadata from file content
                noteRow.setMetaData(NoteMetaData.fromHtmlString(getFirstLine(file)));
                // use filename and not notename since duplicate note names can exist in different groups
                notesList.put(filename, noteRow);
                //System.out.println("Added note " + noteName + " for group " + groupName + " from filename " + filename);
            }
        } catch (IOException | DirectoryIteratorException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // fix #14
        // monitor directory for changes
        myDirMonitor.setDirectoryToMonitor(notesPath);   
    }
    
    private String getFirstLine(final File file) {
        String result = "";
        
        try {
            // use https://stackoverflow.com/a/19486413 to quickly read first line from file
            final BufferedReader in = 
                    new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            result = in.readLine();
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }

    public String getNotesPath() {
        return notesPath;
    }
    
    public ObservableList<GroupData> getGroupsList() {
        return FXCollections.observableArrayList(groupsList.values());
    }

    public ObservableList<NoteData> getNotesList() {
        return FXCollections.observableArrayList(notesList.values());
    }
    
    public NoteData getNoteData(String groupName, String noteName) {
        if (groupName == null || noteName == null) {
            return null;
        }
        
        return getNoteData(buildNoteName(groupName, noteName));
    }
    
    public NoteData getNoteData(final String noteFileName) {
        if (noteFileName == null || noteFileName.isEmpty()) {
            return null;
        }
        
        NoteData result = null;
        
        for (Map.Entry<String, NoteData> note : notesList.entrySet()) {
            if (note.getKey().equals(noteFileName)) {
                result = note.getValue();
            }
        }
        
        return result;
    }
    
    public GroupData getGroupData(final String groupName) {
        if (groupName == null) {
            return null;
        }
        
        GroupData result = null;

        for (Map.Entry<String, GroupData> group : groupsList.entrySet()) {
            if (group.getKey().equals(groupName)) {
                result = group.getValue();
            }
        }
        
        return result;
    }
    
    public GroupData getGroupData(final NoteData noteData) {
        return getGroupData(noteData.getGroupName());
    }

    public boolean deleteNote(final NoteData noteData) {
        assert noteData != null;
        
        return deleteNote(noteData.getGroupName(), noteData.getNoteName());
    }
    
    public boolean deleteNote(final String groupName, final String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        boolean result = true;
        initFilesInProgress();
        
        final String noteFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.delete(Paths.get(notesPath, noteFileName));
            
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

    public String buildNoteName(final String groupName, final String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        return buildGroupName(groupName) + noteName + ".htm";
    }
    
    public String buildNoteName(final NoteData note) {
        assert note != null;
        
        return buildNoteName(note.getGroupName(), note.getNoteName());
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
            Path newPath = Files.createFile(Paths.get(this.notesPath, newFileName));
            
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
        
        try {
            result = new String(Files.readAllBytes(Paths.get(notesPath, buildNoteName(curNote.getGroupName(), curNote.getNoteName()))));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = "";
        }
        
        // TFE; 20200814: store content in NoteData
        curNote.setNoteFileContent(result);
        
        return result;
    }

    public boolean saveNote(final NoteData noteData) {
        assert noteData != null;
        
        boolean result = true;
        initFilesInProgress();

        final String newFileName = buildNoteName(noteData);
        
        String content = "";
        try {
            // TODO: set author
            content = noteData.getNoteEditorContent();
            if (content == null) {
                content = noteData.getNoteFileContent();
            }
            // TFE, 20201024: store note metadata
            noteData.getMetaData().addVersion(new NoteVersion(System.getProperty("user.name"), LocalDateTime.now()));
            final String fullContent = NoteMetaData.toHtmlString(noteData.getMetaData()) + content;
            
            final Path savePath = Files.write(Paths.get(this.notesPath, newFileName), fullContent.getBytes());
            
            // // TF, 20170723: update modified date of the file
            final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(savePath.toFile().lastModified())).toInstant(), ZoneId.systemDefault());
            final NoteData dataRow = notesList.get(newFileName);
            // TFE; 20200814: store content in NoteData
            dataRow.setNoteFileContent(content);
            dataRow.setNoteModified(FormatHelper.getInstance().formatFileTime(filetime));
            notesList.put(newFileName, dataRow);
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        resetFilesInProgress();

        if (result) {
            noteData.setNoteFileContent(content);
        }

        return result;
    }
    
    public boolean renameNote(final String groupName, final String oldNoteName, final String newNoteName) {
        assert groupName != null;
        assert oldNoteName != null;
        assert newNoteName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String oldFileName = buildNoteName(groupName, oldNoteName);
        final Path oldFile = Paths.get(this.notesPath, oldFileName);
        final String newFileName = buildNoteName(groupName, newNoteName);
        final Path newFile = Paths.get(this.notesPath, newFileName);
        
        // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
        // TFE, 20191211: handle the case of only changing upper/lower chars in the file name...
        // tricky under windows, e.g.: https://stackoverflow.com/a/34730781, so handle separately
        final boolean caseSensitiveRename = oldFileName.toLowerCase().equals(newFileName.toLowerCase());
        if (!caseSensitiveRename && Files.exists(newFile)) {
            result = false;
        } else {
            try {
                Files.move(oldFile, newFile, StandardCopyOption.ATOMIC_MOVE);

                final NoteData dataRow = notesList.remove(oldFileName);
                dataRow.setNoteName(newNoteName);
                notesList.put(newFileName, dataRow);
            } catch (Exception ex) {
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
        final Path oldFile = Paths.get(this.notesPath, oldFileName);
        final String newFileName = buildNoteName(newGroupName, noteName);
        final Path newFile = Paths.get(this.notesPath, newFileName);
        
        // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
        // TFE, 20191211: here we don't want to be as case insensitive as  the OS is
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
        
        final boolean caseSensitiveRename = oldGroupName.toLowerCase().equals(newGroupName.toLowerCase());

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
            notesForGroup = Files.newDirectoryStream(Paths.get(this.notesPath), escapedNoteNamePrefix + "*.htm");

            // TFE, 20191211: here we don't want to be as case insensitive as  the OS is
            // in theory we could have groups that only differ by case: TEST and Test
            // since thats not possible under Windows we will exclude it for all platforms...
            if (!caseSensitiveRename) {
                // 2. check all note names against new group name and fail if one already existing
                for (Path path: notesForGroup) {
                    final File file = path.toFile();
                    final String filename = file.getName();
                    //System.out.println("Checking " + filename);

                    final String newFileName = newNoteNamePrefix + filename.substring(oldNoteNamePrefix.length());

                    if (Files.exists(Paths.get(this.notesPath, newFileName))) {
                        result = false;
                        break;
                    }
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
                notesForGroup = Files.newDirectoryStream(Paths.get(this.notesPath), escapedNoteNamePrefix + "*.htm");

                for (Path path: notesForGroup) {
                    final File file = path.toFile();
                    final String filename = file.getName();

                    final String newFileName = newNoteNamePrefix + filename.substring(oldNoteNamePrefix.length());

                    try {
                        Files.move(Paths.get(this.notesPath, filename), Paths.get(this.notesPath, newFileName), StandardCopyOption.ATOMIC_MOVE);
                        
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
        // TF, 20170131; re-insert new group at same position as before - otherwise the coloring gets screwed up
        final GroupData oldGroupRow = groupsList.get(oldGroupName);
        final GroupData newGroupRow = groupsList.get(newGroupName);
        if (newGroupRow == null) {
            oldGroupRow.setGroupName(newGroupName);
            // we need to replace "in place" to keep coloring the same
            // so remove & put don't do the trick since they change the order
            // we have to copy over the list into a new one and replace the old entry
            final LinkedHashMap<String, GroupData> oldList = new LinkedHashMap<>(groupsList);
            groupsList.clear();
            for (Map.Entry<String, GroupData> entry : oldList.entrySet()) {
                if (!oldGroupName.equals(entry.getKey())) {
                    groupsList.put(entry.getKey(), entry.getValue());
                } else {
                    groupsList.put(newGroupName, entry.getValue());
                }
            }
        } else {
            // old map entry can simply be removed
            groupsList.remove(oldGroupName);

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

        return Files.exists(Paths.get(this.notesPath, fileName));
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
    
    public List<NoteData> getNotesWithText(final String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return notesList.values().stream().collect(Collectors.toList());
        }
        
        final List<NoteData> result = new ArrayList<>();
        
        // iterate over all file and check context for searchText
        for (Map.Entry<String, NoteData> note : notesList.entrySet()) {
            // TFE, 20201024: if we already have the note text we don't need the scanner
            if (note.getValue().getNoteFileContent() != null || note.getValue().getNoteEditorContent() != null) {
                String content = note.getValue().getNoteEditorContent();
                if (content == null) {
                    content = note.getValue().getNoteFileContent();
                }
                
                if (content.contains(searchText)) {
                    result.add(note.getValue());
                }
            } else {
                // see https://stackoverflow.com/questions/4886154/whats-the-fastest-way-to-scan-a-very-large-file-in-java/4886765#4886765 for fast algo
                final File noteFile = new File(this.notesPath, buildNoteName(note.getValue().getGroupName(), note.getValue().getNoteName()));

                try (final Scanner scanner = new Scanner(noteFile)) {
                    if (scanner.findWithinHorizon(searchText, 0) != null) {
                        result.add(note.getValue());
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return result;
    }
}
