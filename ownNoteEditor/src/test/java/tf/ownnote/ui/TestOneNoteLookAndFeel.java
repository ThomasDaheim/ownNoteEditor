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
package tf.ownnote.ui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import tf.ownnote.ui.helper.GroupData;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import tf.ownnote.ui.helper.OwnNoteTab;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.main.OwnNoteEditorManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class TestOneNoteLookAndFeel extends ApplicationTest {
    private Stage myStage;
    
    @Override
    public void start(Stage stage) throws Exception {
        myStage = stage;
        
        OwnNoteEditorManager app = new OwnNoteEditorManager();
        // TODO: set command line parameters to avoid tweaking stored values
        app.start(stage);

        /* Do not forget to put the GUI in front of windows. Otherwise, the robots may interact with another
        window, the one in front of all the windows... */
        stage.toFront();
        
        // TF, 20170205: under gradle in netbeans toFront() still leves the window in the background...
        stage.requestFocus();
    }

    private final Testdata myTestdata = new Testdata();
  
    private String currentPath;
    private Path testpath;
    
    private OwnNoteEditorParameters.LookAndFeel currentLookAndFeel;

    @Override
    public void init() throws Exception {
        // System.out.println("running init()");
        super.init();

        // get current look & feel and notes path
        try {
            currentLookAndFeel = OwnNoteEditorParameters.LookAndFeel.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name()));

            currentPath = OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, "");
            // System.out.println("currentPath: " + currentPath);
            // System.out.println("Using preference for ownCloudDir: " + currentPath);
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }        

        // copy test files to directory
        testpath = Files.createTempDirectory("TestNotes");
        // TFE, 20180930: set read/write/ exec for all to avoid exceptions in monitoring thread
        testpath.toFile().setReadable(true, false);
        testpath.toFile().setWritable(true, false);
        testpath.toFile().setExecutable(true, false);
        try {
            myTestdata.copyTestFiles(testpath);
        } catch (Throwable ex) {
            Logger.getLogger(TestOneNoteLookAndFeel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // set look & feel and notes path name
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.oneNote.name());
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, testpath.toString());
        //System.out.println("testpath: " + testpath.toString());
    }

    private Label ownCloudPath;
    private TableView<Map<String, String>> notesTableFXML;
    private TabPane groupsPaneFXML;
    private OwnNoteTab allTab;
    private OwnNoteTab test1Tab;
    private OwnNoteTab test2Tab;
    private OwnNoteTab test3Tab;
    private OwnNoteTab testPLUSTab;
    private TextField noteFilterText;
    private CheckBox noteFilterCheck;

    /* Just a shortcut to retrieve widgets in the GUI. */
    public <T extends Node> T find(final String query) {
        /** TestFX provides many operations to retrieve elements from the loaded GUI. */
        return lookup(query).query();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void getNodes() {
        //System.out.println("running getNodes()");

        notesTableFXML = (TableView<Map<String, String>>) find(".notesTable");
        groupsPaneFXML = (TabPane) find(".groupsPane");
        
        // tabs are not nodes!!! So we have to find them the hard way
        final ObservableList<Tab> tabsList = groupsPaneFXML.getTabs();
        allTab = (OwnNoteTab) tabsList.stream().filter(x -> {
                                                        return ((Label) x.getGraphic()).getText().startsWith(GroupData.ALL_GROUPS);
                                                    }).findFirst().orElse(null);
        test1Tab = (OwnNoteTab) tabsList.stream().filter(x -> {
                                                        return ((Label) x.getGraphic()).getText().startsWith("Test1");
                                                    }).findFirst().orElse(null);
        test2Tab = (OwnNoteTab) tabsList.stream().filter(x -> {
                                                        return ((Label) x.getGraphic()).getText().startsWith("Test2");
                                                    }).findFirst().orElse(null);
        test3Tab = (OwnNoteTab) tabsList.stream().filter(x -> {
                                                        return ((Label) x.getGraphic()).getText().startsWith("Test3");
                                                    }).findFirst().orElse(null);
        testPLUSTab = (OwnNoteTab) tabsList.stream().filter(x -> {
                                                        return "+".equals(((Label) x.getGraphic()).getText());
                                                    }).findFirst().orElse(null);
        
        noteFilterText = (TextField) find(".noteFilterText");
        noteFilterCheck = (CheckBox) find(".noteFilterCheck");
    }
    
    /* IMO, it is quite recommended to clear the ongoing events, in case of. */
    @After
    public void tearDown() throws TimeoutException, IOException {
        //System.out.println("running tearDown()");

        /* Close the window. It will be re-opened at the next test. */
        release(new KeyCode[] {});
        release(new MouseButton[] {});

        // delete temp directory + files
        FileUtils.deleteDirectory(testpath.toFile());
        
        // set look & feel to old value
        if (currentLookAndFeel != null) {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, currentLookAndFeel.name());
        }
        
        // set path name to old value
        if (currentPath != null) {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, currentPath);
        }
    }
    
    private void testAlert(final String headerText, final ButtonBar.ButtonData buttonToPress) {
        final Label label = (Label) find(headerText);
        final DialogPane dialogPane = (DialogPane) (((GridPane)label.getParent()).getParent());
        assertNotNull("Dialogpane", dialogPane);
        
        // now find the button and press it
        // https://stackoverflow.com/questions/30755370/javafx-close-alert-box-or-any-dialog-box-programatically
        interact(() -> {
            Button button = null;
            for ( ButtonType bt : dialogPane.getButtonTypes() )
            {
                if ( buttonToPress.equals(bt.getButtonData()) )
                {
                    button = (Button) dialogPane.lookupButton( bt );
                    button.fire();
                    break;
                }
            }

            // we should have found the button...
            assertNotNull(button);
        });
        
        interact(() -> {
            myStage.toFront();
        });
    }
    
    private void selectTab(final int tabIndex) {
        interact(() -> {
            // only do select if tab has changed
            if (groupsPaneFXML.getSelectionModel().getSelectedIndex() != tabIndex) {
                groupsPaneFXML.getSelectionModel().select(tabIndex);
            }
        });
    }
    
    private void testTab(final int tabIndex, final String tabName, final int tabCount) {
        selectTab(tabIndex);

        assertTrue("Check tab name", ((Label) groupsPaneFXML.getSelectionModel().getSelectedItem().getGraphic()).getText().startsWith(tabName));
        assertTrue("Check notes count", (notesTableFXML.getItems().size() == tabCount));
    }

    @Test
    public void runTests() {
        testNodes();
        testInitialSetup();
        resetForNextTest();
        
        testAddDeleteNote();
        resetForNextTest();

        testRenameNote();
        resetForNextTest();

//        testDragNote();
//        resetForNextTest();

        testGroups();
        resetForNextTest();
        
        testNotesFilter();
        resetForNextTest();
        
        // TFE, 20180930: must be last test and no resetForNextTest() afterwards - to avoid "File has changed" dialogue
//        testFileSystemChange();
    }
    
    private void testNodes() {
        //System.out.println("running testNodes()");

        assertNotNull(notesTableFXML);
        assertNotNull(groupsPaneFXML);
        assertNotNull(allTab);
        assertNotNull(test1Tab);
        assertNotNull(test2Tab);
        assertNotNull(test3Tab);
        assertNotNull(testPLUSTab);
        assertNotNull(noteFilterText);
        assertNotNull(noteFilterCheck);
    }
    
    @SuppressWarnings("unchecked")
    private void testInitialSetup() {
        // #1 ------------------------------------------------------------------
        // check "ALL" tab, that should have 4 entries
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));

        // #2 ------------------------------------------------------------------
        // check "NOT_GROUPED" tab, that should be empty
        testTab(1, GroupData.NOT_GROUPED, myTestdata.getNotesCountForGroup(GroupData.NOT_GROUPED));

        // #3 ------------------------------------------------------------------
        // check "Test 1" tab, that should have 2 entries
        testTab(2, "Test1", myTestdata.getNotesCountForGroup("Test1"));

        // #4 ------------------------------------------------------------------
        // check "Test 2" tab, that should have 1 entry
        testTab(3, "Test2", myTestdata.getNotesCountForGroup("Test2"));

        // #5 ------------------------------------------------------------------
        // check "Test 3" tab, that should have 1 entry
        testTab(4, "Test3", myTestdata.getNotesCountForGroup("Test3"));
    }

    
    @SuppressWarnings("unchecked")
    private void testAddDeleteNote() {
        // #1 ------------------------------------------------------------------
        // add new note with right click + menu item
        rightClickOn(notesTableFXML);
        push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);
        // TFE, 20181003: java 9: somwhow one more "ENTER" is needed
        push(KeyCode.ENTER);

        final int newCount = myTestdata.getNotesList().size() + 1;
        final String newName = "New Note " + newCount;
        assertTrue("Check new notes count", (notesTableFXML.getItems().size() == newCount));
        assertTrue("Check new note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        final NoteData newNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        assertTrue("Check new note label", newNote.getNoteName().startsWith(newName));
        
        // #2 ------------------------------------------------------------------
        // delete note with right click + menu item
        clickOn(notesTableFXML);
        // TODO: get better coordinates to move to
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        rightClickOn();
        push(KeyCode.DOWN);
        push(KeyCode.DOWN);
        // TFE, 20181003: java 9: right click selects the first menu item... so one "DOWN" less here
        //push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        assertTrue("Check new notes count", (notesTableFXML.getItems().size() == myTestdata.getNotesList().size()));
    }
    
    @SuppressWarnings("unchecked")
    private void testRenameNote() {
        // #1 ------------------------------------------------------------------
        // rename note via right click + menu item
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        rightClickOn();
        push(KeyCode.DOWN);
        push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        write("rename1");
        push(KeyCode.ENTER);

        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        NoteData renamedNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("rename1"));

        // #2 ------------------------------------------------------------------
        // rename note via right click + CTRL+R
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        rightClickOn();
        push(KeyCode.CONTROL, KeyCode.R);
        write("rename2");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        renamedNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("rename2"));

        // #3 ------------------------------------------------------------------
        // rename note via double click
        selectTab(0);
        doubleClickOn();
        write("test1");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        renamedNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("test1"));

        // #4 ------------------------------------------------------------------
        // Alert when renaming to existing name
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        rightClickOn();
        push(KeyCode.CONTROL, KeyCode.R);
        write("test2");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);
        
        // dialog "Note with same name exists"
        testAlert("An error occured while renaming the note.", ButtonBar.ButtonData.OK_DONE);
        
        // note should still have old name
        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        renamedNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("test1"));
    }
    
    @SuppressWarnings("unchecked")
    private void testDragNote() {
        // #1 ------------------------------------------------------------------
        // get x, y coordinates from tab 2
        Label testLabel = test2Tab.getLabel();
        Bounds testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        assertNotNull(testBounds);
        double centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        double centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;
        
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        Point p = MouseInfo.getPointerInfo().getLocation();
        Point2D p2d = new Point2D(p.getX(), p.getY());

        FxRobot dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.drag(centerX, centerY);
        dragNote.drop();
        
        // check "Test 1" tab, that should have 1 less entries
        testTab(2, "Test1", myTestdata.getNotesCountForGroup("Test1") - 1);

        // check "Test 2" tab, that should have 1 more entry
        testTab(3, "Test2", myTestdata.getNotesCountForGroup("Test2") + 1);
        
        // #2 ------------------------------------------------------------------
        // drag note back
        testLabel = test1Tab.getLabel();
        testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;
        
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        p = MouseInfo.getPointerInfo().getLocation();
        p2d = new Point2D(p.getX(), p.getY());

        dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.drag(centerX, centerY);
        dragNote.drop();
        
        // check "Test 1" tab, that should have 2 entries
        testTab(2, "Test1", myTestdata.getNotesCountForGroup("Test1"));

        // check "Test 2" tab, that should have 1 entry
        testTab(3, "Test2", myTestdata.getNotesCountForGroup("Test2"));
        
        // #3 ------------------------------------------------------------------
        // Alert when dragging to group with same note name
        testLabel = test3Tab.getLabel();
        testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;

        // go back to the ALL tab
        selectTab(0);

        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        p = MouseInfo.getPointerInfo().getLocation();
        p2d = new Point2D(p.getX(), p.getY());

        dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.drag(centerX, centerY);
        dragNote.drop();
 
        // dialog "Note with same name exists"
        testAlert("An error occured while moving the note.", ButtonBar.ButtonData.OK_DONE);

        // check "Test 1" tab, that should have 2 entries
        testTab(2, "Test1", myTestdata.getNotesCountForGroup("Test1"));

        // check "Test 3" tab, that should have 1 entry
        testTab(4, "Test3", myTestdata.getNotesCountForGroup("Test3"));
   }
    
    @SuppressWarnings("unchecked")
    private void testGroups() {
        // #1 ------------------------------------------------------------------
        // add group
        // get x, y coordinates from PLUS tab
        Label testLabel = testPLUSTab.getLabel();
        Bounds testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        double centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        double centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;
        
        moveTo(centerX, centerY);
        clickOn();
        
        // All, Not Grouped, Test1, Test2, Test3, New Group 3, + => 7 tabs
        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 2);
        
        // #2 ------------------------------------------------------------------
        // rename group
        doubleClickOn();
        write("Test4");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);
        
        assertTrue("Check Test3 tab", ((Label) groupsPaneFXML.getSelectionModel().getSelectedItem().getGraphic()).getText().startsWith("Test4"));
        
        // #3 ------------------------------------------------------------------
        // delete group
        rightClickOn();
        push(KeyCode.DOWN);
        push(KeyCode.DOWN);
        // TFE, 20181003: java 9: right click selects the first menu item... so one "DOWN" less here
        //push(KeyCode.DOWN);
        push(KeyCode.ENTER);

        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 1);
    }

    
    @SuppressWarnings("unchecked")
    private void testNotesFilter() {
        // leerer filter -> alle sichtbar
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        
        //////////////////////////
        // namensfilter
        //////////////////////////

        // "Test1" als namensfilter -> 2 sichtbar
        clickOn(noteFilterText);
        write("Test1");
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForName("Test1"));
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        
        // "SUCH" als namensfilter -> 0 sichtbar
        clickOn(noteFilterText);
        write("SUCH");
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForName("SUCH"));
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        
        //////////////////////////
        // inhaltsfilter
        //////////////////////////

        clickOn(noteFilterCheck);
        
        // "Test1" als inhaltsfilter -> 2 sichtbar
        clickOn(noteFilterText);
        write("Test1");
        testTab(0, GroupData.ALL_GROUPS, 2);
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        
        // "SUCH" als inhaltsfilter -> 1 sichtbar
        clickOn(noteFilterText);
        write("SUCH");
        testTab(0, GroupData.ALL_GROUPS, 1);
    }
    
    @SuppressWarnings("unchecked")
    private void testFileSystemChange() {
        long sleepTime = 1000;
        
        // TFE, 20190930: switch to correct tab initially to avoid later chanegs that might trigger hasChanged() calls
        selectTab(0);
        
        // #1 ------------------------------------------------------------------
        // add a new file to group Test1 and give UI some time to discover it
        assertTrue(myTestdata.createTestFile(testpath, "[Test1] test3.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: add a new file to group Test1");
        
        // check new count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS) + 1);
        
        // #2 ------------------------------------------------------------------
        // delete the new file
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test3.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete the new file");
        
        // check new count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        
        // #3 ------------------------------------------------------------------
        // add a new file to a new group
        assertTrue(myTestdata.createTestFile(testpath, "[Test4] test4.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: add a new file to a new group");
        
        // All, Not Grouped, Test1, Test2, Test3, Test4, + => 7 tabs
        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 2);

        // delete the new file
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test4] test4.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete the new file");

        // All, Not Grouped, Test1, Test2, Test3, + => 7 tabs
        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 1);

        // #4 ------------------------------------------------------------------
        // delete file in editor BUT "Save own"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor BUT \"Save own\"");

        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.OK_DONE);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify old count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));

        // #5 ------------------------------------------------------------------
        // delete file in editor BUT "Save as new"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor BUT \"Save as new\"");

        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.OTHER);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify old count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
        // but with new note name!
        final int newCount = myTestdata.getNotesList().size() + 1;
        final String newName = "New Note " + newCount;
        assertTrue("Check new note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof NoteData));
        final NoteData newNote = (NoteData) notesTableFXML.getSelectionModel().getSelectedItem();
        assertTrue("Check new note label", newName.equals(newNote.getNoteName()));
        
        // rename back again
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * 0.9);
        rightClickOn();
        push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        write("test1");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        // #6 ------------------------------------------------------------------
        // delete file in editor AND "Discard own"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor AND \"Discard own\"");
        
        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.CANCEL_CLOSE);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify new count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS) - 1);
        
        // create back again
        assertTrue(myTestdata.createTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: create back again");

        // verify old count
        testTab(0, GroupData.ALL_GROUPS, myTestdata.getNotesCountForGroup(GroupData.ALL_GROUPS));
    }
    
    private void resetForNextTest() {
        // tabs might have been changed
        getNodes();
        
        // go back to the ALL tab
        selectTab(0);
        // select first note
        interact(() -> {
            notesTableFXML.requestFocus();
            notesTableFXML.getSelectionModel().select(0);
            notesTableFXML.getFocusModel().focus(0);
        });
    }
}