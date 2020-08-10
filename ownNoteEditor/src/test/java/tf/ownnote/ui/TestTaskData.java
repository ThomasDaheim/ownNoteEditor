/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.tasks.TaskData;

/**
 *
 * @author thomas
 */
public class TestTaskData {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        OwnNoteFileManager.getInstance().initOwnNotePath("src/test/resources/");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testTaskData_Exceptions1() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("NoteData is null");
        new TaskData(null, -1);
    }
    
    @Test
    public void testTaskData_Exceptions2() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("TextPos can't be smaller than 0:");
        final NoteData noteData = OwnNoteFileManager.getInstance().getNotesList().get(0);
        new TaskData(noteData, -1);
    }
    
    @Test
    public void testTaskData_Exceptions3() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Text not starting with checkbox pattern:");
        final NoteData noteData = OwnNoteFileManager.getInstance().getNotesList().get(0);
        new TaskData(noteData, 0);
    }
    
    @Test
    public void testTaskDataOpenTask() {
        final NoteData noteData = OwnNoteFileManager.getInstance().getNoteData("Test", "TestTasks");
        
        TaskData taskData = new TaskData(noteData, 63);
        Assert.assertFalse(taskData.isCompleted());
        Assert.assertEquals(" tell me, what to do!", taskData.getDescription());
    }
    
    @Test
    public void testTaskDataCompletedTask() {
        final NoteData noteData = OwnNoteFileManager.getInstance().getNoteData("Test", "TestTasks");
        
        TaskData taskData = new TaskData(noteData, 368);
        Assert.assertTrue(taskData.isCompleted());
        // feel free to figure out how ? is handled correctly in all this string business
        Assert.assertTrue(taskData.getDescription().startsWith(" of course with something special: "));
    }
}
