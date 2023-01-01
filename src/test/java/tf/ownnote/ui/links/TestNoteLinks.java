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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author thomas
 */
public class TestNoteLinks {
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
    public void testAddingLink() {
        // TODO
        
    }

    @Test
    public void testRemovingLink() {
        // TODO
        
    }

    @Test
    public void testRenameNote() {
        // TODO
        
    }

    @Test
    public void testDeleteNote() {
        // TODO
        
    }
}
