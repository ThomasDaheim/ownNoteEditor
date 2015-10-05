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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteFileManager {
    // to reference the columns for notes table
    public static final String[] notesMapKeys = { "noteName", "noteModified", "noteDelete", "groupName" };

    // to reference the columns for groups table
    public static final String[] groupsMapKeys = { "groupName", "groupDelete", "groupCount" };
    
    public static final String ALL_GROUPS = "All";
    public static final String NOT_GROUPED = "Not grouped";
    public static final String NEW_GROUP = "New group";
    
    private static final String deleteString = "";
    
    private String ownNotePath;
    
    private final Map<String, Map<String, String>> groupsList = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> notesList = new LinkedHashMap<>();
    
    public static String[] getNotesMapKeys() {
        return notesMapKeys;
    }
    public static String getNotesMapKey(final int i) {
        return notesMapKeys[i];
    }

    public static String[] getGroupsMapKeys() {
        return groupsMapKeys;
    }
    public static String getGroupsMapKey(final int i) {
        return groupsMapKeys[i];
    }
    
    public void initOwnNotePath(final String ownNotePath) {
        assert ownNotePath != null;
        
        this.ownNotePath = ownNotePath;
        
        // scan directory for files and build groups & notes maps
        groupsList.clear();
        notesList.clear();

        Map<String, String> dataRow = new HashMap<String, String>();
        dataRow.put(groupsMapKeys[0], ALL_GROUPS);
        dataRow.put(groupsMapKeys[1], null);
        dataRow.put(groupsMapKeys[2], "0");
        groupsList.put("All", new HashMap<String, String>(dataRow));
        
        dataRow.clear();
        dataRow.put(groupsMapKeys[0], NOT_GROUPED);
        dataRow.put(groupsMapKeys[1], null);
        dataRow.put(groupsMapKeys[2], "0");
        groupsList.put("Not grouped", new HashMap<String, String>(dataRow));
        
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
                    noteName = filename.substring(filename.indexOf("]")+2, filename.indexOf("."));
                } else {
                    groupName = NOT_GROUPED;
                    noteName = filename.substring(0, filename.indexOf("."));
                }

                if (groupsList.containsKey(groupName)) {
                    // group already exists - increase counter
                    dataRow = groupsList.get(groupName);
                    dataRow.put(groupsMapKeys[2], new Integer(Integer.valueOf(dataRow.get(groupsMapKeys[2]))+1).toString());
                    groupsList.replace(groupName, new HashMap<String, String>(dataRow));
                } else {
                    // new group - add to list
                    dataRow.clear();
                    dataRow.put(groupsMapKeys[0], groupName);
                    dataRow.put(groupsMapKeys[1], OwnNoteFileManager.deleteString);
                    dataRow.put(groupsMapKeys[2], "1");
                    groupsList.put(groupName, new HashMap<String, String>(dataRow));
                }
                // allways increment count for all :-)
                dataRow = groupsList.get(ALL_GROUPS);
                dataRow.put(groupsMapKeys[2], new Integer(Integer.valueOf(dataRow.get(groupsMapKeys[2]))+1).toString());
                groupsList.replace(ALL_GROUPS, new HashMap<String, String>(dataRow));

                dataRow.clear();
                dataRow.put(notesMapKeys[0], noteName);
                // TODO: format properly
                dataRow.put(notesMapKeys[1], formatFileTime(filetime));
                dataRow.put(notesMapKeys[2], OwnNoteFileManager.deleteString);
                dataRow.put(notesMapKeys[3], groupName);
                // use filename and not notename since duplicate note names can exist in diffeent groups
                notesList.put(filename, new HashMap<String, String>(dataRow));
                //System.out.println("Added note " + noteName + " for group " + groupName + " from filename " + filename);
            }
        } catch (IOException | DirectoryIteratorException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        
        final String noteFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.delete(Paths.get(ownNotePath, noteFileName));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        return result;
    }

    public boolean deleteNote(Map<String, String> curNote) {
        assert curNote != null;
        
        // delegate internally
        return this.deleteNote(curNote.get(notesMapKeys[3]), curNote.get(notesMapKeys[0]));
    }

    private String buildNoteName(String groupName, String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        return buildGroupName(groupName) + noteName + ".htm";
    }

    private String buildGroupName(String groupName) {
        assert groupName != null;
        
        String result = null;
        
        if (groupName.equals(NOT_GROUPED) || groupName.isEmpty()) {
            // only the note name
            result = "";
        } else {
            // group name upfront
            result = "[" + groupName + "] ";
        }
        
        return result;
    }

    public Boolean deleteGroup(Map<String, String> curGroup) {
        assert curGroup != null;
        
        // deleting a group is removing the group name from the note name
        return renameGroup(curGroup.get(groupsMapKeys[0]), "");
    }

    public boolean createNewNote(final String groupName, final String noteName) {
        assert groupName != null;
        assert noteName != null;
        
        boolean result = true;

        final String newFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.createFile(Paths.get(this.ownNotePath, newFileName));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        return result;
    }

    public String readNote(final Map<String, String> curNote) {
        assert curNote != null;
        
        String result = "";
        
        final String noteFileName = buildNoteName(curNote.get(notesMapKeys[3]), curNote.get(notesMapKeys[0]));
        
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
        
        Boolean result = true;

        final String newFileName = buildNoteName(groupName, noteName);
        
        try {
            Files.write(Paths.get(this.ownNotePath, newFileName), htmlText.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        return result;
    }

    public boolean renameGroup(final String oldGroupName, final String newGroupName) {
        assert oldGroupName != null;
        assert newGroupName != null;
        
        Boolean result = true;
        
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

        return result;
    }

    private String formatFileTime(final LocalDateTime filetime) {
        assert filetime != null;
        
        String result = "";
        
        // ownNote says "x secs ago", "x minutes ago", , "x hours ago", "x days ago"
        
        final LocalDateTime curtime = LocalDateTime.now();
        // start with longest interval and work your way down...
        result = stringFromDifference(ChronoUnit.DAYS.between(filetime, curtime), "day");
        if (result.isEmpty()) {
            result = stringFromDifference(ChronoUnit.HOURS.between(filetime, curtime), "hour");
            if (result.isEmpty()) {
                result = stringFromDifference(ChronoUnit.MINUTES.between(filetime, curtime), "minute");
                if (result.isEmpty()) {
                    result = stringFromDifference(ChronoUnit.SECONDS.between(filetime, curtime), "second");
                    if (result.isEmpty()) {
                        result = "Right now";
                    }
                }
            }
        }
        
        return result;
    }

    private String stringFromDifference(final long difference, final String unit) {
        assert unit != null;
        
        String result = "";
        
        if (difference > 0) {
            if (difference == 1.0) {
                result = "1 " + unit + " ago";
            } else {
                result = String.valueOf(difference) + " " + unit + "s ago";
            }
        }
        
        return result;
    }
}
