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
package tf.ownnote.ui.main;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.DirectoryChooser;
import tf.ownnote.ui.helper.GroupData;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.helper.OwnNoteTabPane;
import tf.ownnote.ui.helper.OwnNoteTableColumn;
import tf.ownnote.ui.helper.OwnNoteTableView;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditor implements Initializable {

    private final OwnNoteFileManager myFileManager;
    
    private final List<String> filesInProgress = new LinkedList<String>();

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    
    private final static String NEWNOTENAME = "New Note";
    
    private final static int TEXTFIELDWIDTH = 100;
    
    private final List<String> realGroupNames = new LinkedList<String> ();
    
    private FilteredList<Map<String, String>> filteredData = null;
    
    private final BooleanProperty inEditMode = new SimpleBooleanProperty();
    
    private boolean handleQuickSave = false;
    // should we show standard ownNote face or oneNotes?
    private OwnNoteEditorParameters.LookAndFeel classicLook;
    private Double classicGroupWidth;
    private Double oneNoteGroupWidth;
    
    private IGroupListContainer myGroupList = null;
    
    private OwnNoteTabPane groupsPane = null;
    @FXML
    private BorderPane borderPane;
    @FXML
    private GridPane gridPane;
    private SplitPane dividerPane;
    @FXML
    private TableView<Map<String, String>> notesTableFXML;
    private OwnNoteTableView notesTable = null;
    @FXML
    private TableView<Map<String, String>> groupsTableFXML;
    private OwnNoteTableView groupsTable = null;
    @FXML
    private HBox pathBox;
    @FXML
    private Label ownCloudPath;
    @FXML
    private Button setOwnCloudPath;
    @FXML
    private TableColumn<Map, String> noteNameColFXML;
    private OwnNoteTableColumn noteNameCol = null;
    @FXML
    private TableColumn<Map, String> noteModifiedColFXML;
    private OwnNoteTableColumn noteModifiedCol = null;
    @FXML
    private TableColumn<Map, String> noteDeleteColFXML;
    private OwnNoteTableColumn noteDeleteCol = null;
    @FXML
    private TableColumn<Map, String> groupNameColFXML;
    private OwnNoteTableColumn groupNameCol = null;
    @FXML
    private TableColumn<Map, String> groupDeleteColFXML;
    private OwnNoteTableColumn groupDeleteCol = null;
    @FXML
    private TableColumn<Map, String> groupCountColFXML;
    private OwnNoteTableColumn groupCountCol = null;
    @FXML
    private TableColumn<Map, String> noteGroupColFXML;
    private OwnNoteTableColumn noteGroupCol = null;
    @FXML
    private Button newButton;
    @FXML
    private ComboBox<String> groupNameBox;
    @FXML
    private TextField groupNameText;
    @FXML
    private Button createButton;
    @FXML
    private Button cancelButton;
    @FXML
    private TextField noteNameText;
    @FXML
    private HTMLEditor noteEditorFXML;
    private OwnNoteHTMLEditor noteEditor = null;
    @FXML
    private Button quickSaveButton;
    @FXML
    private Button saveButton;
    @FXML
    private HBox buttonBox;
    @FXML
    private TabPane groupsPaneFXML;
    @FXML
    private MenuBar menuBar;
    @FXML
    private RadioMenuItem classicLookAndFeel;
    @FXML
    private RadioMenuItem oneNoteLookAndFeel;
    @FXML
    private ToggleGroup LookAndFeel;
    @FXML
    private Menu menuLookAndFeel;

    public OwnNoteEditor() {
        myFileManager = new OwnNoteFileManager(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // defer initEditor since we need to know the value of the prameters...
    }
    
    public void stop() {
        myFileManager.stop();
        
        // store current percentage of group column width
        // if increment is passed as parameter, we need to remove it from the current value
        // otherwise, the percentage grows with each call :-)
        final String percentWidth = String.valueOf(gridPane.getColumnConstraints().get(0).getPercentWidth());
        // store in the preferences
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTCLASSICGROUPWIDTH, percentWidth);
        } else {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTONENOTEGROUPWIDTH, percentWidth);
        }
    }
    
    public void setParameters() {
        // set look & feel    
        // 1. use passed parameters
        if (OwnNoteEditor.parameters.getLookAndFeel().isPresent()) {
            classicLook = OwnNoteEditor.parameters.getLookAndFeel().get();
        } else {
            // fix for issue #20
            // 2. try the preference settings - what was used last time?
            try {
                classicLook = OwnNoteEditorParameters.LookAndFeel.valueOf(
                        OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name()));
                // System.out.println("Using preference for classicLook: " + classicLook);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // issue #30: get percentages for group column width for classic and onenote look & feel
        try {
            classicGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTCLASSICGROUPWIDTH, "18.3333333"));
            oneNoteGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTONENOTEGROUPWIDTH, "33.3333333"));
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        // paint the look
        initEditor();

        // init pathlabel to parameter or nothing
        String pathname = "";
        // 1. use any passed parameters
        if (OwnNoteEditor.parameters.getOwnCloudDir().isPresent()) {
            pathname = OwnNoteEditor.parameters.getOwnCloudDir().get();
        } else {
            // 2. try the preferences setting - most recent file that was opened
            try {
                pathname = OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, "");
                // System.out.println("Using preference for ownCloudDir: " + pathname);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // 3. set the value
        ownCloudPath.setText(pathname);
    }

    //
    // basic setup is a 2x2 gridpane with a 2 part splitpane in the lower row spanning both cols
    //
    // ------------------------------------------------------
    // |                          |                         |
    // | pathBox                  | classic: buttonBox      |
    // |                          | oneNote: groupsPaneFXML |
    // |                          |                         |
    // ------------------------------------------------------
    // |                          |                         |
    // | dividerPane              |                         |
    // |                          |                         |
    // | classic: groupsTableFXML | classic: notesTableFXML |
    // | oneNote: notesTableFXML  | both: noteEditorFXML    |
    // |                          |                         |
    // ------------------------------------------------------
    //
    // to be able to do proper layout in scenebuilder everything except the dividerPane
    // are added to the fxml into the gridpane - code below does the re-arrangement based on 
    // value of classicLook
    //
    @SuppressWarnings("unchecked")
    private void initEditor() {
        // init menu handling
        // 1. add listener to track changes of layout
        classicLookAndFeel.getToggleGroup().selectedToggleProperty().addListener(
            (ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2) -> {
                // when starting up things might not be initialized properly
                if (arg2 != null) {
                    assert (arg2 instanceof RadioMenuItem);

                    // store in the preferences
                    if (((RadioMenuItem) arg2).equals(classicLookAndFeel)) {
                        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name());
                    } else {
                        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.oneNote.name());
                    }
                }
            });
        // 2. select entry based on value of classicLook
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            classicLookAndFeel.setSelected(true);
        } else {
            oneNoteLookAndFeel.setSelected(true);
        }
        
        // init our wrappers to FXML classes...
        notesTable = new OwnNoteTableView(notesTableFXML);
        groupsTable = new OwnNoteTableView(groupsTableFXML);
        
        noteNameCol = new OwnNoteTableColumn(noteNameColFXML);
        noteModifiedCol = new OwnNoteTableColumn(noteModifiedColFXML);
        noteDeleteCol = new OwnNoteTableColumn(noteDeleteColFXML);
        groupNameCol = new OwnNoteTableColumn(groupNameColFXML);
        groupDeleteCol = new OwnNoteTableColumn(groupDeleteColFXML);
        groupCountCol = new OwnNoteTableColumn(groupCountColFXML);
        noteGroupCol = new OwnNoteTableColumn(noteGroupColFXML);
        
        groupsPane = new OwnNoteTabPane(groupsPaneFXML);
        
        noteEditor = new OwnNoteHTMLEditor(noteEditorFXML);
        noteEditor.setEditor(this);
        
        // hide borders
        borderPane.setBottom(null);
        borderPane.setLeft(null);
        borderPane.setRight(null);
        
        // fixed height top row in grid and set height & width of rest
        gridPane.getRowConstraints().clear();
        gridPane.getRowConstraints().add(new RowConstraints(40, 40, 40));
        RowConstraints row2 = new RowConstraints(160, 560, Double.MAX_VALUE);
        row2.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(row2);
        gridPane.getColumnConstraints().clear();
        ColumnConstraints column1 = new ColumnConstraints();
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            column1.setPercentWidth(classicGroupWidth);
        } else {
            column1.setPercentWidth(oneNoteGroupWidth);
        }
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(100 - column1.getPercentWidth());
        column2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(column1, column2);
        
        notesTable.setEditor(this);
        notesTable.setTableType(OwnNoteTableView.TableType.notesTable);
        // set callback, width, value name, cursor type of columns
        noteNameCol.setTableColumnProperties(this, 0.65, NoteData.getNoteDataName(0), OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook));
        noteModifiedCol.setTableColumnProperties(this, 0.25, NoteData.getNoteDataName(1), false);
        noteDeleteCol.setTableColumnProperties(this, 0.10, NoteData.getNoteDataName(2), false);
        noteGroupCol.setTableColumnProperties(this, 0, NoteData.getNoteDataName(3), false);

        // only new button visible initially
        hideAndDisableAllCreateControls();
        hideAndDisableAllEditControls();
        
        // issue #30: store left and right regions of the second row in the grid - they are needed later on for the divider pane
        Region leftRegion;
        Region rightRegion;
        
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            myGroupList = groupsTable;
            
            hideNoteEditor();
            
            groupsPane.setDisable(true);
            groupsPane.setVisible(false);

            groupsTable.setEditor(this);
            groupsTable.setTableType(OwnNoteTableView.TableType.groupsTable);
            // set callback, width, value name, cursor type of columns
            groupNameCol.setTableColumnProperties(this, 0.65, GroupData.getGroupDataName(0), false);
            groupDeleteCol.setTableColumnProperties(this, 0.15, GroupData.getGroupDataName(1), false);
            groupCountCol.setTableColumnProperties(this, 0.20, GroupData.getGroupDataName(2), false);

            // name can be changed - but not for all entries!
            groupsTable.setEditable(true);
            groupNameCol.setEditable(true);

            // in case the group name changes notes neeed to be renamed
            groupNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final GroupData curEntry =
                        new GroupData((Map<String, String>) t.getTableView().getItems().get(t.getTablePosition().getRow()));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    // rename all notes of the group
                    if (!renameGroupWrapper(t.getOldValue(), t.getNewValue())) {
                        // TODO: revert changes to group name on UI
                        curEntry.setGroupName(t.getOldValue());

                        // workaround til TODO above resolved :-)
                        initFromDirectory(false);
                    } else {
                        // update group name in table
                        curEntry.setGroupName(t.getNewValue());
                    }
                }
            });
            
            // buttons and stuff should only impact layout when visible
            // https://stackoverflow.com/questions/12200195/javafx-hbox-hide-item
            for(Node child: buttonBox.getChildren()){
                child.managedProperty().bind(child.visibleProperty());
            }

            // keep track of the visibility of the editor
            inEditMode.bind(noteEditor.visibleProperty());

            // add listener for note name
            noteNameText.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add listener for group name
            groupNameText.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add listener for group combobox - but before changing values :-)
            groupNameBox.valueProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        // only in case of "new group" selected we show the field to enter an new group name
                        if (newValue.equals(GroupData.NEW_GROUP)) {
                            showAndEnableControl(groupNameText);
                            groupNameText.setPromptText("group title");
                            groupNameText.clear();
                        } else {
                            hideAndDisableControl(groupNameText);
                        }
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add action to the button - show initial create controls and populate combo box
            newButton.setOnAction((ActionEvent event) -> {
                // 1. enable initial create controls
                showAndEnableInitialCreateControls();

                // 2. fill combo box
                initGroupNameBox();
                groupNameBox.setValue(GroupData.NOT_GROUPED);
                groupNameBox.requestFocus();
            });

            // cancel button simply hides all create controls
            cancelButton.setOnAction((ActionEvent event) -> {
                if (!inEditMode.get()) {
                    hideAndDisableAllCreateControls();
                } else {
                    hideAndDisableAllEditControls();
                    hideNoteEditor();
                }
            });

            // create button creates empty note if text fields filled
            createButton.setOnAction((ActionEvent event) -> {
                // check if name fields are filled correctly
                Boolean doCreate = true;

                String newNoteName = noteNameText.getText();
                if (newNoteName.isEmpty()) {
                    // no note name entered...
                    doCreate = false;

                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("No note title given.");

                    alert.showAndWait();
                    alert.close();
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(GroupData.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doCreate = false;

                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText("No group title given.");

                        alert.showAndWait();
                        alert.close();
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doCreate) {
                    if (createNoteWrapper(newGroupName, newNoteName)) {
                        hideAndDisableAllCreateControls();
                        initFromDirectory(false);
                    }
                }
            });

            // quicksave button saves note but stays in editor
            quickSaveButton.setOnAction((ActionEvent event) -> {
                // quicksave = no changes to note name and group name allowed!
                final NoteData curNote =
                        new NoteData((Map<String, String>) noteEditor.getUserData());

                if (myFileManager.saveNote(curNote.getGroupName(),
                        curNote.getNoteName(),
                        noteEditor.getNoteText())) {
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("Note couldn't be saved.");

                    alert.showAndWait();
                    alert.close();
                }
            });

            // save button saves note but stays in editor
            saveButton.setOnAction((ActionEvent event) -> {
                // save = you might have changed note name & group name
                // check if name fields are filled correctly
                Boolean doSave = true;

                String newNoteName = noteNameText.getText();
                if (newNoteName.isEmpty()) {
                    // no note name entered...
                    doSave = false;

                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("No note title given.");

                    alert.showAndWait();
                    alert.close();
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(GroupData.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doSave = false;

                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText("No group title given.");

                        alert.showAndWait();
                        alert.close();
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doSave) {
                    // check against previous note and group name - might have changed!
                    final NoteData curNote =
                        new NoteData((Map<String, String>) noteEditor.getUserData());
                    final String curNoteName = curNote.getNoteName();
                    final String curGroupName = curNote.getGroupName();

                    if (!curNoteName.equals(newNoteName) || !curGroupName.equals(newGroupName)) {
                        // a bit of save transactions: first create new then delete old...
                        if (!createNoteWrapper(newGroupName, newNoteName)) {
                            doSave = false;
                        } else {
                            if (!deleteNoteWrapper(curNote)) {
                                doSave = false;
                                // clean up: delete new empty note - ignore return values
                                myFileManager.deleteNote(newGroupName, newNoteName);
                            }
                        }
                    }
                }

                if (doSave) {
                    if (saveNoteWrapper(newGroupName, newNoteName, noteEditor.getNoteText())) {
                        noteEditor.hasBeenSaved();
                    }
                }
            });
        
            // classic look & feel: groups table to the right, notes table or editor to the left
            leftRegion = groupsTableFXML;
            final StackPane rightPane = new StackPane();
            rightPane.getChildren().addAll(notesTableFXML, noteEditorFXML);
            rightRegion = rightPane;
            
            // remove things not shown directly in the gridPane
            // groupsPaneFXML: only in oneNote
            gridPane.getChildren().remove(groupsPaneFXML);
            // groupsTableFXML: shown in dividerPane
            gridPane.getChildren().remove(groupsTableFXML);
            // notesTableFXML: shown in dividerPane
            gridPane.getChildren().remove(notesTableFXML);
            // noteEditorFXML: shown in dividerPane
            gridPane.getChildren().remove(noteEditorFXML);

        } else {

            myGroupList = groupsPane;
            
            // oneNote look and feel
            // 1. no groups table, no button list
            groupsTable.setDisable(true);
            groupsTable.setVisible(false);

            buttonBox.setDisable(true);
            buttonBox.setVisible(false);
            
            // 3. and can't be deleted with trashcan
            noteNameCol.setWidthPercentage(0.75);
            noteNameCol.setStyle("notename-font-weight: normal");
            noteModifiedCol.setWidthPercentage(0.25);
            noteDeleteCol.setVisible(false);
            
            // name can be changed - but not for all entries!
            noteNameCol.setEditable(true);
            notesTable.setEditable(true);
            
            // From documentation - The .root style class is applied to the root node of the Scene instance.
            notesTable.getScene().getRoot().setStyle("note-selected-background-color: white");
            notesTable.getScene().getRoot().setStyle("note-selected-font-color: black");
            
            // in case the group name changes notes neeed to be renamed
            noteNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final NoteData curNote =
                        new NoteData((Map<String, String>) t.getTableView().getItems().get(t.getTablePosition().getRow()));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    // rename all notes of the group
                    renameNoteWrapper(curNote, t.getNewValue());
                }
            });
            
            groupsPane.setDisable(false);
            groupsPane.setVisible(true);
            groupsPane.setEditor(this);
            
            // oneNote look & feel: notes table to the right, editor to the left
            leftRegion = notesTableFXML;
            rightRegion = noteEditorFXML;
            
            // remove things not shown directly in the gridPane
            // buttonBox: only in classic
            gridPane.getChildren().remove(buttonBox);
            // groupsTableFXML: shown in dividerPane
            gridPane.getChildren().remove(groupsTableFXML);
            // notesTableFXML: shown in dividerPane
            gridPane.getChildren().remove(notesTableFXML);
            // noteEditorFXML: shown in dividerPane
            gridPane.getChildren().remove(noteEditorFXML);
        }
        
        // add changelistener to pathlabel - not that you should actually change its value during runtime...
        ownCloudPath.textProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                // store in the preferences
                OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, newValue);

                // scan files in new directory
                initFromDirectory(false);
            }); 
        
        // add action to the button - open a directory search dialogue...
        setOwnCloudPath.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select ownCloud Notes directory");
            if (ownCloudPath != null && !ownCloudPath.getText().isEmpty()) {
                directoryChooser.setInitialDirectory(new File(ownCloudPath.getText()));
            }
            File selectedDirectory = directoryChooser.showDialog(setOwnCloudPath.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                ownCloudPath.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // issue #30: add transparent split pane on top of the grid pane to have a moveable divider
        dividerPane = new SplitPane();
        // add content from second grid row to the split pane
        dividerPane.getItems().addAll(leftRegion, rightRegion);
        dividerPane.setDividerPosition(0, gridPane.getColumnConstraints().get(0).getPercentWidth() / 100f);
        // show split pane as second row
        gridPane.add(dividerPane, 0, 1);
        GridPane.setColumnSpan(dividerPane, 2);
        
        //Constrain max size of left & right pane:
        leftRegion.minWidthProperty().bind(dividerPane.widthProperty().multiply(0.15));
        leftRegion.maxWidthProperty().bind(dividerPane.widthProperty().multiply(0.5));
        rightRegion.minWidthProperty().bind(dividerPane.widthProperty().multiply(0.5));
        rightRegion.maxWidthProperty().bind(dividerPane.widthProperty().multiply(0.85));

        // move divider with gridpane on resize
        gridPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    dividerPane.setDividerPosition(0, gridPane.getColumnConstraints().get(0).getPercentWidth() / 100f);
                });
            }
        });
        
        // change width of gridpane when moving divider
        dividerPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    final double newPercentage = newValue.doubleValue() * 100f;
                    gridPane.getColumnConstraints().get(0).setPercentWidth(newPercentage);
                    gridPane.getColumnConstraints().get(1).setPercentWidth(100 - newPercentage);
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void initFromDirectory(final boolean updateOnly) {
        checkChangedNote();

        // scan directory
        myFileManager.initOwnNotePath(ownCloudPath.textProperty().getValue());
        
        // add new table entries & disable & enable accordingly
        ObservableList<Map<String, String>> notesList = myFileManager.getNotesList();
        // http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<Map<String, String>>(notesList, p -> true);
        // re-apply filter predicate when already set
        final String curGroupName = (String) notesTable.getTableView().getUserData();
        if (curGroupName != null) {
            setFilterPredicate(curGroupName);
        }

        // 2. Set the filter Predicate whenever the filter changes.
        // done in TabPane and TableView controls

        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<Map<String, String>> sortedData = new SortedList<Map<String, String>>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(notesTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.        
        notesTable.setNotes(sortedData);
        
        ObservableList<Map<String, String>> groupsList = myFileManager.getGroupsList();
        myGroupList.setGroups(groupsList, updateOnly);
        
        // and now store group names (real ones!) for later use
        initGroupNames();
    }
    
    @SuppressWarnings("unchecked")
    public boolean checkChangedNote() {
        Boolean result = true;
        
        // fix for #13: check for unsaved changes
        if (noteEditor.hasChanged()) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Unsaved changes!");
            alert.setHeaderText("Save now or discard your changes?");

            final ButtonType buttonSave = new ButtonType("Save", ButtonData.OTHER);
            final ButtonType buttonDiscard = new ButtonType("Discard", ButtonData.OTHER);
            alert.getButtonTypes().setAll(buttonSave, buttonDiscard);

            // TF: 20151229: cancel doesn#t work since there seems to be no way to avoid change of focus
            // and clicking on other note or group => UI would show a different note than is actualy edited
            // final ButtonType buttonCancel = new ButtonType("Cancel", ButtonData.OTHER);
            // alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

            Optional<ButtonType> saveChanges = alert.showAndWait();
            alert.close();
            if (saveChanges.isPresent()) {
                if (saveChanges.get().equals(buttonSave)) {
                    // save note
                    final NoteData prevNote =
                            new NoteData((Map<String, String>) noteEditor.getUserData());
                    if (saveNoteWrapper(prevNote.getGroupName(), prevNote.getNoteName(), noteEditor.getNoteText())) {
                        noteEditor.hasBeenSaved();
                    }
                }
                
                // if (saveChanges.get().equals(buttonCancel)) {
                //     // stop further processing of new file
                //     result = false;                    
                // }
            }
        }

        return result;
    }
    
    public boolean editNote(final NoteData curNote) {
        boolean result = false;
        
        if (!checkChangedNote()) {
            return result;
        }
        
        // 1. switch views: replace NoteTable with htmlEditor
        showNoteEditor();
        initGroupNameBox();
        noteNameText.setText(curNote.getNoteName());
        groupNameBox.setValue(curNote.getGroupName());
        this.handleQuickSave = true;

        // 2. show content of file in editor
        noteEditor.setNoteText(myFileManager.readNote(curNote));
        
        // 3. store note reference for saving
        noteEditor.setUserData(curNote);
        
        return result;
    }

    private void hideAndDisableAllCreateControls() {
        // show new button
        showAndEnableControl(newButton);
        
        hideAndDisableControl(noteNameText);
        
        hideAndDisableControl(groupNameBox);

        hideAndDisableControl(groupNameText);
        
        hideAndDisableControl(createButton);

        hideAndDisableControl(cancelButton);
    }

    private void showAndEnableInitialCreateControls() {
        // hide new button
        hideAndDisableControl(newButton);
        
        showAndEnableControl(noteNameText);
        noteNameText.setPromptText("note title");

        showAndEnableControl(groupNameBox);
        
        hideAndDisableControl(groupNameText);

        showAndEnableControl(createButton);
        
        // make sure quicksave & save button isn't left over shown from previous
        hideAndDisableControl(quickSaveButton);
        this.handleQuickSave = false;
        hideAndDisableControl(saveButton);

        showAndEnableControl(cancelButton);
    }

    private void hideNoteEditor() {
        noteEditor.setDisable(true);
        noteEditor.setVisible(false);
        noteEditor.setNoteText("");
        noteEditor.setUserData(null);
        
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            notesTable.setDisable(false);
            notesTable.setVisible(true);
        }
    }

    private void showNoteEditor() {
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            notesTable.setDisable(true);
            notesTable.setVisible(false);
        }

        noteEditor.setDisable(false);
        noteEditor.setVisible(true);
        noteEditor.setNoteText("");
        noteEditor.setUserData(null);

        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
            showAndEnableInitialEditControls();
        }
    }
    
    private void hideAndDisableAllEditControls() {
        // show new button
        showAndEnableControl(newButton);
        
        hideAndDisableControl(noteNameText);
        
        hideAndDisableControl(groupNameBox);

        hideAndDisableControl(groupNameText);
        
        hideAndDisableControl(quickSaveButton);
        this.handleQuickSave = false;

        hideAndDisableControl(saveButton);
        
        hideAndDisableControl(cancelButton);
    }

    private void showAndEnableInitialEditControls() {
        // hide new button
        hideAndDisableControl(newButton);
        
        showAndEnableControl(noteNameText);
        noteNameText.setPromptText("note title");

        showAndEnableControl(groupNameBox);
        
        hideAndDisableControl(groupNameText);

        // make sure create button isn't left over shown from previous
        hideAndDisableControl(createButton);

        showAndEnableControl(quickSaveButton);

        showAndEnableControl(saveButton);
        
        showAndEnableControl(cancelButton);
    }
    
    private void hideAndDisableControl(final Control control) {
        control.setDisable(true);
        control.setVisible(false);
        control.setMinWidth(0);
        control.setMaxWidth(0);
        control.setPrefWidth(0);
    }
    
    private void showAndEnableControl(final Control control) {
        control.setDisable(false);
        control.setVisible(true);
        control.setPrefWidth(Control.USE_COMPUTED_SIZE);
        control.setMinWidth(Control.USE_PREF_SIZE);
        control.setMaxWidth(Control.USE_PREF_SIZE);
    }

    private void initGroupNameBox() {
        groupNameBox.getItems().clear();
        groupNameBox.getItems().add(GroupData.NOT_GROUPED);
        groupNameBox.getItems().add(GroupData.NEW_GROUP);
        groupNameBox.getItems().addAll(realGroupNames);
    }

    public boolean createGroupWrapper(final String newGroupName) {
        Boolean result = myFileManager.createNewGroup(newGroupName);

        if (!result) {
            // error message - most likely group with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("New group couldn't be created.");
            alert.setContentText("Group with same name already exists.");

            alert.showAndWait();
            alert.close();
        }
        
        return result;
    }

    public boolean renameGroupWrapper(final String oldValue, final String newValue) {
        boolean result = false;

        // no rename for "All" and "Not Grouped"
        if (!newValue.equals(GroupData.ALL_GROUPS) && !newValue.equals(GroupData.NOT_GROUPED)) {
            result = myFileManager.renameGroup(oldValue, newValue);
            initGroupNames();

            if (!result) {
                // error message - most likely note in new group with same name already exists
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("An error occured while renaming the group.");
                alert.setContentText("A file in the new group has the same name as a file in the old.");

                alert.showAndWait();
                alert.close();
            } else {
                //check if we just moved the current note in the editor...
                noteEditor.checkForNameChange(oldValue, newValue);
            }
        }
        
        return result;
    }

    public Boolean deleteGroupWrapper(final GroupData curGroup) {
        boolean result = false;
                
        final String groupName = curGroup.getGroupName();
        // no delete for "All" and "Not Grouped"
        if (!groupName.equals(GroupData.ALL_GROUPS) && !groupName.equals(GroupData.NOT_GROUPED)) {
            result = myFileManager.deleteGroup(groupName);
            initGroupNames();

            if (!result) {
                // error message - most likely note in "Not grouped" with same name already exists
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("An error occured while deleting the group.");
                alert.setContentText("An ungrouped file has the same name as a file in this group.");

                alert.showAndWait();
                alert.close();
            }
        }
        
        return result;
    }

    private void initGroupNames() {
        realGroupNames.clear();

        final ObservableList<Map<String, String>> groupsList = myFileManager.getGroupsList();
        for (Map<String, String> group: groupsList) {
            final String groupName = (new GroupData(group)).getGroupName();
            if (!groupName.equals(GroupData.NOT_GROUPED) && !groupName.equals(GroupData.ALL_GROUPS)) {
                realGroupNames.add(groupName);
            }
        }
    }

    public Boolean deleteNoteWrapper(final NoteData curNote) {
        Boolean result = myFileManager.deleteNote(curNote.getGroupName(), curNote.getNoteName());

        if (!result) {
            // error message - something went wrong
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("An error occured while deleting the note.");
            alert.setContentText("See log for details.");

            alert.showAndWait();
            alert.close();
        }
        
        return result;
    }

    public boolean createNoteWrapper(final String newGroupName, final String newNoteName) {
        Boolean result = myFileManager.createNewNote(newGroupName, newNoteName);

        if (!result) {
            // error message - most likely note in "Not grouped" with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("New note couldn't be created.");
            alert.setContentText("Note with same group and name already exists.");

            alert.showAndWait();
            alert.close();
        }
        
        return result;
    }

    public boolean renameNoteWrapper(final NoteData curNote, final String newValue) {
        Boolean result = myFileManager.renameNote(curNote.getGroupName(), curNote.getNoteName(), newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("An error occured while renaming the note.");
            alert.setContentText("A note with the same name already exists.");

            alert.showAndWait();
            alert.close();
        } else {
            //check if we just moved the current note in the editor...
            noteEditor.checkForNameChange(curNote.getGroupName(), curNote.getGroupName(), curNote.getNoteName(), newValue);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean moveNoteWrapper(final NoteData curNote, final String newValue) {
        Boolean result = myFileManager.moveNote(curNote.getGroupName(), curNote.getNoteName(), newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("An error occured while moving the note.");
            alert.setContentText("A note with the same name already exists in the new group.");

            alert.showAndWait();
            alert.close();
        } else {
            //check if we just moved the current note in the editor...
            noteEditor.checkForNameChange(curNote.getGroupName(), newValue, curNote.getNoteName(), curNote.getNoteName());
        }
        
        return result;
    }

    public boolean saveNoteWrapper(String newGroupName, String newNoteName, String noteText) {
        Boolean result = myFileManager.saveNote(newGroupName, newNoteName, noteEditor.getNoteText());
                
        if (result) {
            if (OwnNoteEditorParameters.LookAndFeel.classic.equals(classicLook)) {
                hideAndDisableAllEditControls();
                hideNoteEditor();
                initFromDirectory(false);
            }
        } else {
            // error message - most likely note in "Not grouped" with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Note couldn't be saved.");

            alert.showAndWait();
            alert.close();
        }
        
        return result;
    }

    public void setFilterPredicate(final String groupName) {
        notesTable.getTableView().setUserData(groupName);
        
        filteredData.setPredicate(note -> {
            // If filter text is empty, display all persons. Also for "All".
            if (groupName == null || groupName.isEmpty() || groupName.equals(GroupData.ALL_GROUPS) ) {
                return true;
            }

            // Compare note name to filter text.
            if ((new NoteData(note)).getGroupName().equals(groupName)) {
                return true; // Filter matches first name.
            }
            return false; // Does not match.
        });
    }

    public void setNotesTableForNewTab(String style) {
        notesTable.setStyle(style);
        
        selectFirstOrCurrentNote();
    }
    
    @SuppressWarnings("unchecked")
    private void selectFirstOrCurrentNote() {
        // select first or current note - if any
        if (!notesTable.getItems().isEmpty()) {
            // check if current edit is ongoing AND note in the select tab (can happen with drag & drop!)
            Map<String, String> curNote = (Map<String, String>) noteEditor.getUserData();
            
            int selectIndex = 0;
            if (curNote != null && notesTable.getItems().contains(curNote)) {
                selectIndex = notesTable.getItems().indexOf(curNote);
            }
           
            notesTable.selectAndFocusRow(selectIndex);
            editNote(new NoteData((Map<String, String>) notesTable.getItems().get(selectIndex)));
        } else {
            noteEditor.setDisable(true);
            noteEditor.setNoteText("");
            noteEditor.setUserData(null);
        }
    }
    
    public String uniqueNewNoteNameForGroup(final String groupName) {
        String result;
        int newCount = myGroupList.getNotesCount();
        
        do {
            result = OwnNoteEditor.NEWNOTENAME + " " + newCount;
            newCount++;
        } while(myFileManager.noteExists(groupName, result));
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public void processFileChange(final WatchEvent.Kind<?> eventKind, final Path filePath) {
        // System.out.printf("Time %s: Gotcha!\n", getCurrentTimeStamp());
        
        if (!filesInProgress.contains(filePath.getFileName().toString())) {
            final String fileName = filePath.getFileName().toString();
        
            // System.out.printf("Time %s: You're new here!\n", getCurrentTimeStamp());
            filesInProgress.add(fileName);
            
            // re-init list of groups and notes - file has beeen added or removed
            Platform.runLater(() -> {
                if (!StandardWatchEventKinds.ENTRY_CREATE.equals(eventKind)) {
                    // delete & modify is only relevant if we're editing this note...
                    if (noteEditor.getUserData() != null) {
                        final NoteData curNote =
                                new NoteData((Map<String, String>) noteEditor.getUserData());
                        final String curName = myFileManager.buildNoteName(curNote.getGroupName(), curNote.getNoteName());

                        if (curName.equals(filePath.getFileName().toString())) {
                            // same note! lets ask the user what to do...
                            Alert alert = new Alert(AlertType.CONFIRMATION);

                            if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                alert.setTitle("Note has changed in file system!");
                                alert.setHeaderText("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nLoad changed note and discard own changes");
                            } else {
                                alert.setTitle("Note has been deleted in file system!");
                                alert.setHeaderText("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes");
                            }
                            
                            final ButtonType buttonSave = new ButtonType("Save own", ButtonData.OTHER);
                            final ButtonType buttonSaveNew = new ButtonType("Save as new", ButtonData.OTHER);
                            final ButtonType buttonDiscard = new ButtonType("Discard own", ButtonData.OTHER);
                            alert.getButtonTypes().setAll(buttonSave, buttonSaveNew, buttonDiscard);

                            Optional<ButtonType> saveChanges = alert.showAndWait();
                            alert.close();
                            if (saveChanges.isPresent()) {
                                if (saveChanges.get().equals(buttonSave)) {
                                    // save own note independent of file system changes
                                    final NoteData saveNote =
                                            new NoteData((Map<String, String>) noteEditor.getUserData());
                                    if (saveNoteWrapper(saveNote.getGroupName(), saveNote.getNoteName(), noteEditor.getNoteText())) {
                                        noteEditor.hasBeenSaved();
                                    }
                                }

                                if (saveChanges.get().equals(buttonSaveNew)) {
                                    // save own note under new name
                                    final NoteData saveNote =
                                            new NoteData((Map<String, String>) noteEditor.getUserData());
                                    final String newNoteName = uniqueNewNoteNameForGroup(saveNote.getGroupName());
                                    if (createNoteWrapper(saveNote.getGroupName(), newNoteName)) {
                                        if (saveNoteWrapper(saveNote.getGroupName(), newNoteName, noteEditor.getNoteText())) {
                                            noteEditor.hasBeenSaved();

                                            // we effectively just renamed the note...
                                            saveNote.setNoteName(newNoteName);
                                            noteEditor.setUserData(saveNote);
                                            // System.out.printf("User data updated\n");
                                        }
                                    }
                                }

                                if (saveChanges.get().equals(buttonDiscard)) {
                                    // nothing to do for StandardWatchEventKinds.ENTRY_DELETE - initFromDirectory(true) will take care of this
                                    if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                        // re-load into edit for StandardWatchEventKinds.ENTRY_MODIFY
                                        final NoteData loadNote =
                                                new NoteData((Map<String, String>) noteEditor.getUserData());
                                        noteEditor.setNoteText(myFileManager.readNote(loadNote));
                                    }
                                }
                            }
                        }
                    }
                }
            
                // show only notes for selected group
                final String curGroupName = myGroupList.getCurrentGroup().getGroupName();

                initFromDirectory(true);
                selectFirstOrCurrentNote();
                
                // but only if group still exists in the list!
                final List<String> allGroupNames = new LinkedList<String>(realGroupNames);
                allGroupNames.add(GroupData.ALL_GROUPS);
                allGroupNames.add(GroupData.NOT_GROUPED);
                
                if (allGroupNames.contains(curGroupName)) {
                    setFilterPredicate(curGroupName);
                }
                
                filesInProgress.remove(fileName);
            });
        }
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
}
