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

import java.time.LocalDateTime;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.controlsfx.control.PopOver;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.AppClipboard;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * Card to show minimal task data
 * 
 * - description
 * - status
 * - priority
 * - due date
 * 
 * @author thomas
 */
public class TaskCard extends GridPane {
    private final TaskData myTask;
    
    private final Label descLbl = new Label(); 
    private final static String DUEDATE_TEXT = "\u23F0 "; //"\uD83D\uDCC5 "
    private final Label dueDateLbl = new Label(); 
    private final static String PRIO_TEXT = "Prio: ";
    private final Label prioLbl = new Label(); 
    
    // TFE, 20210305: make sure we only attach listener once and it can be removed afterwards
    private boolean inFirstInit = true;
    private final ChangeListener<Boolean> completeListener;
    private final ChangeListener<LocalDateTime> dateListener;
    
    private TaskCard() {
        this(null);
    }
    
    public TaskCard(final TaskData task) {
        myTask = task;

        completeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue != null && !newValue.equals(oldValue)) {
                    setPseudoClass();
                }
            }
        };
        dateListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends LocalDateTime> ov, LocalDateTime oldValue, LocalDateTime newValue) {
                if (newValue != null && !newValue.equals(oldValue)) {
                    setPseudoClass();
                }
            }
        };
        
        initCard();
        initValues();
    }
    
    // should be called explicitly since task cards are created during any change to task status
    // to avoid to have multiple listeners to same task (since unused taskcards might not get garbage collected...)
    public void dettachFromTask() {
        myTask.isCompletedProperty().removeListener(completeListener);
        myTask.dueDateProperty().removeListener(dateListener);
    }
    
    private void initCard() {
        getStyleClass().add("task-card");
        
        final ColumnConstraints col1 = new ColumnConstraints();
        final ColumnConstraints col2 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setPercentWidth(40.0);
        col2.setHgrow(Priority.ALWAYS);
        col2.setPercentWidth(60.0);
        getGridPane().getColumnConstraints().addAll(col1, col2);

        int rowNum = 0;
        // description
        descLbl.getStyleClass().add("taskdata");
        getGridPane().add(descLbl, 0, rowNum, 2, 1);
        GridPane.setMargin(descLbl, AbstractStage.INSET_TOP);

        rowNum++;
        //  priority & due date
        getGridPane().add(prioLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(prioLbl, AbstractStage.INSET_TOP);

        getGridPane().add(dueDateLbl, 1, rowNum, 1, 1);
        GridPane.setMargin(dueDateLbl, AbstractStage.INSET_TOP);

        // support for editing the task on this card
        final PopOver popOver = new PopOver();
        popOver.setAutoHide(false);
        popOver.setAutoFix(true);
        popOver.setCloseButtonEnabled(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        popOver.setArrowSize(0);
        popOver.setContentNode(new TaskEditor(myTask));

        focusedProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && !newValue) {
                popOver.hide();
            }
        });

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem editTask = new MenuItem("Edit task");
        editTask.setOnAction(event -> {
            popOver.show(this);
            initValues();
        });
        contextMenu.getItems().addAll(editTask);
        
        setOnMouseClicked((t) -> {
            requestFocus();
            if (t.getClickCount() == 2) {
                popOver.show(this);
                initValues();
            }
            if (MouseButton.SECONDARY.equals(t.getButton())) {
                contextMenu.show(this, t.getScreenX(), t.getScreenY());
            }
        });

        popOver.addEventHandler(KeyEvent.KEY_PRESSED, (t) -> {
            if (KeyCode.ESCAPE.equals(t.getCode())) {
                popOver.hide();
            }
        });
        
        setOnDragDetected((t) -> {
            setFocused(true);
            AppClipboard.getInstance().addContent(TaskBoard.DRAG_AND_DROP, myTask);

            /* allow any transfer mode */
            Dragboard db = startDragAndDrop(TransferMode.MOVE);

            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.put(TaskBoard.DRAG_AND_DROP, myTask.getId());
            db.setContent(content);

            // use this as image
            // https://stackoverflow.com/a/12331082
            WritableImage img = snapshot(new SnapshotParameters(), null);
            db.setDragView(img);

            t.consume();
        });
        setOnDragDone((t) -> {
            AppClipboard.getInstance().clearContent(TaskBoard.DRAG_AND_DROP);

            t.consume();
        });
   }
    
    private void initValues() {
        Tooltip t = new Tooltip(myTask.getDescription());
        descLbl.setTooltip(t);
        descLbl.textProperty().bind(myTask.descriptionProperty()); 
        if (inFirstInit) {
            myTask.isCompletedProperty().addListener(completeListener);
        }
        setPseudoClass();

        prioLbl.textProperty().bind(Bindings.concat(PRIO_TEXT, myTask.taskPriorityProperty()));
        if (inFirstInit) {
            myTask.dueDateProperty().addListener(dateListener);
        }
        setDueDateLabel();
        
        inFirstInit = false;
    }
    
    private void setPseudoClass() {
        descLbl.pseudoClassStateChanged(TaskManager.TASK_COMPLETED, myTask.isCompleted());
    }
    
    private void setDueDateLabel() {
        final String dueDate = myTask.getDueDate() != null ? DUEDATE_TEXT + OwnNoteEditor.DATE_TIME_FORMATTER.format(myTask.getDueDate()) : "";
        dueDateLbl.setText(dueDate);
    }

    // provision for future conversion into an AbstractStage - not very YAGNI
    private GridPane getGridPane() {
        return this;
    }
    
    public TaskData getTaskData() {
        return myTask;
    }
}
