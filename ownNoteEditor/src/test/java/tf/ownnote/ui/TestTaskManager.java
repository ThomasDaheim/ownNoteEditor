/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui;

import java.util.List;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.tasks.TaskData;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author thomas
 */
public class TestTaskManager {
    @Before
    public void setUp() {
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initOwnNotePath("src/test/resources/");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testFindAllOccurences() {
        final NoteData noteData = OwnNoteFileManager.getInstance().getNoteData("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(noteData);
        Assert.assertEquals(5, TaskManager.getInstance().findAllOccurences(content, OwnNoteEditor.ANY_BOXES).size());
    }

    @Test
    public void testGetTaskList() {
        final NoteData noteData = OwnNoteFileManager.getInstance().getNoteData("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(noteData);
        final List<TaskData> taskList = TaskManager.getInstance().getTaskList();
        Assert.assertEquals(5, taskList.size());
        
        for (TaskData data : taskList) {
            Assert.assertEquals("Test", data.getNoteData().getGroupName());
            Assert.assertEquals("TestTasks", data.getNoteData().getNoteName());
        }
        
        Assert.assertEquals(" tell me, what to do!", taskList.get(0).getDescription());
        Assert.assertTrue(taskList.get(2).getDescription().startsWith(" of course with something special: "));
    }
}
