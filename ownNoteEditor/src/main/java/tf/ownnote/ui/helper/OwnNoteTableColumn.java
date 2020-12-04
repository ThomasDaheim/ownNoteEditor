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
package tf.ownnote.ui.helper;

import java.util.Comparator;
import java.util.Map;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.CellUtils;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.NoteGroup;
import tf.ownnote.ui.notes.Note;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTableColumn {
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
    
    private TableColumn<Map, String> myTableColumn = null;
    
    // we need to know the tabletype as well...
    private OwnNoteTableView.TableType myTableType = null;

    private OwnNoteTableColumn() {
        super();
    }
            
    public OwnNoteTableColumn(final TableColumn<Map, String> tableColumn, final OwnNoteEditor editor) {
        super();    
        myTableColumn = tableColumn;
        myEditor = editor;
        
        // TF, 20160627: select tabletype based on passed TableColumn - safer than having a setTableType method
        if (tableColumn.getId().startsWith("note")) {
            myTableType = OwnNoteTableView.TableType.notesTable;
        }
        if (tableColumn.getId().startsWith("group")) {
            myTableType = OwnNoteTableView.TableType.groupsTable;
        }

        // stop if we haven't been passed a correct TableColumn
        assert (myTableType != null);

        initTableColumn();
    }
    
    public void setWidthPercentage(final double percentage) {
        myTableColumn.prefWidthProperty().bind(myTableColumn.getTableView().widthProperty().multiply(percentage));
    }
    
    public void setTableColumnProperties(final double percentage, final String valueName, final boolean linkCursor) {
        setWidthPercentage(percentage);
        myTableColumn.setCellValueFactory(new MapValueFactory<String>(valueName));
        myTableColumn.setCellFactory(createObjectCellFactory(linkCursor));
    }

    private Callback<TableColumn<Map, String>, TableCell<Map, String>> createObjectCellFactory(final boolean linkCursor) {
        assert (myEditor != null);
        return (TableColumn<Map, String> param) -> new ObjectCell(myEditor, this, linkCursor, new UniversalMouseEvent(myEditor));
    }

    public OwnNoteTableView.TableType getTableType() {
        return myTableType;
    }
    
    public TableColumn<Map, String> getTableColumn() {
        return myTableColumn;
    }
    
    private void initTableColumn() {
        // default is not editable
        myTableColumn.setEditable(false);
    }

    /* Required getter and setter methods are forwarded to internal TableView */

    public void setEditable(final boolean b) {
        myTableColumn.setEditable(b);
    }
    
    public void setOnEditCommit(final EventHandler<TableColumn.CellEditEvent<Map, String>> value) {
        myTableColumn.setOnEditCommit(value);
    }
    
    public void setStyle(final String style) {
        myTableColumn.setStyle(style);
    }

    public void setVisible(final boolean b) {
        myTableColumn.setVisible(b);
    }

    // see issue #42
    public final void setComparator(Comparator<String> value) {
        myTableColumn.setComparator(value);
    }
    
    public void setCellFactory(final Callback<TableColumn<Map, String>, TableCell<Map, String>> value) {
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
        NoteGroup curNoteGroup = null;
        switch(clickedCell.getId()) {
            case "noteNameColFXML":
                //System.out.println("Clicked in noteNameCol");
                curNote =
                    new Note(ObjectsHelper.uncheckedCast(clickedCell.getTableView().getItems().get(clickedCell.getIndex())));
                //reInit = this.myEditor.editNote(curNote);
                break;
            case "noteDeleteColFXML":
                //System.out.println("Clicked in noteDeleteCol");
                curNote =
                    new Note(ObjectsHelper.uncheckedCast(clickedCell.getTableView().getItems().get(clickedCell.getIndex())));
                reInit = this.myEditor.deleteNoteWrapper(curNote);
                break;
            case "groupDeleteColFXML":
                //System.out.println("Clicked in groupDeleteCol");
                curNoteGroup =
                    new NoteGroup(ObjectsHelper.uncheckedCast(clickedCell.getTableView().getItems().get(clickedCell.getIndex())));
                reInit = this.myEditor.deleteGroupWrapper(curNoteGroup);
                break;
            default:
                //System.out.println("Ignoring click into " + clickedCell.getId() + " for controller " + this.myEditor.toString());
        }
        
        if (reInit) {
            // rescan diretory - also group name counters need to be updated...
            this.myEditor.initFromDirectory(false);
        }
    }
};

class ObjectCell extends TextFieldTableCell<Map, String> {
    private static final String valueSet = "valueSet";
    
    private TextField textField;
    
    // store link back to the controller of the scene for callback
    private final OwnNoteEditor myOwnNoteEditor;
    
    private final OwnNoteTableColumn myOwnNoteTableColumn;
    
    public ObjectCell(final OwnNoteEditor ownNoteEditor,
            final OwnNoteTableColumn ownNoteTableColumn, 
            final boolean linkCursor, 
            final EventHandler<MouseEvent> mouseEvent) {
        super(new DefaultStringConverter());
        myOwnNoteEditor = ownNoteEditor;
        myOwnNoteTableColumn = ownNoteTableColumn;
        
        if (linkCursor) {
            this.setCursor(Cursor.HAND);
        }
        
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent);
    }
    
    @Override
    public void startEdit() {
        if (NoteGroup.ALL_GROUPS.equals(getText())
                || NoteGroup.NOT_GROUPED.equals(getText())) {
            return;
        }
        super.startEdit();
        
        if (isEditing()) {
            if (textField == null) {
                // TFE, 20191208: check for valid file names!
                // https://stackoverflow.com/a/54552791
                // https://stackoverflow.com/a/49918923
                // https://stackoverflow.com/a/45201446
                // to check for illegal chars in note & group names

                textField = CellUtils.createTextField(this, getConverter());
                
                // TFE, 20191208: check for valid file names!
                FormatHelper.getInstance().initNoteGroupNameTextField(textField);
            }
            
            CellUtils.startEdit(this, getConverter(), null, null, textField);
        }
    }
    
    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setTooltip(null);
        CellUtils.cancelEdit(this, getConverter(), null);
    }
    
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        
        CellUtils.updateItem(this, getConverter(), null, null, textField);

        if (item != null) {
            // add class to indicate not null content - to be used in css
            this.getStyleClass().add(ObjectCell.valueSet);
        } else {
            // add class to indicate null content - to be used in css
            this.getStyleClass().removeAll(ObjectCell.valueSet);
        }

        setTooltip(null);
    }
}