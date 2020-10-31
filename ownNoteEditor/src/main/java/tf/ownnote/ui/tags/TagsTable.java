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
package tf.ownnote.ui.tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.notes.NoteData;

/**
 * Table for editing tags - similar to e.g. bulk edit table in wordpress.
 * First line check boxes and one "master" checkbox in the title bar.
 * 
 * Behaviour similar to wordpress:
 * - click bulk checkbox: all checked / unchecked
 * - all checked manually: bulk checked as well
 * - one unchecked: bulk unchecked as well
 * 
 * Alternative not done here (maybe later? maybe both?) as in piwigo:
 * - dropdown instead of checkbox: All, None, Invert
 * 
 * @author thomas
 */
public class TagsTable extends TableView<TagInfo>  {
    private BiConsumer<String, String> renameFunction;
    private final CheckBox bulkCheck = new CheckBox();
    
    private boolean inBulkCheckAction = false;
    private boolean inSingleCheckAction = false;
    
    public TagsTable() {
        initTableView();
    }
    
    private void initTableView() {
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setEditable(true);
        setPlaceholder(new Label(""));

        final double tableHeight = 300.0;
        setPrefHeight(tableHeight);
        setMinHeight(tableHeight);
        setMaxHeight(tableHeight);
        
        // columns: action checkbox, tagname (editable), icon (combobox of symbols), notes count
        // https://github.com/SaiPradeepDandem/javafx-demos/blob/master/src/main/java/com/ezest/javafx/demogallery/tableviews/TableViewCheckBoxColumnDemo.java

        // selected: boolean
        final TableColumn<TagInfo, Boolean> selectedCol = new TableColumn<>();
        selectedCol.setText("");
        // set special checkbox as graphics + getter & setter for it
        // com.ezest.javafx.demogallery.tableviews.TableViewCheckBoxColumnDemo
        selectedCol.setGraphic(getBulkCheck());
        selectedCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedCol.setCellFactory((TableColumn<TagInfo, Boolean> p) -> {
            final CheckBoxTableCell<TagInfo, Boolean> cell = new CheckBoxTableCell<>((t) -> {
                // how to listen to changes... not perfect, but better than nothing
                // https://stackoverflow.com/a/28671914
//                System.out.println("singleCheck: changed");
                if (!inBulkCheckAction) {
                    // no endless change events please...
                    inSingleCheckAction = true;
                    // Checking for an unselected employee in the table view.
                    boolean unSelectedFlag = false;
                    for (TagInfo tagInfo : getItems()) {
                        if (!tagInfo.isSelected()) {
                            unSelectedFlag = true;
                            break;
                        }
                    }
                    /*
                     * If at least one item is not selected, then deselecting the check box in the table column header, else if all
                     * items are selected, then selecting the check box in the header.
                     */
//                    System.out.println("singleCheck: updating bulkCheck to " + !unSelectedFlag);
                    bulkCheck.setSelected(!unSelectedFlag);
                    inSingleCheckAction = false;
                }
                return getItems().get(t).selectedProperty();
            });
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        selectedCol.setEditable(true);

        // name: string
        final TableColumn<TagInfo, String> nameCol = new TableColumn<>();
        nameCol.setText("Tag");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit((TableColumn.CellEditEvent<TagInfo, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                assert renameFunction != null;
                renameFunction.accept(t.getOldValue(), t.getNewValue());

                t.getRowValue().setName(t.getNewValue());
            }
        });
        // we can even change the name - since we use a key for the preference store
        nameCol.setEditable(true);
        
        // addAll() leads to unchecked cast - and we don't want that
        getColumns().add(selectedCol);
        getColumns().add(nameCol);

        
        // add, remove via context menu
        setRowFactory((TableView<TagInfo> tableView) -> {
            final TableRow<TagInfo> row = new TableRow<>(){
                @Override
                protected void updateItem(TagInfo item, boolean empty){
                    super.updateItem(item, empty);
                    
                    setContextMenu(newContextMenu(this));
                }
            };

            return row ;  
        });  
        
        setContextMenu(newContextMenu(null));
    }
    
    private ContextMenu newContextMenu(final TableRow<TagInfo> row) {
        final ContextMenu contextMenu = new ContextMenu();

        // new: creates of same type & inserts
        final MenuItem newMenuItem = new MenuItem("New");
        newMenuItem.setOnAction((ActionEvent event) -> {
            final TagInfo tagInfo = new TagInfo(false, "New Tag");
            getItems().add(tagInfo);
        });

        if (row != null && !row.isEmpty()) {
            // delete: only if on a row that is not empty
            final MenuItem deleteMenuItem = new MenuItem("Delete");
            deleteMenuItem.setOnAction((ActionEvent event) -> {
                assert renameFunction != null;
                renameFunction.accept(row.getItem().getName(), null);
                
                getItems().remove(row.getItem());
            });

            contextMenu.getItems().addAll(newMenuItem, deleteMenuItem);
        } else {
            contextMenu.getItems().addAll(newMenuItem);
        }

        return contextMenu;
    }
    
    private CheckBox getBulkCheck() {
        // adding EventHandler to the CheckBox to select/deselect all items in table.
        bulkCheck.setOnAction((t) -> {
//            System.out.println("bulkCheck: clicked");
            // no endless change events please...
            if (!inSingleCheckAction) {
                inBulkCheckAction = true;
//                System.out.println("bulkCheck: updating individual checkboxes to " + bulkCheck.isSelected());
                getItems().stream().forEach((tagInfo) -> {
                    tagInfo.setSelected(bulkCheck.isSelected());
                });
                inBulkCheckAction = false;
            }
        });
        
        return bulkCheck;
    }

    // callback to do the work
    public void setRenameFunction(final BiConsumer<String, String> funct) {
        renameFunction = funct;
    }
    
    // is the bulk checkbox selected?
    public boolean isBulkChecked() {
        return bulkCheck.isSelected();
    }
    
    public void fillTableView(final List<String> excludeTags) {
        final List<NoteData> notesList = OwnNoteFileManager.getInstance().getNotesList();
        final Set<String> tagsList = new HashSet<>();
        for (NoteData note : notesList) {
            tagsList.addAll(note.getMetaData().getTags());
        }
        
        final List<TagInfo> itemList = new ArrayList<>();
        for (String tag : tagsList) {
            if (!excludeTags.contains(tag)) {
                itemList.add(new TagInfo(false, tag));
            }
        }
        
        getItems().clear();
        getItems().addAll(itemList);
    }
}
