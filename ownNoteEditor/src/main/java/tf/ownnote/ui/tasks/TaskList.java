/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;
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
    
    // be able to react to changes of isCompleted in tasks
    // https://stackoverflow.com/a/30915760
    private final ObservableList<TaskData> items = 
                FXCollections.<TaskData>observableArrayList(item -> new Observable[] {item.isCompletedProperty(), item.descriptionProperty()});
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
        
        myTaskList.setCellFactory(lv -> {
            // TFE, 20201230: change strikethrough based on isCompleted
            // https://gist.github.com/james-d/846eb9ff72bd66fdd955
            final ListCell<TaskData> cell = new CheckBoxListCell<>(TaskData::isCompletedProperty, converter) {
                @Override
                public void updateItem(TaskData item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (item != null && !empty) {
                        pseudoClassStateChanged(TaskManager.COMPLETED, item.isCompleted());

                        final Tooltip tooltip = new Tooltip();
                        tooltip.getStyleClass().add("taskdata-popup");
                        tooltip.setOnShowing((t) -> {
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
                                text.append("\u23F0 ");
                                text.append(OwnNoteEditor.DATE_TIME_FORMATTER.format(item.getDueDate()));
                            }
                            tooltip.setText(text.toString());
                        });
                        TooltipHelper.updateTooltipBehavior(tooltip, 1000, 10000, 0, true);
                        setTooltip(tooltip);
                    } else {
                        pseudoClassStateChanged(TaskManager.COMPLETED, false);
                        setTooltip(null);
                    }
                }            
            };
            
            cell.getStyleClass().add("taskdata");
            
            final ContextMenu contextMenu = new ContextMenu();

            final MenuItem editTask = new MenuItem("Edit task");
            final PopOver popOver = new PopOver();
            popOver.setAutoHide(true);
            popOver.setAutoFix(true);
            popOver.setCloseButtonEnabled(true);
            popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
            popOver.setArrowSize(0);
            editTask.setOnAction(event -> {
                popOver.setContentNode(new TaskEditor(cell.getItem()));
                popOver.show(cell);
            });
            popOver.addEventHandler(KeyEvent.KEY_PRESSED, (t) -> {
                if (KeyCode.ESCAPE.equals(t.getCode())) {
                    popOver.hide();
                }
            });

            final MenuItem switchTask = new MenuItem("Change task status");
            switchTask.setOnAction(event -> {
                cell.getItem().setCompleted(!cell.getItem().isCompleted());
            });
            
            contextMenu.getItems().addAll(editTask, switchTask);

            cell.contextMenuProperty().bind(
                    Bindings.when(cell.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu));
            
            return cell ;
        });

        myTaskList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.equals(oldSelection)) {
                if (!TaskManager.getInstance().inFileChange()) {
                    // select group and note
                    myEditor.selectNoteAndCheckBox(newSelection.getNote(), newSelection.getTextPos(), newSelection.getDescription(), newSelection.getId());
                } else {
                    // tricky, we have lost the item in the list because the checkbox was clicked...
                    // we don't want to change the selection to avoid closing of file in tinymce.
                    myTaskList.getSelectionModel().select(-1);
                }
            }
        });       
    }
    
    public void populateTaskList() {
        // items list doesn't receive change events for add & remove - need to attach separate listener to root list
        TaskManager.getInstance().getTaskList().addListener((Change<? extends TaskData> change) -> {
            // run later - since we might be in TaskManager.initTaskList()
            Platform.runLater(() -> {
                items.setAll(TaskManager.getInstance().getTaskList());
                initListData();
            });
        });
        items.setAll(TaskManager.getInstance().getTaskList());
        initListData();

        // add listener to items to get notified of any changes to COMPLETED property
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
    
    private void initListData() {
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
