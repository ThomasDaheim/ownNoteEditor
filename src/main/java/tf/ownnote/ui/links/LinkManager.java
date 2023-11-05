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
package tf.ownnote.ui.links;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import tf.ownnote.ui.editor.HTMLEditor;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.FileManager;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagData;

/**
 * Manager for processes involving note links.
 * 
 * @author thomas
 */
public class LinkManager implements INoteCRMDS, IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static LinkManager INSTANCE = new LinkManager();

    // "<a href='" + link + "' data-note='yes' target='dummy'>"
    private final static String ANY_LINK_PREFIX = "<a href=['\"]" + HTMLEditor.NOTE_HTML_LINK_TYPE;
    private final static String ANY_LINK_LINK = "(.*)htm";
    private final static String ANY_LINK_POSTFIX = "['\"] target=['\"]dummy['\"] data-note=['\"]yes['\"]>";
    private final static String ANY_LINK = ANY_LINK_PREFIX + ANY_LINK_LINK + ANY_LINK_POSTFIX;
    private final static Pattern LINK_PATTERN = Pattern.compile(ANY_LINK);
    private final static int LINK_OFFSET = ANY_LINK_PREFIX.length() - 3;

    private boolean inFileChange = false;
    private boolean noteLinksInitialized = false;
    
    // keep track of all links here: to speed up things and for further functions (link-graph, ...)
    private final Map<Note, Set<Note>> linkList = new HashMap<>();
    private final Map<Note, Set<Note>> backlinkList = new HashMap<>();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private LinkManager() {
        super();
    }

    public static LinkManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        // now we can register everywhere
        FileManager.getInstance().subscribe(INSTANCE);
        myEditor.getNoteEditor().subscribe(INSTANCE);
    }
    
    public void resetLinkLists() {
        linkList.clear();
        backlinkList.clear();
        noteLinksInitialized = false;
    }
    
    public void findNoteLinks() {
        if (!noteLinksInitialized) {
            // lazy loading
            initNotesWithLinks();
            
            noteLinksInitialized = true;
        }
    }
    
    public Set<Note> getLinkedNotesForNote(final Note note) {
        assert note != null;
        
        return linkList.get(note);
    }
    
    public Set<Note> getNotesLinkingToNote(final Note note) {
        assert note != null;
        
        return backlinkList.get(note);
    }
    
    private void initNotesWithLinks() {
        // find all notes containing checkbox and parse to create TaskData for them
        final Set<Note> notesWithLinks = FileManager.getInstance().getNotesWithText(ANY_LINK);
        
        linkList.clear();
        for (Note note : notesWithLinks) {
            initNoteLinks(note, FileManager.getInstance().readNote(note, false).getNoteFileContent());
        }

        // and now find all other notes...
        List<Note> notesWithoutLinks = FileManager.getInstance().getNotesList();
        notesWithoutLinks.removeAll(notesWithLinks);
        for (Note note : notesWithoutLinks) {
            note.getMetaData().getLinkedNotes().clear();
        }
        
        initBacklinks();
    }

    private void initBacklinks() {
        backlinkList.clear();

        // 1) get all notes that are linked to another note
        // = keyset for backlink list
        final Set<Note> linkingNotes = new HashSet<>();
        linkList.values().stream().forEach((t) -> {
            linkingNotes.addAll(t);
        });
        
        // 2) backlink: all keys that have links to the given note
        for (Note note : linkingNotes) {
            final Set<Note> backlinks = new HashSet<>();

            for (Note keyNote : linkList.keySet()) {
                if (linkList.get(keyNote) == null) {
                    System.err.println("That shoudn't have happened!");
                } else {
                    if (linkList.get(keyNote).contains(note)) {
                        backlinks.add(keyNote);
                    }
                }
            }
            
            backlinkList.put(note, backlinks);
            note.getMetaData().getLinkingNotes().addAll(backlinks);
        }

        // and now find all other notes...
        List<Note> notesWithoutBacklinks = FileManager.getInstance().getNotesList();
        notesWithoutBacklinks.removeAll(backlinkList.keySet());
        for (Note note : notesWithoutBacklinks) {
            note.getMetaData().getLinkingNotes().clear();
        }
    }
    
    private boolean initNoteLinks(final Note note, final String noteContent) {
        final Set<Note> linkedNotes = linkedNotesForNoteAndContent(note, noteContent);

        // TFE, 20231103: it could have been a false positive
        note.getMetaData().setLinkedNotes(linkedNotes);
        
        Set<Note> prevLinkedNotes;
        if (!linkedNotes.isEmpty()){
            prevLinkedNotes = linkList.put(note, linkedNotes);
        } else {
            prevLinkedNotes = linkList.remove(note);
        }

        return !linkedNotes.equals(prevLinkedNotes);
    }
    
    
    // noteContent as separate parm since it could be called from change within the editor before save
    protected Set<Note> linkedNotesForNoteAndContent(final Note note, final String noteContent) {
        final Set<Note> result = new HashSet<>();

        // iterate over all matches and read Notes
        final List<Integer> textPossssss = findAllOccurences(noteContent, LINK_PATTERN);
        for (int textPos : textPossssss) {
            // note ref is directly after the ANY_LINK text AND ends with "."NOTE_EXT
            textPos += LINK_OFFSET;
            if (textPos + 1 + FileManager.NOTE_EXT.length() < noteContent.length()) {
                final int linkEnd = noteContent.indexOf(FileManager.NOTE_EXT, textPos) - 1;
                if (linkEnd > 0) {
                    final String linkName = noteContent.substring(textPos, linkEnd) + "." + FileManager.NOTE_EXT;
                    final Note linkedNote = FileManager.getInstance().getNote(linkName);
                    if (linkedNote != null) {
                        result.add(FileManager.getInstance().getNote(linkName));
                    }
                }
            }
        }
        
        return result;
    }
    
    private static List<Integer> findAllOccurences(final String text, final Pattern pattern) {
        final List<Integer> result = new LinkedList<>();
        
        if (text.isEmpty()) {
            return result;
        }
        
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.start());
        }
        
        return result;
    }
    
    private boolean updateExistingLinks(final String oldNoteName, final String newNoteName) {
        boolean result = true;

        // find all notes with the old link
        // we're called after the fact - linked note already has a new name...
        final Set<Note> linkedNotes = findNotesWithLink(newNoteName);
        
        // update content to point to the new link
        for (Note note : linkedNotes) {
            if (myEditor != null && note.equals(myEditor.getEditedNote())) {
                // the currently edited note - let htmleditor do the work
                myEditor.replaceNoteLinks(oldNoteName, newNoteName);
            } else {
                FileManager.getInstance().readNote(note, false);
                String content = note.getNoteFileContent();
                
                content = replaceNoteLinks(content, oldNoteName, newNoteName);

                // set back content - also to editor content for next editing of note
                note.setNoteFileContent(content);
                if (note.getNoteEditorContent() != null) {
                    note.setNoteEditorContent(content);
                }

                // suppress messages since we won't find all check boxes anymore
                if (!FileManager.getInstance().saveNote(note, true)) {
                    result = false;
                    break;
                }
            }
        }
        
        // init everything here...
        noteLinksInitialized = false;
        findNoteLinks();

        return result;
    }

    private boolean invalidateExistingLinks(final String noteName) {
        boolean result = true;

        // find all notes with the old link
        // we're called after the fact - linked note has gone away...
        final Set<Note> linkedNotes = findNotesWithLink(noteName);

        // update content to replace link by name
        for (Note note : linkedNotes) {
            if (note.equals(myEditor.getEditedNote())) {
                // the currently edited note - let htmleditor do the work
                myEditor.invalidateNoteLinks(noteName);
            } else {
                FileManager.getInstance().readNote(note, false);
                String content = note.getNoteFileContent();
                
                content = invalidateNoteLinks(content, noteName);

                // set back content - also to editor content for next editing of note
                note.setNoteFileContent(content);
                if (note.getNoteEditorContent() != null) {
                    note.setNoteEditorContent(content);
                }

                // suppress messages since we won't find all check boxes anymore
                if (!FileManager.getInstance().saveNote(note, true)) {
                    result = false;
                    break;
                }
            }
        }

        // init everything here...
        noteLinksInitialized = false;
        findNoteLinks();

        return result;
    }
    
    public static String replaceNoteLinks(final String noteContent, final String oldNoteName, final String newNoteName) {
        // we need to replace the link and the text of the link
        // AND we might have multiple occurences of the link...
        // SO replace everything - even if it might also not be used as a link...
        String oldNote = oldNoteName.replace("." + FileManager.NOTE_EXT, "").replace("[", "\\[").replace("]", "\\]");
        String newNote = newNoteName.replace("." + FileManager.NOTE_EXT, "");
        
        return noteContent.replaceAll(oldNote, newNote);
    }
    
    public static String invalidateNoteLinks(final String noteContent, final String noteName) {
        // we need to remove the link BUT leave the text of the link
        final String THIS_LINK = ANY_LINK_PREFIX + noteName.replace("[", "\\[").replace("]", "\\]") + ANY_LINK_POSTFIX;
        
        return noteContent.replaceAll(THIS_LINK, "");
    }
    
    private Set<Note> findNotesWithLink(final String noteName) {
        final Set<Note> linkedNotes = new HashSet<>();
        for (Note note : linkList.keySet()) {
            final long linkCount = note.getMetaData().getLinkedNotes().stream().filter((t) -> {
                return (t != null) && (t.getNoteFileName().equals(noteName));
            }).count();
            
            if (linkCount > 0) {
                linkedNotes.add(note);
            }
        }
        
        return linkedNotes;
    }

    @Override
    public boolean createNote(TagData newGroup, String newNoteName) {
        // nothing to do
        return true;
    }

    @Override
    public boolean renameNote(Note curNote, String newName) {
        final String newValue = FileManager.getInstance().buildNoteName(curNote.getGroup(), newName);

        return updateExistingLinks(curNote.getNoteFileName(), newValue);
    }

    @Override
    public boolean moveNote(Note curNote, TagData newGroup) {
        // what is the name of the note after moving to the new group?
        final String newValue = FileManager.getInstance().buildNoteName(newGroup, curNote.getNoteName());

        return updateExistingLinks(curNote.getNoteFileName(), newValue);
    }

    @Override
    public boolean deleteNote(Note curNote) {
        return invalidateExistingLinks(curNote.getNoteFileName());
    }

    @Override
    public boolean saveNote(Note note) {
        // nothing to do
        return true;
    }

    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        if (inFileChange) {
            return true;
        }

        final Note curNote = myEditor.getEditedNote();
        if (curNote != null && FileManager.getInstance().buildNoteName(curNote).equals(filePath.getFileName().toString())) {
            return true;
        }
        
        Platform.runLater(() -> {
            inFileChange = true;
            // only act for files not currently shown - that will come via FileContentChange...
            if (StandardWatchEventKinds.ENTRY_DELETE.equals(eventKind) || StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                // file deleted -> remove any links
                // unfortunately, the java watcher implementation doesn't provide a "before" and "after" in the case of modify - so we can only try to delete as well
                invalidateExistingLinks(filePath.getFileName().toString());
            }

            inFileChange = false;
        });
        
        return true;
    }

    @Override
    public boolean processFileContentChange(FileContentChangeType changeType, Note note, String oldContent, String newContent) {
        if (inFileChange) {
            return true;
        }

        inFileChange = true;
        // check if links have changed
        if (initNoteLinks(note, newContent)) {
            // links have changed - now update any backlinks
            initBacklinks();
        }
        inFileChange = false;
        
        return true;
    }
}
