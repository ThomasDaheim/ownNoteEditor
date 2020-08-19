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
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
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
    private final String PLUS_TAB = "+";
    
    private TabPane myTabPane = null;
    
    // since we have no buttons we do context menus
    private final ContextMenu fullMenu = new ContextMenu();
    private final ContextMenu newMenu = new ContextMenu();

    private final MenuItem newGroup1 = new MenuItem("New Group");
    private final MenuItem newGroup2 = new MenuItem("New Group");
    private final MenuItem renameGroup = new MenuItem("Rename Group");
    private final MenuItem deleteGroup = new MenuItem("Delete Group");

    // enable renaming of tabs by showing a textfield as required
    private final TextField nameField = new TextField();  
    private OwnNoteTab activeTab = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    // store selected group before changing the group lists for later re-select
    private String selectedGroupName = GroupData.ALL_GROUPS;

    private OwnNoteTabPane() {
        super();
    }
    
    public OwnNoteTabPane(final TabPane tabPane, final OwnNoteEditor editor) {
        super();
        myTabPane = tabPane;
        myEditor = editor;
        
        initTabPane();
    }

    private void initTabPane() {
        // drop down menu doesn't update when renaming tabs - would need to implement own TabPaneSkin :-(
        // https://stackoverflow.com/questions/31734292/show-some-tabs-ahead-from-selected-tab-in-a-javafx-8-tabpane-header
        
        // TF, 20160617 - add a new tab with a "+" tab
        // https://community.oracle.com/thread/2535484?tstart=0
        addPlusTab();
        
        initNameField();
        
        initContextMenu();
        
        myTabPane.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
            assert (myEditor != null);
        
            if (newTab != null && !newTab.equals(oldTab)) {
                assert (newTab instanceof OwnNoteTab);
                if (((OwnNoteTab) newTab).getTabName().equals("+")) {
                    // you have clicked on the "+" tab
                    if (myTabPane.getTabs().size() > 1) {
                        // and we have more tabs shown - so its not the intial adding of tabs...
                        OwnNoteTab addedTab = addNewTab();
                        if (addedTab != null) {
                           myTabPane.getSelectionModel().select(addedTab);
                        }
                    }
                } else {
                    // select matching notes for group
                    assert (newTab.getUserData() instanceof GroupData);
                    final String groupName = ((GroupData) newTab.getUserData()).getGroupName();

                    myEditor.setGroupNameFilter(groupName);
                    // set color of notes table to tab color
                    myEditor.setNotesTableForNewTab(newTab.getStyle());

                    myTabPane.setStyle(newTab.getStyle());
                }
            } 
        });            
    }

    private void addPlusTab() {
        final OwnNoteTab newTab = new OwnNoteTab(PLUS_TAB, null, myEditor);
        newTab.setClosable(false);
        newTab.setDetachable(false);
        newTab.setProtectedTab(true);
        newTab.setDroptarget(false);
        
        // special style - not a normal tab...
        newTab.getStyleClass().add("plusTab");
        
        myTabPane.getTabs().add(newTab);
    }

    private OwnNoteTab addNewTab() {
        assert (myEditor != null);
        
        final String tabName = "New Group " + myTabPane.getTabs().size();
        
        OwnNoteTab newTab = null;
        if (myEditor.createGroupWrapper(tabName)) {
            newTab = new OwnNoteTab(tabName, "0", myEditor);
            newTab.setClosable(true);
            newTab.setDetachable(false);
            newTab.setProtectedTab(false);
            newTab.setDroptarget(true);

            // set user data - required for change listener
            final GroupData dataRow = new GroupData();
            dataRow.setGroupName(tabName);
            dataRow.setGroupDelete(OwnNoteFileManager.deleteString);
            dataRow.setGroupCount("0");
            newTab.setUserData(dataRow);

            addOwnNoteTab(newTab);
        }

        return newTab;
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
        final String groupColor = myEditor.getNewGroupColor(newTab.getTabName());
        newTab.setTabColor(groupColor);
        //System.out.println("addOwnNoteTab - groupName, groupColor: " + newTab.getTabName() + ", " + groupColor);
        
        myTabPane.getTabs().add(myTabPane.getTabs().size() - 1, newTab);
    }

    @Override
    public void setGroups(final ObservableList<GroupData> groupsList, final boolean updateOnly) {
        if (!updateOnly) {
            myTabPane.getTabs().clear();
            addPlusTab();
        } else {
            storeSelectedGroup();
        }
        
        // name lists for current and new groups
        final List<String> currentGroupNames =
                myTabPane.getTabs().stream().
                    map(s -> {
                        assert (s instanceof OwnNoteTab);
                        final OwnNoteTab os = (OwnNoteTab) s;
                        return os.getTabName();
                    }).
                    collect(Collectors.toList());
        final List<String> newGroupNames =
                groupsList.stream().
                    map((GroupData s) -> {
                        return s.getGroupName();
                    }).
                    collect(Collectors.toList());
                
        OwnNoteTab newTab = null;
        for (GroupData group: groupsList) {
           final String groupName = group.getGroupName();
           final String groupCount = group.getGroupCount();
            
            if (!updateOnly || !currentGroupNames.contains(groupName)) {
                newTab = new OwnNoteTab(groupName, groupCount, myEditor);
                newTab.setClosable(true);
                newTab.setDetachable(false);

                // ALL and NOT are reserved names
                if (!groupName.equals(GroupData.NOT_GROUPED) && !groupName.equals(GroupData.ALL_GROUPS)) {
                    newTab.setProtectedTab(false);
                    newTab.setDroptarget(true);
                    newTab.setClosable(true);
                } else {
                    newTab.setProtectedTab(true);
                    newTab.setDroptarget(false);
                    newTab.setClosable(false);
                }

                // store full group info for later use
                newTab.setUserData(group);

                addOwnNoteTab(newTab);
            } else {
                // TF, 20161105: update tabCount
                for (Tab tab: myTabPane.getTabs()) {
                    assert (tab instanceof OwnNoteTab);
                    final String tabLabel = ((OwnNoteTab) tab).getTabName();
                    
                    if (groupName.equals(tabLabel)) {
                        ((OwnNoteTab) tab).setTabCount(groupCount);
                    }
                }
            }
        }
        // TF, 20160816: and now check reverse - a tab exists thats no longer in the groupsList (can only happen for updateOnly)
        if (updateOnly) {
            for (Tab tab: myTabPane.getTabs()) {
                assert (tab instanceof OwnNoteTab);

                final String tabLabel = ((OwnNoteTab) tab).getTabName();
                if (!newGroupNames.contains(tabLabel) &&
                        !PLUS_TAB.equals(tabLabel)) {
                    // if tab name not in group list AND not the "+" tab: remove it
                    myTabPane.getTabs().remove(tab);
                }
            }
        }
        
        if (!updateOnly) {
            // select the "All" tab
            myTabPane.getSelectionModel().select(0);
        } else {
            // select the previous tab
            restoreSelectedGroup();
        }
    }
    
    public void selectGroupForNote(final NoteData noteData) {
        // find tab that has group as userdata
        final GroupData groupData = OwnNoteFileManager.getInstance().getGroupData(noteData);
        
        Tab groupTab = null;
        for (Tab tab : myTabPane.getTabs()) {
            if (groupData.equals(tab.getUserData())) {
                groupTab = tab;
                break;
            }
        }
        
        if (groupTab != null) {
            myTabPane.getSelectionModel().select(groupTab);
        }
    }
    
    @Override
    public GroupData getCurrentGroup() {
        return ((GroupData) myTabPane.getSelectionModel().getSelectedItem().getUserData());
    }
    
    public String getMatchingPaneColor(final String groupName) {
        String groupColor = "darkgrey";

        // name lists for current and new groups
        final List<String> groupNames =
            myTabPane.getTabs().stream().
                map(s -> {
                    assert (s instanceof OwnNoteTab);
                    final OwnNoteTab os = (OwnNoteTab) s;
                    return os.getTabName();
                }).
                collect(Collectors.toList());
        
        // find tab for group name and return its tab-color style value
        if (groupNames.contains(groupName)) {
            final OwnNoteTab groupTab = (OwnNoteTab) myTabPane.getTabs().get(groupNames.indexOf(groupName));
             
            groupColor = groupTab.getTabColor();
        }
        
        return groupColor;
    }
    
    private void initNameField() {
        // TFE, 20191208: check for valid file names!
        FormatHelper.getInstance().initNameTextField(nameField);

        // change on ENTER
        nameField.setOnAction((ActionEvent event) -> {
            assert (myEditor != null);
        
            // 1. check whether new name is not "ALL" or "+"
            final String newGroupName = nameField.getText();
            if (GroupData.ALL_GROUPS.equals(newGroupName) || PLUS_TAB.equals(newGroupName)) {
                // error message
                myEditor.showAlert(Alert.AlertType.ERROR, "Error Dialog", "New group name invalid.", "A group cannot be named '" + GroupData.ALL_GROUPS + "' or '" + PLUS_TAB + "'.");
                
                return;
            }

            // 2. rename group
            final GroupData curGroup = (GroupData) activeTab.getUserData();
            
            if (myEditor.renameGroupWrapper(curGroup.getGroupName(), newGroupName)) {
                // 3. if tab with same name exists, delete it
                for (Tab sameNameTab: myTabPane.getTabs()) {
                    assert (sameNameTab instanceof OwnNoteTab);
                    
                    if (((OwnNoteTab) sameNameTab).getTabName().equals(newGroupName)) {
                        myTabPane.getTabs().remove(sameNameTab);
                        break;
                    }
                }

                // 4. rename tab and update user data
                activeTab.setLabelText(newGroupName);
                curGroup.setGroupName(newGroupName);
                activeTab.setUserData(curGroup);
                
                // update notes list
                myEditor.setGroupNameFilter(newGroupName);
            }
        });

        // restore on lost focus
        nameField.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (! newValue) {
                activeTab.setLabelText(activeTab.getTabName());
            }  
        });  

        // restore on ESC
        nameField.addEventHandler(KeyEvent.KEY_PRESSED, (final KeyEvent keyEvent) -> {
            if (KeyCode.ESCAPE.equals(keyEvent.getCode())) {
                activeTab.setLabelText(activeTab.getTabName());
                
                keyEvent.consume();
            }
        });
        
    }

    private void initContextMenu() {
        newGroup1.setOnAction((ActionEvent event) -> {
            OwnNoteTab addedTab = addNewTab();
            if (addedTab != null) {
               myTabPane.getSelectionModel().select(addedTab);
            }
        });
        newGroup2.setOnAction((ActionEvent event) -> {
            OwnNoteTab addedTab = addNewTab();
            if (addedTab != null) {
               myTabPane.getSelectionModel().select(addedTab);
            }
        });
        renameGroup.setOnAction((ActionEvent event) -> {
            startEditingTabLabel();
        });
        deleteGroup.setOnAction((ActionEvent event) -> {
            assert (myEditor != null);
            
            final Tab deleteTab = myTabPane.getSelectionModel().selectedItemProperty().getValue();
            final GroupData curGroup = (GroupData) deleteTab.getUserData();
            
            if (myEditor.deleteGroupWrapper(curGroup)) {
                myEditor.initFromDirectory(false);
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
        nameField.setText(activeTab.getTabName());  
        activeTab.setGraphic(nameField);  
        nameField.selectAll();  
        nameField.requestFocus();  
    }

    private void storeSelectedGroup() {
        if (myTabPane.getSelectionModel().getSelectedItem() != null) {
            selectedGroupName = getCurrentGroup().getGroupName();
        } else {
            selectedGroupName = GroupData.ALL_GROUPS;
        }
    }

    private void restoreSelectedGroup() {
        int i = 0;
        int selectIndex = 0;

        for (Tab tab: myTabPane.getTabs()) {
            assert (tab instanceof OwnNoteTab);

            if (selectedGroupName.equals(((OwnNoteTab) tab).getTabName())) {
                selectIndex = i;
                break;
            }
            i++;
        }

        myTabPane.getSelectionModel().clearAndSelect(selectIndex);
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
