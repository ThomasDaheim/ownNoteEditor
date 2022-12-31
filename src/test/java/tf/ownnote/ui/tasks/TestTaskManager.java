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

import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author thomas
 */
public class TestTaskManager {
    @BeforeEach
    public void setUp() {
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources/");
        
        TaskManager.getInstance().resetTaskList();
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testFindAllOccurences() {
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().groupForName("Test", false), "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        Assertions.assertEquals(5, TaskManager.getInstance().tasksForNote(note).size());
    }

    @Test
    public void testGetTaskList() {
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().groupForName("Test", false), "TestTasks");
        
        // need to sort task list since other tests might have screwed with the order
        final List<TaskData> taskList = TaskManager.getInstance().tasksForNote(note).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        Assertions.assertEquals(5, taskList.size());
        
        for (TaskData data : taskList) {
            Assertions.assertEquals("Test", data.getNote().getGroupName());
            Assertions.assertEquals("TestTasks", data.getNote().getNoteName());
        }
        
        Assertions.assertEquals(" tell me, what to do!", taskList.get(0).getEventDescription().get());
        Assertions.assertTrue(taskList.get(2).getDescription().startsWith(" of course with something special: "));
    }
    
    @Test
    public void testChangeContent1() {
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().groupForName("Test", false), "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        TaskManager.getInstance().tasksForNote(note);
        final ObservableSet<TaskData> taskList = note.getMetaData().getTasks();
        Assertions.assertEquals(5, taskList.size());
        
        BooleanProperty wasUpdated = new SimpleBooleanProperty(Boolean.FALSE);
        BooleanProperty wasAdded = new SimpleBooleanProperty(Boolean.FALSE);
        BooleanProperty wasRemoved = new SimpleBooleanProperty(Boolean.FALSE);
        taskList.addListener((SetChangeListener.Change<? extends TaskData> change) -> {
            if (change.wasAdded()) {
                wasAdded.setValue(Boolean.TRUE);
            }
            if (change.wasRemoved()) {
                wasRemoved.setValue(Boolean.TRUE);
            }
        });
        
        // add something in front of content - tasklist shouldn't change
        String newContent = "TEST" + content;
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertFalse(wasAdded.getValue());
        Assertions.assertFalse(wasRemoved.getValue());
        
        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add something to end of content - tasklist shouldn't change
        newContent = content + "TEST";
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertFalse(wasAdded.getValue());
        Assertions.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add checkbox in front of content - tasklist should change
        newContent = TaskData.UNCHECKED_BOXES_1 + "TEST" + content;
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertTrue(wasAdded.getValue());
        Assertions.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // remove checkbox - tasklist should change
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertFalse(wasAdded.getValue());
        Assertions.assertTrue(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // add checkbox after content - tasklist should change
        newContent = content + TaskData.UNCHECKED_BOXES_1 + "TEST";
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertTrue(wasAdded.getValue());
        Assertions.assertFalse(wasRemoved.getValue());

        // reset wasXYZ
        wasAdded.setValue(Boolean.FALSE);
        wasRemoved.setValue(Boolean.FALSE);
        wasUpdated.setValue(Boolean.FALSE);
        
        // remove checkbox - tasklist should change
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assertions.assertFalse(wasUpdated.getValue());
        Assertions.assertFalse(wasAdded.getValue());
        Assertions.assertTrue(wasRemoved.getValue());
    }
    
    @Test
    public void testChangeContent2() {
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().groupForName("Test", false), "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        // need to sort task list since other tests might have screwed with the order
        final List<TaskData> taskList = TaskManager.getInstance().tasksForNote(note).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        Assertions.assertEquals(5, taskList.size());
        
        // change an existing checkbox to see if updated works
        final TaskData firstTask = taskList.get(0);
        final String firstDescription = firstTask.getHtmlText();
        int textPos = content.indexOf(firstDescription);
        String newContent = content.substring(0, textPos) + " - TEST - " + content.substring(textPos);

        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertNotEquals(firstDescription, firstTask.getHtmlText());
        Assertions.assertEquals(" - TEST - " + firstDescription, firstTask.getHtmlText());
        
        // change back
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assertions.assertEquals(firstDescription, firstTask.getHtmlText());
        
        // switch between checked / unchecked
        Assertions.assertFalse(firstTask.isCompleted());
        textPos = content.indexOf(firstTask.getHtmlText());
        newContent = content.substring(0, firstTask.getTextPos()) + TaskData.CHECKED_BOXES_1 + content.substring(textPos);

        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);
        Assertions.assertTrue(firstTask.isCompleted());
        
        // change back
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, newContent, content);
        Assertions.assertFalse(firstTask.isCompleted());
    }
    
    @Test
    public void testChangeContent3() {
        // add checkbox id twice to make sure second gets a new value
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().groupForName("Test", false), "TestTasks");
        
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        // need to sort task list since other tests might have screwed with the order
        final List<TaskData> taskList = TaskManager.getInstance().tasksForNote(note).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        Assertions.assertEquals(5, taskList.size());
        
        // change an existing checkbox to see if updated works
        final TaskData firstTask = taskList.get(0);
        final String firstRawText = firstTask.getRawText();

        // add checkbox twice at the end
        String newContent = content + " /// " + firstRawText + " /// " + firstRawText;
        TaskManager.getInstance().processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, content, newContent);

        // get new task list
        final List<TaskData> newTaskList = TaskManager.getInstance().tasksForNoteAndContent(note, newContent).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        // should be 2 tasks longer
        Assertions.assertEquals(7, newTaskList.size());
        
        // new tasks should have different id than first one
        Assertions.assertFalse(firstTask.getId().equals(newTaskList.get(5).getId()));
        Assertions.assertFalse(firstTask.getId().equals(newTaskList.get(6).getId()));
        // and different from each other
        Assertions.assertFalse(newTaskList.get(5).getId().equals(newTaskList.get(6).getId()));
    }
}
