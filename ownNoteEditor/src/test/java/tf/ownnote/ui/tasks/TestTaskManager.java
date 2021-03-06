/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author thomas
 */
public class TestTaskManager {
    @Before
    public void setUp() {
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources/");
        
        TaskManager.getInstance().resetTaskList();
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testFindAllOccurences() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        Assert.assertEquals(5, TaskManager.getInstance().tasksFromNote(note, content).size());
    }

    @Test
    public void testGetTaskList() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        final List<TaskData> taskList = TaskManager.getInstance().getTaskList();
        Assert.assertEquals(5, taskList.size());
        
        for (TaskData data : taskList) {
            Assert.assertEquals("Test", data.getNote().getGroupName());
            Assert.assertEquals("TestTasks", data.getNote().getNoteName());
        }
        
        Assert.assertEquals(" tell me, what to do!", taskList.get(0).getDescription());
        Assert.assertTrue(taskList.get(2).getDescription().startsWith(" of course with something special: "));
    }
    
    @Test
    public void testChangeContent1() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        final ObservableList<TaskData> taskList = TaskManager.getInstance().getTaskList();
        Assert.assertEquals(5, taskList.size());
        
        BooleanProperty wasUpdated = new SimpleBooleanProperty(Boolean.FALSE);
        BooleanProperty wasAdded = new SimpleBooleanProperty(Boolean.FALSE);
        BooleanProperty wasRemoved = new SimpleBooleanProperty(Boolean.FALSE);
        taskList.addListener((ListChangeListener.Change<? extends TaskData> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    wasAdded.setValue(Boolean.TRUE);
                }
                if (change.wasRemoved()) {
                    wasRemoved.setValue(Boolean.TRUE);
                }
                if (change.wasUpdated()) {
                    wasUpdated.setValue(Boolean.TRUE);
                }
            }
        });
        
        // add something in front of content - tasklist shouldn't change
        String newContent = "TEST" + content;
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertFalse(wasAdded.getValue());
        Assert.assertFalse(wasRemoved.getValue());
        
        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add something to end of content - tasklist shouldn't change
        newContent = content + "TEST";
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertFalse(wasAdded.getValue());
        Assert.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add checkbox in front of content - tasklist should change
        newContent = TaskData.UNCHECKED_BOXES_1 + "TEST" + content;
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertTrue(wasAdded.getValue());
        Assert.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // remove checkbox - tasklist should change
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertFalse(wasAdded.getValue());
        Assert.assertTrue(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add checkbox after content - tasklist should change
        newContent = content + TaskData.UNCHECKED_BOXES_1 + "TEST";
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertTrue(wasAdded.getValue());
        Assert.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // remove checkbox - tasklist should change
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assert.assertFalse(wasUpdated.getValue());
        Assert.assertFalse(wasAdded.getValue());
        Assert.assertTrue(wasRemoved.getValue());
    }
    
    @Test
    public void testChangeContent2() {
        final Note note = OwnNoteFileManager.getInstance().getNote("Test", "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        final ObservableList<TaskData> taskList = TaskManager.getInstance().getTaskList();
        Assert.assertEquals(5, taskList.size());
        
        // change an existing checkbox to see if updated works
        final TaskData firstTask = taskList.get(0);
        final String firstDescription = firstTask.getDescription();
        int textPos = content.indexOf(firstTask.getDescription());
        String newContent = content.substring(0, textPos) + " - TEST - " + content.substring(textPos);

        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertNotEquals(firstDescription, firstTask.getDescription());
        Assert.assertEquals(" - TEST - " + firstDescription, firstTask.getDescription());
        
        // change back
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assert.assertEquals(firstDescription, firstTask.getDescription());
        
        // switch between checked / unchecked
        Assert.assertFalse(firstTask.isCompleted());
        textPos = content.indexOf(firstTask.getDescription());
        newContent = content.substring(0, firstTask.getTextPos()) + TaskData.CHECKED_BOXES_1 + content.substring(textPos);

        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assert.assertTrue(firstTask.isCompleted());
        
        // change back
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assert.assertFalse(firstTask.isCompleted());
    }
}
