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

import java.util.Map;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTableColumn {
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
    
    private TableColumn<Map, String> myTableColumn = null;
    
    private OwnNoteTableColumn() {
        super();
    }
            
    public OwnNoteTableColumn(final TableColumn<Map, String> tableColumn) {
        super();    
        
        myTableColumn = tableColumn;

        initTableColumn();
    }
    
    public void setEditor(final OwnNoteEditor editor) {
        myEditor = editor;
    }
    
    public void setWidthPercentage(final double percentage) {
        myTableColumn.prefWidthProperty().bind(myTableColumn.getTableView().widthProperty().multiply(percentage));
    }
    
    public void setTableColumnProperties(final OwnNoteEditor editor, final double percentage, final String valueName, final boolean linkCursor) {
        setEditor(editor);
        setWidthPercentage(percentage);
        myTableColumn.setCellValueFactory(new MapValueFactory<String>(valueName));
        myTableColumn.setCellFactory(createObjectCellFactory(linkCursor));
    }

    private Callback<TableColumn<Map, String>, TableCell<Map, String>>
        createObjectCellFactory(final boolean linkCursor) {
        assert (this.myEditor != null);
        return (TableColumn<Map, String> param) -> new ObjectCell(this.myEditor, linkCursor, new UniversalMouseEvent(this.myEditor));
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
    
    @SuppressWarnings("unchecked")
    private void handleTableClick(final TableCell clickedCell) {
        assert (this.myEditor != null);
        
        // only do things for row with data
        if (clickedCell.getTableRow().isEmpty()) {
            return;
        }
                            
        boolean reInit = false;
        
        NoteData curNoteData = null;
        GroupData curGroupData = null;
        switch(clickedCell.getId()) {
            case "noteNameColFXML":
                //System.out.println("Clicked in noteNameCol");
                curNoteData =
                    new NoteData((Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex()));
                reInit = this.myEditor.editNote(curNoteData);
                break;
            case "noteDeleteColFXML":
                //System.out.println("Clicked in noteDeleteCol");
                curNoteData =
                    new NoteData((Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex()));
                reInit = this.myEditor.deleteNoteWrapper(curNoteData);
                break;
            case "groupDeleteColFXML":
                //System.out.println("Clicked in groupDeleteCol");
                curGroupData =
                    new GroupData((Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex()));
                reInit = this.myEditor.deleteGroupWrapper(curGroupData);
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
    private static final String labelClass = "myLabel";
    
    // store link back to the controller of the scene for callback
    private OwnNoteEditor myOwnNoteEditor;
    
    public ObjectCell(final OwnNoteEditor ownNoteEditor, final boolean linkCursor, final EventHandler<MouseEvent> mouseEvent) {
        super(new DefaultStringConverter());
        
        if (linkCursor) {
            this.setCursor(Cursor.HAND);
        }
        
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent);
    }
    
    @Override
    public void startEdit() {
        if (GroupData.ALL_GROUPS.equals(getText())
                || GroupData.NOT_GROUPED.equals(getText())) {
            return;
        }
        super.startEdit();
    }
    
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null) {
            setText((String) item);
            setGraphic(null);
            
            // add class to indicate not null content - to be used in css
            this.getStyleClass().add(ObjectCell.valueSet);
        } else {
            setText(null);
            setGraphic(null);
            
            // add class to indicate null content - to be used in css
            this.getStyleClass().removeAll(ObjectCell.valueSet);
        }
        
        // pass on styles to children so that css can find them
        // TODO: getChildren() returns empty list
        for (Node childNode: this.getChildren()) {
            childNode.getStyleClass().removeAll(ObjectCell.labelClass);
            childNode.getStyleClass().add(ObjectCell.labelClass);
        }
    }
}