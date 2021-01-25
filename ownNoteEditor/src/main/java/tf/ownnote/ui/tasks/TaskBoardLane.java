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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AppClipboard;
import tf.helper.javafx.StyleHelper;

/**
 * Status lane for the KANBAN Board.
 * 
 * Holds the status name & task count in the header. List of tasks (as task cards) in that staus below.
 * Supports drag & drop.
 * 
 * @author thomas
 */
public class TaskBoardLane extends VBox {
    private final static String TASK_LANE_COLOR_CSS = "task-lane-color";
    
    private final TaskData.TaskStatus myStatus;
    
    private final Label laneHeader = new Label();
    private final VBox taskBox = new VBox();
    
    private static final ObservableList<TaskData> items = FXCollections.<TaskData>observableArrayList(item -> new Observable[] {item.taskStatusProperty()});
    private FilteredList<TaskData> filteredData;
    private final Map<TaskData, TaskCard> myCardMap = new HashMap<>();
    
    private TaskBoardLane() {
        this(null);
    }
    
    public TaskBoardLane(final TaskData.TaskStatus status) {
        myStatus = status;
        
        initViewer();
    }
    
    private void initViewer() {
        getStyleClass().add("task-board-lane");
        laneHeader.getStyleClass().add("task-board-lane-header");
        taskBox.getStyleClass().add("task-board-lane-taskbox");
        
        setStyle(StyleHelper.addStyle(this, StyleHelper.cssString(TASK_LANE_COLOR_CSS, TaskBoard.getLaneColor(myStatus))));
        laneHeader.setStyle(StyleHelper.addStyle(laneHeader, StyleHelper.cssString(TASK_LANE_COLOR_CSS, TaskBoard.getLaneColor(myStatus))));
        taskBox.setStyle(StyleHelper.addStyle(taskBox, StyleHelper.cssString(TASK_LANE_COLOR_CSS, TaskBoard.getLaneColor(myStatus))));
        
        final ScrollPane scrollPane = new ScrollPane(taskBox);
        scrollPane.getStyleClass().add("task-board-lane-scrollpane");
        scrollPane.setStyle(StyleHelper.addStyle(scrollPane, StyleHelper.cssString(TASK_LANE_COLOR_CSS, TaskBoard.getLaneColor(myStatus))));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        taskBox.setPrefWidth(USE_PREF_SIZE);
        taskBox.setPrefHeight(USE_PREF_SIZE);

        laneHeader.prefWidthProperty().bind(widthProperty());
        laneHeader.setText(myStatus.toString() + " (0)");
        taskBox.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
            laneHeader.setText(myStatus.toString() + " (" + taskBox.getChildren().size() + ")");
        });
        
        // change when status changes
        TaskManager.getInstance().getTaskList().addListener((ListChangeListener.Change<? extends TaskData> c) -> {
            Platform.runLater(() -> {
                // run later - since we might be in TaskManager.initTaskList()
                items.setAll(TaskManager.getInstance().getTaskList());
            });
        });
        items.setAll(TaskManager.getInstance().getTaskList());
        
        // add listener to items to get notified of any changes to COMPLETED property
        items.addListener((ListChangeListener.Change<? extends TaskData> c) -> {
            filteredData = items.filtered((t) -> {
                return myStatus.equals(t.getTaskStatus());
            });
        });

        // use filter
        filteredData = items.filtered((t) -> {
            return myStatus.equals(t.getTaskStatus());
        });
        filteredData.addListener((ListChangeListener.Change<? extends TaskData> c) -> {
            updateTaskCards();
        });
        updateTaskCards();
        
        getChildren().addAll(laneHeader, scrollPane);

        VBox.setVgrow(laneHeader, Priority.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        if (myStatus.isCompleted()) {
            final ContextMenu contextMenu = new ContextMenu();

            final MenuItem archiveItem = new MenuItem("Archive completed tasks");
            archiveItem.setOnAction((t) -> {
                TaskManager.getInstance().archiveCompletedTasks(myCardMap.keySet());
            });
            final MenuItem restoreItem = new MenuItem("Restore archived tasks");
            restoreItem.setOnAction((t) -> {
                TaskManager.getInstance().restoreArchivedTasks();
            });

            contextMenu.getItems().addAll(archiveItem, restoreItem);
            laneHeader.setContextMenu(contextMenu);
        }
        
        taskBox.setOnDragOver((t) -> {
            if (AppClipboard.getInstance().hasContent(TaskBoard.DRAG_AND_DROP)) {
                t.acceptTransferModes(TransferMode.MOVE);
            }
            t.consume();
        });
        taskBox.setOnDragDropped((t) -> {
            boolean success = false;

            if (AppClipboard.getInstance().hasContent(TaskBoard.DRAG_AND_DROP)) {
                final TaskData task = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(TaskBoard.DRAG_AND_DROP));
                // read & save note as required
                success = TaskManager.getInstance().processTaskStatusChanged(task, myStatus, false);
            }

            AppClipboard.getInstance().clearContent(TaskBoard.DRAG_AND_DROP);
            t.setDropCompleted(success);
            t.consume();
        });
    };
    
    private void updateTaskCards() {
        // and now check against current cards
        // 1. remove obsolete cards
        final List<TaskData> taskToBeRemoved = new ArrayList<>();
        for (Map.Entry<TaskData, TaskCard> entry : myCardMap.entrySet()) {
            if (!filteredData.contains(entry.getKey())) {
                taskBox.getChildren().remove(entry.getValue());
                taskToBeRemoved.add(entry.getKey());
            }
        }
        for (TaskData task : taskToBeRemoved) {
            myCardMap.remove(task);
        }

        // 2. add missing cards
        for (TaskData task : filteredData) {
            if (!myCardMap.containsKey(task)) {
                final TaskCard card = new TaskCard(task);
                card.setPrefWidth(taskBox.getWidth());
                card.prefWidthProperty().bind(taskBox.widthProperty());
                taskBox.getChildren().add(card);
                myCardMap.put(task, card);
            }
        }
    }
}
