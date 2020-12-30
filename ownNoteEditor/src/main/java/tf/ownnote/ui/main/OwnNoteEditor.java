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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
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
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.tuple.Pair;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AboutMenu;
import tf.helper.javafx.EnumHelper;
import tf.helper.javafx.TableMenuUtils;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.helper.OwnNoteMetaEditor;
import tf.ownnote.ui.helper.OwnNoteTabPane;
import tf.ownnote.ui.helper.OwnNoteTableColumn;
import tf.ownnote.ui.helper.OwnNoteTableView;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteGroup;
import tf.ownnote.ui.tags.TagEditor;
import tf.ownnote.ui.tags.TagManager;
import tf.ownnote.ui.tags.TagsTreeView;
import tf.ownnote.ui.tasks.TaskData;
import tf.ownnote.ui.tasks.TaskList;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditor implements Initializable, IFileChangeSubscriber, INoteCRMDS {

    private final List<String> filesInProgress = new LinkedList<>();

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    
    public final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss");

    private final static String NEW_NOTENAME = "New Note";
    
    public final static String GROUP_COLOR_CSS = "group-color";
    
    // TFE, 20200712: add search of unchecked boxes
    // TFE, 20201103: actual both variants of html are valid and need to be supported equally
    public final static String UNCHECKED_BOXES_1 = "<input type=\"checkbox\" />";
    public final static String CHECKED_BOXES_1 = "<input type=\"checkbox\" checked=\"checked\" />";
    public final static String UNCHECKED_BOXES_2 = "<input type=\"checkbox\">";
    public final static String CHECKED_BOXES_2 = "<input type=\"checkbox\" checked=\"checked\">";
    public final static String ANY_BOXES = "<input type=\"checkbox\"";
    
    private final static int TEXTFIELD_WIDTH = 100;  
    
    private final List<String> realGroupNames = new LinkedList<>();
    
    private ObservableList<Note> notesList = null;
    
    private final BooleanProperty inEditMode = new SimpleBooleanProperty();
    private boolean firstNoteAccess = true;
    
    private boolean handleQuickSave = false;
    // should we show standard ownNote face or groupTabs?
    // TF, 20160630: refactored from "classicLook" to show its real meeaning
    private OwnNoteEditorParameters.LookAndFeel currentLookAndFeel;

    private Double tagTreeWidth;
    private Double classicGroupWidth;
    private Double groupTabsGroupWidth;
    private Double taskListWidth;
    
    private BooleanProperty tasklistVisible = new SimpleBooleanProperty(true);
    
    private Note curNote;
    
    // Indicates that the divider is currently dragged by the mouse
    // see https://stackoverflow.com/a/40707931
    private boolean mouseDragOnDivider = false;    
    
    private IGroupListContainer myGroupList = null;
    
    // available colors for tabs to rotate through
    // issue #36 - have "All" without color
    // TF, 20170122: use colors similar to OneNote - a bit less bright
    //private static final String[] groupColors = { "lightgrey", "darkseagreen", "cornflowerblue", "lightsalmon", "gold", "orchid", "cadetblue", "goldenrod", "darkorange", "MediumVioletRed" };
    private static final String[] groupColors = { "#F3D275", "#F4A6A6", "#99D0DF", "#F1B87F", "#F2A8D1", "#9FB2E1", "#B4AFDF", "#D4B298", "#C6DA82", "#A2D07F", "#F1B5B5" };
    
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
    
    private OwnNoteTabPane groupsPane = null;
    @FXML
    private BorderPane borderPane;
    @FXML
    private GridPane gridPane;
    @FXML
    private TableView<Map<String, String>> notesTableFXML;
    private OwnNoteTableView notesTable = null;
    @FXML
    private TableView<Map<String, String>> groupsTableFXML;
    private OwnNoteTableView groupsTable = null;
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
    private WebView noteHTMLEditorFXML;
    private OwnNoteHTMLEditor noteHTMLEditor = null;
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
    private RadioMenuItem groupTabsLookAndFeel;
    @FXML
    private ToggleGroup LookAndFeel;
    @FXML
    private Menu menuLookAndFeel;
    private StackPane noteEditorPaneFXML;
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
    private HBox taskFilterBox;
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
    private OwnNoteMetaEditor noteMetaEditor = null;
    @FXML
    private RadioMenuItem tagTreeLookAndFeel;
    @FXML
    private CheckMenuItem menuShowTasklist;
    @FXML
    private StackPane tagsTreePaneXML;
    private TagsTreeView tagsTreeView;

    public OwnNoteEditor() {
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // defer initEditor since we need to know the value of the prameters...
    }
    
    public void stop(final boolean productiveRun) {
        OwnNoteFileManager.getInstance().stop();
        
        if (productiveRun) {
            // store current percentage of group column width
            // if increment is passed as parameter, we need to remove it from the current value
            // otherwise, the percentage grows with each call :-)
            final String percentWidth = String.valueOf(gridPane.getColumnConstraints().get(NOTE_GROUP_COLUMN).getPercentWidth());
            // store in the preferences
            if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_CLASSIC_GROUPWIDTH, percentWidth);
            } else {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_GROUPTABS_GROUPWIDTH, percentWidth);
            }
            // TFE, 20201204: store tag tree width only for this look & feel
            if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_TAGTREE_WIDTH, String.valueOf(gridPane.getColumnConstraints().get(TAGTREE_COLUMN).getPercentWidth()));
            }
            // TFE, 20201203: taskList can be hidden (and therefore have column has width 0)
            if (tasklistVisible.get()) {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_TASKLIST_WIDTH, String.valueOf(gridPane.getColumnConstraints().get(TASKLIST_COLUMN).getPercentWidth()));
            } else {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_TASKLIST_WIDTH, String.valueOf(taskListWidth));
            }
            OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_TASKLIST_VISIBLE, String.valueOf(tasklistVisible.get()));

            // issue #45 store sort order for tables
            groupsTable.savePreferences(OwnNoteEditorPreferences.getInstance());
            notesTable.savePreferences(OwnNoteEditorPreferences.getInstance());

            // TFE, 20200903: store groups tabs order as well
            groupsPane.savePreferences(OwnNoteEditorPreferences.getInstance());

            // TFE, 20201030: store name of last edited note
            if (noteHTMLEditor.getEditedNote() != null) {
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.LAST_EDITED_NOTE, noteHTMLEditor.getEditedNote().getNoteName());
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.LAST_EDITED_GROUP, noteHTMLEditor.getEditedNote().getGroupName());
            }

            // TFE, 20201121: tag info is now stored in a separate file
            TagManager.getInstance().saveTags();
        }
    }
    
    public void setParameters() {
        // set look & feel    
        // 1. use passed parameters
        if (OwnNoteEditor.parameters.getLookAndFeel().isPresent()) {
            currentLookAndFeel = OwnNoteEditor.parameters.getLookAndFeel().get();
        } else {
            // fix for issue #20
            // 2. try the preference settings - what was used last time?
            try {
                currentLookAndFeel = EnumHelper.getInstance().enumFromPreferenceWithDefault(
                        OwnNoteEditorPreferences.getInstance(),
                        OwnNoteEditorPreferences.RECENT_LOOKANDFEEL,
                        OwnNoteEditorParameters.LookAndFeel.class,
                        OwnNoteEditorParameters.LookAndFeel.classic.name());
                // System.out.println("Using preference for currentLookAndFeel: " + currentLookAndFeel);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
                currentLookAndFeel = OwnNoteEditorParameters.LookAndFeel.groupTabs;
            }
        }
        
        // issue #30: get percentages for group column width for classic and onenote look & feel
        // issue #45 store sort order for tables
        try {
            tagTreeWidth = Double.valueOf(
                    OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_TAGTREE_WIDTH, "18.3333333"));
            classicGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_CLASSIC_GROUPWIDTH, "18.3333333"));
            groupTabsGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_GROUPTABS_GROUPWIDTH, "30.0"));
            taskListWidth = Double.valueOf(
                    OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_TASKLIST_WIDTH, "15.0"));
            tasklistVisible.set(Boolean.valueOf(
                    OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_TASKLIST_VISIBLE, "true")));
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            tagTreeWidth = 18.3333333;
            classicGroupWidth = 18.3333333;
            groupTabsGroupWidth = 33.3333333;
            taskListWidth = 15.0;
            tasklistVisible.set(true);
        }
        
        // TFE, 20201205: limit values to allowed ones
        tagTreeWidth = limit(tagTreeWidth, paneSizes.get(TAGTREE_COLUMN).getLeft(), paneSizes.get(TAGTREE_COLUMN).getRight());
        classicGroupWidth = limit(classicGroupWidth, paneSizes.get(NOTE_GROUP_COLUMN).getLeft(), paneSizes.get(NOTE_GROUP_COLUMN).getRight());
        groupTabsGroupWidth = limit(groupTabsGroupWidth, paneSizes.get(NOTE_GROUP_COLUMN).getLeft(), paneSizes.get(NOTE_GROUP_COLUMN).getRight());
        taskListWidth = limit(taskListWidth, paneSizes.get(TASKLIST_COLUMN).getLeft(), paneSizes.get(TASKLIST_COLUMN).getRight());
        
        // paint the look
        initEditor();

        // init ownCloudPath to parameter or nothing
        pathLabel.setMinWidth(Region.USE_PREF_SIZE);
        String pathname = "";
        // 1. use any passed parameters
        if (OwnNoteEditor.parameters.getOwnCloudDir().isPresent()) {
            pathname = OwnNoteEditor.parameters.getOwnCloudDir().get();
        } else {
            // 2. try the preferences setting - most recent file that was opened
            try {
                pathname = OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.RECENT_OWNCLOUDPATH, "");
                // System.out.println("Using preference for ownCloudDir: " + pathname);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // 3. set the value
        ownCloudPath.setText(pathname);
    }

    //
    // basic setup is a 3x2 gridpane with a 3 part splitpane in the lower row
    // TFE: 20181028: pathBox has moved to menu to make room for filterBox
    // TFE, 20200810: 3rd column added for task handling
    // TFE, 20201204: column to the left added for tag tree
    //
    // -------------------------------------------------------------------------------------------------------------------
    // |                            |                            |                           |                           |
    // |                            | all : noteFilterBox        | classic: buttonBox        | all: taskFilterBox        |
    // |                            |                            | groupTabs: groupsPaneFXML |                           |
    // |                            |                            | tagTree: nothing          |                           |
    // |                            |                            |                           |                           |
    // -------------------------------------------------------------------------------------------------------------------
    // |                            |                            |                           |                           |
    // | classic: nothing           | classic: groupsTableFXML   | all: noteEditorFXML       | all: taskListFXML         |
    // | groupTabs: nothing         | groupTabs: notesTableFXML  |                           |                           |
    // | tagTree: TagsTreeView      | tagTree: notesTableFXML    |                           |                           |
    // |                            |                            |                           |                           |
    // -------------------------------------------------------------------------------------------------------------------
    //
    // to be able to do proper layout in scenebuilder everything except the dividerPane
    // are added to the fxml into the gridpane - code below does the re-arrangement based on 
    // value of currentLookAndFeel
    //
    private void initEditor() {
        // init our wrappers to FXML classes...
        noteNameCol = new OwnNoteTableColumn(noteNameColFXML, this);
        noteNameColFXML.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        noteModifiedCol = new OwnNoteTableColumn(noteModifiedColFXML, this);
        noteDeleteCol = new OwnNoteTableColumn(noteDeleteColFXML, this);
        noteDeleteColFXML.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        noteGroupCol = new OwnNoteTableColumn(noteGroupColFXML, this);
        noteGroupColFXML.setVisible(false);
        noteGroupColFXML.setMinWidth(0d);
        noteGroupColFXML.setMaxWidth(0d);
        noteGroupColFXML.setUserData(TableMenuUtils.NO_LIST_COLUMN);
        notesTable = new OwnNoteTableView(notesTableFXML, this);
        notesTable.loadPreferences(OwnNoteEditorPreferences.getInstance());

        groupNameCol = new OwnNoteTableColumn(groupNameColFXML, this);
        groupNameColFXML.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        groupDeleteCol = new OwnNoteTableColumn(groupDeleteColFXML, this);
        groupDeleteColFXML.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        groupCountCol = new OwnNoteTableColumn(groupCountColFXML, this);
        groupsTable = new OwnNoteTableView(groupsTableFXML, this);
        groupsTable.loadPreferences(OwnNoteEditorPreferences.getInstance());
        
        groupsPane = new OwnNoteTabPane(groupsPaneFXML, this);
        groupsPane.loadPreferences(OwnNoteEditorPreferences.getInstance());
        
        VBox.setVgrow(noteHTMLEditorFXML, Priority.ALWAYS);
        VBox.setVgrow(noteMetaEditorFXML, Priority.NEVER);
        noteHTMLEditor = new OwnNoteHTMLEditor(noteHTMLEditorFXML, this);
        noteMetaEditor = new OwnNoteMetaEditor(noteMetaEditorFXML, this);
        
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
        
        // 1st column: tag tree
        ColumnConstraints column1 = new ColumnConstraints();
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            column1.setPercentWidth(tagTreeWidth);
        } else {
            column1.setPercentWidth(0d);
        }

        // 2nd column: groups table or notes table
        ColumnConstraints column2 = new ColumnConstraints();
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            column2.setPercentWidth(classicGroupWidth);
        } else {
            column2.setPercentWidth(groupTabsGroupWidth);
        }
        column2.setHgrow(Priority.ALWAYS);

        // 3rd column: notes editor
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setHgrow(Priority.ALWAYS);

        // 4th column: tasklist
        ColumnConstraints column4 = new ColumnConstraints();
        column4.setPercentWidth(taskListWidth);
        column4.setHgrow(Priority.ALWAYS);

        gridPane.getColumnConstraints().addAll(column1, column2, column3, column4);
        // EDITOR_COLUMN width is gicen by all other width values
        setRemainingColumnWidth(EDITOR_COLUMN);
        
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
        noteNameCol.setTableColumnProperties(0.65, Note.getNoteName(0), OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel));
        noteModifiedCol.setTableColumnProperties(0.25, Note.getNoteName(1), false);
        // see issue #42
        noteModifiedCol.setComparator(FormatHelper.getInstance().getFileTimeComparator());
        noteDeleteCol.setTableColumnProperties(0.10, Note.getNoteName(2), false);
        noteGroupCol.setTableColumnProperties(0, Note.getNoteName(3), false);

        // only new button visible initially
        hideAndDisableAllCreateControls();
        hideAndDisableAllEditControls();
        
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

        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            myGroupList = groupsTable;
            
            hideNoteEditor();
            
            groupsPane.setDisable(true);
            groupsPane.setVisible(false);

            // set callback, width, value name, cursor type of columns
            groupNameCol.setTableColumnProperties(0.65, NoteGroup.getNoteGroupName(0), false);
            groupDeleteCol.setTableColumnProperties(0.15, NoteGroup.getNoteGroupName(1), false);
            groupCountCol.setTableColumnProperties(0.20, NoteGroup.getNoteGroupName(2), false);

            // name can be changed - but not for all entries!
            groupsTable.setEditable(true);
            groupNameCol.setEditable(true);

            // in case the group name changes notes neeed to be renamed
            groupNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final NoteGroup curEntry =
                        new NoteGroup(ObjectsHelper.uncheckedCast(t.getTableView().getItems().get(t.getTablePosition().getRow())));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    // rename all notes of the group
                    if (!renameGroupWrapper(t.getOldValue(), t.getNewValue())) {
                        // TODO: revert changes to group name on UI
                        curEntry.setGroupName(t.getOldValue());

                        // workaround til TODO above resolved :-)
                        initFromDirectory(false, false);
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
            inEditMode.bind(noteHTMLEditor.visibleProperty());

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
                        if (newValue.equals(NoteGroup.NEW_GROUP)) {
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
                groupNameBox.setValue(NoteGroup.NOT_GROUPED);
                groupNameBox.requestFocus();
            });
            // issue #41
            newButton.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> {
                newButton.fire();
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

                    showAlert(AlertType.ERROR, "Error Dialog", "No note title given.", null);
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(NoteGroup.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doCreate = false;

                        showAlert(AlertType.ERROR, "Error Dialog", "No group title given.", null);
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doCreate) {
                    if (createNote(newGroupName, newNoteName)) {
                        hideAndDisableAllCreateControls();
                        initFromDirectory(false, false);
                    }
                }
            });

            // quicksave button saves note but stays in editor
            quickSaveButton.setOnAction((ActionEvent event) -> {
                // quicksave = no changes to note name and group name allowed!
                final Note curNote = noteHTMLEditor.getEditedNote();
                curNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                
                if (OwnNoteFileManager.getInstance().saveNote(curNote)) {
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    showAlert(AlertType.ERROR, "Error Dialog", "Note couldn't be saved.", null);
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

                    showAlert(AlertType.ERROR, "Error Dialog", "No note title given.", null);
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(NoteGroup.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doSave = false;

                        showAlert(AlertType.ERROR, "Error Dialog", "No group title given.", null);
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doSave) {
                    // check against previous note and group name - might have changed!
                    final Note curNote = noteHTMLEditor.getEditedNote();
                    final String curNoteName = curNote.getNoteName();
                    final String curGroupName = curNote.getGroupName();

                    if (!curNoteName.equals(newNoteName) || !curGroupName.equals(newGroupName)) {
                        // a bit of save transactions: first create new then delete old...
                        if (!createNote(newGroupName, newNoteName)) {
                            doSave = false;
                        } else {
                            if (!deleteNote(curNote)) {
                                doSave = false;
                                // clean up: delete new empty note - ignore return values
                                OwnNoteFileManager.getInstance().deleteNote(newGroupName, newNoteName);
                            }
                        }
                    }
                }

                if (doSave) {
                    final Note newNote = new Note(newGroupName, newNoteName);
                    newNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                    if (saveNote(newNote)) {
                    }
                }
            });

        } else {

            myGroupList = groupsPane;
            
            // groupTabs look and feel
            // 1. no groups table, no button list
            groupsTable.setDisable(true);
            groupsTable.setVisible(false);

            buttonBox.setDisable(true);
            buttonBox.setVisible(false);
            
            // TFE, 20201204: no groupsPane left for tagtree layout
            if (OwnNoteEditorParameters.LookAndFeel.groupTabs.equals(currentLookAndFeel)) {
                groupsPane.setDisable(false);
                groupsPane.setVisible(true);
            } else {
                groupsPane.setDisable(true);
                groupsPane.setVisible(false);
            }
            
            // 2. note table is shown left
            middlePaneXML.getChildren().remove(notesTableFXML);
            leftPaneXML.getChildren().setAll(notesTableFXML);
            
            // 3. and can't be deleted with trashcan
            noteDeleteColFXML.setVisible(false);
            noteDeleteColFXML.setMinWidth(0d);
            noteDeleteColFXML.setMaxWidth(0d);
            notesTableFXML.getColumns().remove(noteDeleteColFXML);

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
            gridPane.getScene().getRoot().setStyle("note-selected-background-color: white");
            gridPane.getScene().getRoot().setStyle("note-selected-font-color: black");
            
            // renaming note
            noteNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final Note curNote = ObjectsHelper.uncheckedCast(t.getTableView().getItems().get(t.getTablePosition().getRow()));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    if (!renameNote(curNote, t.getNewValue())) {
                        // TF, 20160815: restore old name in case of error

                        // https://stackoverflow.com/questions/20798634/restore-oldvalue-in-tableview-after-editing-the-cell-javafx
                        t.getTableView().getColumns().get(0).setVisible(false);
                        t.getTableView().getColumns().get(0).setVisible(true);
                    }
                }
            });
            
        }
        
        // TFE, 20201204: new column to the left for tagtree layout
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            // show TagsTreeView (special version without checkboxes & drag/drop of tags)
            tagsTreeView = new TagsTreeView(this);
            tagsTreeView.setRenameFunction(TagManager.getInstance()::doRenameTag);
            
            tagsTreePaneXML.getChildren().add(tagsTreeView);
            
            myGroupList = tagsTreeView;
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
        TagEditor.getInstance().setCallback(this);
        TagManager.getInstance().setCallback(this);
        
        // run layout to have everything set up
        splitPaneXML.applyCss();
        splitPaneXML.requestLayout();
        
        // For each divider register a mouse pressed and a released listener
        for (Node node: splitPaneXML.lookupAll(".split-pane-divider")) {
            node.setOnMousePressed(evMousePressed -> mouseDragOnDivider = true);
            node.setOnMouseReleased(evMouseReleased -> mouseDragOnDivider = false );
        }
        
        // now sync splitpane dividers with grid column width
        // TFE, 20201204: gets more tricky with for columns :-)
        // #1 tagtree is easy - use own percentage
        splitPaneXML.setDividerPosition(TAGTREE_NOTE_GROUP_DIVIDER, 
                gridPane.getColumnConstraints().get(TAGTREE_COLUMN).getPercentWidth()/100d);
        // #2 note/group is easy - use percentage of tagtree + own percentage
        splitPaneXML.setDividerPosition(NOTE_GROUP_EDITOR_DIVIDER, 
                (gridPane.getColumnConstraints().get(TAGTREE_COLUMN).getPercentWidth() + gridPane.getColumnConstraints().get(NOTE_GROUP_COLUMN).getPercentWidth())/100d);
        // #4 tasklist is easy - use 100 - own percentage
        splitPaneXML.setDividerPosition(EDITOR_TASKLIST_DIVIDER, 
                (100d - gridPane.getColumnConstraints().get(TASKLIST_COLUMN).getPercentWidth())/100d);
        
//        System.out.println("TAGTREE_COLUMN: " + gridPane.getColumnConstraints().get(TAGTREE_COLUMN).getPercentWidth());
//        System.out.println("NOTE_GROUP_COLUMN: " + gridPane.getColumnConstraints().get(NOTE_GROUP_COLUMN).getPercentWidth());
//        System.out.println("EDITOR_COLUMN: " + gridPane.getColumnConstraints().get(EDITOR_COLUMN).getPercentWidth());
//        System.out.println("TASKLIST_COLUMN: " + gridPane.getColumnConstraints().get(TASKLIST_COLUMN).getPercentWidth());
//        
//        System.out.println("TAGTREE_NOTE_GROUP_DIVIDER: " + splitPaneXML.getDividerPositions()[TAGTREE_NOTE_GROUP_DIVIDER]);
//        System.out.println("NOTE_GROUP_EDITOR_DIVIDER: " + splitPaneXML.getDividerPositions()[NOTE_GROUP_EDITOR_DIVIDER]);
//        System.out.println("EDITOR_TASKLIST_DIVIDER: " + splitPaneXML.getDividerPositions()[EDITOR_TASKLIST_DIVIDER]);

        // change width of gridpane when moving divider - but only after initial values have been set
        splitPaneXML.getDividers().get(TAGTREE_NOTE_GROUP_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // only do magic once the window is showing to avoid initial layout pass
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    final double newPercentage = newValue.doubleValue() * 100d;
                    // needs to take 4 column into account - if shown
                    gridPane.getColumnConstraints().get(TAGTREE_COLUMN).setPercentWidth(newPercentage);
                    setRemainingColumnWidth(EDITOR_COLUMN);

                    tagTreeWidth = newPercentage;

//                    System.out.println("Moved TagTree-Note/Group divider to " + newPercentage + "%");
                });
            }
        });

        splitPaneXML.getDividers().get(NOTE_GROUP_EDITOR_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // only do magic once the window is showing to avoid initial layout pass
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    final double newPercentage = newValue.doubleValue() * 100d;
                    // needs to take 2 columns into account
                    gridPane.getColumnConstraints().get(NOTE_GROUP_COLUMN).setPercentWidth(
                            newPercentage - gridPane.getColumnConstraints().get(TAGTREE_COLUMN).getPercentWidth());
                    setRemainingColumnWidth(EDITOR_COLUMN);

                    if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
                        classicGroupWidth = newPercentage;
                    } else {
                        groupTabsGroupWidth = newPercentage;
                    }

//                    System.out.println("Moved Note/Group-Editor divider to " + newPercentage + "%");
                });
            }
        });

        splitPaneXML.getDividers().get(EDITOR_TASKLIST_DIVIDER).positionProperty().addListener((observable, oldValue, newValue) -> {
            // TFE, 20201203: ignore divider in case tasklist not visible
            if (newValue != null && (Math.abs(newValue.doubleValue() - oldValue.doubleValue()) > 0.001)) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    final double newPercentage = newValue.doubleValue() * 100d;
                    gridPane.getColumnConstraints().get(TASKLIST_COLUMN).setPercentWidth(100d - newPercentage);
                    setRemainingColumnWidth(EDITOR_COLUMN);

                    if (tasklistVisible.get()) {
                        taskListWidth = newPercentage;
                    }

//                    System.out.println("Moved Editor-TaskList divider to " + newPercentage + "%");
                });
            }
        });

        // init menu handling
        // TFE, 20201203: do this last since it does changes to layout depending on tasklistVisible 
        initMenus();
    }
    
    private void setRemainingColumnWidth(final int column) {
        double result = 100d;
        
        for (int i = 0; i < gridPane.getColumnConstraints().size(); i++) {
            if (i != column) {
                result -= gridPane.getColumnConstraints().get(i).getPercentWidth();
            }
        }
        
        gridPane.getColumnConstraints().get(column).setPercentWidth(result);
    }
    
    private void initMenus() {
        // 1. select entry based on value of currentLookAndFeel
        switch (currentLookAndFeel) {
            case classic:
                classicLookAndFeel.setSelected(true);
                break;
            case groupTabs:
                groupTabsLookAndFeel.setSelected(true);
                break;
            case tagTree:
                tagTreeLookAndFeel.setSelected(true);
                break;
        }

        // 2. add listener to track changes of layout - only after setting it initially
        classicLookAndFeel.getToggleGroup().selectedToggleProperty().addListener(
            (ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2) -> {
                // when starting up things might not be initialized properly
                if (arg2 != null) {
                    assert (arg2 instanceof RadioMenuItem);

                    // store in the preferences - don't overwrite local variable!
                    final RadioMenuItem radioArg = ((RadioMenuItem) arg2);
                    if (radioArg.equals(classicLookAndFeel)) {
                        OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_LOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name());
                    } else if (radioArg.equals(groupTabsLookAndFeel)) {
                        OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_LOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.groupTabs.name());
                    } else if (radioArg.equals(tagTreeLookAndFeel)) {
                        OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_LOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.tagTree.name());
                    }
                }
            });
        
        // add changelistener to pathlabel - not that you should actually change its value during runtime...
        ownCloudPath.textProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                // store in the preferences
                OwnNoteEditorPreferences.getInstance().put(OwnNoteEditorPreferences.RECENT_OWNCLOUDPATH, newValue);

                // scan files in new directory
                initFromDirectory(false, true);
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

        // TFE, 20201025: and now we have tag management as well :-)
        menuEditTags.setOnAction((t) -> {
            TagEditor.getInstance().editTags(null);
        });
        menuGroups2Tags.setOnAction((t) -> {
            TagManager.getInstance().groupsToTags();
        });

        AboutMenu.getInstance().addAboutMenu(OwnNoteEditor.class, borderPane.getScene().getWindow(), menuBar, "OwnNoteEditor", "v5.0", "https://github.com/ThomasDaheim/ownNoteEditor");
    }
    
    // do everything to show / hide tasklist
    private void setTasklistVisible(final boolean visible) {
//        System.out.println("Start switching taskList to " + visible);
        if (taskFilterBox.isVisible() == visible) {
            return;
        }

        // 1) show / hide taskFilterBox
        taskFilterBox.setVisible(visible);
        taskFilterBox.setDisable(!visible);
        taskFilterBox.setManaged(visible);

        // 2) show / hide taskListFXML container
        rightPaneXML.setVisible(visible);
        rightPaneXML.setDisable(!visible);
        rightPaneXML.setManaged(visible);

        if (visible) {
            // 2) show / hide taskListFXML container
            rightPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.1));
            rightPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.3));

            // 3) show / hide grid column
            gridPane.getColumnConstraints().get(TASKLIST_COLUMN).setPercentWidth(taskListWidth);
        } else {
            // 2) show / hide taskListFXML container
            rightPaneXML.minWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.0));
            rightPaneXML.maxWidthProperty().bind(splitPaneXML.widthProperty().multiply(0.0));

            // 3) show / hide grid column
            gridPane.getColumnConstraints().get(TASKLIST_COLUMN).setPercentWidth(0d);
        }

        // 4) update calculation of percentages for resize
        splitPaneXML.setDividerPosition(EDITOR_TASKLIST_DIVIDER, (100d - gridPane.getColumnConstraints().get(TASKLIST_COLUMN).getPercentWidth())/100d);

//        System.out.println("Done switching taskList");
    }
    
    public void initFromDirectory(final boolean updateOnly, final boolean resetTasksTags) {
        checkChangedNote();

        if (resetTasksTags) {
            // TFE, 20201115: throw away any current tasklist - we might have changed the path!
            TaskManager.getInstance().resetTaskList();
            TagManager.getInstance().resetTagList();
        }
        
        // scan directory and re-populate lists
        OwnNoteFileManager.getInstance().initNotesPath(ownCloudPath.textProperty().getValue());

        taskList.populateTaskList();
        // TFE, 20201206: re-populate tags treeview as well - if shown
        if (OwnNoteEditorParameters.LookAndFeel.tagTree.equals(currentLookAndFeel)) {
            tagsTreeView.fillTreeView(TagsTreeView.WorkMode.LIST_MODE, null);
        }
        
        // add new table entries & disable & enable accordingly
        notesList = OwnNoteFileManager.getInstance().getNotesList();
        // http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        
        // Issue #59: advanced filtering & sorting
        // do the stuff in the OwnNoteTableView - thats the right place!
        notesTable.setNotes(notesList);
        
        myGroupList.setGroups(OwnNoteFileManager.getInstance().getGroupsList(), updateOnly);
        
        // and now store group names (real ones!) for later use
        initGroupNames();
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
                if (saveChanges.get().equals(buttonSave)) {
                    // save note
                    final Note prevNote = noteHTMLEditor.getEditedNote();
                    prevNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                    if (saveNote(prevNote)) {
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
    
    public boolean editNote(final Note curNote) {
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
        if (curNote.getNoteFileContent() == null) {
            curNote.setNoteFileContent(OwnNoteFileManager.getInstance().readNote(curNote));
        }
        noteHTMLEditor.editNote(curNote, curNote.getNoteFileContent());
        noteMetaEditor.editNote(curNote);
        
        return result;
    }
    
    public Note getEditedNote() {
        return noteHTMLEditor.getEditedNote();
    }
    
    public void selectNoteAndCheckBox(final Note note, final int textPos, final String htmlText) {
        // need to distinguish between views to select group
        myGroupList.selectGroupForNote(note);
        
        // and now select the note - leads to callback to editNote to fill the htmleditor
        notesTable.selectNote(note);
        
        noteHTMLEditor.scrollToCheckBox(textPos, htmlText);
    }
    
    public void selectNoteAndToggleCheckBox(final Note note, final int textPos, final String htmlText, final boolean newStatus) {
        // make sure the note is shown and the cursor is in place
        selectNoteAndCheckBox(note, textPos, htmlText);
        
        // now change the status
        noteHTMLEditor.toggleCheckBox(textPos, htmlText, newStatus);
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
        noteHTMLEditor.setDisable(true);
        noteHTMLEditor.setVisible(false);
        
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            notesTable.setDisable(false);
            notesTable.setVisible(true);
        }
    }

    private void showNoteEditor() {
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            notesTable.setDisable(true);
            notesTable.setVisible(false);
        }

        noteHTMLEditor.setDisable(false);
        noteHTMLEditor.setVisible(true);

        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
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
        groupNameBox.getItems().add(NoteGroup.NOT_GROUPED);
        groupNameBox.getItems().add(NoteGroup.NEW_GROUP);
        groupNameBox.getItems().addAll(realGroupNames);
    }
    
    public OwnNoteHTMLEditor getNoteEditor() {
        return noteHTMLEditor;
    }

    public boolean createGroupWrapper(final String newGroupName) {
        Boolean result = OwnNoteFileManager.getInstance().createGroup(newGroupName);

        if (!result) {
            // error message - most likely group with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "New group couldn't be created.", "Group with same name already exists.");
        }
        
        return result;
    }

    public boolean renameGroupWrapper(final String oldValue, final String newValue) {
        boolean result = false;

        // no rename for "All" and "Not Grouped"
        if (!NoteGroup.isSpecialGroup(newValue)) {
            result = OwnNoteFileManager.getInstance().renameGroup(oldValue, newValue);
            initGroupNames();

            if (!result) {
                // error message - most likely note in new group with same name already exists
                showAlert(AlertType.ERROR, "Error Dialog", "An error occured while renaming the group.", "A file in the new group has the same name as a file in the old.");
            } else {
                //check if we just moved the current note in the editor...
                if (noteHTMLEditor.getEditedNote() != null) {
                    noteHTMLEditor.doNameChange(oldValue, newValue, noteHTMLEditor.getEditedNote().getNoteName(), noteHTMLEditor.getEditedNote().getNoteName());
                }
            }
        }
        
        return result;
    }

    public Boolean deleteGroupWrapper(final NoteGroup curGroup) {
        boolean result = false;
                
        final String groupName = curGroup.getGroupName();
        // no delete for "All" and "Not Grouped"
        if (!NoteGroup.isSpecialGroup(groupName)) {
            result = OwnNoteFileManager.getInstance().deleteGroup(groupName);
            initGroupNames();

            if (!result) {
                // error message - most likely note in "Not grouped" with same name already exists
                showAlert(AlertType.ERROR, "Error Dialog", "An error occured while deleting the group.", "An ungrouped file has the same name as a file in this group.");
            }
        }
        
        return result;
    }

    private void initGroupNames() {
        realGroupNames.clear();

        final ObservableList<NoteGroup> groupsList = OwnNoteFileManager.getInstance().getGroupsList();
        for (NoteGroup group: groupsList) {
            final String groupName = group.getGroupName();
            if (!NoteGroup.isSpecialGroup(groupName)) {
                realGroupNames.add(groupName);
            }
        }
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
        }
        
        return result;
    }

    @Override
    public boolean createNote(final String newGroupName, final String newNoteName) {
        boolean result = OwnNoteFileManager.getInstance().createNote(newGroupName, newNoteName);

        if (!result) {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "New note couldn't be created.", "Note with same group and name already exists.");
        } else {
            // update group tags as well
            TagManager.getInstance().createNote(newGroupName, newNoteName);
        }
        
        return result;
    }

    @Override
    public boolean renameNote(final Note curNote, final String newValue) {
        boolean result = OwnNoteFileManager.getInstance().renameNote(curNote, newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while renaming the note.", "A note with the same name already exists.");
        } else {
            //check if we just moved the current note in the editor...
            noteHTMLEditor.doNameChange(curNote.getGroupName(), curNote.getGroupName(), curNote.getNoteName(), newValue);

            // update group tags as well
            TagManager.getInstance().renameNote(curNote, newValue);
        }
        
        return result;
    }

    @Override
    public boolean moveNote(final Note curNote, final String newGroupName) {
        final String oldGroupName = curNote.getGroupName();
        final Note origNote = new Note(curNote);
        
        boolean result = OwnNoteFileManager.getInstance().moveNote(curNote, newGroupName);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while moving the note.", "A note with the same name already exists in the new group.");
        } else {
            //check if we just moved the current note in the editor...
            noteHTMLEditor.doNameChange(curNote.getGroupName(), newGroupName, curNote.getNoteName(), curNote.getNoteName());

            // update group tags as well
            TagManager.getInstance().moveNote(origNote, newGroupName);
        }
        
        return result;
    }

    public boolean saveNote(final Note note) {
        boolean result = OwnNoteFileManager.getInstance().saveNote(note);
                
        if (result) {
            if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
                hideAndDisableAllEditControls();
                hideNoteEditor();
                initFromDirectory(false, false);
            } else {
                // TF, 20170723: refresh notes list since modified has changed
                notesTableFXML.refresh();
            }
            
            // update all editors
            noteHTMLEditor.hasBeenSaved();
            noteMetaEditor.hasBeenSaved();

            // update group tags as well
            TagManager.getInstance().saveNote(note);
        } else {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "Note couldn't be saved.", null);
        }
        
        return result;
    }

    public void setGroupNameFilter(final String groupName) {
        notesTable.setGroupNameFilter(groupName);
    }
    
    public void setNotesFilter(final Set<Note> notes) {
        notesTable.setNotesFilter(notes);
    }

    public void setNotesTableBackgroundColor(final String color) {
        notesTable.setBackgroundColor(color);
    }
    
    public void selectFirstOrCurrentNote() {
        // select first or current note - if any
        if (!notesTable.getItems().isEmpty()) {
            // check if current edit is ongoing AND note in the select tab (can happen with drag & drop!)
            curNote = noteHTMLEditor.getEditedNote();

            // TFE, 20201030: support re-load of last edited note
            if (curNote == null) {
                final String lastGroupName = OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.LAST_EDITED_GROUP, "");
                final String lastNoteName = OwnNoteEditorPreferences.getInstance().get(OwnNoteEditorPreferences.LAST_EDITED_NOTE, "");
                if (OwnNoteFileManager.getInstance().noteExists(lastGroupName, lastNoteName)) {
                    curNote = OwnNoteFileManager.getInstance().getNote(lastGroupName, lastNoteName);
                    
                    if (firstNoteAccess) {
                        // done, selectGroupForNote calls selectFirstOrCurrentNote() internally - BUT NO LOOPS PLEASE
                        firstNoteAccess = false;
                        Platform.runLater(() -> {
                            if (!OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
                                myGroupList.selectGroupForNote(curNote);
                            }
                        });
                        return;
                    }
                }
            }
            
            int selectIndex = 0;
            if (curNote != null && notesTable.getItems().contains(curNote)) {
                selectIndex = notesTable.getItems().indexOf(curNote);
            }

            // TFE, 20201030: this also starts editing of the note
            notesTable.selectAndFocusRow(selectIndex);
        } else {
            // TFE, 20201012: check for changes also when no new note (e.g. selecting an empty group...
            if (!checkChangedNote()) {
                return;
            }

            noteHTMLEditor.setDisable(true);
            noteHTMLEditor.editNote(null, "");
            noteMetaEditor.editNote(null);
        }
    }
    
    public String uniqueNewNoteNameForGroup(final String groupName) {
        String result;
        int newCount = notesList.size() + 1;
        
        do {
            result = OwnNoteEditor.NEW_NOTENAME + " " + newCount;
            newCount++;
        } while(OwnNoteFileManager.getInstance().noteExists(groupName, result));
        
        return result;
    }

    @Override
    public boolean processFileChange(final WatchEvent.Kind<?> eventKind, final Path filePath) {
        // System.out.printf("Time %s: Gotcha!\n", getCurrentTimeStamp());
        boolean result = true;
        
        final String fileName = filePath.getFileName().toString();
        if (!filesInProgress.contains(fileName)) {
        
            // System.out.printf("Time %s: You're new here!\n", getCurrentTimeStamp());
            filesInProgress.add(fileName);
            
            // re-init list of groups and notes - file has beeen added or removed
            Platform.runLater(() -> {
                if (!StandardWatchEventKinds.ENTRY_CREATE.equals(eventKind)) {
                    // delete & modify is only relevant if we're editing this note...
                    if (noteHTMLEditor.getEditedNote() != null) {
                        final Note curNote = noteHTMLEditor.getEditedNote();
                        final String curName = OwnNoteFileManager.getInstance().buildNoteName(curNote.getGroupName(), curNote.getNoteName());

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
                                    // save own note under new name
                                    final Note saveNote = noteHTMLEditor.getEditedNote();
                                    final String newNoteName = uniqueNewNoteNameForGroup(saveNote.getGroupName());
                                    if (createNote(saveNote.getGroupName(), newNoteName)) {
                                        final Note newNote = new Note(saveNote.getGroupName(), newNoteName);
                                        newNote.setNoteEditorContent(noteHTMLEditor.getNoteText());
                                        if (saveNote(newNote)) {
                                            // we effectively just renamed the note...
                                            final String oldNoteName = saveNote.getNoteName();
                                            saveNote.setNoteName(newNoteName);
                                            noteHTMLEditor.doNameChange(saveNote.getGroupName(), saveNote.getGroupName(), oldNoteName, newNoteName);
                                            // System.out.printf("User data updated\n");
                                        }
                                    }
                                }

                                if (saveChanges.get().equals(buttonDiscard)) {
                                    // nothing to do for StandardWatchEventKinds.ENTRY_DELETE - initFromDirectory(true) will take care of this
                                    if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                        // re-load into edit for StandardWatchEventKinds.ENTRY_MODIFY
                                        final Note loadNote = noteHTMLEditor.getEditedNote();
                                        loadNote.setNoteFileContent(OwnNoteFileManager.getInstance().readNote(loadNote));
                                        noteHTMLEditor.editNote(loadNote, loadNote.getNoteFileContent());
                                        noteMetaEditor.editNote(loadNote);
                                    }
                                }
                            }
                        }
                    }
                }
            
                // show only notes for selected group
                final String curGroupName = myGroupList.getCurrentGroup().getGroupName();

                initFromDirectory(true, true);
                selectFirstOrCurrentNote();
                
                // but only if group still exists in the list!
                final List<String> allGroupNames = new LinkedList<>(realGroupNames);
                allGroupNames.add(NoteGroup.ALL_GROUPS);
                allGroupNames.add(NoteGroup.NOT_GROUPED);
                
                if (allGroupNames.contains(curGroupName)) {
                    setGroupNameFilter(curGroupName);
                }
                
                filesInProgress.remove(fileName);
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

    // TF, 20160703: to support coloring of notes table view for individual notes
    // TF, 20170528: determine color from groupname for new colors
    public static String getGroupColor(String groupName) {
        final FilteredList<NoteGroup> filteredGroups = OwnNoteFileManager.getInstance().getGroupsList().filtered((NoteGroup group) -> {
            // Compare group name to filter text.
            return group.getGroupName().equals(groupName); 
        });
        
        String groupColor = "darkgrey";
        if (!filteredGroups.isEmpty()) {
            final NoteGroup group = (NoteGroup) filteredGroups.get(0);
            groupColor = group.getGroupColor();

            if (groupColor == null) {
                final int groupIndex = OwnNoteFileManager.getInstance().getGroupsList().indexOf(group);

                // TF, 20170122: "All" & "Not grouped" have their own colors ("darkgrey", "lightgrey"), rest uses list of colors
                switch (groupIndex) {
                    case 0: groupColor = "darkgrey";
                            break;
                    case 1: groupColor = "lightgrey";
                            break;
                    default: groupColor = groupColors[groupIndex % groupColors.length];
                            break;
                }
//                System.out.println("Found group: " + groupName + " as number: " + groupIndex + " color: " + groupColor);
            }
        }
        return groupColor;
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
}
