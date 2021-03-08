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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import jfxtras.scene.control.CalendarTextField;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;
import tf.helper.javafx.EnumHelper;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * Show & edit task data as a GridPane.
 * 
 * - id - SHOW only
 * - description - TODO EDIT: would need to change note text as well
 * - status
 * - priority
 * - due date
 * - comment
 * 
 * @author thomas
 */
public class TaskEditor extends GridPane {
    private final TaskData myTask;
    
    private final Label idLbl = new Label(); 
    private final Label descLbl = new Label(); 
    private final ChoiceBox<TaskData.TaskStatus> statusBox = 
            EnumHelper.getInstance().createChoiceBox(TaskData.TaskStatus.class, TaskData.TaskStatus.OPEN);
    private final ChoiceBox<TaskData.TaskPriority> prioBox = 
            EnumHelper.getInstance().createChoiceBox(TaskData.TaskPriority.class, TaskData.TaskPriority.MEDIUM);
    private final CalendarTextField duedateTimeTxt = new CalendarTextField();
    private final TextArea commentArea = new TextArea();
    
    private TaskEditor() {
        this(null);
    }
    
    public TaskEditor(final TaskData task) {
        myTask = task;
        
        initCard();
        initValues();
    }
    
    private void initCard() {
        // needs to be done in case it becomes a stage...
        // (new JMetro(Style.LIGHT)).setScene(getScene());
        // getScene().getStylesheets().add(EditGPXWaypoint.class.getResource("/GPXEditor.css").toExternalForm());
        
        getStyleClass().add("taskEditor");

        final ColumnConstraints col1 = new ColumnConstraints();
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(100.0);
        col2.setMaxWidth(260.0);
        col2.setHgrow(Priority.ALWAYS);
        getGridPane().getColumnConstraints().addAll(col1,col2);

        int rowNum = 0;
        // description
        Tooltip t = new Tooltip("Task descrioption from note text");
        final Label lbldesc = new Label("Description:");
        lbldesc.setTooltip(t);
        getGridPane().add(lbldesc, 0, rowNum, 1, 1);
        GridPane.setValignment(lbldesc, VPos.CENTER);
        GridPane.setMargin(lbldesc, INSET_TOP);
        
        getGridPane().add(descLbl, 1, rowNum, 1, 1);
        GridPane.setMargin(descLbl, INSET_TOP);

        rowNum++;
        // status
        t = new Tooltip("Status");
        final Label lblstatus = new Label("Status:");
        lblstatus.setTooltip(t);
        getGridPane().add(lblstatus, 0, rowNum, 1, 1);
        GridPane.setValignment(lblstatus, VPos.CENTER);
        GridPane.setMargin(lblstatus, INSET_TOP);
        
        getGridPane().add(statusBox, 1, rowNum, 1, 1);
        GridPane.setMargin(statusBox, INSET_TOP);

        rowNum++;
        // priority
        t = new Tooltip("Priority");
        final Label lblprio = new Label("Priority:");
        lblprio.setTooltip(t);
        getGridPane().add(lblprio, 0, rowNum, 1, 1);
        GridPane.setValignment(lblprio, VPos.CENTER);
        GridPane.setMargin(lblprio, INSET_TOP);
        
        getGridPane().add(prioBox, 1, rowNum, 1, 1);
        GridPane.setMargin(prioBox, INSET_TOP);

        rowNum++;
        // due date
        t = new Tooltip("Due date");
        final Label lblduedate = new Label("Due date:");
        lblduedate.setTooltip(t);
        getGridPane().add(lblduedate, 0, rowNum, 1, 1);
        GridPane.setValignment(lblduedate, VPos.CENTER);
        GridPane.setMargin(lblduedate, INSET_TOP);
        
        duedateTimeTxt.setAllowNull(true);
        duedateTimeTxt.setShowTime(true);
        duedateTimeTxt.setDateFormat(new SimpleDateFormat(OwnNoteEditor.DATE_TIME_FORMAT));
        getGridPane().add(duedateTimeTxt, 1, rowNum, 1, 1);
        GridPane.setMargin(duedateTimeTxt, INSET_TOP);

        rowNum++;
        // comment
        t = new Tooltip("Comment");
        final Label lblcomment = new Label("Comment:");
        lblcomment.setTooltip(t);
        getGridPane().add(lblcomment, 0, rowNum, 1, 1);
        GridPane.setValignment(lblcomment, VPos.TOP);
        GridPane.setMargin(lblcomment, INSET_TOP);
        
        commentArea.setPrefRowCount(3);
        getGridPane().add(commentArea, 1, rowNum, 1, 1);
        GridPane.setMargin(commentArea, INSET_TOP);

        rowNum++;
        // task id
        t = new Tooltip("Task id");
        final Label lblid = new Label("Task id:");
        lblid.setTooltip(t);
        getGridPane().add(lblid, 0, rowNum, 1, 1);
        GridPane.setValignment(lblid, VPos.CENTER);
        GridPane.setMargin(lblid, INSET_TOP);
        
        getGridPane().add(idLbl, 1, rowNum, 1, 1);
        GridPane.setMargin(idLbl, INSET_TOP);

        rowNum++;
        // save + cancel buttons
        final Button saveButton = new Button("Save");
        saveButton.setOnAction((ActionEvent event) -> {
            saveValues();
            // give parent node the oportunity to close
            fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, KeyCode.ESCAPE.toString(), KeyCode.ESCAPE.toString(), KeyCode.ESCAPE, false, false, false, false));
        });
        // not working since no scene yet...
//        getScene().getAccelerators().put(UsefulKeyCodes.CNTRL_S.getKeyCodeCombination(), () -> {
//            saveButton.fire();
//        });
        getGridPane().add(saveButton, 0, rowNum, 1, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, INSET_TOP_BOTTOM);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            initValues();
            // give parent node the oportunity to close
            fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, KeyCode.ESCAPE.toString(), KeyCode.ESCAPE.toString(), KeyCode.ESCAPE, false, false, false, false));
        });
        // not working since no scene yet...
//        getScene().getAccelerators().put(UsefulKeyCodes.ESCAPE.getKeyCodeCombination(), () -> {
//            cancelBtn.fire();
//        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setHalignment(cancelBtn, HPos.CENTER);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
    }
    
    private void initValues() {
        idLbl.setText(myTask.getId()); 
        Tooltip t = new Tooltip(myTask.getDescription());
        descLbl.setTooltip(t);
        descLbl.setText(myTask.getDescription()); 
        EnumHelper.getInstance().selectEnum(statusBox, myTask.getTaskStatus());
        EnumHelper.getInstance().selectEnum(prioBox, myTask.getTaskPriority());
        if (myTask.getDueDate()!= null) {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(
                    myTask.getDueDate().getYear(), 
                    myTask.getDueDate().getMonthValue()-1, 
                    myTask.getDueDate().getDayOfMonth(), 
                    myTask.getDueDate().getHour(), 
                    myTask.getDueDate().getMinute(), 
                    myTask.getDueDate().getSecond());
            duedateTimeTxt.setCalendar(calendar);
        } else {
            duedateTimeTxt.setText("");
        }
        t = new Tooltip(myTask.getComment());
        commentArea.setTooltip(t);
        commentArea.setText(myTask.getComment());
    }
    
    public void saveValues() {
        // don't set directly but let taskmanager do the work - since we want to update the rest of the world as well
//        myTask.setTaskStatus(EnumHelper.getInstance().selectedEnumChoiceBox(TaskData.TaskStatus.class, statusBox));
        // TODO: replace update process in taskmanager with a listener to task status...
        TaskManager.getInstance().processTaskStatusChanged(myTask, EnumHelper.getInstance().selectedEnumChoiceBox(TaskData.TaskStatus.class, statusBox), false, true);
        myTask.setTaskPriority(EnumHelper.getInstance().selectedEnumChoiceBox(TaskData.TaskPriority.class, prioBox));
        if (!duedateTimeTxt.getText().isEmpty()) {
            final Date date = duedateTimeTxt.getCalendar().getTime();
            myTask.setDueDate(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        } else {
            myTask.setDueDate(null);
        }
        String comment = null;
        if (commentArea.getText() != null && !commentArea.getText().isEmpty()) {
            comment = commentArea.getText();
        }
        myTask.setComment(comment);
    }

    // provision for future conversion into an AbstractStage - not very YAGNI
    private GridPane getGridPane() {
        return this;
    }
    
}
