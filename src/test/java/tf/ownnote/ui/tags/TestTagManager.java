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

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.ownnote.ui.helper.FileManager;

/**
 *
 * @author thomas
 */
public class TestTagManager {
    private static ListChangeListener<TagData> tagTestListener;
    private static ListChangeListener<TagData> tagTestListener2;
    
    private enum ChangeType {
        REMOVED,
        ADDED,
        UPDATED;
        
        private String myListener = "NOT SET";
        
        public ChangeType setListener(final String listener) {
            myListener = listener;
            return this;
        }
        
        public String getListener() {
            return myListener;
        }
    }
    
    // used to track calls to the change listener
    private static final ObservableList<ChangeType> testChangeType = FXCollections.<ChangeType>observableArrayList();

    @BeforeAll
    public static void setUpClass() {
        tagTestListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        testChangeType.add(ChangeType.REMOVED.setListener("tagTestListener"));
                    }
                    if (change.wasAdded()) {
                        testChangeType.add(ChangeType.ADDED.setListener("tagTestListener"));
                    }
                    if (change.wasUpdated()) {
                        testChangeType.add(ChangeType.UPDATED.setListener("tagTestListener"));
                    }
                }
            }
        };
        
        tagTestListener2 = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        testChangeType.add(ChangeType.REMOVED.setListener("tagTestListener2"));
                    }
                    if (change.wasAdded()) {
                        testChangeType.add(ChangeType.ADDED.setListener("tagTestListener2"));
                    }
                    if (change.wasUpdated()) {
                        testChangeType.add(ChangeType.UPDATED.setListener("tagTestListener2"));
                    }
                }
            }
        };

        testChangeType.addListener((ListChangeListener.Change<? extends ChangeType> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
//                    System.out.println("Change event was removed: " + change.getRemoved() + " for listener " + change.getRemoved().get(0).getListener());
                }
                if (change.wasAdded()) {
//                    System.out.println("Change event was added: " + change.getAddedSubList() + " for listener " + change.getAddedSubList().get(0).getListener());
                }
            }
        });
    }
    
    @BeforeEach
    public void setUp() {
        TagManager.getInstance().resetTagList();
        FileManager.getInstance().setCallback(null);
        FileManager.getInstance().initNotesPath("src/test/resources/LookAndFeel");
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    public void testTagList() {
        final TagData rootTag = TagManager.getInstance().getRootTag();
        Assertions.assertTrue(TagManager.ROOT_TAG_NAME.equals(rootTag.getName()), "Testing name of root tag");
        
        Assertions.assertTrue(TagManager.ReservedTag.Groups.name().equals(rootTag.getChildren().get(0).getName()), "Testing name of groups root tag");
        
        final List<TagData> groupTags = TagManager.getInstance().getGroupTags(false);
        Assertions.assertEquals(6, groupTags.size(), "Testing size of group tag list");
        
        final List<TagData> tagsList = TagManager.getInstance().getRootTag().getChildren();
        Assertions.assertEquals(1, tagsList.size(), "Testing size of root tag list");
        Assertions.assertEquals(6, tagsList.get(0).getChildren().size(), "Testing size of child tag list");
    }

    @Test
    public void testTagAttributes() {
        final List<TagData> groupTags = TagManager.getInstance().getGroupTags(true);
        
        Assertions.assertTrue(TagManager.ReservedTag.All.getTagName().equals(groupTags.get(0).getName()), "Testing ALL group tag");
        Assertions.assertTrue(TagManager.ReservedTag.NotGrouped.getTagName().equals(groupTags.get(1).getName()), "Testing NOT_GROUPED group tag");
        
        final TagData test3 = groupTags.get(4);
        // TFE, 20240212: since we now clone after loading from xml we can't test id anymore
//        Assertions.assertTrue("b2eeee278206".equals(test3.getId()), "Testing TEST3 id");
        Assertions.assertTrue("Test3".equals(test3.getName()), "Testing TEST3 name");
        Assertions.assertTrue("CAMERA_RETRO".equals(test3.getIconName()), "Testing TEST3 iconName");
        Assertions.assertTrue("#99D0DF".equals(test3.getColorName()), "Testing TEST3 colorName");
        Assertions.assertEquals(1, test3.getLinkedNotes().size(), "Testing TEST3 linked notes");
    }

    @Test
    public void testGroupChangeListeners() {
        final ObservableList<TagData> groupTags = TagManager.getInstance().getGroupTags(true);
        try {
            groupTags.addListener(tagTestListener);
            groupTags.get(0).getChildren().addListener(tagTestListener2);
            groupTags.get(1).getChildren().addListener(tagTestListener2);

            // change attributes
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: setName of group #1");
            groupTags.get(0).setName("DUMMY");
            Assertions.assertEquals("DUMMY", groupTags.get(0).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: resetName of group #1");
            groupTags.get(0).setName(TagManager.ReservedTag.All.getTagName());
            Assertions.assertEquals(TagManager.ReservedTag.All.getTagName(), groupTags.get(0).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");

            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: setName of group #2");
            groupTags.get(1).setName("DUMMY");
            Assertions.assertEquals("DUMMY", groupTags.get(1).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            groupTags.get(1).setName(TagManager.ReservedTag.NotGrouped.getTagName());

            // add a non-group child
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: add non-group child to group #1");
            groupTags.get(0).getChildren().add(TagManager.getInstance().createTagWithParent("TEST_CHILD_1", groupTags.get(0)));
            // "ADDED" for children list change
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "Children have been added");

            // remove tag by deleting it -> no change triggered, since getChildren not in property extractor
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(groupTags.get(0).getChildren().get(0), null);
            // "REMOVED" for children
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "Children have been removed");
            Assertions.assertTrue(groupTags.get(0).getChildren().isEmpty(), "No more children");

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: add group child to group #1");
            final TagData groupChild = TagManager.getInstance().createTagWithParent("TEST_CHILD_2", true, false, null);
            groupTags.get(1).getChildren().add(groupChild);
            // "ADDED" from add to list
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "\"ADDED\" 1. from .add()");
            // "ADDED" from setParent
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(1), "\"ADDED\" 2. from .add() from tagTestListener2");
            // "UPDATED" from add to list
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(2), "\"UPDATED\" 3. from .setColor() for group tag");
            // "ADDED" from add to list
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(3), "\"ADDED\" 4. from .add() from tagTestListener2");
            Assertions.assertEquals(8, groupTags.size(), "Size of group list should have increased");

            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(groupChild, null);
            // "REMOVED" for children
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "\"REMOVED\" 1. from implicit .remove() for new name null");
            // "REMOVED" for children
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(1), "\"REMOVED\" 1. from implicit .remove() for new name null from tagTestListener2");
            Assertions.assertEquals(7, groupTags.size(), "Size of group list should have decreased");
            Assertions.assertTrue(groupTags.get(1).getChildren().isEmpty(), "No more children");
        }
        finally {
//            System.out.println("\ntestGroupChangeListeners: cleaning up after test");
            groupTags.removeListener(tagTestListener);
            groupTags.get(0).getChildren().removeListener(tagTestListener2);
            groupTags.get(1).getChildren().removeListener(tagTestListener2);
            testChangeType.clear();
        }
    }

    @Test
    public void testLocalTagTreeListener() {
        try {
            TagManager.getInstance().addListChangeListener(tagTestListener);
            
            // rebuild tag tree as from metadata
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: creating local tag tree");
            final TagData groups = TagManager.getInstance().tagForName("LGroups", null, true, true);
            
            final TagData all = TagManager.getInstance().createTagWithParent("LAll", groups);
            groups.getChildren().add(all);
            final TagData notGrouped = TagManager.getInstance().createTagWithParent("LNot grouped", groups);
            groups.getChildren().add(notGrouped);
            final TagData test1 = TagManager.getInstance().createTagWithParent("LTest1", groups);
            groups.getChildren().add(test1);
            final TagData test2 = TagManager.getInstance().createTagWithParent("LTest2", groups);
            groups.getChildren().add(test2);
            final TagData test3 = TagManager.getInstance().createTagWithParent("LTest3", groups);
            groups.getChildren().add(test3);
            // 18 changes for 6 new tags: ADDED + UPDATED for tag creation + ADDED to (local) tag tree
            Assertions.assertEquals(18, testChangeType.size(), "A lot has happened");
            
            // rename group root tag
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: rename group root");
            groups.setName("DUMMY");
            Assertions.assertEquals("DUMMY", groups.getName());
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0));
            groups.setName("LGroups");

            // get groups tags but as children of children of root tag
            final ObservableList<TagData> tagsList = groups.getChildren();
            
            // change attributes
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: rename group tag #1");
            tagsList.get(0).setName("DUMMY");
            Assertions.assertEquals("DUMMY", tagsList.get(0).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(0).setName("LAll");

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #3");
            tagsList.get(1).setName("DUMMY");
            Assertions.assertEquals("DUMMY", tagsList.get(1).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(1).setName("LTest1");

            // add a non-group child
            int tagChildren = tagsList.get(0).getChildren().size();
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add non-group child to group #1");
            TagData newChild1 = TagManager.getInstance().createTagWithParent("TEST_CHILD_1", tagsList.get(0));
            tagsList.get(0).getChildren().add(newChild1);
            // "ADDED"
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "\"ADDED\" 1. from .add()");

            // change attributes of created tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename TEST_CHILD_1");
            newChild1.setName("DUMMY");
            Assertions.assertEquals("DUMMY", newChild1.getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(0).setName("TEST_CHILD_1");

            // remove tag by deleting it -> change triggered, since getChildren in property extractor
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(newChild1, null);
            // "REMOVED"
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "\"REMOVED\" 1. from implicit .remove() for new name null");
            Assertions.assertEquals(tagChildren, tagsList.get(0).getChildren().size(), "Previous number of children");

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add group child to group #1");
            TagData newChild2 = TagManager.getInstance().createTagWithParent("TEST_CHILD_2", tagsList.get(1));
            tagsList.get(1).getChildren().add(newChild2);
            // "ADDED"
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "\"ADDED\" 1. from .add()");

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(newChild2, null);
            // "REMOVED" fourth from implicit clear to tagTestListener from flatTags.setAll
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "\"REMOVED\" 1. from implicit .remove() for new name null");
        }
        finally {
//            System.out.println("\ntestTagChangeListeners: cleaning up after test");
            TagManager.getInstance().removeListener(tagTestListener);
            testChangeType.clear();
        }
    }

    @Test
    public void testTagChangeListeners() {
        try {
            TagManager.getInstance().addListChangeListener(tagTestListener);
//            doAddListener(TagManager.getInstance().getRootTag(), tagTestListener2);

            final TagData groupRoot = TagManager.getInstance().tagForName(TagManager.ReservedTag.Groups.name(), null, false, true);

            // rename group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group root");
            groupRoot.setName("DUMMY");
            Assertions.assertEquals("DUMMY", groupRoot.getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            groupRoot.setName(TagManager.ReservedTag.Groups.name());

            // get groups tags but as children of children of root tag
            final ObservableList<TagData> tagsList = groupRoot.getChildren();

            // change attributes
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #1");
            tagsList.get(0).setName("DUMMY");
            Assertions.assertEquals("DUMMY", tagsList.get(0).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(0).setName(TagManager.ReservedTag.All.getTagName());

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #3");
            tagsList.get(1).setName("DUMMY");
            Assertions.assertEquals("DUMMY", tagsList.get(1).getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(1).setName("Test1");

            // add a non-group child
            int tagChildren = tagsList.get(0).getChildren().size();
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add non-group child to group #1");
            TagData newChild1 = TagManager.getInstance().createTagWithParent("TEST_CHILD_1", tagsList.get(0));
            tagsList.get(0).getChildren().add(newChild1);
            // "ADDED"
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "\"ADDED\" 1. from .add()");

            // change attributes of created tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename TEST_CHILD_1");
            newChild1.setName("DUMMY");
            Assertions.assertEquals("DUMMY", newChild1.getName(), "Attribue has changed");
            Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
            tagsList.get(0).setName("TEST_CHILD_1");

            // remove tag by deleting it -> change triggered, since getChildren in property extractor
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(newChild1, null);
            // "REMOVED"
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "\"REMOVED\" 1. from implicit .remove() for new name null");
            Assertions.assertEquals(tagChildren, tagsList.get(0).getChildren().size(), "Previous number of children");

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add group child to group #1");
            TagData newChild2 = TagManager.getInstance().createTagWithParent("TEST_CHILD_2", tagsList.get(1));
            tagsList.get(1).getChildren().add(newChild2);
            // "ADDED"
            Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "\"ADDED\" 1. from .add()");

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(newChild2, null);
            // "REMOVED" fourth from implicit clear to tagTestListener from flatTags.setAll
            Assertions.assertEquals(ChangeType.REMOVED, testChangeType.get(0), "\"REMOVED\" 1. from implicit .remove() for new name null");
        }
        finally {
//            System.out.println("\ntestTagChangeListeners: cleaning up after test");
            TagManager.getInstance().removeListener(tagTestListener);
            testChangeType.clear();
        }
    }
    
    @Test
    public void testTagLevels() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData rootTag = TagManager.getInstance().getRootTag();
        Assertions.assertEquals(0, rootTag.getLevel().intValue(), "Testing root level");
        
        for (TagData tag: rootTag.getChildren()) {
            Assertions.assertEquals(1, tag.getLevel().intValue(), "Testing children level 1");
        }
        
        for (TagData tag : TagManager.getInstance().getGroupTags(false)) {
            Assertions.assertEquals(2, tag.getLevel().intValue(), "Testing children level 2");
        }

        final TagData test3 = TagManager.getInstance().getGroupTags(false).get(4);
        Assertions.assertFalse(test3.getChildren().isEmpty());
        for (TagData tag : test3.getChildren()) {
            Assertions.assertEquals(3, tag.getLevel().intValue(), "Testing children level 3");
        }
        
        // add child and see if automatic update works
        TagData newChild1 = TagManager.getInstance().createTagWithParent("TEST_CHILD_1", test3);
        Assertions.assertEquals(0, newChild1.getLevel().intValue(), "Testing new tag not yet added");
        test3.getChildren().add(newChild1);
        Assertions.assertEquals(3, newChild1.getLevel().intValue(), "Testing new tag after adding");
    }

    
    @Test
    public void testTagLevelLookup() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData rootTag = TagManager.getInstance().getRootTag();
        final TagData grouspRootTag = rootTag.getChildren().get(0);
        
        Assertions.assertFalse(TagManager.getInstance().compareTagsHierarchy(rootTag, grouspRootTag, TagManager.TagCompare.BY_IDENTITY, false));
        Assertions.assertTrue(TagManager.getInstance().compareTagsHierarchy(rootTag, grouspRootTag, TagManager.TagCompare.BY_IDENTITY, true));

        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        Assertions.assertFalse(TagManager.getInstance().compareTagsHierarchy(grouspRootTag, test3, TagManager.TagCompare.BY_IDENTITY, false));
        Assertions.assertTrue(TagManager.getInstance().compareTagsHierarchy(grouspRootTag, test3, TagManager.TagCompare.BY_IDENTITY, true));

        final TagData level2 = test3.getChildren().get(0);
        Assertions.assertFalse(TagManager.getInstance().compareTagsHierarchy(grouspRootTag, level2, TagManager.TagCompare.BY_IDENTITY, false));
        Assertions.assertTrue(TagManager.getInstance().compareTagsHierarchy(grouspRootTag, level2, TagManager.TagCompare.BY_IDENTITY, true));
    }
    
    @Test
    public void testTagGroupAssignement() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData rootTag = TagManager.getInstance().getRootTag();
        final TagData grouspRootTag = rootTag.getChildren().get(0);
        
        Assertions.assertFalse(rootTag.isGroup());
        Assertions.assertTrue(grouspRootTag.isGroup());

        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        Assertions.assertTrue(test3.isGroup());

        final TagData level2 = test3.getChildren().get(0);
        Assertions.assertTrue(level2.isGroup());
    }
    
    @Test
    public void testExternalName() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        final TagData level2 = test3.getChildren().get(0);
        
        // 1st check: external name is built up over the hierarchy
        Assertions.assertEquals(level2.getExternalName(), TagManager.getExternalName(level2));
        Assertions.assertEquals("Test3~Level 2", level2.getExternalName());
        
        // 2nd check: lookup by external name gives same object
        final TagData newLevel2 = TagManager.getInstance().groupForExternalName(level2.getExternalName(), false);
        Assertions.assertEquals(level2, newLevel2);
    }
    
    @Test
    public void testIsNewChangedTagName() {
        final TagData test2 = TagManager.getInstance().getGroupTags(true).get(3);
        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        final TagData level2 = test3.getChildren().get(0);

        // we can have "Level2" on level of test3 - "Levels" is on another level
        Assertions.assertTrue(TagManager.getInstance().isValidNewTagName(level2.getName(), test3.getParent()));
        Assertions.assertTrue(TagManager.getInstance().isValidNewTagParent(level2, test3.getParent()));
        
        // we can't have another "Test2" on level of group
        Assertions.assertFalse(TagManager.getInstance().isValidNewTagName(test2.getName(), test3.getParent()));
        // we can have "Test2" on level of group - since its the same things
        Assertions.assertTrue(TagManager.getInstance().isValidNewTagParent(test2, test3.getParent()));
        
        // invalid group name
        Assertions.assertFalse(TagManager.getInstance().isValidNewTagName(level2.getName() + "~", test3.getParent()));
        
        // and now add a non-group tag and check it
        final TagData nonGroupTag = TagManager.getInstance().tagForName("TestTag", null, true, true);
        Assertions.assertNotNull(nonGroupTag);
        Assertions.assertTrue(TagManager.getInstance().isValidNewTagName("TestTag2", nonGroupTag));
        Assertions.assertFalse(TagManager.getInstance().isValidNewTagName(level2.getName(), nonGroupTag));
        // can't have group tag under non group tag
        Assertions.assertFalse(TagManager.getInstance().isValidNewTagParent(level2, nonGroupTag));

        // we don't care about invalid group name
        Assertions.assertTrue(TagManager.getInstance().isValidNewTagName("TestTag2~", nonGroupTag));
    }

    @Test
    public void testIsValidChangedTagName() {
        final TagData test2 = TagManager.getInstance().getGroupTags(true).get(3);
        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        final TagData level2 = test3.getChildren().get(0);
        
        // we can't rename test3 to test 2 - thats a siblings
        Assertions.assertFalse(TagManager.getInstance().isValidChangedTagName(test2.getName(), test3));

        // we can rename level2 to test 2 - thats not a siblings
        Assertions.assertTrue(TagManager.getInstance().isValidChangedTagName(test2.getName(), level2));

        // invalid group name
        Assertions.assertFalse(TagManager.getInstance().isValidChangedTagName(test3.getName() + "~", test3));
        
        // and now add a non-group tag and check it
        final TagData nonGroupTag = TagManager.getInstance().tagForName("TestTag", null, true, true);
        Assertions.assertNotNull(nonGroupTag);
        Assertions.assertFalse(TagManager.getInstance().isValidChangedTagName(test3.getName(), nonGroupTag));
        
        // we don't care about invalid group name
        Assertions.assertTrue(TagManager.getInstance().isValidChangedTagName(nonGroupTag.getName() + "~", nonGroupTag));
    }

    @Test
    public void testGetComplementaryGroup() {
        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        final TagData level2 = test3.getChildren().get(0);

        // we don't have that under archive in our test data...
        Assertions.assertNull(TagManager.getInstance().getComplementaryGroup(test3, false));
        
        final TagData test3Archive = TagManager.getInstance().getComplementaryGroup(test3, true);
        Assertions.assertNotNull(test3Archive);
        
        // we are our complementaries
        Assertions.assertNotNull(TagManager.getInstance().getComplementaryGroup(test3Archive, false));
        Assertions.assertEquals(test3, TagManager.getInstance().getComplementaryGroup(test3Archive, false));
        
        // hierarchies are preserved in the archive sector
        final TagData level2Archive = TagManager.getInstance().getComplementaryGroup(level2, true);
        Assertions.assertNotNull(level2Archive);
        Assertions.assertEquals(level2Archive.getParent(), test3Archive);
    }

    private void doAddListener(final TagData tagRoot, ListChangeListener<? super TagData> ll) {
        // add listener to my children and to the children of my children
//        System.out.println("Adding listener " + ll + " to tag " + tagRoot.getName());
        tagRoot.getChildren().addListener(ll);
        for (TagData tag : tagRoot.getChildren()) {
            doAddListener(tag, ll);
        }
    }
}
