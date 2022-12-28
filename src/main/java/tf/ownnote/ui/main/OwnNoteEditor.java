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
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import tf.helper.general.AppInfo;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AboutMenu;
import tf.helper.javafx.TableMenuUtils;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.helper.OwnNoteTabPane;
import tf.ownnote.ui.helper.OwnNoteTableColumn;
import tf.ownnote.ui.helper.OwnNoteTableView;
import tf.ownnote.ui.helper.RecentNoteForGroup;
import tf.ownnote.ui.links.LinkManager;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteMetaDataEditor;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;
import tf.ownnote.ui.tags.TagsEditor;
import tf.ownnote.ui.tags.TagsTreeView;
import tf.ownnote.ui.tasks.TaskBoard;
import tf.ownnote.ui.tasks.TaskData;
import tf.ownnote.ui.tasks.TaskList;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditor implements Initializable, IFileChangeSubscriber, INoteCRMDS {

    // TFE, 20220506: keep track of app version numbers
    // to be able to determine any need for data migration
    public enum AppVersion {
        NONE(6.0),
        V6_1(6.1),
        V6_2(6.2),
        CURRENT(6.2);
        
        private double versionId;
        
        private AppVersion(final double id) {
            versionId = id;
        }
        
        public double getVersionId() {
            return versionId;
        }
        
        private void setVersionId(final double id) {
            versionId = id;
        }
        
        public boolean isHigherAppVersionThan(final double other) {
            return versionId > other;
        }
        public boolean isHigherAppVersionThan(final AppVersion other) {
            assert other != null;
            
            return isHigherAppVersionThan(other.versionId);
        }

        public boolean isLowerAppVersionThan(final double other) {
            return versionId < other;
        }
        public boolean isLowerAppVersionThan(final AppVersion other) {
            return isLowerAppVersionThan(other.versionId);
        }
        
        // simple minded version of Comparable interface
        public int compareToAppVersion(final double other) {
            int result = 0;
            
            if (isHigherAppVersionThan(other)) {
                result = 1;
            } else if (isLowerAppVersionThan(other)) {
                result = -1;
            }
            
            return result;
        }
        public int compareToAppVersion(final AppVersion other) {
            assert other != null;

            return compareToAppVersion(other.versionId);
        }
    }

    private final List<String> filesInProgress = new LinkedList<>();

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    
    public final static String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";
    public final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    private final static String NEW_NOTENAME = "New Note";
    
    public final static String GROUP_COLOR_CSS = "group-color";
    
    private final static int TEXTFIELD_WIDTH = 100;  
    
    private ObservableList<Note> notesList = null;
    
    private final BooleanProperty inEditMode = new SimpleBooleanProperty();
    private boolean firstNoteAccess = true;
    
    // should we show standard ownNote face or groupTabs?
    // TF, 20160630: refactored from "classicLook" to show its real meeaning
    private OwnNoteEditorParameters.LookAndFeel currentLookAndFeel;

    private Double tagTreeWidth;
    private Double groupTabsGroupWidth;
    private Double taskListWidth;
    
    private final BooleanProperty tasklistVisible = new SimpleBooleanProperty(true);
    
    // TFE, 20210301: make this a property so that rest of the world can listen to changes...
    private final ReadOnlyObjectWrapper<Note> currentNoteProperty = new ReadOnlyObjectWrapper<>(null);
    // TFE, 20210401: have current group tag as property as well, e.g. to listen to changes of color & icon
    private final ReadOnlyObjectWrapper<TagData> currentGroupTagProperty = new ReadOnlyObjectWrapper<>(null);
    
    private IGroupListContainer myGroupList = null;
    
    // TFE, 20201203: some constants for the different columns of our gridpane
    private static final int TAGTREE_COLUMN = 0;
    private static final int NOTE_GROUP_COLUMN = 1;
    private static final int EDITOR_COLUMN = 2;
    private static final int TASKLIST_COLUMN = 3;
    
    private static final int TAGTREE_NOTE_GROUP_DIVIDER = 0;
    private static final int NOTE_GROUP_EDITOR_DIVIDER = 1;
    private static final int EDITOR_TASKLIST_DIVIDER = 2;
    
    // limiting values for width per each column
    private static final Map<Integer, Pair<Double, Double>> paneSizes = new HashMap<>(); 
    static {
        paneSizes.put(TAGTREE_COLUMN, Pair.of(10d, 30d));
        paneSizes.put(NOTE_GROUP_COLUMN, Pair.of(10d, 30d));
        paneSizes.put(EDITOR_COLUMN, Pair.of(20d, 80d));
        paneSizes.put(TASKLIST_COLUMN, Pair.of(10d, 20d));
    }
    // https://stackoverflow.com/a/37459951
    private static <T extends Comparable<? super T>> T limit(T o, T min, T max)
    {
        if (o.compareTo(min) < 0) return min;
        if (o.compareTo(max) > 0) return max;
        return o;
    }
    
    // TFE, 20221129: get rid of overall grid pane
    @FXML
    private VBox noteListBoxFXML;
    @FXML
    private VBox noteEditBoxFXML;
    @FXML
    private VBox taskListBoxFXML;
    @FXML
    private HBox noteSearchFXML;
    @FXML
    private HBox taskFilterBoxFXML;

    @FXML
    private BorderPane borderPane;
    @FXML
    private TableView<Note> notesTableFXML;
    private OwnNoteTableView notesTable = null;
    @FXML
    private Label ownCloudPath;
    @FXML
    private Button setOwnCloudPath;
    @FXML
    private TableColumn<Note, String> noteNameColFXML;
    private OwnNoteTableColumn noteNameCol = null;
    @FXML
    private TableColumn<Note, String> noteModifiedColFXML;
    private OwnNoteTableColumn noteModifiedCol = null;
    @FXML
    private WebView noteHTMLEditorFXML;
    private OwnNoteHTMLEditor noteHTMLEditor = null;
    @FXML
    private TabPane groupsPaneFXML;
    private OwnNoteTabPane groupsPane = null;
    @FXML
    private MenuBar menuBar;
    @FXML
    private RadioMenuItem groupTabsLookAndFeel;
    @FXML
    private ToggleGroup LookAndFeel;
    @FXML
    private Menu menuLookAndFeel;
    @FXML
    private Label pathLabel;
    @FXML
    private TextField noteFilterText;
    @FXML
    private Label noteFilterMode;
    @FXML
    private Label taskFilterMode;
    @FXML
    private ListView<TaskData> taskListFXML;
    private TaskList taskList = null;

    final CheckBox noteFilterCheck = new CheckBox();
    final CheckBox taskFilterCheck = new CheckBox();
    @FXML
    private SplitPane splitPaneXML;
    @FXML
    private StackPane leftPaneXML;
    @FXML
    private StackPane middlePaneXML;
    @FXML
    private StackPane rightPaneXML;
    @FXML
    private MenuItem menuEditTags;
    @FXML
    private MenuItem menuGroups2Tags;
    @FXML
    private VBox noteEditorFXML;
    @FXML
    private HBox noteMetaEditorFXML;
    private NoteMetaDataEditor noteMetaEditor = null;
    @FXML
    private RadioMenuItem tagTreeLookAndFeel;
    @FXML
    private CheckMenuItem menuShowTasklist;
    @FXML
    private StackPane tagsTreePaneXML;
    private TagsTreeView tagsTreeView;
    @FXML
    private MenuItem menuTaskboard;

    public OwnNoteEditor() {
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // defer initEditor since we need to know the value of the parameters...
    }
    
    public void stop(final boolean productiveRun) {
        OwnNoteFileManager.getInstance().stop();
        
        if (productiveRun) {
            // store current percentage of group column width
            // if increment is passed as parameter, we need to remove it from the current value
            // otherwise, the percentage grows with each call :-)
            // store in the preferences
            
            // TFE, 20221129 gridpane has gone, we now need to derive from splitpane divider positions
//            // #1 tagtree is easy - use own percentage
//            splitPaneXML.setDividerPosition(TAGTREE_NOTE_GROUP_DIVIDER, tagTreeWidth/100d);
//            // #2 note/group is easy - use percentage of tagtree + own percentage
//            splitPaneXML.setDividerPosition(NOTE_GROUP_EDITOR_DIVIDER, (tagTreeWidth + groupTabsGroupWidth)/100d);
//            // #4 tasklist is easy - use 100 - own percentage
//            splitPaneXML.setDividerPosition(EDITOR_TASKLIST_DIVIDER, (100d - taskListWidth)/100d);

            // TFE, 20201204: store tag tree width only for this look & feel
            if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
                OwnNoteEditorPreferences.RECENT_TAGTREE_WIDTH.put(tagTreeWidth);
            } 
            OwnNoteEditorPreferences.RECENT_GROUPTABS_GROUPWIDTH.put(groupTabsGroupWidth);
            // TFE, 20201203: taskList can be hidden (and therefore have column has width 0)
            if (tasklistVisible.get()) {
                OwnNoteEditorPreferences.RECENT_TASKLIST_WIDTH.put(taskListWidth);
            }
            OwnNoteEditorPreferences.RECENT_TASKLIST_VISIBLE.put(tasklistVisible.get());

            // issue #45 store sort order for tables
            notesTable.savePreferences(OwnNoteEditorPreferences.INSTANCE);

            // TFE, 20200903: store groups tabs order as well
            if (groupsPane != null) {
                groupsPane.savePreferences(OwnNoteEditorPreferences.INSTANCE);
            }

            // TFE, 20201030: store name of last edited note
            if (noteHTMLEditor.getEditedNote() != null) {
                OwnNoteEditorPreferences.LAST_EDITED_NOTE.put(noteHTMLEditor.getEditedNote().getNoteName());
                OwnNoteEditorPreferences.LAST_EDITED_GROUP.put(noteHTMLEditor.getEditedNote().getGroup().getExternalName());
            }
            
            // TFE, 20210716: store recent note for group to references
            OwnNoteEditorPreferences.RECENT_NOTE_FOR_GROUP.put(RecentNoteForGroup.getInstance().toPreferenceString());

            // TFE, 20201121: tag info is now stored in a separate file
            TagManager.getInstance().saveTags();

            // TFE, 20201230: task metadata is now stored in a separate file
            TaskManager.getInstance().saveTaskList();
        }
    }
    
    public void setParameters() {
        // set look & feel    
        // 1. use passed parameters
        if (OwnNoteEditor.parameters.getLookAndFeel().isPresent()) {
            currentLookAndFeel = OwnNoteEditor.parameters.getLookAndFeel().get();
        } else {
            try {
                currentLookAndFeel = OwnNoteEditorPreferences.RECENT_LOOKANDFEEL.getAsType();
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
                currentLookAndFeel = OwnNoteEditorParameters.LookAndFeel.groupTabs;
            }
        }
        
        // issue #30: get percentages for group column width for classic and onenote look & feel
        // issue #45 store sort order for tables
        try {
            tagTreeWidth = OwnNoteEditorPreferences.RECENT_TAGTREE_WIDTH.getAsType();
            groupTabsGroupWidth = OwnNoteEditorPreferences.RECENT_GROUPTABS_GROUPWIDTH.getAsType();
            taskListWidth = OwnNoteEditorPreferences.RECENT_TASKLIST_WIDTH.getAsType();
            tasklistVisible.set(OwnNoteEditorPreferences.RECENT_TASKLIST_VISIBLE.getAsType());
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            tagTreeWidth = 18.3333333;
            groupTabsGroupWidth = 33.3333333;
            taskListWidth = 15.0;
            tasklistVisible.set(true);
        }
        
        // TFE, 20201205: limit values to allowed ones
        tagTreeWidth = limit(tagTreeWidth, paneSizes.get(TAGTREE_COLUMN).getLeft(), paneSizes.get(TAGTREE_COLUMN).getRight());
        groupTabsGroupWidth = limit(groupTabsGroupWidth, paneSizes.get(NOTE_GROUP_COLUMN).getLeft(), paneSizes.get(NOTE_GROUP_COLUMN).getRight());
        taskListWidth = limit(taskListWidth, paneSizes.get(TASKLIST_COLUMN).getLeft(), paneSizes.get(TASKLIST_COLUMN).getRight());
        tasklistVisible.set(OwnNoteEditorPreferences.RECENT_TASKLIST_VISIBLE.getAsType());
        
        // init ownCloudPath to parameter or nothing
        pathLabel.setMinWidth(Region.USE_PREF_SIZE);
        String pathname = "";
        // 1. use any passed parameters
        if (OwnNoteEditor.parameters.getOwnCloudDir().isPresent()) {
            pathname = OwnNoteEditor.parameters.getOwnCloudDir().get();
        } else {
            // 2. try the preferences setting - most recent file that was opened
            try {
                pathname = OwnNoteEditorPreferences.RECENT_OWNCLOUDPATH.getAsType();
                // System.out.println("Using preference for ownCloudDir: " + pathname);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // 3. set the value
        ownCloudPath.setText(pathname);

        // paint the look
        initEditor();
        
        // scan files in directory
        initFromDirectory(false, true);
        
        // TFE, 20210715: if note changes, group might need to change as well
        currentNoteProperty.addListener((ov, oldNote, newNote) -> {
            if (newNote != null && !newNote.equals(oldNote)) {
                currentGroupTagProperty.set(newNote.getGroup());
            }
            
        });
        
        // TFE, 20210716: get recent note for group from references
        RecentNoteForGroup.getInstance().fromPreferenceString(OwnNoteEditorPreferences.RECENT_NOTE_FOR_GROUP.getAsType());
    }

    //
    // basic setup is a 3x2 gridpane with a 3 part splitpane in the lower row
    // TFE: 20181028: pathBox has moved to menu to make room for filterBox
    // TFE, 20200810: 3rd column added for task handling
    // TFE, 20201204: column to the left added for tag tree
    // TFE, 20210412: no more classic look & feel
    // TFE, 20221129: remove gridpane and have vboxes in each split of splitpane instead
    //
    // -------------------------------------------------------------------------------------------------------------------
    // |                            |                            |                           |                           |
    // |                            | all : noteFilterBox        | groupTabs: groupsPaneFXML | all: taskFilterBox        |
    // |                            |                            | tagTree: nothing          |                           |
    // |                            |                            |                           |                           |
    // -------------------------------------------------------------------------------------------------------------------
    // |                            |                            |                           |                           |
    // | groupTabs: nothing         | groupTabs: notesTableFXML  | all: noteEditorFXML       | all: taskListFXML         |
    // | tagTree: TagsTreeView      | tagTree: notesTableFXML    |                           |                           |
    // |                            |                            |                           |                           |
    // -------------------------------------------------------------------------------------------------------------------
    //
    // to be able to do proper layout in scenebuilder everything except the dividerPane
    // are added to the fxml into the gridpane - code below does the re-arrangement based on 
    // value of currentLookAndFeel
    //
    private void initEditor() {
        
        // noteListBoxXML: as high as possible
        VBox.setVgrow(noteListBoxFXML, Priority.ALWAYS);
        // noteSearchXML: standard height, always above
        VBox.setVgrow(noteSearchFXML, Priority.NEVER);
        // notesTableFXML: as high as possible
        VBox.setVgrow(notesTableFXML, Priority.ALWAYS);

        // init our wrappers to FXML classes...
        noteNameCol = new OwnNoteTableColumn(noteNameColFXML, this);
        noteNameColFXML.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        noteModifiedCol = new OwnNoteTableColumn(noteModifiedColFXML, this);
        notesTable = new OwnNoteTableView(notesTableFXML, this);
        notesTable.loadPreferences(OwnNoteEditorPreferences.INSTANCE);

        // noteEditorFXML: as high as possible
        VBox.setVgrow(noteEditBoxFXML, Priority.ALWAYS);
        // groupsPaneFXML: standard height, always above
        VBox.setVgrow(groupsPaneFXML, Priority.NEVER);
        groupsPaneFXML.setDisable(true);
        groupsPaneFXML.setVisible(false);

        // noteEditorFXML: as high as possible
        // noteHTMLEditorFXML: as high as possible, always above
        // noteMetaEditorFXML: standard hight, always below
        VBox.setVgrow(noteEditorFXML, Priority.ALWAYS);
        VBox.setVgrow(noteHTMLEditorFXML, Priority.ALWAYS);
        // error when trying this in the fxml: "java.lang.IllegalArgumentException: Unable to coerce noteHTMLEditor to interface java.util.Collection."
        noteHTMLEditorFXML.getStyleClass().add("noteHTMLEditor");
        VBox.setVgrow(noteMetaEditorFXML, Priority.NEVER);
        noteMetaEditorFXML.getStyleClass().add("noteMetaEditor");

        // init our wrappers to FXML classes...
        noteHTMLEditor = new OwnNoteHTMLEditor(noteHTMLEditorFXML, this);
        noteHTMLEditor.setVisible(true);
        noteHTMLEditor.setDisable(false);
        noteMetaEditor = new NoteMetaDataEditor(noteMetaEditorFXML, this);
        
        // taskListBoxFXML: as high as possible
        VBox.setVgrow(taskListBoxFXML, Priority.ALWAYS);
        // taskFilterBoxFXML: standard height, always above
        VBox.setVgrow(taskFilterBoxFXML, Priority.NEVER);
        // taskListFXML: as high as possible
        VBox.setVgrow(taskListFXML, Priority.ALWAYS);
        
        // hide borders
        borderPane.setBottom(null);
        borderPane.setLeft(null);
        borderPane.setRight(null);
        
        //Constrain max size of left & right pane:
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            tagsTreePaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(TAGTREE_COLUMN).getLeft()/100d));
            tagsTreePaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(TAGTREE_COLUMN).getRight()/100d));
        } else {
            tagsTreePaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(0d));
            tagsTreePaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(0d));
        }
        leftPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(NOTE_GROUP_COLUMN).getLeft()/100d));
        leftPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(NOTE_GROUP_COLUMN).getRight()/100d));
        middlePaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(EDITOR_COLUMN).getLeft()/100d));
        middlePaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(EDITOR_COLUMN).getRight()/100d));
        rightPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(TASKLIST_COLUMN).getLeft()/100d));
        rightPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(paneSizes.get(TASKLIST_COLUMN).getRight()/100d));

        // set callback, width, value name, cursor type of columns
        noteNameCol.setTableColumnProperties(0.65, Note::getNoteName, false);
        noteModifiedCol.setTableColumnProperties(0.25, Note::getNoteModifiedFormatted, false);
        // see issue #42
        noteModifiedCol.setComparator(FormatHelper.getInstance().getFileTimeComparator());

        // issue #59: support filtering of note names
        // https://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        noteFilterText.textProperty().addListener((observable, oldValue, newValue) -> {
            notesTable.setNoteFilterText(newValue);
        });
        noteFilterText.setOnKeyReleased((t) -> {
            if (KeyCode.ESCAPE.equals(t.getCode())) {
                noteFilterText.setText("");
            }
        });
        noteFilterCheck.getStyleClass().add("noteFilterCheck");
        noteFilterCheck.selectedProperty().addListener((o) -> {
            notesTable.setNoteFilterMode(noteFilterCheck.isSelected());
        });
        noteFilterMode.setGraphic(noteFilterCheck);
        noteFilterMode.setContentDisplay(ContentDisplay.RIGHT);

        // groupTabs look and feel
        if (OwnNoteEditorParameters.LookAndFeel.groupTabs.equals(currentLookAndFeel)) {
            groupsPane = new OwnNoteTabPane(groupsPaneFXML, this);
            groupsPane.loadPreferences(OwnNoteEditorPreferences.INSTANCE);
            groupsPane.setDisable(false);
            groupsPane.setVisible(true);

            myGroupList = groupsPane;
        } else {
            // show TagsTreeView (special version without checkboxes & drag/drop of tags)
            tagsTreeView = new TagsTreeView(this);

            tagsTreePaneXML.getChildren().add(tagsTreeView);

            myGroupList = tagsTreeView;
        }

        // maximize noteNameCol
        // http://bekwam.blogspot.com/2016/02/getting-around-javafx-tableview.html
        noteNameColFXML.prefWidthProperty().bind(
                            notesTableFXML.widthProperty()
                            .subtract(noteModifiedColFXML.widthProperty())
                            .subtract(2)
                         );

        // name can be changed - but not for all entries!
        noteNameCol.setEditable(true);
        notesTable.setEditable(true);

        // From documentation - The .root style class is applied to the root node of the Scene instance.
        splitPaneXML.getScene().getRoot().setStyle("note-selected-background-color: white");
        splitPaneXML.getScene().getRoot().setStyle("note-selected-font-color: black");

        // renaming note
        noteNameCol.setOnEditCommit((CellEditEvent<Note, String> t) -> {
            final Note curNote = ObjectsHelper.uncheckedCast(t.getTableView().getItems().get(t.getTablePosition().getRow()));

            if (!t.getNewValue().equals(t.getOldValue())) {
                renameNote(curNote, t.getNewValue());
                
                // TFE, 20221124: always init
                // rename failed: restore previous name
                // rename worked: update sort order
                initFromDirectory(true, false);
            }
        });
            
        // TFE, 2022118: merge grid cells with tagTree and editor for noteEditorFXML look & feel
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            noteEditBoxFXML.getChildren().remove(groupsPaneFXML);
        }
        
        // TFE, 20200810: adding third gridpane column for task handling
        taskList = new TaskList(taskListFXML, this);

        taskFilterCheck.getStyleClass().add("noteFilterCheck");
        taskFilterCheck.selectedProperty().addListener((o) -> {
            taskList.setTaskFilterMode(taskFilterCheck.isSelected());
        });
        taskFilterCheck.setSelected(false);
        taskFilterMode.setGraphic(taskFilterCheck);
        taskFilterMode.setContentDisplay(ContentDisplay.RIGHT);

        // all setup, lets spread the news
        OwnNoteFileManager.getInstance().setCallback(this);
        TaskManager.getInstance().setCallback(this);
        TagsEditor.getInstance().setCallback(this);
        TagManager.getInstance().setCallback(this);
        LinkManager.getInstance().setCallback(this);
        
        // run layout to have everything set up
        splitPaneXML.applyCss();
        splitPaneXML.requestLayout();
        
        // hide notesTableFXML header
        final Pane header = (Pane) notesTableFXML.lookup("TableHeaderRow");
        header.setMinHeight(0);
        header.setPrefHeight(0);
        header.setMaxHeight(0);
        header.setVisible(false);

        // 1st column: tag tree
        ColumnConstraints column1 = new ColumnConstraints();
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            column1.setPercentWidth(tagTreeWidth);
        } else {
            column1.setPercentWidth(0d);
        }

        // now set splitpane dividers with from preferences
        // TFE, 20201204: gets more tricky with for columns :-)
        // #1 tagtree is easy - use own percentage
        splitPaneXML.setDividerPosition(TAGTREE_NOTE_GROUP_DIVIDER, tagTreeWidth/100d);
        // #2 note/group is easy - use percentage of tagtree + own percentage
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            splitPaneXML.setDividerPosition(NOTE_GROUP_EDITOR_DIVIDER, (tagTreeWidth + groupTabsGroupWidth)/100d);
        } else {
            splitPaneXML.setDividerPosition(NOTE_GROUP_EDITOR_DIVIDER, groupTabsGroupWidth/100d);
        }
        // #4 tasklist is easy - use 100 - own percentage
        splitPaneXML.setDividerPosition(EDITOR_TASKLIST_DIVIDER, (100d - taskListWidth)/100d);
        
//        System.out.println("TAGTREE_COLUMN: " + tagTreeWidth);
//        System.out.println("NOTE_GROUP_COLUMN: " + groupTabsGroupWidth);
//        System.out.println("EDITOR_COLUMN: " + (100 - tagTreeWidth - groupTabsGroupWidth - taskListWidth));
//        System.out.println("TASKLIST_COLUMN: " + taskListWidth);
//        
//        System.out.println("TAGTREE_NOTE_GROUP_DIVIDER: " + splitPaneXML.getDividerPositions()[TAGTREE_NOTE_GROUP_DIVIDER]);
//        System.out.println("NOTE_GROUP_EDITOR_DIVIDER: " + splitPaneXML.getDividerPositions()[NOTE_GROUP_EDITOR_DIVIDER]);
//        System.out.println("EDITOR_TASKLIST_DIVIDER: " + splitPaneXML.getDividerPositions()[EDITOR_TASKLIST_DIVIDER]);

        // change width of gridpane when moving divider - but only after initial values have been set
        splitPaneXML.getDividers().get(TAGTREE_NOTE_GROUP_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // only do magic once the window is showing to avoid initial layout pass
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                tagTreeWidth = newValue.doubleValue() * 100d;
            }
        });

        splitPaneXML.getDividers().get(NOTE_GROUP_EDITOR_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // only do magic once the window is showing to avoid initial layout pass
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                groupTabsGroupWidth = newValue.doubleValue() * 100d;
                if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
                    groupTabsGroupWidth -= tagTreeWidth;
                }
            }
        });

        splitPaneXML.getDividers().get(EDITOR_TASKLIST_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // only do magic once the window is showing to avoid initial layout pass
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                // TFE, 20201203: ignore divider in case tasklist not visible
                if (tasklistVisible.get()) {
                    taskListWidth = newValue.doubleValue() * 100d;
                }
            }
        });

        // init menu handling
        // TFE, 20201203: do this last since it does changes to layout depending on tasklistVisible 
        initMenus();
    }
    
    private void initMenus() {
        // 1. select entry based on value of currentLookAndFeel
        switch (currentLookAndFeel) {
            case groupTabs:
                groupTabsLookAndFeel.setSelected(true);
                break;
            case tagTree:
                tagTreeLookAndFeel.setSelected(true);
                break;
        }

        // 2. add listener to track changes of layout - only after setting it initially
        groupTabsLookAndFeel.getToggleGroup().selectedToggleProperty().addListener(
            (ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2) -> {
                // when starting up things might not be initialized properly
                if (arg2 != null) {
                    assert (arg2 instanceof RadioMenuItem);

                    // store in the preferences - don't overwrite local variable!
                    final RadioMenuItem radioArg = ((RadioMenuItem) arg2);
                    if (radioArg.equals(groupTabsLookAndFeel)) {
                        OwnNoteEditorPreferences.RECENT_LOOKANDFEEL.put(OwnNoteEditorParameters.LookAndFeel.groupTabs);
                    } else if (radioArg.equals(tagTreeLookAndFeel)) {
                        OwnNoteEditorPreferences.RECENT_LOOKANDFEEL.put(OwnNoteEditorParameters.LookAndFeel.tagTree);
                    }
                }
            });
        
        // add changelistener to pathlabel - not that you should actually change its value during runtime...
        ownCloudPath.textProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    // store in the preferences
                    OwnNoteEditorPreferences.RECENT_OWNCLOUDPATH.put(newValue);

                    // scan files in new directory
                    initFromDirectory(false, true);
                }
            }); 
        // TFE, 20181028: open file chooser also when left clicking on pathBox
        ownCloudPath.setOnMouseClicked((event) -> {
            if (MouseButton.PRIMARY.equals(event.getButton())) {
                setOwnCloudPath.fire();
            }
        });
        
        // add action to the button - open a directory search dialogue...
        setOwnCloudPath.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select ownCloud Notes directory");
            if (ownCloudPath != null && !ownCloudPath.getText().isEmpty()) {
                final File ownFile = new File(ownCloudPath.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                ownCloudPath.setText(selectedDirectory.getAbsolutePath());
            }
        });
        
        // TFE, 20201130: show / hide tasklist
        setTasklistVisible(tasklistVisible.get());
        tasklistVisible.addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                setTasklistVisible(newValue);
            }
        });
        menuShowTasklist.setSelected(tasklistVisible.get());
        tasklistVisible.bindBidirectional(menuShowTasklist.selectedProperty());
        
        // TFE, 20210106: our own little KANBAN board...
        menuTaskboard.setOnAction((t) -> {
            TaskBoard.getInstance().show();
        });

        // TFE, 20201025: and now we have tag management as well :-)
        menuEditTags.setOnAction((t) -> {
            TagsEditor.getInstance().editTags(null);
        });
        menuGroups2Tags.setOnAction((t) -> {
            TagManager.getInstance().groupsToTags();
        });

        AboutMenu.getInstance().addAboutMenu(OwnNoteEditor.class, borderPane.getScene().getWindow(), menuBar, "OwnNoteEditor", "v6.2", "https://github.com/ThomasDaheim/ownNoteEditor");
        
        // TFE, 20220429: get app version so that we can compare it with one from registry - to determine migration needs!
        AppInfo.getInstance().initAppInfo(OwnNoteEditor.class, "OwnNoteEditor", "v6.2", "https://github.com/ThomasDaheim/ownNoteEditor");
        AppVersion.CURRENT.setVersionId(Double.valueOf(AppInfo.getInstance().getAppVersion()));
    }
    
    // do everything to show / hide tasklist
    private void setTasklistVisible(final boolean visible) {
//        System.out.println("Start switching taskList to " + visible);
        if (taskFilterBoxFXML.isVisible() == visible) {
            return;
        }

        // 1) show / hide taskFilterBoxFXML
        taskFilterBoxFXML.setVisible(visible);
        taskFilterBoxFXML.setDisable(!visible);
        taskFilterBoxFXML.setManaged(visible);

        // 2) show / hide taskListFXML container
        rightPaneXML.setVisible(visible);
        rightPaneXML.setDisable(!visible);
        rightPaneXML.setManaged(visible);

        if (visible) {
            // 2) show / hide taskListFXML container
            rightPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.1));
            rightPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.3));

            // 3) show / hide grid column
//            gridPane.getColumnConstraints().get(TASKLIST_COLUMN).setPercentWidth(taskListWidth);
        } else {
            // 2) show / hide taskListFXML container
            rightPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.0));
            rightPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.0));

            // 3) show / hide grid column
//            gridPane.getColumnConstraints().get(TASKLIST_COLUMN).setPercentWidth(0d);
        }

        // 4) update calculation of percentages for resize
//        splitPaneXML.setDividerPosition(EDITOR_TASKLIST_DIVIDER, (100d - gridPane.getColumnConstraints().get(TASKLIST_COLUMN).getPercentWidth())/100d);

//        System.out.println("Done switching taskList");
    }
    
    public static boolean noteVersionLower(final Note note, final AppVersion appVersion) {
        // check note last save version against required version number
        return appVersion.getVersionId() > note.getMetaData().getAppVersion();
    }
    
    public void initFromDirectory(final boolean updateOnly, final boolean resetTasksTags) {
        checkChangedNote();

        if (resetTasksTags) {
            // TFE, 20201115: throw away any current tasklist - we might have changed the path!
            TaskManager.getInstance().resetTaskList();
            TagManager.getInstance().resetTagList();

            RecentNoteForGroup.getInstance().clear();
        }
        
        // scan directory and re-populate lists
        OwnNoteFileManager.getInstance().initNotesPath(ownCloudPath.textProperty().getValue());

        if (resetTasksTags) {
            // TFE, 20201206: re-populate tags treeview as well - if shown
            if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
                tagsTreeView.fillTreeView(TagsTreeView.WorkMode.LIST_MODE, null);
            }
            
            Platform.runLater(() -> {
                taskList.populateTaskList();
                
                LinkManager.getInstance().findNoteLinks();
            });
        }
        
        // add new table entries & disable & enable accordingly
        notesList = OwnNoteFileManager.getInstance().getNotesList();
        // http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        
        // Issue #59: advanced filtering & sorting
        // do the stuff in the OwnNoteTableView - thats the right place!
        notesTable.setNotes(notesList);
        
        myGroupList.setGroups(TagManager.getInstance().getGroupTags(false), updateOnly);
    }
    
    public boolean checkChangedNote() {
        Boolean result = true;
        
        // fix for #13: check for unsaved changes
        if (noteHTMLEditor.hasChanged() || noteMetaEditor.hasChanged()) {
            final ButtonType buttonSave = new ButtonType("Save", ButtonData.OTHER);
            final ButtonType buttonDiscard = new ButtonType("Discard", ButtonData.OTHER);

            Optional<ButtonType> saveChanges = showAlert(AlertType.CONFIRMATION, "Unsaved changes!", "Save now or discard your changes?", null, buttonSave, buttonDiscard);

            // TF: 20151229: cancel doesn#t work since there seems to be no way to avoid change of focus
            // and clicking on other note or group => UI would show a different note than is actualy edited
            // final ButtonType buttonCancel = new ButtonType("Cancel", ButtonData.OTHER);
            // alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

            if (saveChanges.isPresent()) {
                final Note prevNote = noteHTMLEditor.getEditedNote();
                if (saveChanges.get().equals(buttonSave)) {
                    // save note
                    prevNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                    if (!saveNote(prevNote)) {
                    }
                } else {
                    OwnNoteFileManager.getInstance().readNote(prevNote, true);
                }
            }
        }

        return result;
    }
    
    public boolean editNote(final Note note) {
        boolean result = false;
        
        // TFE, 20210102: are we already editing that note? than we're done here...
        if (note.equals(noteHTMLEditor.getEditedNote()) || !checkChangedNote()) {
            return result;
        }
        
        // 2. show content of file in editor
        if (note.getNoteFileContent() == null) {
            OwnNoteFileManager.getInstance().readNote(note, true);
        }
        noteHTMLEditor.editNote(note);
        noteMetaEditor.editNote(note);
        currentNoteProperty.set(note);
        RecentNoteForGroup.getInstance().put(note.getGroup().getExternalName(), note);
        
        return result;
    }
    
    public Note getEditedNote() {
        return noteHTMLEditor.getEditedNote();
    }
    
    public void selectNote(final String noteName) {
        selectNote(OwnNoteFileManager.getInstance().getNote(noteName));
    }

    private void selectNote(final Note note) {
        // clear any filters to make sure group & note can be shown
        noteFilterText.setText("");
        noteFilterCheck.setSelected(false);

        // need to distinguish between views to select group
        myGroupList.selectGroupForNote(note);
        
        // and now select the note - leads to callback to editNote to fill the htmleditor
        notesTable.selectNote(note);
    }
    
    public void selectNoteAndCheckBox(final Note note, final int textPos, final String htmlText, final String taskId) {
        selectNote(note);
        
        noteHTMLEditor.scrollToCheckBox(textPos, htmlText, taskId);
    }
    
    public void selectNoteAndToggleCheckBox(final Note note, final int textPos, final String htmlText, final String taskId, final boolean isChecked) {
        selectNote(note);
        
        // scroll & change the status
        noteHTMLEditor.scrollToAndToggleCheckBox(textPos, htmlText, taskId, isChecked);
    }

    public OwnNoteHTMLEditor getNoteEditor() {
        return noteHTMLEditor;
    }

    @Override
    public boolean deleteNote(final Note curNote) {
        boolean result = OwnNoteFileManager.getInstance().deleteNote(curNote);

        if (!result) {
            // error message - something went wrong
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while deleting the note.", "See log for details.");
        } else {
            // update group tags as well
            TagManager.getInstance().deleteNote(curNote);

            LinkManager.getInstance().deleteNote(curNote);
        }
        
        return result;
    }

    @Override
    public boolean createNote(final TagData newGroup, final String newNoteName) {
        boolean result = OwnNoteFileManager.getInstance().createNote(newGroup, newNoteName);

        if (!result) {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "New note couldn't be created.", "Note with same group and name already exists.");
        } else {
            // update group tags as well
            TagManager.getInstance().createNote(newGroup, newNoteName);

            LinkManager.getInstance().createNote(newGroup, newNoteName);
        }
        
        return result;
    }

    @Override
    public boolean renameNote(final Note curNote, final String newValue) {
        final Note origNote = new Note(curNote);
        
        boolean result = OwnNoteFileManager.getInstance().renameNote(curNote, newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while renaming the note.", "A note with the same name already exists.");
        } else {
            //check if we just moved the current note in the editor...
            noteHTMLEditor.doNameChange(curNote.getGroup(), curNote.getGroup(), curNote.getNoteName(), newValue);

            // update group tags as well
            TagManager.getInstance().renameNote(origNote, newValue);
            
            LinkManager.getInstance().renameNote(origNote, newValue);
        }
        
        return result;
    }

    @Override
    public boolean moveNote(final Note curNote, final TagData newGroup) {
        final Note origNote = new Note(curNote);
        
        boolean result = OwnNoteFileManager.getInstance().moveNote(curNote, newGroup);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while moving the note.", "A note with the same name already exists in the new group.");
        } else {
            //check if we just moved the current note in the editor...
            noteHTMLEditor.doNameChange(curNote.getGroup(), newGroup, curNote.getNoteName(), curNote.getNoteName());

            // update group tags as well
            TagManager.getInstance().moveNote(origNote, newGroup);
            
            LinkManager.getInstance().moveNote(origNote, newGroup);

            refilterNotesList();
        }
        
        return result;
    }

    @Override
    public boolean saveNote(final Note note) {
        boolean result = OwnNoteFileManager.getInstance().saveNote(note);
                
        if (result) {
            // TF, 20170723: refresh notes list since modified has changed
            notesTableFXML.refresh();
            
            // update all editors
            noteHTMLEditor.hasBeenSaved();
            noteMetaEditor.hasBeenSaved();

            // update group tags as well
            TagManager.getInstance().saveNote(note);

            LinkManager.getInstance().saveNote(note);
        } else {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "Note couldn't be saved.", null);
        }
        
        return result;
    }
    
    public boolean doArchiveRestoreNote(final Note note) {
        // its only a kind of move to another, special group...
        return moveNote(note, TagManager.getInstance().getComplementaryGroup(note.getGroup(), true));
    }
    
    public void refilterNotesList() {
        notesTable.setFilterPredicate();
    }
    
    public void setGroupFilter(final TagData group) {
        currentGroupTagProperty.set(group);
        
        notesTable.setGroupFilter(group);
        
        // TFE, 20210715: select most recent note for this group - if any
        if (RecentNoteForGroup.getInstance().containsKey(group.getExternalName())) {
            notesTable.selectNote(RecentNoteForGroup.getInstance().get(group.getExternalName()));
        }
    }
    
    public void setTagFilter(final TagData tag) {
        notesTable.setTagFilter(tag);
    }

    public void setNotesTableBackgroundColor(final String color) {
        notesTable.setBackgroundColor(color);
    }
    
    public void selectFirstOrCurrentNote() {
        // select first or current note - if any
        if (!notesTable.getItems().isEmpty()) {
            // check if current edit is ongoing AND note in the select tab (can happen with drag & drop!)
            currentNoteProperty.set(noteHTMLEditor.getEditedNote());

            // TFE, 20201030: support re-load of last edited note
            if (currentNoteProperty.get() == null) {
                final String lastGroupName = OwnNoteEditorPreferences.LAST_EDITED_GROUP.getAsType();
                final TagData lastGroup = TagManager.getInstance().groupForExternalName(lastGroupName, false);
                final String lastNoteName = OwnNoteEditorPreferences.LAST_EDITED_NOTE.getAsType();
                if (lastGroup != null && OwnNoteFileManager.getInstance().noteExists(lastGroup, lastNoteName)) {
                    currentNoteProperty.set(OwnNoteFileManager.getInstance().getNote(lastGroup, lastNoteName));
                    
                    if (firstNoteAccess) {
                        // done, selectGroupForNote calls selectFirstOrCurrentNote() internally - BUT NO LOOPS PLEASE
                        firstNoteAccess = false;
                        Platform.runLater(() -> {
                            myGroupList.selectGroupForNote(currentNoteProperty.get());
                        });
                        return;
                    }
                }
            }
            
            int selectIndex = 0;
            if (currentNoteProperty.get() != null && notesTable.getItems().contains(currentNoteProperty.get())) {
                selectIndex = notesTable.getItems().indexOf(currentNoteProperty.get());
            }

            // TFE, 20201030: this also starts editing of the note
            notesTable.selectAndFocusRow(selectIndex);
        } else {
            // TFE, 20201012: check for changes also when no new note (e.g. selecting an empty group...
            if (!checkChangedNote()) {
                return;
            }

            noteHTMLEditor.setDisable(true);
            noteHTMLEditor.editNote(null);
            noteMetaEditor.editNote(null);
        }
    }
    
    public String uniqueNewNoteNameForGroup(final TagData group) {
        String result;
        int newCount = notesList.size() + 1;
        
        do {
            result = OwnNoteEditor.NEW_NOTENAME + " " + newCount;
            newCount++;
        } while(OwnNoteFileManager.getInstance().noteExists(group, result));
        
        return result;
    }

    @Override
    public boolean processFileChange(final WatchEvent.Kind<?> eventKind, final Path filePath) {
        // System.out.printf("Time %s: Gotcha!\n", getCurrentTimeStamp());
        boolean result = true;
        
        final String fileName = filePath.getFileName().toString();
        if (!filesInProgress.contains(fileName)) {
        
//            System.out.printf("Time %s: You're new here! %s\n", getCurrentTimeStamp(), fileName);
            filesInProgress.add(fileName);
            
            // re-init list of groups and notes - file has beeen added or removed
            Platform.runLater(() -> {
                if (!StandardWatchEventKinds.ENTRY_CREATE.equals(eventKind)) {
                    // delete & modify is only relevant if we're editing this note...
                    if (noteHTMLEditor.getEditedNote() != null) {
                        final Note curNote = noteHTMLEditor.getEditedNote();
                        final String curName = OwnNoteFileManager.getInstance().buildNoteName(curNote);

                        if (curName.equals(filePath.getFileName().toString())) {
                            String alertTitle;
                            String alertHeaderText;
                            if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                alertTitle = "Note has changed in file system!";
                                alertHeaderText = "Options:\nSave own note to different name\nSave own note and overwrite file system changes\nLoad changed note and discard own changes";
                            } else {
                                alertTitle = "Note has been deleted in file system!";
                                alertHeaderText = "Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes";
                            }
                            
                            // same note! lets ask the user what to do...
                            final ButtonType buttonSave = new ButtonType("Save own", ButtonData.OK_DONE);
                            final ButtonType buttonSaveNew = new ButtonType("Save as new", ButtonData.OTHER);
                            final ButtonType buttonDiscard = new ButtonType("Discard own", ButtonData.CANCEL_CLOSE);
                            Optional<ButtonType> saveChanges = showAlert(AlertType.CONFIRMATION, alertTitle, alertHeaderText, null, buttonSave, buttonSaveNew, buttonDiscard);

                            if (saveChanges.isPresent()) {
                                if (saveChanges.get().equals(buttonSave)) {
                                    // save own note independent of file system changes
                                    final Note saveNote = noteHTMLEditor.getEditedNote();
                                    saveNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                                    if (saveNote(saveNote)) {
                                    }
                                }

                                if (saveChanges.get().equals(buttonSaveNew)) {
                                    // save own note under new name => rename note & save
                                    final Note saveNote = noteHTMLEditor.getEditedNote();
                                    final String newNoteName = uniqueNewNoteNameForGroup(saveNote.getGroup());
                                    // can't use rename to change note name since this would use the new version / deleted version in the file system
                                    saveNote.setNoteName(newNoteName);
                                    saveNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                                    if (saveNote(saveNote)) {
                                    }
                                }

                                if (saveChanges.get().equals(buttonDiscard)) {
                                    // nothing to do for StandardWatchEventKinds.ENTRY_DELETE - initFromDirectory(true) will take care of this
                                    if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                        // re-load into edit for StandardWatchEventKinds.ENTRY_MODIFY
                                        final Note loadNote = noteHTMLEditor.getEditedNote();
                                        OwnNoteFileManager.getInstance().readNote(loadNote, true);
                                        noteHTMLEditor.editNote(loadNote);
                                    }
                                }
                            }
                        }
                    }
                }
            
                // TFE, 20210101: only initFromDirectory if anything related to note files has changed!
                if (OwnNoteFileManager.NOTE_EXT.equals(FilenameUtils.getExtension(fileName))) {
                    // show only notes for selected group
                    initFromDirectory(true, true);
                    selectFirstOrCurrentNote();

                    // but only if group still exists in the list!
                    if (TagManager.getInstance().getGroupTags(true).contains(myGroupList.getCurrentGroup())) {
                        setGroupFilter(myGroupList.getCurrentGroup());
                    }

                    filesInProgress.remove(fileName);
                }
            });
        }
        
        return result;
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    // TF, 20160630: refactored from "getClassicLook" to show its real meeaning
    public OwnNoteEditorParameters.LookAndFeel getCurrentLookAndFeel() {
        return currentLookAndFeel;
    }
    
    // TF, 20160816: wrapper for alerts that stores the alert as long as its shown - needed for testing alerts with testfx
    public Optional<ButtonType> showAlert(final AlertType alertType, final String title, final String headerText, final String contentText, final ButtonType ... buttons) {
        Alert result;
        
        result = new Alert(alertType);
        if (title != null) {
            result.setTitle(title);
        }
        if (headerText != null) {
            result.setHeaderText(headerText);
        }
        if (contentText != null) {
            result.setContentText(contentText);
        }
        
        // add optional buttons
        if (buttons.length > 0) {
            result.getButtonTypes().setAll(buttons);
        }
        
        // add info for later lookup in testfx - doesn't work yet
        result.getDialogPane().getStyleClass().add("alertDialog");
        result.initOwner(borderPane.getScene().getWindow());

        // get button pressed
        Optional<ButtonType> buttonPressed = result.showAndWait();
        result.close();
        
        return buttonPressed;
    }
    
    public Set<Note> getNotesWithText(final String searchText) {
        return OwnNoteFileManager.getInstance().getNotesWithText(searchText);
    }
    
    public Window getWindow() {
        return borderPane.getScene().getWindow();
    }

    // not to confuse with the static method in TaskManager - this does the bookkeeping for the current node as well
    public void replaceCheckedBoxes() {
        noteHTMLEditor.replaceCheckedBoxes();
    }
    
    // not to confuse with the static method in TaskManager - this does the bookkeeping for the current node as well
    public void replaceCheckmarks() {
        noteHTMLEditor.replaceCheckmarks();
    }
    
    // not to confuse with the static method in ListManager - this does the bookkeeping for the current node as well
    public void replaceNoteLinks(final String oldNoteName, final String newNoteName) {
        noteHTMLEditor.replaceNoteLinks(oldNoteName, newNoteName);
    }
    
    // not to confuse with the static method in ListManager - this does the bookkeeping for the current node as well
    public void invalidateNoteLinks(final String noteName) {
        noteHTMLEditor.invalidateNoteLinks(noteName);
    }
    
    // and now everyone can listen to note selection changes...
    public ReadOnlyObjectProperty<Note> currentNoteProperty() {
        return currentNoteProperty.getReadOnlyProperty();
    }

    // and now everyone can listen to group attribute changes...
    public ReadOnlyObjectProperty<TagData> currentGroupTagProperty() {
        return currentGroupTagProperty.getReadOnlyProperty();
    }
}
