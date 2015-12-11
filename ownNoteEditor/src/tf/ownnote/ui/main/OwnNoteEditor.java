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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Application.Parameters;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.scene.web.HTMLEditor;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import tf.ownnote.ui.helper.LinkDialog;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteFileManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditor implements Initializable {

    private final OwnNoteFileManager myFileManager = new OwnNoteFileManager();

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    
    private Preferences myPreferences = null;
    private final static String RECENTOWNCLOUDPATH = "recentOwnCloudPath";
    
    private final static int TEXTFIELDWIDTH = 100;
    
    private List<String> realGroupNames = null;
    
    private final BooleanProperty inEditMode = new SimpleBooleanProperty();
    
    private boolean handleQuickSave = false;

    @FXML
    private BorderPane borderPane;
    @FXML
    private GridPane gridPane;
    @FXML
    private TableView<Map<String, String>> notesTable;
    @FXML
    private TableView<Map<String, String>> groupsTable;
    @FXML
    private HBox pathBox;
    @FXML
    private Label ownCloudPath;
    @FXML
    private Button setOwnCloudPath;
    @FXML
    private TableColumn<Map<String, String>, String> noteNameCol;
    @FXML
    private TableColumn<Map<String, String>, String> noteModifiedCol;
    @FXML
    private TableColumn<Map<String, String>, String> noteDeleteCol;
    @FXML
    private TableColumn<Map<String, String>, String> groupNameCol;
    @FXML
    private TableColumn<Map<String, String>, String> groupDeleteCol;
    @FXML
    private TableColumn<Map<String, String>, String> groupCountCol;
    @FXML
    private TableColumn<Map<String, String>, String> noteGroupCol;
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
    private HTMLEditor noteEditor;
    @FXML
    private Button quickSaveButton;
    @FXML
    private Button saveButton;
    @FXML
    private HBox buttonBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEditor();
    }
    
    public void setParameters(final Parameters parameters) {
        // now we have three kinds of parameters :-(
        // 1) named: name, value pairs from jnlp
        // 2) unnamed: values only from jnlp
        // 3) raw: good, old command line parameters
        // http://java-buddy.blogspot.de/2014/02/get-parametersarguments-in-javafx.html
        
        // for now just use raw parameters since the code as lready there for this :-)
        // let some one else deal with the command line parameters
        if ((parameters.getRaw() != null) && !parameters.getRaw().isEmpty()) {
            OwnNoteEditor.parameters.init(parameters.getRaw().toArray(new String[0]));
        } else {
            OwnNoteEditor.parameters.init(null);
        }

        // init pathlabel to parameter or nothing
        // 1. try the preferences setting
        String pathname = "";
        // most recent file that was opened
        try {
            myPreferences = Preferences.userNodeForPackage(OwnNoteEditor.class);
            pathname = myPreferences.get(OwnNoteEditor.RECENTOWNCLOUDPATH, "");
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // 2. if nothing there use any passed parameters
        if (pathname.isEmpty() && OwnNoteEditor.parameters.getOwnCloudDir() != null) {
            pathname = OwnNoteEditor.parameters.getOwnCloudDir();
        }
        
        // 3. set the value
        ownCloudPath.setText(pathname);
    }

    private void initEditor() {
        // hide borders
        borderPane.setTop(null);
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
        column1.setPercentWidth(220.0/1200.0*100.0);
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(100 - column1.getPercentWidth());
        column2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(column1, column2);
        
        // set width of columns to percentage of table
        noteNameCol.prefWidthProperty().bind(notesTable.widthProperty().multiply(0.65));
        noteModifiedCol.prefWidthProperty().bind(notesTable.widthProperty().multiply(0.25));
        noteDeleteCol.prefWidthProperty().bind(notesTable.widthProperty().multiply(0.10));
        notesTable.setPlaceholder(new Text(""));
        notesTable.setDisable(false);
        notesTable.getSelectionModel().setCellSelectionEnabled(false);

        groupNameCol.prefWidthProperty().bind(groupsTable.widthProperty().multiply(0.65));
        groupDeleteCol.prefWidthProperty().bind(groupsTable.widthProperty().multiply(0.15));
        groupCountCol.prefWidthProperty().bind(groupsTable.widthProperty().multiply(0.20));
        groupsTable.setPlaceholder(new Text(""));
        groupsTable.setDisable(false);
        groupsTable.getSelectionModel().setCellSelectionEnabled(false);

        // set map value factories for all table columns
        int i = 0;
        for(TableColumn<Map<String, String>, ?> column : notesTable.getColumns()) {
            column.setCellValueFactory(new MapValueFactory(OwnNoteFileManager.getNotesMapKey(i)));
            i++;
        }
        // .getColumns returns TableColumn<Map<String, Object>, ?> instead of TableColumn<Map<String, Object>, Object> - CRAP
        // therefore below can't be done in the loop above
        noteNameCol.setCellFactory(createObjectCellFactory(true));
        noteModifiedCol.setCellFactory(createObjectCellFactory(false));
        noteDeleteCol.setCellFactory(createObjectCellFactory(false));
        noteGroupCol.setCellFactory(createObjectCellFactory(false));

        i = 0;
        for(TableColumn<Map<String, String>, ?> column : groupsTable.getColumns()) {
            column.setCellValueFactory(new MapValueFactory(OwnNoteFileManager.getGroupsMapKey(i)));
            i++;
        }
        groupNameCol.setCellFactory(createObjectCellFactory(false));
        groupDeleteCol.setCellFactory(createObjectCellFactory(false));
        groupCountCol.setCellFactory(createObjectCellFactory(false));
        
        // name can be changed - but not for all entries!
        groupsTable.setEditable(true);
        groupNameCol.setEditable(true);
        
        // in case the group name changes notes neeed to be renamed
        groupNameCol.setOnEditCommit((CellEditEvent<Map<String, String>, String> t) -> {
            Map<String, String> curEntry =
                    (Map<String, String>) t.getTableView().getItems().get(t.getTablePosition().getRow());
            
            if (!t.getNewValue().equals(t.getOldValue())) {
                final TableColumn editCol = (TableColumn) t.getSource();
                
                // rename all notes of the group
                if (!myFileManager.renameGroup(t.getOldValue(), t.getNewValue())) {
                    // error message - most likely note in new group with same name already exists
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("An error occured while renaming the group.");
                    alert.setContentText("A file in the new group has the same name as a file in the old.");

                    alert.showAndWait();
                    
                    // TODO: revert changes to group name on UI
                    curEntry.put(OwnNoteFileManager.groupsMapKeys[0], t.getOldValue());
                
                    // workaround til TODO above resolved :-)
                    initFromDirectory();
                } else {
                    // update group name in table
                    curEntry.put(OwnNoteFileManager.groupsMapKeys[0], t.getNewValue());
                }
            }
        });

        // add changelistener to pathlabel - not that you should actually change its value during runtime...
        ownCloudPath.textProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                // store in the preferences
                myPreferences.put(OwnNoteEditor.RECENTOWNCLOUDPATH, newValue);

                // scan files in new directory
                initFromDirectory();
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
        
        
        // buttons and stuff should only impact layout when visible
        // https://stackoverflow.com/questions/12200195/javafx-hbox-hide-item
        for(Node child: buttonBox.getChildren()){
            child.managedProperty().bind(child.visibleProperty());
        }
        
        // only new button visible initially
        hideAndDisableAllCreateControls();
        hideAndDisableAllEditControls();
        hideNoteEditor();
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
                    if (newValue.equals(OwnNoteFileManager.NEW_GROUP)) {
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
            groupNameBox.setValue(OwnNoteFileManager.NOT_GROUPED);
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
            }
            String newGroupName = "";
            if (groupNameBox.getValue().equals(OwnNoteFileManager.NEW_GROUP)) {
                newGroupName = groupNameText.getText();

                if (newGroupName.isEmpty()) {
                    // "new group" selected and no group name entered...
                    doCreate = false;

                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("No group title given.");

                    alert.showAndWait();
                }
            } else {
                newGroupName = groupNameBox.getValue();
            }
            
            if (doCreate) {
                if (createNewNoteWrapper(newGroupName, newNoteName)) {
                    hideAndDisableAllCreateControls();
                    initFromDirectory();
                }
            }
        });

        // quicksave button saves note but stays in editor
        quickSaveButton.setOnAction((ActionEvent event) -> {
            // quicksave = no changes to note name and group name allowed!
            final Map<String, Object> curNote =
                    (Map<String, Object>) noteEditor.getUserData();
            
            String noteHtml = noteEditor.getHtmlText();
            final Document doc = Jsoup.parse(noteHtml);
            // get rid of "<font face="Segoe UI">" tags - see bug report https://bugs.openjdk.java.net/browse/JDK-8133833
            doc.getElementsByTag("font").unwrap();
            // only store content in <body>
            noteHtml = doc.select("body").html();
            
            if (myFileManager.saveNote((String) curNote.get(OwnNoteFileManager.notesMapKeys[3]),
                    (String) curNote.get(OwnNoteFileManager.notesMapKeys[0]),
                    noteHtml)) {
            } else {
                // error message - most likely note in "Not grouped" with same name already exists
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("Note couldn't be saved.");

                alert.showAndWait();
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
            }
            String newGroupName = "";
            if (groupNameBox.getValue().equals(OwnNoteFileManager.NEW_GROUP)) {
                newGroupName = groupNameText.getText();

                if (newGroupName.isEmpty()) {
                    // "new group" selected and no group name entered...
                    doSave = false;

                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("No group title given.");

                    alert.showAndWait();
                }
            } else {
                newGroupName = groupNameBox.getValue();
            }
            
            if (doSave) {
                // check against previous note and group name - might have changed!
                final Map<String, String> curNote =
                        (Map<String, String>) noteEditor.getUserData();
                final String curNoteName = (String) curNote.get(OwnNoteFileManager.notesMapKeys[0]);
                final String curGroupName = (String) curNote.get(OwnNoteFileManager.notesMapKeys[3]);
                
                if (!curNoteName.equals(newNoteName) || !curGroupName.equals(newGroupName)) {
                    // a bit of save transactions: first create new then delete old...
                    if (!createNewNoteWrapper(newGroupName, newNoteName)) {
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
                String noteHtml = noteEditor.getHtmlText();
                Document doc = Jsoup.parse(noteHtml);
                // get rid of "<font face="Segoe UI">" tags - see bug report https://bugs.openjdk.java.net/browse/JDK-8133833
                doc.getElementsByTag("font").unwrap();
                // only store content in <body>
                noteHtml = doc.select("body").html();

                if (myFileManager.saveNote(newGroupName, newNoteName, noteHtml)) {
                    hideAndDisableAllEditControls();
                    hideNoteEditor();
                    initFromDirectory();
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("Note couldn't be saved.");

                    alert.showAndWait();
                }
            }
        });
        
        // init html editor when visible - otherwise controls aren't there
    }

    private void initFromDirectory() {
        // scan directory
        myFileManager.initOwnNotePath(ownCloudPath.textProperty().getValue());
        
        // add new table entries & disable & enable accordingly
        ObservableList<Map<String, String>> notesList = myFileManager.getNotesList();
        // http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Map<String, String>> filteredData = new FilteredList<>(notesList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        // change notesTable selection based on group selected
        groupsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.equals(oldSelection)) {
                // select matching notes for group
                final String groupName = groupsTable.getSelectionModel().getSelectedItem().get(OwnNoteFileManager.getGroupsMapKey(0));

                filteredData.setPredicate(note -> {
                    // If filter text is empty, display all persons. Also for "All".
                    if (groupName == null || groupName.isEmpty() || groupName.equals(OwnNoteFileManager.ALL_GROUPS) ) {
                        return true;
                    }

                    // Compare note name to filter text.
                    if (note.get(OwnNoteFileManager.getNotesMapKey(3)).equals(groupName)) {
                        return true; // Filter matches first name.
                    }
                    return false; // Does not match.
                });
            }
        });        

        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<Map<String, String>> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(notesTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.        
        notesTable.setItems(null);
        notesTable.layout();
        notesTable.setItems(sortedData);
        
        ObservableList<Map<String, String>> groupsList = myFileManager.getGroupsList();
        groupsTable.setItems(null);
        groupsTable.layout();
        groupsTable.setItems(myFileManager.getGroupsList());
        groupsTable.getSelectionModel().clearAndSelect(0);
        
        // and now store group names (real ones!) for later use
        realGroupNames = new LinkedList<String> ();
        
        for (Map<String, String> group: groupsList) {
            final String groupName = group.get(OwnNoteFileManager.groupsMapKeys[0]);
            if (!groupName.equals(OwnNoteFileManager.NOT_GROUPED) && !groupName.equals(OwnNoteFileManager.ALL_GROUPS)) {
                realGroupNames.add(groupName);
            }
        }
    }
    
    private Callback<TableColumn<Map<String, String>, String>, TableCell<Map<String, String>, String>>
        createObjectCellFactory(final boolean linkCursor) {
        return (TableColumn<Map<String, String>, String> param) -> new ObjectCell(this, linkCursor, new UniversalMouseEvent(this));
    }

    public boolean editNote(final TableCell clickedCell) {
        boolean result = false;

        Map<String, String> curNote =
                    (Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex());

        // 1. switch views: replace NoteTable with htmlEditor
        showNotesEditor();
        initGroupNameBox();
        noteNameText.setText(curNote.get(OwnNoteFileManager.notesMapKeys[0]));
        groupNameBox.setValue(curNote.get(OwnNoteFileManager.notesMapKeys[3]));
        this.handleQuickSave = true;

        // 2. show content of file in editor
        final String noteHtml = myFileManager.readNote(curNote);
        noteEditor.setHtmlText(noteHtml);
        
        // 3. store note reference for saving
        noteEditor.setUserData(curNote);
        
        return result;
    }

    public boolean deleteNote(final TableCell clickedCell) {
        // delete note from filesystem and update list
        Map<String, String> curNote =
                    (Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex());
        return deleteNoteWrapper(curNote);
    }

    public boolean deleteGroup(final TableCell clickedCell) {
        Map<String, String> curGroup =
                    (Map<String, String>) clickedCell.getTableView().getItems().get(clickedCell.getIndex());
        
        boolean result = false;
                
        final String groupName = curGroup.get(OwnNoteFileManager.groupsMapKeys[0]);
        // no delete for "All" and "Not Grouped"
        if (!groupName.equals(OwnNoteFileManager.ALL_GROUPS) && !groupName.equals(OwnNoteFileManager.NOT_GROUPED)) {
            result = myFileManager.deleteGroup(curGroup);

            if (!result) {
                // error message - most likely note in "Not grouped" with same name already exists
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("An error occured while deleting the group.");
                alert.setContentText("An ungrouped file has the same name as a file in this group.");

                alert.showAndWait();
            }
        }
        
        return result;
    }

    void handleTableClick(final TableCell clickedCell) {
        boolean reInit = false;
                            
        switch(clickedCell.getId()) {
            case "noteNameCol":
                //System.out.println("Clicked in noteNameCol");
                reInit = editNote(clickedCell);
                break;
            case "noteDeleteCol":
                //System.out.println("Clicked in noteDeleteCol");
                reInit = deleteNote(clickedCell);
                break;
            case "groupDeleteCol":
                //System.out.println("Clicked in groupDeleteCol");
                reInit = deleteGroup(clickedCell);
                break;
            default:
                //System.out.println("Ignoring click into " + clickedCell.getId() + " for controller " + this.myOwnNoteEditor.toString());
        }
        
        if (reInit) {
            // rescan diretory - also group name counters need to be updated...
            initFromDirectory();
        }
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

        notesTable.setDisable(false);
        notesTable.setVisible(true);
    }

    private void showNotesEditor() {
        notesTable.setDisable(true);
        notesTable.setVisible(false);

        noteEditor.setDisable(false);
        noteEditor.setVisible(true);
        noteEditor.setHtmlText("");

        // remove: foreground & background control
        hideNode(noteEditor, ".html-editor-foreground", 1);
        hideNode(noteEditor, ".html-editor-background", 1);
        // remove: font type & font size control - the 2nd and 3rd control with "font-menu-button" style class
        hideNode(noteEditor, ".font-menu-button", 2);
        hideNode(noteEditor, ".font-menu-button", 3);
        // add: insert link & picture controls
        addNoteEditorControls();
        // add: undo & redo button, back button
        
        showAndEnableInitialEditControls();
    }
    
    private static void hideNode(final Node startNode, final String lookupString, final int occurence) {
        final Set<Node> nodes = startNode.lookupAll(lookupString);
        if (nodes != null && nodes.size() >= occurence) {
            // no simple way to ge nth member of set :-(
            Iterator<Node> itr = nodes.iterator();
            Node node = null;
            for(int i = 0; itr.hasNext() && i<occurence; i++) {
                node = itr.next();
            }
            if (node != null) {
                node.setVisible(false);
                node.setManaged(false);
            }
        }
    }

    private void addNoteEditorControls() {
        Node node = noteEditor.lookup(".top-toolbar");
        if (node != null && node instanceof ToolBar) {
            ToolBar toolbar = (ToolBar) node;

            // copy styles from other buttons in toolbar
            ObservableList<String> buttonStyles = null;
            node = toolbar.lookup(".html-editor-cut");
            if (node != null && node instanceof Button) {
                buttonStyles = ((Button) node).getStyleClass();
                // not the own button style, please
                buttonStyles.removeAll("html-editor-cut");
            }

            // add button to insert link - but only once
            if (toolbar.lookup(".html-editor-insertlink") == null) {
                final ImageView graphic =
                        new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/link.png"), 16, 16, true, true));
                final Button insertLink = new Button("", graphic);
                insertLink.getStyleClass().add("html-editor-insertlink");
                if (buttonStyles != null) {
                    insertLink.getStyleClass().addAll(buttonStyles);
                }

                insertLink.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent t) {
                        LinkDialog linkDialog = new LinkDialog();
                        if (linkDialog.showAndWait()) {
                            // dialog has been ended with OK - now check if values are fine
                            if (!linkDialog.getLinkUrl().isEmpty() && !linkDialog.getLinkText().isEmpty()) {
                                final String hrefString = 
                                        "<a href=\"" +
                                        linkDialog.getLinkUrl().trim() +
                                        "\" title=\"" +
                                        linkDialog.getLinkTitle().trim() +
                                        "\" target=\"" +
                                        // decide between _self and _blank on the fly
                                        linkDialog.getWindowMode()+
                                        "\">" +
                                        linkDialog.getLinkText().trim() + "</a>";
                                noteEditor.setHtmlText(noteEditor.getHtmlText() + hrefString);
                            }
                        }
                    }
                });

                toolbar.getItems().add(insertLink);
            }

            // add button to insert image - but only once
            if (toolbar.lookup(".html-editor-insertimage") == null) {
                final ImageView graphic =
                        new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/insertimage.gif"), 22, 22, true, true));
                final Button insertImage = new Button("", graphic);
                insertImage.getStyleClass().add("html-editor-insertimage");
                if (buttonStyles != null) {
                    insertImage.getStyleClass().addAll(buttonStyles);
                }

                insertImage.setOnAction((ActionEvent arg0) -> {
                    final List<String> extFilter = Arrays.asList("*.jpg", "*.png", "*.gif");
                    final List<String> extValues = Arrays.asList("jpg", "png", "gif");

                    final FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Embed an image");
                    fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Pictures", extFilter));
                    final File selectedFile = fileChooser.showOpenDialog(null);
                    
                    if (selectedFile != null) {
                        if (extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                            try {
                                // we really have selected a picture - now add it
                                noteEditor.setHtmlText(
                                        noteEditor.getHtmlText() + "<img src='" + selectedFile.toURI().toURL().toExternalForm() +"'>");
                            } catch (MalformedURLException ex) {
                                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }                        
                    }
                });

                toolbar.getItems().add(insertImage);
            }
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

    private Boolean getNoteAndGroupName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void initGroupNameBox() {
        groupNameBox.getItems().clear();
        groupNameBox.getItems().add(OwnNoteFileManager.NOT_GROUPED);
        groupNameBox.getItems().add(OwnNoteFileManager.NEW_GROUP);
        groupNameBox.getItems().addAll(realGroupNames);
    }

    private Boolean deleteNoteWrapper(Map<String, String> curNote) {
        Boolean result = myFileManager.deleteNote(curNote);

        if (!result) {
            // error message - something went wrong
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("An error occured while deleting the note.");
            alert.setContentText("See log for details.");

            alert.showAndWait();
        }
        
        return result;
    }

    private boolean createNewNoteWrapper(String newGroupName, String newNoteName) {
        Boolean result = myFileManager.createNewNote(newGroupName, newNoteName);

        if (!result) {
            // error message - most likely note in "Not grouped" with same name already exists
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("New note couldn't be created.");
            alert.setContentText("Note with same group and name already exists.");

            alert.showAndWait();
        }
        
        return result;
    }
}

class UniversalMouseEvent implements EventHandler<MouseEvent> {
    // store link back to the controller of the scene for callback
    private OwnNoteEditor myOwnNoteEditor;
    
    public UniversalMouseEvent(final OwnNoteEditor ownNoteEditor) {
        myOwnNoteEditor = ownNoteEditor;
    }

    @Override
    public void handle(MouseEvent event) {
        if (event.getSource() instanceof TableCell) {
            TableCell clickedCell = (TableCell) event.getSource();
            
            this.myOwnNoteEditor.handleTableClick(clickedCell);
        }
    }
};

class ObjectCell extends TextFieldTableCell<Map<String, String>, String> {
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
        if (OwnNoteFileManager.ALL_GROUPS.equals(getText())
                || OwnNoteFileManager.NOT_GROUPED.equals(getText())) {
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