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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTabPane implements IGroupListContainer {
    
    private TabPane myTabPane = null;
    
    // since we have no buttons we do context menus
    private final ContextMenu fullMenu = new ContextMenu();
    private final ContextMenu newMenu = new ContextMenu();

    private final MenuItem newGroup1 = new MenuItem("New Group");
    private final MenuItem newGroup2 = new MenuItem("New Group");
    private final MenuItem renameGroup = new MenuItem("Rename Group");
    private final MenuItem deleteGroup = new MenuItem("Delete Group");
    
    // available colors for tabs to rotate through
    private static final String[] tabColors = { "lightgreen", "lightblue", "lightsalmon", "lightyellow", "cadetblue", "goldenrod", "darkorange" };
    private int colorCount = 0;

    // enable renaming of tabs by showing a textfield as required
    private final TextField nameField = new TextField();  
    private OwnNoteTab activeTab = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private OwnNoteTabPane() {
        super();
    }
    
    public OwnNoteTabPane(final TabPane tabPane) {
        super();
        
        myTabPane = tabPane;

        colorCount = tabColors.length;
        
        initTabPane();
    }

    @SuppressWarnings("unchecked")
    private void initTabPane() {
        initNameField();
        
        initContextMenu();
        
        myTabPane.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
            assert (myEditor != null);
        
            if (newTab != null && !newTab.equals(oldTab)) {
                // select matching notes for group
                final String groupName = ((GroupData) newTab.getUserData()).getGroupName();

                myEditor.setFilterPredicate(groupName);
                myEditor.setNotesTableForNewTab(newTab.getStyle());

                // set color of notes table to tab color
                myTabPane.setStyle(newTab.getStyle());
            } 
        });            
    }

    private void addNewTab() {
        assert (myEditor != null);
        
        final String tabName = "New Group " + myTabPane.getTabs().size();
        
        if (myEditor.createGroupWrapper(tabName)) {
            final OwnNoteTab newTab = new OwnNoteTab(tabName);
            newTab.setEditor(myEditor);
            newTab.setClosable(true);
            newTab.setDetachable(false);
            newTab.setProtectedTab(false);

            // set user data - required for change listener
            final GroupData dataRow = new GroupData();
            dataRow.setGroupName(tabName);
            dataRow.setGroupDelete(OwnNoteFileManager.deleteString);
            dataRow.setGroupCount("0");
            newTab.setUserData(dataRow);

            addOwnNoteTab(newTab);
        }
    }
    
    private void addOwnNoteTab(final OwnNoteTab newTab) {
        if (!newTab.isProtectedTab()) {
            // rename on double click to label
            newTab.getLabel().setOnMouseClicked((MouseEvent event) -> {
                if (event.getClickCount()==2) {
                    startEditingTabLabel();
                }  
            }); 
            
            newTab.setContextMenu(fullMenu);
        } else {
            newTab.setContextMenu(newMenu);
        }
        
        // set color of tab to something fancy
        final int curColor = (myTabPane.getTabs().size()+1) % colorCount;
        newTab.setStyle("tab-color: " + tabColors[curColor]);
        
        myTabPane.getTabs().add(newTab);
    }

    @Override
    public void setEditor(final OwnNoteEditor editor) {
        myEditor = editor;
    }
    
    @Override
    public void setGroups(final ObservableList<Map<String, String>> groupsList, final boolean updateOnly) {
        if (!updateOnly) {
            myTabPane.getTabs().clear();
        }
        final List<String> tabNames =
                myTabPane.getTabs().stream().
                    map(s -> {
                        assert (s instanceof OwnNoteTab);
                        final OwnNoteTab os = (OwnNoteTab) s;
                        return os.getLabelText();
                    }).
                    collect(Collectors.toList());
                
        OwnNoteTab newTab = null;
        for (Map<String, String> group: groupsList) {
           final String groupName = (new GroupData(group)).getGroupName();
            
            if (!updateOnly || !tabNames.contains(groupName)) {
                newTab = new OwnNoteTab(groupName);
                newTab.setEditor(myEditor);
                newTab.setClosable(true);
                newTab.setDetachable(false);

                // ALL and NOT are reserved names
                if (!groupName.equals(GroupData.NOT_GROUPED) && !groupName.equals(GroupData.ALL_GROUPS)) {
                    newTab.setProtectedTab(false);
                } else {
                    newTab.setProtectedTab(true);
                }

                // store full group info for later use
                newTab.setUserData(group);

                addOwnNoteTab(newTab);
            }
        }
    }
    
    @Override
    public int getNotesCount() {
        return myTabPane.getTabs().size();
    }
    
    @Override
    public GroupData getCurrentGroup() {
        return ((GroupData) myTabPane.getSelectionModel().getSelectedItem().getUserData());
    }
    
    @SuppressWarnings("unchecked")
    private void initNameField() {
        // change on ENTER
        nameField.setOnAction((ActionEvent event) -> {
            assert (myEditor != null);
        
            // 1. check whether new name is unique

            // 2. rename group
            final GroupData curGroup = (GroupData) activeTab.getUserData();
            final String newGroupName = nameField.getText();
            
            if (myEditor.renameGroupWrapper(curGroup.getGroupName(), newGroupName)) {
                // 3. if tab with same name exists, delete it
                for (Tab sameNameTab: myTabPane.getTabs()) {
                    assert (sameNameTab instanceof OwnNoteTab);
                    
                    if (((OwnNoteTab) sameNameTab).getLabelText().equals(newGroupName)) {
                        myTabPane.getTabs().remove(sameNameTab);
                        break;
                    }
                }

                // 4. rename tab and update user data
                activeTab.setLabelText(newGroupName);
                curGroup.setGroupName(newGroupName);
                activeTab.setUserData(curGroup);
                
                // update notes list
                myEditor.setFilterPredicate(newGroupName);
            }
        });

        // restore on lost focus
        nameField.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (! newValue) {
                activeTab.setLabelText(activeTab.getLabelText());
            }  
        });  

        // restore on ESC
        nameField.addEventHandler(KeyEvent.KEY_PRESSED, (final KeyEvent keyEvent) -> {
            if (KeyCode.ESCAPE.equals(keyEvent.getCode())) {
                activeTab.setLabelText(activeTab.getLabelText());
                
                keyEvent.consume();
            }
        });
        
    }

    @SuppressWarnings("unchecked")
    private void initContextMenu() {
        newGroup1.setOnAction((ActionEvent event) -> {
            addNewTab();
        });
        newGroup2.setOnAction((ActionEvent event) -> {
            addNewTab();
        });
        renameGroup.setOnAction((ActionEvent event) -> {
            startEditingTabLabel();
        });
        deleteGroup.setOnAction((ActionEvent event) -> {
            assert (myEditor != null);
            
            final Tab deleteTab = myTabPane.getSelectionModel().selectedItemProperty().getValue();
            final GroupData curGroup = new GroupData((Map<String, String>) deleteTab.getUserData());
            
            if (myEditor.deleteGroupWrapper(curGroup)) {
                myTabPane.getTabs().remove(deleteTab);
            }
        });
        
        fullMenu.getItems().addAll(newGroup1, renameGroup, deleteGroup);
        newMenu.getItems().addAll(newGroup2);
    }
    
    private void startEditingTabLabel() {
        // no other types of tabs, please!
        assert (myTabPane.getSelectionModel().selectedItemProperty().getValue() instanceof OwnNoteTab);

        activeTab = (OwnNoteTab) myTabPane.getSelectionModel().selectedItemProperty().getValue();

        // 1. show our textfield on tab label
        nameField.setText(activeTab.getLabelText());  
        activeTab.setGraphic(nameField);  
        nameField.selectAll();  
        nameField.requestFocus();  
    }

    /* Required getter and setter methods are forwarded to internal TableView */

    @Override
    public void setStyle(final String style) {
        myTabPane.setStyle(style);
    }

    @Override
    public void setDisable(final boolean b) {
        myTabPane.setDisable(b);
    }

    @Override
    public void setVisible(final boolean b) {
        myTabPane.setVisible(b);
    }
}
