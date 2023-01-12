/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.editor;

import java.util.Comparator;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.CellUtils;
import tf.helper.javafx.StyleHelper;
import tf.ownnote.ui.helper.CmdLineParameters;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class EditorTableColumn {
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
    
    private TableColumn<Note, String> myTableColumn = null;
    private String backgroundColor = "white";
    
    private EditorTableColumn() {
        super();
    }
            
    public EditorTableColumn(final TableColumn<Note, String> tableColumn, final OwnNoteEditor editor) {
        super();    
        myTableColumn = tableColumn;
        myEditor = editor;
        
        initTableColumn();
    }
    
    public void setWidthPercentage(final double percentage) {
        myTableColumn.prefWidthProperty().bind(myTableColumn.getTableView().widthProperty().multiply(percentage));
    }
    
    public void setTableColumnProperties(final double percentage, final Function<Note, String> accessor, final boolean linkCursor) {
        setWidthPercentage(percentage);
        myTableColumn.setCellValueFactory(
                            (TableColumn.CellDataFeatures<Note, String> p) -> new SimpleStringProperty(accessor.apply(p.getValue())));
        myTableColumn.setCellFactory(createObjectCellFactory(linkCursor));
    }

    private Callback<TableColumn<Note, String>, TableCell<Note, String>> createObjectCellFactory(final boolean linkCursor) {
        assert (myEditor != null);
        return (TableColumn<Note, String> param) -> new ObjectCell(myEditor, this, linkCursor, new UniversalMouseEvent(myEditor));
    }

    public TableColumn<Note, String> getTableColumn() {
        return myTableColumn;
    }
    
    private void initTableColumn() {
        // default is not editable
        myTableColumn.setEditable(false);
        
        // TFE, 20220222: remove space for invisible columns
        myTableColumn.visibleProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null & !newValue.equals(oldValue)) {
                // remove/disable column from table
                if (newValue) {
                    myTableColumn.setMaxWidth(5000);
                } else {
                    myTableColumn.setMaxWidth(0);
                }
            }
        });
    }

    /* Required getter and setter methods are forwarded to internal TableView */

    public void setEditable(final boolean b) {
        myTableColumn.setEditable(b);
    }
    
    public void setOnEditCommit(final EventHandler<TableColumn.CellEditEvent<Note, String>> value) {
        myTableColumn.setOnEditCommit(value);
    }
    
    public void setBackgroundColor(final String color) {
        myTableColumn.setStyle(StyleHelper.addAndRemoveStyles(
                myTableColumn, 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, color), 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, backgroundColor)));
        backgroundColor = color;
    }

    public void setVisible(final boolean b) {
        myTableColumn.setVisible(b);
    }

    // see issue #42
    public final void setComparator(Comparator<String> value) {
        myTableColumn.setComparator(value);
    }
    
    public void setCellFactory(final Callback<TableColumn<Note, String>, TableCell<Note, String>> value) {
        myTableColumn.setCellFactory(value);
    }
}

class UniversalMouseEvent implements EventHandler<MouseEvent> {
    // store link back to the controller of the scene for callback
    private OwnNoteEditor myEditor = null;
    
    public UniversalMouseEvent(final OwnNoteEditor ownNoteEditor) {
        myEditor = ownNoteEditor;
    }

    @Override
    public void handle(MouseEvent event) {
        if (event.getSource() instanceof TableCell) {
            TableCell clickedCell = (TableCell) event.getSource();
            
            handleTableClick(clickedCell);
        }
    }
    
    private void handleTableClick(final TableCell clickedCell) {
        assert (this.myEditor != null);
        
        // only do things for row with data
        if (clickedCell.getTableRow().isEmpty()) {
            return;
        }
                            
        boolean reInit = false;
        
        Note curNote = null;
        switch(clickedCell.getId()) {
            case "noteNameColFXML":
                //System.out.println("Clicked in noteNameCol");
                curNote =
                    ObjectsHelper.uncheckedCast(clickedCell.getTableView().getItems().get(clickedCell.getIndex()));
                //reInit = this.myEditor.editNote(curNote);
                break;
            default:
                //System.out.println("Ignoring click into " + clickedCell.getId() + " for controller " + this.myEditor.toString());
        }
        
        if (reInit) {
            // rescan diretory - also group name counters need to be updated...
            this.myEditor.initFromDirectory(false, false);
        }
    }
};

class ObjectCell extends TextFieldTableCell<Note, String> {
    private static final String valueSet = "valueSet";
    
    private final TextField textField;
    private final HBox hbox;
    // store graphic for update & cancelEdit
    private Label graphic = null;
    
    // store link back to the controller of the scene for callback
    private final OwnNoteEditor myEditor;
    
    private final EditorTableColumn myTableColumn;
    
    public ObjectCell(final OwnNoteEditor editor,
            final EditorTableColumn tableColumn, 
            final boolean linkCursor, 
            final EventHandler<MouseEvent> mouseEvent) {
        super(new DefaultStringConverter());
        myEditor = editor;
        myTableColumn = tableColumn;
        
        // TFE, 20191208: check for valid file names!
        // https://stackoverflow.com/a/54552791
        // https://stackoverflow.com/a/49918923
        // https://stackoverflow.com/a/45201446
        // to check for illegal chars in note & group names
        textField = CellUtils.createTextField(this, getConverter());
        // TFE, 20191208: check for valid file names!
        FormatHelper.getInstance().initNoteGroupNameTextField(textField, FormatHelper.VALIDNOTEGROUPNAME);
        hbox = new HBox();
        
        if (linkCursor) {
            this.setCursor(Cursor.HAND);
        }
        
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent);
    }
    
    @Override
    public void startEdit() {
        super.startEdit();
        
        if (isEditing()) {
            CellUtils.startEdit(this, getConverter(), null, null, textField);
        }
    }
    
    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setTooltip(null);
        CellUtils.cancelEdit(this, getConverter(), graphic);
    }
    
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        
        graphic = null;
        if (item != null && getTableRow().getItem() != null &&
                "noteNameColFXML".equals(getId()) && 
                CmdLineParameters.LookAndFeel.tagTree.equals(myEditor.getCurrentLookAndFeel())) {
            final Note note = ObjectsHelper.uncheckedCast(getTableRow().getItem());
            final String groupColor = note.getGroup().getColorName();
            graphic = new Label("    ");
            graphic.setStyle("-fx-background-color: " + groupColor + ";");
        }
        if (item != null) {
            // add class to indicate not null content - to be used in css
            getStyleClass().add(ObjectCell.valueSet);
        } else {
            // add class to indicate null content - to be used in css
            getStyleClass().removeAll(ObjectCell.valueSet);
        }
        setGraphic(graphic);

        CellUtils.updateItem(this, getConverter(), hbox, graphic, textField);

        setTooltip(null);
    }
}