/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    
    private FilteredList<TaskData> filteredData = null;
    // should only open tasks be shown?
    private boolean myTaskFilterMode = true;
    
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
        
        myTaskList.setCellFactory(CheckBoxListCell.forListView(TaskData::isCompletedProperty, converter));

        myTaskList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.equals(oldSelection)) {
                // select group and note
                myEditor.selectNoteAndPosition(newSelection.getNoteData(), newSelection.getTextPos(), newSelection.getHtmlText());
            }
        });       
    }
    
    public void populateTaskList() {
        // be able to react to changes of isCompleted in tasks
        // https://stackoverflow.com/a/30915760
        final ObservableList<TaskData> items = FXCollections.observableArrayList(item -> new Observable[] {item.isCompletedProperty()});
        items.addAll(TaskManager.getInstance().getTaskList());
        
        // wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(items);
        setFilterPredicate();
        
        myTaskList.setItems(null);
        myTaskList.layout();
        myTaskList.setItems(filteredData);
        
        // add listener to items to get notified of any changes to completed property
        items.addListener((Change<? extends TaskData> c) -> {
            while (c.next()) {
                if (c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        TaskManager.getInstance().processTaskCompletedChanged(c.getList().get(i));
                    }
                }
            }
        });
    }
    
    private void setFilterPredicate() {
        filteredData.setPredicate((t) -> {
            if (myTaskFilterMode) {
                return !t.isCompleted();
            } else {
                return true;
            }
        });
    }
    
    public void setTaskFilterMode(final boolean isSelected) {
        // if item is selected on UI we want to show all tasks
        myTaskFilterMode = !isSelected;
        
        // trigger refresh of filtered list - is there any better way???
        setFilterPredicate();
    }
}
