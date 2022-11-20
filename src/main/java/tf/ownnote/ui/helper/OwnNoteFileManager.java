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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.apache.commons.io.FileUtils;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteMetaData;
import tf.ownnote.ui.notes.NoteVersion;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteFileManager implements INoteCRMDS {
    private final static OwnNoteFileManager INSTANCE = new OwnNoteFileManager();
    
    // TFE, 20220108: upgrade notes to full html...
    private final static String MINIMAL_HTML_PREFIX = "<!doctype html><html lang=\"en\"><head><meta charset=utf-8><title>&lmr;</title></head><body>";
    private final static String MINIMAL_HTML_SUFFIX = "</body>";
    
    public final static String NOTE_EXT = "htm";
    public final static String ALL_NOTES = "*." + NOTE_EXT;
    
    // TFE: 20210125: and now with backup, too!
    private final static String BACKUP_DIR = File.separator + "Backup";

    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor;
    
    // monitor for changes using java Watcher service
    private final OwnNoteDirectoryMonitor myDirMonitor = new OwnNoteDirectoryMonitor();

    private String notesPath;
    
    private final Map<String, Note> notesList = new LinkedHashMap<>();
    
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
        notesList.clear();

        // iterate over all files from directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(this.notesPath), ALL_NOTES);) {
            for (Path path: stream) {
                final File file = path.toFile();
                final String filename = file.getName();
                
                String noteName;
                String groupName;
                // split filename to notes & group names
                if (filename.startsWith("[")) {
                    groupName = filename.substring(1, filename.indexOf("]"));
                    // see pull request #44
                    noteName = filename.substring(filename.indexOf("]")+2, filename.lastIndexOf("."));
                } else {
                    groupName = TagManager.ReservedTag.NotGrouped.getTagName();
                    // see pull request #44
                    noteName = filename.substring(0, filename.lastIndexOf("."));
                }

                // extract info from file and fill maps accordingly
                final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(file.lastModified())).toInstant(), ZoneId.systemDefault());

//                System.out.println("Creating note '" + noteName + "' in group '"+ groupName + "'");
                // TFE, 20220414: we use group tags instead of group names
                final Note note = new Note(TagManager.getInstance().groupForExternalName(groupName, true), noteName);
                note.setNoteModified(filetime);
                // TFE; 20201023: set note metadata from file content
                note.setMetaData(NoteMetaData.fromHtmlComment(note, getFirstLine(file)));
                // use filename and not notename since duplicate note names can exist in different groups
                notesList.put(filename, note);
//                System.out.println("Added note '" + note.getNoteName() + "' for group '" + note.getGroup().getExternalName() + "' from filename '" + filename + "'");

                // backlink note to group
                note.getGroup().getLinkedNotes().add(note);
            }
        } catch (IOException | DirectoryIteratorException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        // TFE, 20210508: don't forget to add notes to group ALL as well...
        TagManager.ReservedTag.All.getTag().getLinkedNotes().clear();
        TagManager.ReservedTag.All.getTag().getLinkedNotes().addAll(notesList.values());

        // fix #14
        // monitor directory for changes
        myDirMonitor.setDirectoryToMonitor(notesPath);
    }
    
    private String getFirstLine(final File file) {
        String result = "";
        
        try (final BufferedReader in = 
                    new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            // use https://stackoverflow.com/a/19486413 to quickly read first line from file
            result = in.readLine();
            
            // TFE, 20220419: if we have (for some reason) an empty body, readLine() results in NULL
            if (result == null) {
                result = "";
            }
            
            // TFE, 20220108: upgrade notes to full html...
            if (result.startsWith(MINIMAL_HTML_PREFIX)) {
                result = result.replaceFirst(MINIMAL_HTML_PREFIX, "");
            }
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }

    public String getNotesPath() {
        return notesPath;
    }
    
    public ObservableList<Note> getNotesList() {
        return FXCollections.<Note>observableArrayList(notesList.values());
    }
    
    public Note getNote(final TagData group, final String noteName) {
        if (group == null || noteName == null) {
            return null;
        }
        
        return getNote(buildNoteName(group, noteName));
    }
    
    public Note getNote(final String noteFileName) {
        if (noteFileName == null || noteFileName.isEmpty()) {
            return null;
        }
        
        // TFE, 20210411: not sure what below code was good for...
//        Note result = null;
//        
//        for (Map.Entry<String, Note> note : notesList.entrySet()) {
//            if (note.getKey().equals(noteFileName)) {
//                result = note.getValue();
//                break;
//            }
//        }
        
        return notesList.get(noteFileName);
    }
    
    @Override
    public boolean deleteNote(final Note note) {
        assert note != null;
        
        boolean result = true;
        initFilesInProgress();
        
        final String noteFileName = buildNoteName(note);
        
        try {
            Files.delete(Paths.get(notesPath, noteFileName));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        resetFilesInProgress();
        return result;
    }

    public String buildNoteName(final TagData group, final String noteName) {
        assert noteName != null;
        
        return buildGroupName(group) + noteName + "." + NOTE_EXT;
    }
    
    public String buildNoteName(final Note note) {
        assert note != null;
        
        return buildNoteName(note.getGroup(), note.getNoteName());
    }

    private String buildGroupName(final TagData group) {
        String result;
        
        if (group == null || TagManager.isSpecialGroup(group)) {
            // only the note name
            result = "";
        } else {
            // group name upfront
            // TFE, 20220414: we now can have subgroups and need to combine all group names with a separator
            result = "[" + TagManager.getInstance().getExternalName(group) + "] ";
        }
        
        return result;
    }
    
    private String buildNewGroupName(final TagData group, final String newGroupName) {
        // this is tricky coding since it assumes that the old group name is
        // a) part of the name prefix AND
        // b) last occurence needs to be replaced
        // TODO: think of a better logic and where to place it...
        String result = buildGroupName(group);
        
        return replaceLast(result, group.getName(), newGroupName);
    }
    
    public static String replaceLast(String input, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        int lastMatchStart=0;
        do {
            lastMatchStart=matcher.start();
        } while (matcher.find());
        matcher.find(lastMatchStart);

        StringBuffer sb = new StringBuffer(input.length());
        matcher.appendReplacement(sb, replacement);
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public boolean createNote(final TagData group, final String noteName) {
        assert group != null;
        assert noteName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String newFileName = buildNoteName(group, noteName);
        
        try {
            Path newPath = Files.createFile(Paths.get(this.notesPath, newFileName));
            
            // TF, 20151129
            // update notesList as well
            final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(newPath.toFile().lastModified())).toInstant(), ZoneId.systemDefault());

            final Note noteRow = new Note(group, noteName);
            noteRow.setNoteModified(filetime);
            // TFE, 20210113: init data as well - especially charset
            noteRow.setNoteFileContent("");
            noteRow.setMetaData(new NoteMetaData(noteRow));
            noteRow.getMetaData().setCharset(StandardCharsets.UTF_8);

            // use filename and not notename since duplicate note names can exist in diffeent groups
            notesList.put(newFileName, noteRow);

            // save metadata
            saveNote(noteRow);
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        resetFilesInProgress();
        return result;
    }
    
    public Note readNote(final Note curNote, final boolean forceRead) {
        assert curNote != null;
        
        // only way to debug when you have a long list of notes...
//        if ("ownNotesWin".equals(curNote.getNoteName())) {
//            System.out.println("Found you");
//        }

        // TFE, 20201231: only read if you really have to
        if (curNote.getNoteFileContent() == null || forceRead) {
            final StringBuffer result = new StringBuffer("");

            final Path readPath = Paths.get(notesPath, buildNoteName(curNote));
            if (StandardCharsets.ISO_8859_1.equals(curNote.getMetaData().getCharset())) {
                try {
                    result.append(Files.readAllBytes(readPath));
                } catch (IOException ex) {
                    Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try (final BufferedReader reader = 
                    new BufferedReader(new InputStreamReader(new FileInputStream(readPath.toFile()), StandardCharsets.UTF_8))) {

                    boolean firstLine = true;
                    String str;
                    while ((str = reader.readLine()) != null) {
                        if (!firstLine) {
                            // don't use System.lineseparator() to avoid messup with metadata parsing
                            result.append("\n");
                        }
                        result.append(str);

                        firstLine = false;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // TFE, 20220108: upgrade notes to full html...
            String fullContent = result.toString();
            if (fullContent.startsWith(MINIMAL_HTML_PREFIX)) {
                fullContent = fullContent.replaceFirst(MINIMAL_HTML_PREFIX, "");
            }
            if (fullContent.endsWith(MINIMAL_HTML_SUFFIX)) {
                assert fullContent.length() > MINIMAL_HTML_SUFFIX.length();
                fullContent = fullContent.substring(0, fullContent.length() - MINIMAL_HTML_SUFFIX.length());
            }

            // TFE; 20200814: store content in Note
            curNote.setNoteFileContent(fullContent);
            
            // TFE, 20210121: things get too complicated with metadata - at least check file consistency
            if (!VerifyNoteContent.getInstance().verifyNoteFileContent(curNote) && myEditor != null) {
                final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                myEditor.showAlert(
                        Alert.AlertType.ERROR, 
                        "Error", 
                        "File inconsistency found!", 
                        "File: " + buildNoteName(curNote) + "\nCheck error log for further details.", 
                        buttonOK);
            }
        }
        
        return curNote;
    }

    @Override
    public boolean saveNote(final Note note) {
        return saveNote(note, false);
    }
        
    public boolean saveNote(final Note note, final boolean suppressMessages) {
        assert note != null;
        
        boolean result = true;
        initFilesInProgress();

        final String newFileName = buildNoteName(note);
        
        // TFE, 20201230: update task ids
        TaskManager.getInstance().replaceTaskDataInNote(note, suppressMessages);

        String content = note.getNoteEditorContent();
        if (content == null) {
            content = note.getNoteFileContent();
        }
        // TFE, 20201024: store note metadata
        note.getMetaData().addVersion(new NoteVersion(System.getProperty("user.name"), LocalDateTime.now()));
        // TFE, 20201217: from now on you're UTF-8
        note.getMetaData().setCharset(StandardCharsets.UTF_8);
        // TFE, 20220505: set note version to current app version
        note.getMetaData().setAppVersion(OwnNoteEditor.AppVersion.CURRENT.getVersionId());
        // TFE, 20220108: upgrade notes to full html...
        final String fullContent = 
                MINIMAL_HTML_PREFIX + 
                NoteMetaData.toHtmlComment(note.getMetaData()) + content +
                MINIMAL_HTML_SUFFIX;

        // TFE, 20201217: make sure we write UTF-8...
//            final Path savePath = Files.write(Paths.get(this.notesPath, newFileName), fullContent.getBytes());
        final Path savePath = Paths.get(this.notesPath, newFileName);
        try (FileWriter fw = new FileWriter(savePath.toFile(), StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(fw)) {
            writer.write(fullContent);
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        Note dataRow = notesList.get(newFileName);
        // TFE, 20220419: note might not yet exist in notesList!
        if (dataRow == null) {
            dataRow = note;
        }
        // TF, 20170723: update modified date of the file
        final LocalDateTime filetime = LocalDateTime.ofInstant((new Date(savePath.toFile().lastModified())).toInstant(), ZoneId.systemDefault());
        // TFE; 20200814: store content in Note
        dataRow.setNoteFileContent(content);
        dataRow.setNoteModified(filetime);
        notesList.put(newFileName, dataRow);

        resetFilesInProgress();

        if (result) {
            note.setNoteFileContent(content);
            if (note.getNoteEditorContent() != null) {
                note.setNoteEditorContent(content);
            }
            note.setUnsavedChanges(false);
            
            // TFE, 20210121: things get too complicated with metadata - at least check file consistency
            if (!VerifyNoteContent.getInstance().verifyNoteFileContent(note) && myEditor != null) {
                final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                myEditor.showAlert(
                        Alert.AlertType.ERROR, 
                        "Error", 
                        "File inconsistency found!", 
                        "File: " + newFileName + "\nCheck error log for further details.", 
                        buttonOK);
            }
        }

        return result;
    }
    
    @Override
    public boolean renameNote(final Note note, final String newNoteName) {
        assert note != null;
        assert newNoteName != null;
        
        boolean result = true;
        initFilesInProgress();

        final String oldFileName = buildNoteName(note);
        final Path oldFile = Paths.get(this.notesPath, oldFileName);
        final String newFileName = buildNoteName(note.getGroup(), newNoteName);
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

                final Note dataRow = notesList.remove(oldFileName);
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
    
    @Override
    public boolean moveNote(final Note note, final TagData newGroup) {
        assert note != null;
        assert newGroup != null;
        
        boolean result = true;
        initFilesInProgress();

        final String oldFileName = buildNoteName(note);
        final Path oldFile = Paths.get(this.notesPath, oldFileName);
        final String newFileName = buildNoteName(newGroup, note.getNoteName());
        final Path newFile = Paths.get(this.notesPath, newFileName);
        
        // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
        // TFE, 20191211: here we don't want to be as case insensitive as  the OS is
        if (Files.exists(newFile)) {
            result = false;
        } else {
            try {
                // System.out.printf("Time %s: Added files\n", getCurrentTimeStamp());
                Files.move(oldFile, newFile, StandardCopyOption.ATOMIC_MOVE);

                final Note dataRow = notesList.remove(oldFileName);
                dataRow.setGroup(newGroup);
                notesList.put(newFileName, dataRow);
            } catch (IOException ex) {
                Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }
        
        if (result) {
            // TFE, 20201227: the benfits of testing... finding an old bug :-)
            note.setGroup(newGroup);
        }
        
        resetFilesInProgress();
        return result;
    }

    public boolean renameGroup(final TagData group, final String newGroupName) {
        assert group != null;
        assert newGroupName != null;
        
        final boolean caseSensitiveRename = group.getName().toLowerCase().equals(newGroupName.toLowerCase());

        boolean result = true;
        initFilesInProgress();
        
        // old and new part of note name
        final String oldNoteNamePrefix = buildGroupName(group);
        // TFE, 20220417: there is no "new" group - so we need to construct the new name from the current name
        final String newNoteNamePrefix = buildNewGroupName(group, newGroupName);

        // [ and ] are special chars in glob syntax...
        final String escapedNoteNamePrefix = oldNoteNamePrefix.replace("[", "\\[").replace("]", "\\]");
        //System.out.println("Searching for -" + oldNoteNamePrefix + "- as prefix");
        
        // renaming a group means renaming all notes in the group to the new group name BUT only if no note with same new filename already exists
        DirectoryStream<Path> notesForGroup = null;
        // TFE, 20191211: here we don't want to be as case insensitive as the OS is
        // in theory we could have groups that only differ by case: TEST and Test
        // since thats not possible under Windows we will exclude it for all platforms...
        if (!caseSensitiveRename) {
            try {
                // 1. get all note names for group
                notesForGroup = Files.newDirectoryStream(Paths.get(this.notesPath), escapedNoteNamePrefix + ALL_NOTES);

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
            } catch (IOException | DirectoryIteratorException ex) {
                Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }
        
        // 3. rename all notes
        Note noteRow = null;
        if (result) {
            try {
                // need to re-read since iterator can only be used once - don't ask
                // https://stackoverflow.com/questions/25089294/java-lang-illegalstateexception-iterator-already-obtained
                notesForGroup = Files.newDirectoryStream(Paths.get(this.notesPath), escapedNoteNamePrefix + ALL_NOTES);

                for (Path path: notesForGroup) {
                    final File file = path.toFile();
                    final String filename = file.getName();

                    final String newFileName = newNoteNamePrefix + filename.substring(oldNoteNamePrefix.length());

                    try {
                        Files.move(Paths.get(this.notesPath, filename), Paths.get(this.notesPath, newFileName), StandardCopyOption.ATOMIC_MOVE);
                        
                        // TF, 20151129
                        // update notelist as well
                        noteRow = notesList.remove(filename);
                        // TFE, 20220417: we attach group now - that doesn't change...
//                        noteRow.setGroup(TagManager.isNotGroupedName(newGroup) ? TagManager.NOT_GROUPED : newGroup);
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
        
        resetFilesInProgress();
        return result;
    }

    public boolean noteExists(final TagData group, final String noteName) {
        final String fileName = buildNoteName(group, noteName);

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
    
    public Set<Note> getNotesWithText(final String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return notesList.values().stream().collect(Collectors.toSet());
        }
        
        final Set<Note> result = new HashSet<>();
        
        // iterate over all file and check context for searchText
        for (Map.Entry<String, Note> note : notesList.entrySet()) {
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
                final File noteFile = new File(this.notesPath, buildNoteName(note.getValue().getGroup(), note.getValue().getNoteName()));

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
    
    public boolean backupNote(final Note note, final String suffix) {
        assert note != null;
        assert suffix != null;
        
        boolean result = true;
        
        try {
            FileUtils.forceMkdir(new File(getNotesPath() + BACKUP_DIR));
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        if (result) {
            final String noteName = buildNoteName(note);
            final Path curFile = Paths.get(this.notesPath, noteName);
            final String backupName = buildNoteName(note.getGroup(), note.getNoteName() + suffix);
            final Path backupFile = Paths.get(this.notesPath + BACKUP_DIR, backupName);

            // TF, 20160815: check existence of the file - not something that should be done by catching the exception...
            // TFE, 20191211: here we don't want to be as case insensitive as  the OS is
            if (Files.exists(backupFile)) {
                result = false;
            } else {
                try {
                    // System.out.printf("Time %s: Added files\n", getCurrentTimeStamp());
                    Files.copy(curFile, backupFile, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ex) {
                    Logger.getLogger(OwnNoteFileManager.class.getName()).log(Level.SEVERE, null, ex);
                    result = false;
                }
            }
        }
        
        return result;
    }
}
