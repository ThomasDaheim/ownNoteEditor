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
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;
import tf.helper.javafx.TooltipHelper;
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
    
    private final PseudoClass completed = PseudoClass.getPseudoClass("completed");
    
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
        
        myTaskList.setCellFactory(lv -> {
            // TFE, 20201230: change strikethrough based on isCompleted
            // https://gist.github.com/james-d/846eb9ff72bd66fdd955
            final ListCell<TaskData> cell = new CheckBoxListCell<>(TaskData::isCompletedProperty, converter) {
                @Override
                public void updateItem(TaskData item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (item != null && !empty) {
                        pseudoClassStateChanged(completed, item.isCompleted());

                        final Tooltip tooltip = new Tooltip();
                        tooltip.getStyleClass().add("taskdata-popup");
                        final StringBuilder text = new StringBuilder();
                        text.append(item.getNote().getNoteFileName());
                        text.append(System.lineSeparator());
                        text.append("Prio: ");
                        text.append(item.getTaskPriority().toString());
                        text.append(", ");
                        text.append("Status: ");
                        text.append(item.getTaskStatus().toString());
                        if (item.getDueDate() != null) {
                            text.append(", ");
                            text.append("Due date: ");
                            text.append(OwnNoteEditor.DATE_TIME_FORMATTER.format(item.getDueDate()));
                        }
                        tooltip.setText(text.toString());
                        TooltipHelper.updateTooltipBehavior(tooltip, 1000, 10000, 0, true);
                        setTooltip(tooltip);
                    } else {
                        pseudoClassStateChanged(completed, false);
                        setTooltip(null);
                    }
                }            
            };
            
            cell.getStyleClass().add("taskdata");
            
            return cell ;
        });

        myTaskList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.equals(oldSelection)) {
                if (!TaskManager.getInstance().inFileChange()) {
                    // select group and note
                    myEditor.selectNoteAndCheckBox(newSelection.getNote(), newSelection.getTextPos(), newSelection.getDescription());
                } else {
                    // tricky, we have lost the item in the list because the checkbox was clicked...
                    // we don't want to change the selection to avoid closing of file in tinymce.
                    myTaskList.getSelectionModel().select(-1);
                }
            }
        });       
    }
    
    public void populateTaskList() {
        // be able to react to changes of isCompleted in tasks
        // https://stackoverflow.com/a/30915760
        final ObservableList<TaskData> items = 
                FXCollections.<TaskData>observableArrayList(item -> new Observable[] {item.isCompletedProperty(), item.descriptionProperty()});
        items.setAll(TaskManager.getInstance().getTaskList());
        
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

        // items list doesn't receive change events for add & remove - need to attach separate listener to root list
        TaskManager.getInstance().getTaskList().addListener((Change<? extends TaskData> change) -> {
            items.setAll(TaskManager.getInstance().getTaskList());
        });

        // wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(items);
        setFilterPredicate();
        
        // add sorting by notename & textpos to have tasks grouped together
        SortedList<TaskData> sortedData = new SortedList<>(filteredData);
        sortedData.setComparator((o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            // compare note names
            int result = o1.getNote().getNoteName().compareTo(o2.getNote().getNoteName());
            if (result == 0) {
                // sort by textpos for same note
                result = o1.getTextPos() - o2.getTextPos();
            }
            return result;
        });
        
        myTaskList.setItems(null);
        myTaskList.layout();
        myTaskList.setItems(sortedData);
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
    
    public void setDisable(final boolean value) {
        myTaskList.setDisable(value);
    }
    
    public void setVisible(final boolean value) {
        myTaskList.setVisible(value);
    }
}
