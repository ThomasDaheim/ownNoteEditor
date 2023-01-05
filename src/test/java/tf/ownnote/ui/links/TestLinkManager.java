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

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author thomas
 */
public class TestLinkManager {
    final static String NEW_NOTE_LINK = "<p><a href=\"file:///[Test] TestVerify_ADD_ATTR.htm\" target=\"dummy\" data-note=\"yes\">[Test] TestVerify_ADD_ATTR</a></p>";
    
    @BeforeEach
    public void setUp() {
        LinkManager.getInstance().resetLinkList();
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources");
        LinkManager.getInstance().findNoteLinks();
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    public void testNoteWithLinks() {
        final Note note = OwnNoteFileManager.getInstance().getNote("[Test] TestLinks.htm");
        Assertions.assertNotNull(note);
        
        Assertions.assertEquals(2, note.getMetaData().getLinkedNotes().size(), "You have two links");
        Assertions.assertEquals(0, note.getMetaData().getLinkingNotes().size(), "You have no backlinks");
    }
    
    @Test
    public void testNoteWithBacklinks() {
        final Note note = OwnNoteFileManager.getInstance().getNote("[Test] TestVerify_OK.htm");
        Assertions.assertNotNull(note);
        
        Assertions.assertEquals(0, note.getMetaData().getLinkedNotes().size(), "You have no links");
        Assertions.assertEquals(1, note.getMetaData().getLinkingNotes().size(), "You have one backlink");
    }

    @Test
    public void testAddingAndRemoveLink() {
        final Note note = OwnNoteFileManager.getInstance().getNote("[Test] TestLinks.htm");
        final String originalContent = OwnNoteFileManager.getInstance().readNote(note, false).getNoteFileContent();
        
        final Note linkedNote = OwnNoteFileManager.getInstance().getNote("[Test] TestVerify_ADD_ATTR.htm");
        Assertions.assertEquals(0, linkedNote.getMetaData().getLinkedNotes().size(), "You have no links");
        Assertions.assertEquals(0, linkedNote.getMetaData().getLinkingNotes().size(), "You have no backlinks");

        // add new link at the end
        final String newContent = originalContent + NEW_NOTE_LINK + System.lineSeparator();
        note.setNoteEditorContent(newContent);
        
        // update links
        LinkManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, originalContent, newContent);
        
        Assertions.assertEquals(3, note.getMetaData().getLinkedNotes().size(), "You have three links");
        Assertions.assertEquals(0, note.getMetaData().getLinkingNotes().size(), "You have no backlinks");

        Assertions.assertEquals(0, linkedNote.getMetaData().getLinkedNotes().size(), "You have no links");
        Assertions.assertEquals(1, linkedNote.getMetaData().getLinkingNotes().size(), "You have one backlink");
        
        // undo changes
        note.setNoteEditorContent(originalContent);
        
        // update links
        LinkManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, originalContent);
        
        Assertions.assertEquals(2, note.getMetaData().getLinkedNotes().size(), "You have two links");
        Assertions.assertEquals(0, note.getMetaData().getLinkingNotes().size(), "You have no backlinks");

        Assertions.assertEquals(0, linkedNote.getMetaData().getLinkedNotes().size(), "You have no links");
        Assertions.assertEquals(0, linkedNote.getMetaData().getLinkingNotes().size(), "You have no backlinks");
    }

    @Test
    public void testRenameNote() {
        // THIS IS TRICKY
        // WE NEED TO CHANGE THE FILE NAME OF NOTE "[Test] TestVerify_OK.htm" AND THE CONTENT OF NOTE "[Test] TestLinks.htm"
        // IN CASE ANYTHING FAILS WE MIGHT NOT HAVE UNDONE ALL CHANGES!!!
        // IN THAT CASE UNDO MANUALLY:
        // NOTE NAME MUST BE "[Test] TestVerify_OK.htm" AND NOT "[Test] TestVerify_NEW_OK.htm"
        // NOTE CONTENT OF "[Test] TestLinks.htm" MUST LINK TO "[Test] TestVerify_OK.htm" AND NOT "[Test] TestVerify_NEW_OK.htm"
        final Note note = OwnNoteFileManager.getInstance().getNote("[Test] TestLinks.htm");
        final Note linkedNote = OwnNoteFileManager.getInstance().getNote("[Test] TestVerify_OK.htm");
        
        Note origNote = new Note(linkedNote);
        boolean result = OwnNoteFileManager.getInstance().renameNote(linkedNote, "TestVerify_NEW_OK");
        if (!result) {
            System.err.println("Moving of test note \"[Test] TestVerify_OK.htm\" failed!!!" );
        } else {
            LinkManager.getInstance().renameNote(origNote, "TestVerify_NEW_OK");
            
            // get new numbers
            final int noteLinks = note.getMetaData().getLinkedNotes().size();
            final int noteBacklinks = note.getMetaData().getLinkingNotes().size();
            final int linkedNoteLinks = linkedNote.getMetaData().getLinkedNotes().size();
            final int linkedNoteBacklinks = linkedNote.getMetaData().getLinkingNotes().size();

            // undo rename before testing - to avoid messing up the file system in case assertion fails
            origNote = new Note(linkedNote);
            result = OwnNoteFileManager.getInstance().renameNote(linkedNote, "TestVerify_OK");
            if (!result) {
                System.err.println("Restoring of test note \"[Test] TestVerify_OK.htm\" failed!!!" );
            }
            LinkManager.getInstance().renameNote(origNote, "TestVerify_OK");

            // nothing should happen
            Assertions.assertEquals(2, noteLinks, "You have two links");
            Assertions.assertEquals(0, noteBacklinks, "You have no backlinks");
        
            Assertions.assertEquals(0, linkedNoteLinks, "You have no links");
            Assertions.assertEquals(1, linkedNoteBacklinks, "You have one backlink");
        }
    }
}
