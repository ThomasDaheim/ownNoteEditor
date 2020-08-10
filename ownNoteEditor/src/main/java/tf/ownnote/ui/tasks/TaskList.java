/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import org.controlsfx.control.CheckListView;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * UI control for Tasks.
 * 
 * List with Checkbox, title and note reference.
 * 
 * @author thomas
 */
public class TaskList {
    private CheckListView<TaskData> myTaskList = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
            
    private TaskList() {
        super();
    }
            
    public TaskList(final CheckListView<TaskData> taskListFXML, final OwnNoteEditor editor) {
        myTaskList = taskListFXML;
        myEditor = editor;
        
        initTaskList();
    }
    
    private void initTaskList() {
        
    }
    
    public void setTaskFilterMode(final boolean isSelected) {
        // filter the list accordingly
    }
}
