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

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.calendarview.CalendarView;
import tf.helper.javafx.calendarview.CalendarViewEvent;
import tf.helper.javafx.calendarview.CalenderViewOptions;
import tf.helper.javafx.calendarview.GermanHolidayProvider;
import tf.helper.javafx.calendarview.HolidayProviderFactory;
import tf.helper.javafx.calendarview.ICalendarEvent;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
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
        getIcons().clear();
        getIcons().add(new Image(TaskBoard.class.getResourceAsStream("/OwnNoteEditorManager.png")));
        initModality(Modality.NONE); 
        initStyle(StageStyle.DECORATED);
        setResizable(true);
        setMinWidth(400.0);
        setMinHeight(200.0);

        Double recentWindowWidth = OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_WIDTH.getAsType();
        Double recentWindowHeigth = OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_HEIGTH.getAsType();
        final Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        Double recentWindowLeft = OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_LEFT.getAsType();
        if (Double.isNaN(recentWindowLeft)) {
            recentWindowLeft = (primScreenBounds.getWidth() - recentWindowWidth) / 2.0;
        }
        Double recentWindowTop = OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_TOP.getAsType();
        if (Double.isNaN(recentWindowTop)) {
            recentWindowTop = (primScreenBounds.getHeight() - recentWindowHeigth) / 2.0;
        }
        // TFE, 20201011: check that not larger than current screens - might happen with multiple monitors
        if (Screen.getScreensForRectangle(recentWindowLeft, recentWindowTop, recentWindowWidth, recentWindowHeigth).isEmpty()) {
            recentWindowWidth = 800.0;
            recentWindowHeigth = 600.0;
            recentWindowLeft = (primScreenBounds.getWidth() - recentWindowWidth) / 2.0;
            recentWindowTop = (primScreenBounds.getHeight() - recentWindowHeigth) / 2.0;
        }

        setWidth(recentWindowWidth);
        setHeight(recentWindowHeigth);
        setX(recentWindowLeft);
        setY(recentWindowTop);
        
        setOnCloseRequest((t) -> {
            if (!isMaximized() && !isIconified()) {
                OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_WIDTH.put(getWidth());
                OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_HEIGTH.put(getHeight());
                OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_LEFT.put(getX());
                OwnNoteEditorPreferences.RECENT_KANBAN_WINDOW_TOP.put(getY());
            }
        });
        
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

        final RowConstraints rowConstraint1 = new RowConstraints();
        rowConstraint1.setVgrow(Priority.NEVER);
        rowConstraint1.setMinHeight(220);
        rowConstraint1.setMaxHeight(220);
        final RowConstraints rowConstraint2 = new RowConstraints();
        rowConstraint2.setValignment(VPos.TOP);
        rowConstraint2.setVgrow(Priority.ALWAYS);
        rowConstraint2.setFillHeight(true);
        getGridPane().getRowConstraints().addAll(rowConstraint1, rowConstraint2);

        int rowNum = 0;
        // add calendar on top
        final CalendarView calendar = new CalendarView(YearMonth.now(), 
                new CalenderViewOptions().setAdditionalMonths(3).setMarkToday(true).setMarkWeekends(true).setShowWeekNumber(true));
        final ScrollPane calendarView = calendar.getCalendarView();
        calendarView.setFitToWidth(true);
        calendarView.setFitToHeight(true);
        calendarView.setMaxHeight(220);
        calendarView.setMinHeight(220);
        calendarView.setPrefHeight(220);
        // show german holidays in calendar
        HolidayProviderFactory.getInstance().registerHolidayProvider(Locale.GERMANY, GermanHolidayProvider.getInstance());
        calendar.addCalendarProviders(Arrays.asList(HolidayProviderFactory.getInstance()));

        // the array trick to avoid "Variables used in lambda should be final or effectively final"
        boolean [] firstShown = {false};
        // show tasks as events in calendar
        // TODO: filter auf offene Tasks
        TaskManager.getInstance().getTaskList().addListener((ListChangeListener.Change<? extends TaskData> change) -> {
            if (firstShown[0]) {
                Platform.runLater(() -> {
                    // run later - since we might be in TaskManager.initTaskList()
                    while (change.next()) {
                        if (change.wasRemoved()) {
                            final List<ICalendarEvent> temp = new ArrayList<>();
                            temp.addAll(change.getRemoved());
                            calendar.removeCalendarEvents(temp);
                        }
                        if (change.wasAdded()) {
                            final List<ICalendarEvent> temp = new ArrayList<>();
                            temp.addAll(change.getAddedSubList());
                            calendar.addCalendarEvents(temp);
                        }
                        if (change.wasUpdated()) {
                            // force rebuild of calendar - dates might have changed
                            calendar.rebuildCalendar();
                        }
                    }
                });
            }
        });
        // show tasks as events on first showing
        showingProperty().addListener((ov, t, t1) -> {
            if (!firstShown[0]) {
                final List<ICalendarEvent> temp = new ArrayList<>();
                temp.addAll(TaskManager.getInstance().getTaskList());
                calendar.addCalendarEvents(temp);
                firstShown[0] = true;
            }
        });
        
        // update tasks if dropped on a calendar
        calendar.addEventHandler(new EventHandler<>() {
            @Override
            public void handle(CalendarViewEvent t) {
                if (CalendarViewEvent.OBJECT_DROPPED.equals(t.getEventType()) && t.getDroppedObject() instanceof TaskCard) {
                    final TaskCard taskCard = ObjectsHelper.uncheckedCast(t.getDroppedObject());
                    taskCard.getTaskData().setDueDate(t.getDropDate().atTime(LocalTime.now()));
                }
            }
        });
        
        getGridPane().add(calendarView, 0, rowNum, statusCount, 1);
        GridPane.setHalignment(calendarView, HPos.CENTER);
        GridPane.setVgrow(calendarView, Priority.NEVER);
        
        rowNum++;
        // add task board lanes
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
