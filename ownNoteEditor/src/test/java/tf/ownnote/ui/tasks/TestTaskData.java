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
package tf.ownnote.ui.tasks;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author thomas
 */
public class TestTaskData {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources/");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testTaskData_Exceptions1() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Note is null");
        new TaskData(null, "", -1);
    }
    
    @Test
    public void testTaskData_Exceptions2() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("TextPos can't be smaller than 0:");
        final Note note = OwnNoteFileManager.getInstance().getNotesList().get(0);
        new TaskData(note, "", -1);
    }
    
    @Test
    public void testTaskData_Exceptions3() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        final Note note = OwnNoteFileManager.getInstance().getNotesList().get(0);
        final String noteContent = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        exceptionRule.expectMessage("Text not starting with checkbox pattern: " + noteContent.split("\\n")[0]);
        new TaskData(note, noteContent, 0);
    }
    
    @Test
    public void testTaskDataOpenTask() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        final String noteContent = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        
        TaskData taskData = new TaskData(note, noteContent, 63);
        Assert.assertFalse(taskData.isCompleted());
        Assert.assertEquals(" tell me, what to do!", taskData.getDescription());
    }
    
    @Test
    public void testTaskDataCompletedTask() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        final String noteContent = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        
        TaskData taskData = new TaskData(note, noteContent, 368);
        Assert.assertTrue(taskData.isCompleted());
        // feel free to figure out how ? is handled correctly in all this string business
        Assert.assertTrue(taskData.getDescription().startsWith(" of course with something special: "));
    }
}
