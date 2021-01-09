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
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.helper.javafx.AbstractStage;
import tf.ownnote.ui.main.OwnNoteEditorManager;

/**
 * KANBAN Board for Tasks.
 * 
 * @author thomas
 */
public class TaskBoard extends AbstractStage {
    private final static TaskBoard INSTANCE = new TaskBoard();
    
    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/ownnoteeditor-taskboard-dnd");
    
    private TaskBoard() {
        super();
        
        initViewer();
    }
        
    public static TaskBoard getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        setTitle("Task Board");
        initModality(Modality.NONE); 
        initStyle(StageStyle.DECORATED);
        setResizable(true);
        setMinWidth(400.0);
        setMinHeight(200.0);
        
        getGridPane().getStyleClass().add("task-board");
        getGridPane().getChildren().clear();

        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(OwnNoteEditorManager.class.getResource("/css/ownnote.min.css").toExternalForm());
        
        // things are based on number of status values to be shown...
        final int statusCount = TaskData.TaskStatus.values().length;
        final List<ColumnConstraints> columnConstraints = new ArrayList<>();
        for (int i = 0; i < statusCount; i++) {
            final ColumnConstraints columnConstraint = new ColumnConstraints();
            columnConstraint.setPercentWidth(100.0 / statusCount);
            columnConstraints.add(columnConstraint);
        }
        getGridPane().getColumnConstraints().addAll(columnConstraints);

        final RowConstraints rowConstraint = new RowConstraints();
        rowConstraint.setValignment(VPos.TOP);
        rowConstraint.setVgrow(Priority.ALWAYS);
        rowConstraint.setFillHeight(true);
        getGridPane().getRowConstraints().addAll(rowConstraint);

        int rowNum = 0;
        for (int i = 0; i < statusCount; i++) {
            final TaskBoardLane lane = new TaskBoardLane(TaskData.TaskStatus.values()[i]);
            lane.setMinWidth(200.0);
            lane.setPrefWidth(300.0);
            lane.setMinHeight(400.0);
            lane.setPrefHeight(600.0);
            lane.setMaxHeight(Double.MAX_VALUE);
            getGridPane().add(lane, i, rowNum);
            GridPane.setValignment(lane, VPos.TOP);
            GridPane.setVgrow(lane, Priority.ALWAYS);
        }
    }

    protected static String getLaneColor(final TaskData.TaskStatus status) {
        switch (status) {
            case OPEN:
                return "#00AAFF";
            case IN_PROGRESS:
                return "#FFD500";
            case BLOCKED:
                return "#D93651";
            case DONE:
                return "#8ACC47";
            default:
                return "darkgrey";
        }
    }
}
