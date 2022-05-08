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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;
import tf.helper.javafx.StyleHelper;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteTabPane implements IGroupListContainer, IPreferencesHolder  {
    private final String PLUS_TAB = "+";
    
    private TabPane myTabPane = null;
    private String backgroundColor = "white";
    
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
    private String selectedGroupName = TagManager.ReservedTag.All.getTagName();
    
    // TFE, 20200907: keep track of group order
    private final List<String> tabOrder = new LinkedList<>();
    
    private boolean inSortTabs = false;

    private OwnNoteTabPane() {
        super();
    }
    
    public OwnNoteTabPane(final TabPane tabPane, final OwnNoteEditor editor) {
        super();
        myTabPane = tabPane;
        myTabPane.setUserData(this);
        myEditor = editor;
        
        initTabPane();
    }

    private void initTabPane() {
        myTabPane.applyCss();
        // drop down menu doesn't update when renaming tabs - would need to implement own TabPaneSkin :-(
        // https://stackoverflow.com/questions/31734292/show-some-tabs-ahead-from-selected-tab-in-a-javafx-8-tabpane-header
        
        // TF, 20160617 - add a new tab with a "+" tab
        // https://community.oracle.com/thread/2535484?tstart=0
        addPlusTab();
        
        initNameField();
        
        initContextMenu();
        
        myTabPane.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
            assert (myEditor != null);
            
            // TFE, 20201030: sorting of tabs can lead to temporary selection change :-(
            if (inSortTabs) {
                return;
            }
        
            if (newTab != null) {
                boolean doSelectNote = true;
                
                if (!newTab.equals(oldTab)) {
                    assert (newTab instanceof OwnNoteTab);
                    if (((OwnNoteTab) newTab).getTabName().equals("+")) {
                        // you have clicked on the "+" tab
                        if (myTabPane.getTabs().size() > 1) {
                            // and we have more tabs shown - so its not the intial adding of tabs...
                            OwnNoteTab addedTab = addNewTab();
                            if (addedTab != null) {
                               myTabPane.getSelectionModel().select(addedTab);
                            }
                        } else {
                            doSelectNote = false;
                        }
                    } else {
                        // select matching notes for group
                        assert (newTab.getUserData() instanceof TagData);
                        final TagData group = (TagData) newTab.getUserData();

                        myEditor.setGroupFilter(group);
                        // set color of notes table to tab color
                        myEditor.setNotesTableBackgroundColor(((OwnNoteTab) newTab).getBackgroundColor());

                        setBackgroundColor(((OwnNoteTab) newTab).getBackgroundColor());
                    }
                }
                
                if (doSelectNote) {
                    // TFE, 20201030: select edited note
                    myEditor.selectFirstOrCurrentNote();
                }
            } 
            
        });            
    }

    private void addPlusTab() {
        final OwnNoteTab newTab = new OwnNoteTab(PLUS_TAB, null, null, myEditor);
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
        
        final TagData newTag = TagManager.getInstance().groupForName(tabName, true);
        final OwnNoteTab newTab = new OwnNoteTab(tabName, "0", newTag, myEditor);
        newTab.setClosable(true);
        newTab.setDetachable(false);
        newTab.setProtectedTab(false);
        newTab.setDroptarget(true);

        // set user data - required for change listener
        // and create new tab while where at it...
        newTab.setUserData(newTag);

        addOwnNoteTab(newTab);

        // update internal bookkeeping
        updateTabOrder();

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
        final String groupColor = newTab.getTabTag().getColorName();
        newTab.setBackgroundColor(groupColor);
        //System.out.println("addOwnNoteTab - groupName, groupColor: " + newTab.getTabName() + ", " + groupColor);
        
        myTabPane.getTabs().add(myTabPane.getTabs().size() - 1, newTab);
    }
    
    public void sortTabs() {
        final List<Tab> tabs = new LinkedList<>(myTabPane.getTabs());

        inSortTabs = true;
        
        // now sort tabs accordingly
        int startIndex = 2;
        for (String tabLabel : tabOrder) {
            // find tab in list of tabs
            Optional<Tab> tab = tabs.stream().filter((t) -> {
                assert (t instanceof OwnNoteTab);
                
                return tabLabel.equals(((OwnNoteTab) t).getTabName());
            }).findFirst();
            
            // move if found and different position
            if (tab.isPresent()) {
                if (tabs.indexOf(tab.get()) != startIndex) {
//                    System.out.println("Moving tab '" + tabLabel + "' from position " + tabs.indexOf(tab.get()) + " to position " + startIndex);
                    final Tab moveTab = tab.get();
                    // restore selection if required
                    final boolean isSelected = tab.equals(myTabPane.getSelectionModel().getSelectedItem());
                    
                    myTabPane.getTabs().remove(tab.get());
                    myTabPane.getTabs().add(startIndex, tab.get());
                    
                    if (isSelected) {
                        myTabPane.getSelectionModel().select(moveTab);
                    }
                } else {
//                    System.out.println("Nothing to do for tab '" + tabLabel + "'");
                }
                
                startIndex++;
            } else {
//                System.out.println("Tab '" + tabLabel + "' not found!");
            }
        }

        inSortTabs = false;
    }
    
    public void updateTabOrder() {
        tabOrder.clear();
        for (Tab tab: myTabPane.getTabs()) {
            assert (tab instanceof OwnNoteTab);

            final TagData tabTag = ((OwnNoteTab) tab).getTabTag();
            final String tabName = ((OwnNoteTab) tab).getTabName();
            if (!TagManager.isSpecialGroup(tabTag) && !tabName.equals(PLUS_TAB)) {
                // add group name
                tabOrder.add(tabName);
            }
        }
        
//        System.out.println("tab order updated: " + tabOrder.stream().collect(Collectors.joining(OwnNoteEditorPreferences.PREF_STRING_SEP)));
    }

    @Override
    public void loadPreferences(final IPreferencesStore store) {
        final String prefString = OwnNoteEditorPreferences.RECENT_TAB_ORDER.getAsType();

        if (prefString.isEmpty()) {
            return;
        }
        if (!prefString.startsWith(OwnNoteEditorPreferences.PREF_STRING_PREFIX)) {
            return;
        }
        if (!prefString.endsWith(OwnNoteEditorPreferences.PREF_STRING_SUFFIX)) {
            return;
        }
        
        tabOrder.clear();
        tabOrder.addAll(new LinkedList<>(Arrays.asList(
                        prefString.substring(OwnNoteEditorPreferences.PREF_STRING_PREFIX.length(), prefString.length()-OwnNoteEditorPreferences.PREF_STRING_SUFFIX.length()).
                        strip().split(OwnNoteEditorPreferences.PREF_STRING_SEP))));
        
        sortTabs();
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
        // save group names in current order of tabs on pane - ignoring All, Not Grouped, +
        final StringBuilder prefString = new StringBuilder();
        
        prefString.append(OwnNoteEditorPreferences.PREF_STRING_PREFIX);
        
        updateTabOrder();
        prefString.append(tabOrder.stream().collect(Collectors.joining(OwnNoteEditorPreferences.PREF_STRING_SEP)));
        
        prefString.append(OwnNoteEditorPreferences.PREF_STRING_SUFFIX);
        
        OwnNoteEditorPreferences.RECENT_TAB_ORDER.put(prefString);
    }

    @Override
    public void setGroups(final ObservableList<TagData> groupsList, final boolean updateOnly) {
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
                    map((TagData s) -> {
                        return s.getName();
                    }).
                    collect(Collectors.toList());
                
        OwnNoteTab newTab = null;
        for (TagData group: groupsList) {
           final String groupName = group.getName();
           final String groupCount = String.valueOf(group.getLinkedNotes().size());
            
            if (!updateOnly || !currentGroupNames.contains(groupName)) {
                newTab = new OwnNoteTab(groupName, groupCount, group, myEditor);
                newTab.setClosable(true);
                newTab.setDetachable(false);

                // ALL and NOT are reserved names
                if (!TagManager.isSpecialGroup(group)) {
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
        
        // sort with old names
        sortTabs();
        // update internal bookkeeping
        updateTabOrder();
    }
    
    @Override
    public void selectGroupForNote(final Note note) {
        // find tab that has group as userdata
        final TagData curGroup = note.getGroup();
        
        Tab groupTab = null;
        for (Tab tab : myTabPane.getTabs()) {
            if (curGroup.equals(tab.getUserData())) {
                groupTab = tab;
                break;
            }
        }
        
        if (groupTab != null) {
            myTabPane.getSelectionModel().select(groupTab);
        }
    }
    
    @Override
    public TagData getCurrentGroup() {
        return ((TagData) myTabPane.getSelectionModel().getSelectedItem().getUserData());
    }
    
    private void initNameField() {
        // TFE, 20191208: check for valid file names!
        FormatHelper.getInstance().initNoteGroupNameTextField(nameField, FormatHelper.VALIDNOTEGROUPNAME);

        // change on ENTER
        nameField.setOnAction((ActionEvent event) -> {
            assert (myEditor != null);
        
            // 1. check whether new name is not "ALL" or "+"
            final String newGroupName = nameField.getText();
            if (TagManager.ReservedTag.All.getTagName().equals(newGroupName) || PLUS_TAB.equals(newGroupName)) {
                // error message
                myEditor.showAlert(
                        Alert.AlertType.ERROR, 
                        "Error Dialog", 
                        "New group name invalid.", "A group cannot be named '" + TagManager.ReservedTag.All.getTagName() + "' or '" + PLUS_TAB + "'.");
                
                return;
            }

            // 2. rename group
            final TagData curGroup = (TagData) activeTab.getUserData();
            if (TagManager.getInstance().renameTag(curGroup, newGroupName)) {
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
                
                // update notes list
                myEditor.setGroupFilter(curGroup);
                
                // TFE, 20200907: update internal bookkeeping
                updateTabOrder();
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
            final TagData curGroup = (TagData) deleteTab.getUserData();
            
            if (TagManager.getInstance().deleteTag(curGroup)) {
                myEditor.initFromDirectory(false, false);
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
            selectedGroupName = getCurrentGroup().getName();
        } else {
            selectedGroupName = TagManager.ReservedTag.All.getTagName();
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
    public void setBackgroundColor(final String color) {
        myTabPane.setStyle(StyleHelper.addAndRemoveStyles(
                myTabPane, 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, color), 
                StyleHelper.cssString(OwnNoteEditor.GROUP_COLOR_CSS, backgroundColor)));
        backgroundColor = color;
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
