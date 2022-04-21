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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.ownnote.ui.helper.OwnNoteFileManager;

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

    @BeforeClass
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
    
    @Before 
    public void setUp() {
        TagManager.getInstance().resetTagList();
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources/LookAndFeel");
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testTagList() {
        final TagData rootTag = TagManager.getInstance().getRootTag();
        Assert.assertTrue("Testing name of root tag", TagManager.ROOT_TAG_NAME.equals(rootTag.getName()));
        
        Assert.assertTrue("Testing name of groups root tag", TagManager.ReservedTag.Groups.name().equals(rootTag.getChildren().get(0).getName()));
        
        final List<TagData> groupTags = TagManager.getInstance().getGroupTags(false);
        Assert.assertEquals("Testing size of group tag list", 6, groupTags.size());
        
        final List<TagData> tagsList = TagManager.getInstance().getRootTag().getChildren();
        Assert.assertEquals("Testing size of root tag list", 1, tagsList.size());
        Assert.assertEquals("Testing size of child tag list", 6, tagsList.get(0).getChildren().size());
    }

    @Test
    public void testTagAttributes() {
        final List<TagData> groupTags = TagManager.getInstance().getGroupTags(true);
        
        Assert.assertTrue("Testing ALL group tag", TagManager.ReservedTag.All.getTagName().equals(groupTags.get(0).getName()));
        Assert.assertTrue("Testing NOT_GROUPED group tag", TagManager.ReservedTag.NotGrouped.getTagName().equals(groupTags.get(1).getName()));
        
        final TagData test3 = groupTags.get(4);
        Assert.assertTrue("Testing TEST3 id", "b2eeee278206".equals(test3.getId()));
        Assert.assertTrue("Testing TEST3 name", "Test3".equals(test3.getName()));
        Assert.assertTrue("Testing TEST3 iconName", "CAMERA_RETRO".equals(test3.getIconName()));
        Assert.assertTrue("Testing TEST3 colorName", "#99D0DF".equals(test3.getColorName()));
        Assert.assertEquals("Testing TEST3 linked notes", 1, test3.getLinkedNotes().size());
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
            Assert.assertEquals("Attribue has changed", "DUMMY", groupTags.get(0).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: resetName of group #1");
            groupTags.get(0).setName(TagManager.ReservedTag.All.getTagName());
            Assert.assertEquals("Attribue has changed", TagManager.ReservedTag.All.getTagName(), groupTags.get(0).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));

            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: setName of group #2");
            groupTags.get(1).setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", groupTags.get(1).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            groupTags.get(1).setName(TagManager.ReservedTag.NotGrouped.getTagName());

            // add a non-group child
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: add non-group child to group #1");
            groupTags.get(0).getChildren().add(TagManager.getInstance().createTagBelowParent("TEST_CHILD_1", false, groupTags.get(0)));
            // "ADDED" for children list change
            Assert.assertEquals("Children have been added", ChangeType.ADDED, testChangeType.get(0));

            // remove tag by deleting it -> no change triggered, since getChildren not in property extractor
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(groupTags.get(0).getChildren().get(0), null);
            // "REMOVED" for children
            Assert.assertEquals("Children have been removed", ChangeType.REMOVED, testChangeType.get(0));
            Assert.assertTrue("No more children", groupTags.get(0).getChildren().isEmpty());

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: add group child to group #1");
            final TagData groupChild = TagManager.getInstance().createTagBelowParent("TEST_CHILD_2", true, null);
            groupTags.get(1).getChildren().add(groupChild);
            // "ADDED" from add to list
            Assert.assertEquals("\"ADDED\" 1. from .add()", ChangeType.ADDED, testChangeType.get(0));
            // "ADDED" from setParent
            Assert.assertEquals("\"ADDED\" 2. from .add() from tagTestListener2", ChangeType.ADDED, testChangeType.get(1));
            // "UPDATED" from add to list
            Assert.assertEquals("\"UPDATED\" 3. from .setColor() for group tag", ChangeType.UPDATED, testChangeType.get(2));
            // "ADDED" from add to list
            Assert.assertEquals("\"ADDED\" 4. from .add() from tagTestListener2", ChangeType.ADDED, testChangeType.get(3));
            Assert.assertEquals("Size of group list should have increased", 8, groupTags.size());

            testChangeType.clear();
//            System.out.println("\ntestGroupChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(groupChild, null);
            // "REMOVED" for children
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null", ChangeType.REMOVED, testChangeType.get(0));
            // "REMOVED" for children
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null from tagTestListener2", ChangeType.REMOVED, testChangeType.get(1));
            Assert.assertEquals("Size of group list should have decreased", 7, groupTags.size());
            Assert.assertTrue("No more children", groupTags.get(1).getChildren().isEmpty());
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
            TagManager.getInstance().addListener(tagTestListener);
            
            // rebuild tag tree as from metadata
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: creating local tag tree");
            final TagData groups = TagManager.getInstance().tagForName("LGroups", null, true);
            
            final TagData all = TagManager.getInstance().createTagBelowParent("LAll", true, groups);
            groups.getChildren().add(all);
            final TagData notGrouped = TagManager.getInstance().createTagBelowParent("LNot grouped", true, groups);
            groups.getChildren().add(notGrouped);
            final TagData test1 = TagManager.getInstance().createTagBelowParent("LTest1", true, groups);
            groups.getChildren().add(test1);
            final TagData test2 = TagManager.getInstance().createTagBelowParent("LTest2", true, groups);
            groups.getChildren().add(test2);
            final TagData test3 = TagManager.getInstance().createTagBelowParent("LTest3", true, groups);
            groups.getChildren().add(test3);
            // 18 changes for 6 new tags: ADDED + UPDATED for tag creation + ADDED to (local) tag tree
            Assert.assertEquals("A lot has happened", 18, testChangeType.size());
            
            // rename group root tag
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: rename group root");
            groups.setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", groups.getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            groups.setName("LGroups");

            // get groups tags but as children of children of root tag
            final ObservableList<TagData> tagsList = groups.getChildren();
            
            // change attributes
            testChangeType.clear();
//            System.out.println("\ntestLocalTagTreeListener: rename group tag #1");
            tagsList.get(0).setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", tagsList.get(0).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(0).setName("LAll");

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #3");
            tagsList.get(1).setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", tagsList.get(1).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(1).setName("LTest1");

            // add a non-group child
            int tagChildren = tagsList.get(0).getChildren().size();
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add non-group child to group #1");
            TagData newChild1 = TagManager.getInstance().createTagBelowParent("TEST_CHILD_1", false, tagsList.get(0));
            tagsList.get(0).getChildren().add(newChild1);
            // "ADDED"
            Assert.assertEquals("\"ADDED\" 1. from .add()", ChangeType.ADDED, testChangeType.get(0));

            // change attributes of created tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename TEST_CHILD_1");
            newChild1.setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", newChild1.getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(0).setName("TEST_CHILD_1");

            // remove tag by deleting it -> change triggered, since getChildren in property extractor
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(newChild1, null);
            // "REMOVED"
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null", ChangeType.REMOVED, testChangeType.get(0));
            Assert.assertEquals("Previous number of children", tagChildren, tagsList.get(0).getChildren().size());

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add group child to group #1");
            TagData newChild2 = TagManager.getInstance().createTagBelowParent("TEST_CHILD_2", true, tagsList.get(1));
            tagsList.get(1).getChildren().add(newChild2);
            // "ADDED"
            Assert.assertEquals("\"ADDED\" 1. from .add()", ChangeType.ADDED, testChangeType.get(0));

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(newChild2, null);
            // "REMOVED" fourth from implicit clear to tagTestListener from flatTags.setAll
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null", ChangeType.REMOVED, testChangeType.get(0));
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
            TagManager.getInstance().addListener(tagTestListener);
//            doAddListener(TagManager.getInstance().getRootTag(), tagTestListener2);

            final TagData groupRoot = TagManager.getInstance().tagForName(TagManager.ReservedTag.Groups.name(), null, false);

            // rename group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group root");
            groupRoot.setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", groupRoot.getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            groupRoot.setName(TagManager.ReservedTag.Groups.name());

            // get groups tags but as children of children of root tag
            final ObservableList<TagData> tagsList = groupRoot.getChildren();

            // change attributes
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #1");
            tagsList.get(0).setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", tagsList.get(0).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(0).setName(TagManager.ReservedTag.All.getTagName());

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename group tag #3");
            tagsList.get(1).setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", tagsList.get(1).getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(1).setName("Test1");

            // add a non-group child
            int tagChildren = tagsList.get(0).getChildren().size();
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add non-group child to group #1");
            TagData newChild1 = TagManager.getInstance().createTagBelowParent("TEST_CHILD_1", false, tagsList.get(0));
            tagsList.get(0).getChildren().add(newChild1);
            // "ADDED"
            Assert.assertEquals("\"ADDED\" 1. from .add()", ChangeType.ADDED, testChangeType.get(0));

            // change attributes of created tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: rename TEST_CHILD_1");
            newChild1.setName("DUMMY");
            Assert.assertEquals("Attribue has changed", "DUMMY", newChild1.getName());
            Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
            tagsList.get(0).setName("TEST_CHILD_1");

            // remove tag by deleting it -> change triggered, since getChildren in property extractor
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove non-group child from group #1");
            TagManager.getInstance().renameTag(newChild1, null);
            // "REMOVED"
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null", ChangeType.REMOVED, testChangeType.get(0));
            Assert.assertEquals("Previous number of children", tagChildren, tagsList.get(0).getChildren().size());

            // add a group child -> change triggered BUT on groupTags level since its also added directly to the group root tag
            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: add group child to group #1");
            TagData newChild2 = TagManager.getInstance().createTagBelowParent("TEST_CHILD_2", true, tagsList.get(1));
            tagsList.get(1).getChildren().add(newChild2);
            // "ADDED"
            Assert.assertEquals("\"ADDED\" 1. from .add()", ChangeType.ADDED, testChangeType.get(0));

            testChangeType.clear();
//            System.out.println("\ntestTagChangeListeners: remove group child from group #1");
            TagManager.getInstance().renameTag(newChild2, null);
            // "REMOVED" fourth from implicit clear to tagTestListener from flatTags.setAll
            Assert.assertEquals("\"REMOVED\" 1. from implicit .remove() for new name null", ChangeType.REMOVED, testChangeType.get(0));
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
        Assert.assertEquals("Testing root level", 0, rootTag.getLevel().intValue());
        
        for (TagData tag: rootTag.getChildren()) {
            Assert.assertEquals("Testing children level 1", 1, tag.getLevel().intValue());
        }
        
        for (TagData tag : TagManager.getInstance().getGroupTags(false)) {
            Assert.assertEquals("Testing children level 2", 2, tag.getLevel().intValue());
        }

        final TagData test3 = TagManager.getInstance().getGroupTags(false).get(4);
        Assert.assertFalse(test3.getChildren().isEmpty());
        for (TagData tag : test3.getChildren()) {
            Assert.assertEquals("Testing children level 3", 3, tag.getLevel().intValue());
        }
        
        // add child and see if automatic update works
        TagData newChild1 = TagManager.getInstance().createTagBelowParent("TEST_CHILD_1", false, test3);
        Assert.assertEquals("Testing new tag not yet added", 0, newChild1.getLevel().intValue());
        test3.getChildren().add(newChild1);
        Assert.assertEquals("Testing new tag after adding", 3, newChild1.getLevel().intValue());
    }

    
    @Test
    public void testTagLevelLookup() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData rootTag = TagManager.getInstance().getRootTag();
        final TagData grouspRootTag = rootTag.getChildren().get(0);
        
        Assert.assertFalse(TagManager.getInstance().isSameGroupOrChildGroup(rootTag, grouspRootTag, false));
        Assert.assertTrue(TagManager.getInstance().isSameGroupOrChildGroup(rootTag, grouspRootTag, true));

        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        Assert.assertFalse(TagManager.getInstance().isSameGroupOrChildGroup(grouspRootTag, test3, false));
        Assert.assertTrue(TagManager.getInstance().isSameGroupOrChildGroup(grouspRootTag, test3, true));

        final TagData level2 = test3.getChildren().get(0);
        Assert.assertFalse(TagManager.getInstance().isSameGroupOrChildGroup(grouspRootTag, level2, false));
        Assert.assertTrue(TagManager.getInstance().isSameGroupOrChildGroup(grouspRootTag, level2, true));
    }
    
    @Test
    public void testTagGroupAssignement() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData rootTag = TagManager.getInstance().getRootTag();
        final TagData grouspRootTag = rootTag.getChildren().get(0);
        
        Assert.assertFalse(rootTag.isGroup());
        Assert.assertTrue(grouspRootTag.isGroup());

        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        Assert.assertTrue(test3.isGroup());

        final TagData level2 = test3.getChildren().get(0);
        Assert.assertTrue(level2.isGroup());
    }
    
    @Test
    public void TestExternalName() {
        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        final TagData test3 = TagManager.getInstance().getGroupTags(true).get(4);
        final TagData level2 = test3.getChildren().get(0);
        
        // 1st check: external name is built up over the hierarchy
        Assert.assertEquals(level2.getExternalName(), TagManager.getInstance().getExternalName(level2));
        Assert.assertEquals("Test3~Level 2", level2.getExternalName());
        
        // 2nd check: lookup by external name gives same object
        final TagData newLevel2 = TagManager.getInstance().tagForExternalName(level2.getExternalName(), false);
        Assert.assertEquals(level2, newLevel2);
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
