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
package tf.ownnote.ui.main;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import tf.ownnote.ui.helper.CmdLineParameters;
import tf.ownnote.ui.helper.EditorPreferences;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.TestNoteData;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;
import tf.ownnote.ui.tags.TagTextFieldTreeCell;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class TestTagTreeLookAndFeel extends ApplicationTest {
    private static double SCALING = 0.85;
    
    private enum CheckMode {
        TAG_CHILDREN,
        TABLE_ELEMENTS,
        BOTH;
        
        public static boolean doCheckTagChildren(final CheckMode mode) {
            return (TAG_CHILDREN.equals(mode) || BOTH.equals(mode));
        }
        
        public static boolean doCheckTableElements(final CheckMode mode) {
            return (TABLE_ELEMENTS.equals(mode) || BOTH.equals(mode));
        }
    }
    
    private Stage myStage;
    private OwnNoteEditorManager myApp;
    
    @Override
    public void start(Stage stage) throws Exception {
        myStage = stage;
        
        myApp = new OwnNoteEditorManager();
        // TODO: set command line parameters to avoid tweaking stored values
        myApp.start(myStage);
        
        /* Do not forget to put the GUI in front of windows. Otherwise, the robots may interact with another
        window, the one in front of all the windows... */
        myStage.toFront();
        
        // TF, 20170205: under gradle in netbeans toFront() still leves the window in the background...
        myStage.requestFocus();

        myStage.setAlwaysOnTop(true);
        myStage.setAlwaysOnTop(false);
    }

    private final TestNoteData myTestdata = new TestNoteData();
  
    private String currentPath;
    private Path testpath;

    private String lastGroupName;
    private String lastNoteName;
    
    private CmdLineParameters.LookAndFeel currentLookAndFeel;

    @Override
    public void init() throws Exception {
        super.init();

        // get current look & feel and notes path
        try {
            currentLookAndFeel = EditorPreferences.RECENT_LOOKANDFEEL.getAsType();

            currentPath = EditorPreferences.RECENT_OWNCLOUDPATH.getAsType();
//            System.out.println("currentPath: " + currentPath);

            lastGroupName = EditorPreferences.LAST_EDITED_GROUP.getAsType();
            lastNoteName = EditorPreferences.LAST_EDITED_NOTE.getAsType();
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }        

        // copy test files to directory
        testpath = Files.createTempDirectory("TestTagTreeLookAndFeel");
        // TFE, 20180930: set read/write/ exec for all to avoid exceptions in monitoring thread
        testpath.toFile().setReadable(true, false);
        testpath.toFile().setWritable(true, false);
        testpath.toFile().setExecutable(true, false);
        try {
            myTestdata.copyTestFiles(testpath);
        } catch (Throwable ex) {
            Logger.getLogger(TestTagTreeLookAndFeel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // set look & feel and notes path name
        EditorPreferences.RECENT_LOOKANDFEEL.put(CmdLineParameters.LookAndFeel.tagTree);
        EditorPreferences.RECENT_OWNCLOUDPATH.put(testpath.toString());
        EditorPreferences.LAST_EDITED_GROUP.put("");
        EditorPreferences.LAST_EDITED_NOTE.put("");
        //System.out.println("testpath: " + testpath.toString());
    }

    private Label ownCloudPath;
    private TableView<Map<String, String>> notesTableFXML;
    private TreeView<TagData> tagsTreeView;
    private TagTextFieldTreeCell allTag;
    private TagTextFieldTreeCell test1Tag;
    private TagTextFieldTreeCell test2Tag;
    private TagTextFieldTreeCell test3Tag;
    private TextField noteFilterText;
    private CheckBox noteFilterCheck;

    /* Just a shortcut to retrieve widgets in the GUI. */
    public <T extends Node> T find(final String query) {
        /** TestFX provides many operations to retrieve elements from the loaded GUI. */
        return lookup(query).query();
    }

    private void getNodes() {
        System.out.println("running getNodes()");

        notesTableFXML = (TableView<Map<String, String>>) find(".notesTable");
        tagsTreeView = (TreeView<TagData>) find(".tagsTreeView");
        
        // TFE, 20220418: we don't use name as ID anymore... see tag_info.xml for values
        // TFE, 20240212: since we clone the imported tags we now need to find the id for tne names once again
        TagData testTag = TagManager.getInstance().groupForName("All", false);
        assertNotNull(this);
        allTag = (TagTextFieldTreeCell) find("#" + testTag.getId());
        testTag = TagManager.getInstance().groupForName("Test1", false);
        assertNotNull(this);
        test1Tag = (TagTextFieldTreeCell) find("#" + testTag.getId());
        testTag = TagManager.getInstance().groupForName("Test2", false);
        assertNotNull(this);
        test2Tag = (TagTextFieldTreeCell) find("#" + testTag.getId());
        testTag = TagManager.getInstance().groupForName("Test3", false);
        assertNotNull(this);
        test3Tag = (TagTextFieldTreeCell) find("#" + testTag.getId());

        noteFilterText = (TextField) find(".noteFilterText");
        noteFilterCheck = (CheckBox) find(".noteFilterCheck");
    }
    
    /* IMO, it is quite recommended to clear the ongoing events, in case of. */
    private void tearDown() {
        /* Close the window. It will be re-opened at the next test. */
        release(new KeyCode[] {});
        release(new MouseButton[] {});
        
        try {
            myApp.closeStage(false);
        } catch (Exception ex) {
            Logger.getLogger(TestTagTreeLookAndFeel.class.getName()).log(Level.SEVERE, null, ex);
        }

        // delete temp directory + files
        //FileUtils.deleteDirectory(testpath.toFile());
        
        // set look & feel to old value
        if (currentLookAndFeel != null) {
            EditorPreferences.RECENT_LOOKANDFEEL.put(currentLookAndFeel);
        }
        
        // set path name to old value
        if (currentPath != null) {
            EditorPreferences.RECENT_OWNCLOUDPATH.put(currentPath);
        }
        
        if (lastGroupName != null) {
            EditorPreferences.LAST_EDITED_GROUP.put(lastGroupName);
        }
        if (lastNoteName != null) {
            EditorPreferences.LAST_EDITED_NOTE.put(lastNoteName);
        }
        
        try {
            FileUtils.deleteDirectory(testpath.toFile());
        } catch (IOException ex) {
            Logger.getLogger(TestTagTreeLookAndFeel.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private void selectTag(final TagTextFieldTreeCell tag) {
        interact(() -> {
            clickOn(tag, MouseButton.PRIMARY);
        });
    }
    
    private void testTag(final String tagName, final int tabCount, final CheckMode mode) {
        if (CheckMode.doCheckTagChildren(mode)) {
            assertTrue("Check notes children for tag " + tagName, (TagManager.getInstance().groupForName(tagName, false).getLinkedNotes().size() == tabCount));
        }
        if (CheckMode.doCheckTableElements(mode)) {
            assertTrue("Check table elements for tag " + tagName, (notesTableFXML.getItems().size() == tabCount));
        }
    }
    
    private void waitOnJavaFxPlatformEventsDone() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(countDownLatch::countDown);
        try {
            countDownLatch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(TestTagTreeLookAndFeel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void runTests() {
        resetForNextTest();

//        testNodes();
//        testInitialSetup();
//        resetForNextTest();
//        
//        // TFE, 20210411: test rename first - otherwise some strang note selection issue
//        testRenameNote();
//        resetForNextTest();
//
//        testAddDeleteNote();
//        resetForNextTest();

        // TFE; 20201115: dragging with testfx doesn't work anymore for some time now :-(
        // and the fix is: https://github.com/TestFX/TestFX/issues/639#issuecomment-448609608
        testDragNote();
        resetForNextTest();

        // TFE; 20201115: "java.lang.NullPointerException: om.sun.javafx.scene.control.behavior.TableViewBehaviorBase.activate(TableViewBehaviorBase.java:898)"
        // after trying to rename the new group - doesn't occur in "real live"
        // and the fix is: https://stackoverflow.com/a/27396322
        testGroups();
        resetForNextTest();
        
        testNotesFilter();
        resetForNextTest();
        
        // TFE, 20180930: must be last test and no resetForNextTest() afterwards - to avoid "File has changed" dialogue
        testFileSystemChange();
        
        tearDown();
    }
    
    private void testNodes() {
        System.out.println("running testNodes()");

        assertNotNull(notesTableFXML);
        assertNotNull(tagsTreeView);
        assertNotNull(allTag);
        assertNotNull(test1Tag);
        assertNotNull(test2Tag);
        assertNotNull(test3Tag);
        assertNotNull(noteFilterText);
        assertNotNull(noteFilterCheck);
    }
    
    private void testInitialSetup() {
        System.out.println("running testInitialSetup()");

        // #1 ------------------------------------------------------------------
        // check "ALL" tag, that should have 4 entries
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.TAG_CHILDREN);

        // #2 ------------------------------------------------------------------
        // check "NOT_GROUPED" tag, that should be empty
        testTag(TagManager.ReservedTag.NotGrouped.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.NotGrouped.getTagName()), CheckMode.TAG_CHILDREN);

        // #2b ------------------------------------------------------------------
        // check "ARCHIVE" tag, that should be empty
        testTag(TagManager.ReservedTag.Archive.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.Archive.getTagName()), CheckMode.TAG_CHILDREN);

        // #3 ------------------------------------------------------------------
        // check "Test 1" tag, that should have 2 entries
        testTag("Test1", myTestdata.getNotesCountForGroup("Test1"), CheckMode.TAG_CHILDREN);

        // #4 ------------------------------------------------------------------
        // check "Test 2" tag, that should have 1 entry
        testTag("Test2", myTestdata.getNotesCountForGroup("Test2"), CheckMode.TAG_CHILDREN);

        // #5 ------------------------------------------------------------------
        // check "Test 3" tag, that should have 1 entry
        testTag("Test3", myTestdata.getNotesCountForGroup("Test3"), CheckMode.TAG_CHILDREN);
    }

    
    private void testAddDeleteNote() {
        System.out.println("running testAddDeleteNote()");

        // #1 ------------------------------------------------------------------
        // add new note with right click + menu item
        rightClickOn(notesTableFXML);
        push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);
        // TFE, 20181003: java 9: somehow one more "ENTER" is needed
        push(KeyCode.ENTER);

        // TFE, 20240212: something is fucked up with TableView.Edit() - no easy way to abort this and remove edit cell & fokus
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        clickOn();
        clickOn();
        push(KeyCode.ESCAPE);

        final int newCount = myTestdata.getNotesList().size() + 1;
        final String newName = "New Note " + newCount;
        assertTrue("Check new notes count", (notesTableFXML.getItems().size() == newCount));
        assertTrue("Check new note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
        final Note newNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
        assertTrue("Check new note label", newNote.getNoteName().startsWith(newName));
        
        // #2 ------------------------------------------------------------------
        // delete note with right click + menu item
        clickOn(notesTableFXML);
        // TODO: get better coordinates to move to
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        rightClickOn();
        push(KeyCode.DOWN);
        push(KeyCode.DOWN);
        // TFE, 20181003: java 9: right click selects the first menu item... so one "DOWN" less here
        //push(KeyCode.DOWN);
        push(KeyCode.ENTER);
        assertTrue("Check new notes count", (notesTableFXML.getItems().size() == myTestdata.getNotesList().size()));
    }
    
    private void testRenameNote() {
        System.out.println("running testRenameNote()");

        // #1 ------------------------------------------------------------------
        // rename note via right click + menu item
//        clickOn(notesTableFXML);
//        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
//        rightClickOn();
//        push(KeyCode.DOWN);
//        // TFE, 20181003: java 9: right click selects the first menu item... so one "DOWN" less here
//        //push(KeyCode.DOWN);
//        push(KeyCode.ENTER);
//        // TFE, 20220212: now we must also click again into the field?!
//        // TFE, 20191220: note names can be CaSe sensitive
//        write("TEST1");
//        push(KeyCode.ENTER);
//
//        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
//        Note renamedNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
//        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("TEST1"));

        // TFE, 20220419: CTRL+R doesn't select the menu item any more... so we need to do WHAT? get rid fo menu entry!
//        // #2 ------------------------------------------------------------------
//        // rename note via right click + CTRL+R

        // #2 ------------------------------------------------------------------
        // rename note via single click
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        clickOn();
        write("rename2");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
        Note renamedNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("rename2"));

        // #3 ------------------------------------------------------------------
        // rename note via double click
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        doubleClickOn();
        write("test1");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
        renamedNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("test1"));

        // #4 ------------------------------------------------------------------
        // Alert when renaming to existing name
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        doubleClickOn();
        write("test2");
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);
        
        // note should still have old name
        assertTrue("Check renamed note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
        renamedNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
        //System.out.println(renamedNote);
        assertTrue("Check renamed note label", renamedNote.getNoteName().startsWith("test1"));
    }
    
    private void testDragNote() {
        System.out.println("running testDragNote()");

        // #1 ------------------------------------------------------------------
        // get x, y coordinates from tab 2
        Node testLabel = null;
        testLabel = test2Tag.getGraphic();
        Bounds testBounds = test2Tag.localToScreen(testLabel.getBoundsInLocal());
        assertNotNull(testBounds);
        double centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        double centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;
        
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        Point p = MouseInfo.getPointerInfo().getLocation();
        Point2D p2d = new Point2D(p.getX(), p.getY());

        FxRobot dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.dropTo(centerX, centerY);
        
        waitOnJavaFxPlatformEventsDone();
        
        // check "Test 1" tab, that should have 1 less entries
        testTag("Test1", myTestdata.getNotesCountForGroup("Test1") - 1, CheckMode.TAG_CHILDREN);

        // check "Test 2" tab, that should have 1 more entry
        testTag("Test2", myTestdata.getNotesCountForGroup("Test2") + 1, CheckMode.TAG_CHILDREN);
        
        // #2 ------------------------------------------------------------------
        // drag note back
        testLabel = test1Tag.getGraphic();
        testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;
        
        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        p = MouseInfo.getPointerInfo().getLocation();
        p2d = new Point2D(p.getX(), p.getY());

        dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.dropTo(centerX, centerY);
        
        // check "Test 1" tab, that should have 2 entries
        testTag("Test1", myTestdata.getNotesCountForGroup("Test1"), CheckMode.TAG_CHILDREN);

        // check "Test 2" tab, that should have 1 entry
        testTag("Test2", myTestdata.getNotesCountForGroup("Test2"), CheckMode.TAG_CHILDREN);
        
        // #3 ------------------------------------------------------------------
        // Alert when dragging to group with same note name
        testLabel = test3Tag.getGraphic();
        testBounds = testLabel.localToScreen(testLabel.getBoundsInLocal());
        centerX = testBounds.getMinX() + (testBounds.getMaxX() - testBounds.getMinX())/2.0;
        centerY = testBounds.getMinY() + (testBounds.getMaxY() - testBounds.getMinY())/2.0;

        // go back to the ALL tab
        selectTag(allTag);

        clickOn(notesTableFXML);
        moveBy(0, - notesTableFXML.getHeight() / 2 * SCALING);
        p = MouseInfo.getPointerInfo().getLocation();
        p2d = new Point2D(p.getX(), p.getY());

        dragNote = drag(p2d, MouseButton.PRIMARY);
        dragNote.dropTo(centerX, centerY);
 
        // dialog "Note with same name exists"
        testAlert("An error occured while moving the note.", ButtonBar.ButtonData.OK_DONE);

        // check "Test 1" tab, that should have 2 entries
        testTag("Test1", myTestdata.getNotesCountForGroup("Test1"), CheckMode.TAG_CHILDREN);

        // check "Test 3" tab, that should have 1 entry
        testTag("Test3", myTestdata.getNotesCountForGroup("Test3"), CheckMode.TAG_CHILDREN);
   }
    
    private void testGroups() {
        System.out.println("running testGroups()");

        // All, Not Grouped, Test1, Test2, Test3 => 5 tags
        assertTrue(TagManager.ReservedTag.Groups.getTag().getChildren().size() == myTestdata.getGroupsList().size());
        assertTrue(tagsTreeView.getRoot().getChildren().get(0).getChildren().size() == myTestdata.getGroupsList().size());
        
//        // #2 ------------------------------------------------------------------
//        // rename group
//        doubleClickOn();
//        write("Test4");
//        push(KeyCode.ENTER);
//        push(KeyCode.ENTER);
//        
////        assertTrue("Check Test4 tab", ((Label) groupsPaneFXML.getSelectionModel().getSelectedItem().getGraphic()).getText().startsWith("Test4"));
//        
//        // #3 ------------------------------------------------------------------
//        // delete group
//        rightClickOn();
//        push(KeyCode.DOWN);
//        push(KeyCode.DOWN);
//        // TFE, 20181003: java 9: right click selects the first menu item... so one "DOWN" less here
//        //push(KeyCode.DOWN);
//        push(KeyCode.ENTER);

//        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 1);
    }

    
    private void testNotesFilter() {
        System.out.println("running testNotesFilter()");

        // leerer filter -> alle sichtbar
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.TABLE_ELEMENTS);
        
        //////////////////////////
        // namensfilter
        //////////////////////////

        // "Test1" als namensfilter -> 0 sichtbar
        clickOn(noteFilterText);
        write("Test1");
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForName("Test1"), CheckMode.TABLE_ELEMENTS);
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.TABLE_ELEMENTS);
        
        // "SUCH" als namensfilter -> 0 sichtbar
        clickOn(noteFilterText);
        write("SUCH");
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForName("SUCH"), CheckMode.TABLE_ELEMENTS);
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.TABLE_ELEMENTS);
        
        //////////////////////////
        // inhaltsfilter
        //////////////////////////

        clickOn(noteFilterCheck);
        
        // "Test1" als inhaltsfilter -> 2 sichtbar
        clickOn(noteFilterText);
        write("Test1");
        testTag(TagManager.ReservedTag.All.getTagName(), 2, CheckMode.TABLE_ELEMENTS);
        
        // "ESC" -> alle sichtbar
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.TABLE_ELEMENTS);
        
        // "SUCH" als inhaltsfilter -> 1 sichtbar
        clickOn(noteFilterText);
        write("SUCH");
        testTag(TagManager.ReservedTag.All.getTagName(), 1, CheckMode.TABLE_ELEMENTS);
        
        // reset everything, PLEASE
        clickOn(noteFilterCheck);
        clickOn(noteFilterText);
        push(KeyCode.ESCAPE);
    }
    
    private void testFileSystemChange() {
        System.out.println("running testFileSystemChange()");

        long sleepTime = 1200;
        
        // TFE, 20190930: switch to correct tab initially to avoid later chanegs that might trigger hasChanged() calls
        selectTag(allTag);
        
        // #1 ------------------------------------------------------------------
        // add a new file to group Test1 and give UI some time to discover it
        assertTrue(myTestdata.createTestFile(testpath, "[Test1] test3.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: add a new file to group Test1");
        
        // check new count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()) + 1, CheckMode.BOTH);
        
        // #2 ------------------------------------------------------------------
        // delete the new file
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test3.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete the new file");
        
        // check new count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.BOTH);
        
        // #3 ------------------------------------------------------------------
        // add a new file to a new group
        assertTrue(myTestdata.createTestFile(testpath, "[Test4] test4.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: add a new file to a new group");
        
        // All, Not Grouped, Test1, Test2, Test3, Test4, + => 7 tabs
//        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 2);

        // delete the new file
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test4] test4.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete the new file");

        // All, Not Grouped, Test1, Test2, Test3, + => 7 tabs
//        assertTrue(groupsPaneFXML.getTabs().size() == myTestdata.getGroupsList().size() + 1);

        // #4 ------------------------------------------------------------------
        // delete file in editor BUT "Save own"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor BUT \"Save own\"");

        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.OK_DONE);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify old count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.BOTH);

        // #5 ------------------------------------------------------------------
        // delete file in editor BUT "Save as new"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor BUT \"Save as new\"");

        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.OTHER);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify old count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.BOTH);
        // but with new note name!
        final int newCount = myTestdata.getNotesList().size() + 1;
        final String newName = "New Note " + newCount;
        assertTrue("Check new note type", (notesTableFXML.getSelectionModel().getSelectedItem() instanceof Note));
        final Note newNote = (Note) notesTableFXML.getSelectionModel().getSelectedItem();
        assertTrue("Check new note label", newName.equals(newNote.getNoteName()));
        
        // #6 ------------------------------------------------------------------
        // delete file in editor AND "Discard own"
        assertTrue(myTestdata.deleteTestFile(testpath, "[Test1] " + newName + ".htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: delete file in editor AND \"Discard own\"");
        
        testAlert("Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes", ButtonBar.ButtonData.CANCEL_CLOSE);
        sleep(sleepTime, TimeUnit.MILLISECONDS);

        // verify new count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()) - 1, CheckMode.BOTH);
        
        // create back again
        assertTrue(myTestdata.createTestFile(testpath, "[Test1] test1.htm"));
        sleep(sleepTime, TimeUnit.MILLISECONDS);
//        System.out.println("after sleep for: create back again");

        // verify old count
        testTag(TagManager.ReservedTag.All.getTagName(), myTestdata.getNotesCountForGroup(TagManager.ReservedTag.All.getTagName()), CheckMode.BOTH);
    }
    
    private void resetForNextTest() {
        // tabs might have been changed
        getNodes();

        // select first note
        interact(() -> {
            notesTableFXML.requestFocus();
            notesTableFXML.getSelectionModel().select(0);
            notesTableFXML.getFocusModel().focus(0);
        });

        // go back to the ALL tab
        selectTag(allTag);
    }
}