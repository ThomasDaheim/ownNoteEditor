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
package tf.ownnote.ui.notes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.ownnote.ui.helper.FileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author thomas
 */
public class TestNoteAppVersion {
    @BeforeEach
    public void setUp() {
        TagManager.getInstance().resetTagList();
        FileManager.getInstance().setCallback(null);
        FileManager.getInstance().initNotesPath("src/test/resources/LookAndFeel");
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    public void testAppVersionExistingNote() {
        // this one should have no app version
        Note testNote = FileManager.getInstance().getNote("[Test1] test1.htm");
        
        Assertions.assertNotNull(testNote);
        Assertions.assertEquals(OwnNoteEditor.AppVersion.NONE.getVersionId(), testNote.getMetaData().getAppVersion(), 0.1);

        // this one should have 6.1
        testNote = FileManager.getInstance().getNote("[Test3] test1.htm");
        
        Assertions.assertNotNull(testNote);
        Assertions.assertEquals(OwnNoteEditor.AppVersion.V6_1.getVersionId(), testNote.getMetaData().getAppVersion(), 0.1);
    }
    
    @Test
    public void testAppVersionNewNote() {
        Note testNote = new Note("Test3", "TestAppVersion");

        Assertions.assertNotNull(testNote);
        Assertions.assertEquals(OwnNoteEditor.AppVersion.NONE.getVersionId(), testNote.getMetaData().getAppVersion(), 0.1);
    }
}
