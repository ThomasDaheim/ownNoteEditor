/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * UI control for Tasks.
 * 
 * List with Checkbox, title and note reference.
 * 
 * @author thomas
 */
public class TaskList {
    private ListView<TaskData> myTaskList = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
            
    private TaskList() {
        super();
    }
            
    public TaskList(final ListView<TaskData> taskListFXML, final OwnNoteEditor editor) {
        myTaskList = taskListFXML;
        myEditor = editor;
        
        initTaskList();
    }
    
    private void initTaskList() {
        // TODO: switch to treeview with tasks per file
        // https://github.com/james-d/heterogeneous-tree-example
        
        final StringConverter<TaskData> converter = new StringConverter<TaskData>() {
            @Override
            public String toString(TaskData task) {
                return task.getDescription();
            }

            // not actually used by CheckBoxListCell
            @Override
            public TaskData fromString(String string) {
                return null;
            }
        };
        
        myTaskList.setCellFactory(CheckBoxListCell.forListView(TaskData::isCompleted, converter));        
    }
    
    public void populateTaskList() {
        myTaskList.getItems().setAll(TaskManager.getInstance().getTaskList());
    }
    
    public void setTaskFilterMode(final boolean isSelected) {
        // filter the list accordingly
    }
}
