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

import java.util.concurrent.Callable;
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
public class TestTagData {
    private static ListChangeListener<TagData> tagTestListener;
    
    private enum ChangeType {
        REMOVED,
        ADDED,
        UPDATED
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
                        testChangeType.add(ChangeType.REMOVED);
                    }
                    if (change.wasAdded()) {
                        testChangeType.add(ChangeType.ADDED);
                    }
                    if (change.wasUpdated()) {
                        testChangeType.add(ChangeType.UPDATED);
//                        for (TagData tag : change.getList().subList(change.getFrom(), change.getTo())) {
//                            System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was updated");
//                        }
                    }
                }
            }
        };
        
        testChangeType.addListener((ListChangeListener.Change<? extends ChangeType> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
//                    System.out.println("Change event was removed: " + change.getRemoved());
                }
                if (change.wasAdded()) {
//                    System.out.println("Change event was added: " + change.getAddedSubList());
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
    
    private Callable<Boolean> changeOccured() {
	return () -> testChangeType.size() > 0;
    }
    
    @Test
    public void testPropertyExtractor() {
        final TagData localRoot = new TagData("ROOT", false, false);
        localRoot.getChildren().addListener(tagTestListener);
        
        final TagData localChild1 = new TagData("CHILD_1", false, false);
        localChild1.getChildren().addListener(tagTestListener);
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: add of 1 child to root");
        localRoot.getChildren().add(localChild1);
        Assert.assertEquals("Count of changes", 3, testChangeType.size());
        // 1. we have "ADDED" to the list
        Assert.assertEquals("1. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(0));
        // 2. we have "UPDATED" of the parent property
        Assert.assertEquals("2. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(1));
        // 3. we have "ADDED" to the list
        Assert.assertEquals("3. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(2));
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: setName of child #1");
        localChild1.setName("DUMMY");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
        Assert.assertEquals("Attribue has changed", "DUMMY", localChild1.getName());
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: resetName of child #1");
        // TFE, 20210428: WTF??? property extractor only triggers change event for the second change if property is read after setting it?!?!?!
        localChild1.setName("CHILD_1");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
        Assert.assertEquals("Attribue has changed", "CHILD_1", localChild1.getName());

        // and now for childs of childs
        final TagData localChildChild1 = new TagData("CHILD_CHILD_1", false, false);
        final TagData localChildChild2 = new TagData("CHILD_CHILD_2", false, false);
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: addAll of 2 childs to child #1");
        localChild1.getChildren().addAll(localChildChild1, localChildChild2);

        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: setName of child child #1");
        localChildChild1.setName("DUMMY");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
        Assert.assertEquals("Attribue has changed", "DUMMY", localChildChild1.getName());
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: resetName of child #1");
        localChildChild1.setName("CHILD_CHILD_1");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
        Assert.assertEquals("Attribue has changed", "CHILD_CHILD_1", localChildChild1.getName());
    }
    
    @Test
    public void testChildListenerLocal() {
        final TagData localRoot = new TagData("ROOT", false, false);
        localRoot.getChildren().addListener(tagTestListener);
        
        final TagData localChild1 = new TagData("CHILD_1", false, false);
        final TagData localChild2 = new TagData("CHILD_2", false, false);
        
        testChangeType.clear();
        System.out.println("testChildListenerLocal: addAll of 2 childs to root");
        localRoot.getChildren().addAll(localChild1, localChild2);
        Assert.assertEquals("Count of changes", 5, testChangeType.size());
        // 1. we have "ADDED" to the list
        Assert.assertEquals("1. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(0));
        // 2. we have "UPDATED" of the parent property
        Assert.assertEquals("2. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(1));
        // 3. we have "ADDED" to the list
        Assert.assertEquals("3. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(2));
        // 4. we have "UPDATED" of the parent property
        Assert.assertEquals("4. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(3));
        // 5. we have "ADDED" from the parent tag
        Assert.assertEquals("5. we have \"ADDED\" from the parent tag", ChangeType.ADDED, testChangeType.get(4));

        // change attributes
        testChangeType.clear();
        System.out.println("testChildListenerLocal: setName of root");
        localRoot.setName("DUMMY");
        // no listener => no change triggered
        Assert.assertTrue("No change listener", testChangeType.isEmpty());

        testChangeType.clear();
        System.out.println("testChildListenerLocal: setName of child #1");
        localChild1.setName("DUMMY");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
    }
    
    @Test
    public void testChildListenerManager() {
        TagManager.getInstance().addListChangeListener(tagTestListener);
        final TagData localRoot = TagManager.getInstance().createTagBelowParent("ROOT", null);
        
        final TagData localChild1 = TagManager.getInstance().createTagBelowParent("CHILD_1", localRoot);
        final TagData localChild2 = TagManager.getInstance().createTagBelowParent("CHILD_2", localRoot);
        
        testChangeType.clear();
        System.out.println("testChildListenerManager: addAll of 2 childs to root");
        localRoot.getChildren().addAll(localChild1, localChild2);
        Assert.assertEquals("Count of changes", 5, testChangeType.size());
        // 1. we have "ADDED" to the list
        Assert.assertEquals("1. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(0));
        // 2. we have "UPDATED" of the parent property
        Assert.assertEquals("2. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(1));
        // 3. we have "ADDED" to the list
        Assert.assertEquals("3. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(2));
        // 4. we have "UPDATED" of the parent property
        Assert.assertEquals("4. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(3));
        // 5. we have "ADDED" from the parent tag
        Assert.assertEquals("5. we have \"ADDED\" from the parent tag", ChangeType.ADDED, testChangeType.get(4));

        // change attributes
        testChangeType.clear();
        System.out.println("testChildListenerManager: setName of root");
        localRoot.setName("DUMMY");
        // no listener => no change triggered
        Assert.assertTrue("No change listener", testChangeType.isEmpty());

        testChangeType.clear();
        System.out.println("testChildListenerManager: setName of child #1");
        localChild1.setName("DUMMY");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
    }

    @Test
    public void testChildListenerReuse() {
        final TagData localRoot = new TagData("ROOT", false, false);
        localRoot.getChildren().addListener(tagTestListener);
        
        final TagData localChild1 = new TagData("CHILD_1", false, false);
        final TagData localChild2 = new TagData("CHILD_2", false, false);
        
        testChangeType.clear();
        System.out.println("testChildListenerReuse: addAll of 2 childs to root");
        localRoot.getChildren().addAll(localChild1, localChild2);
        Assert.assertEquals("Count of changes", 5, testChangeType.size());
        // 1. we have "ADDED" to the list
        Assert.assertEquals("1. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(0));
        // 2. we have "UPDATED" of the parent property
        Assert.assertEquals("2. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(1));
        // 3. we have "ADDED" to the list
        Assert.assertEquals("3. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(2));
        // 4. we have "UPDATED" of the parent property
        Assert.assertEquals("4. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(3));
        // 5. we have "ADDED" from the parent tag
        Assert.assertEquals("5. we have \"ADDED\" from the parent tag", ChangeType.ADDED, testChangeType.get(4));

        // re-use tagTestListener on first child
        final TagData firstChild = localRoot.getChildren().get(0);
        firstChild.getChildren().addListener(tagTestListener);

        final TagData localChildChild1 = new TagData("CHILD_CHILD_1", false, false);
        final TagData localChildChild2 = new TagData("CHILD_CHILD_2", false, false);
        
        testChangeType.clear();
        System.out.println("testChildListenerReuse: addAll of 2 childs to child #1");
        firstChild.getChildren().addAll(localChildChild1, localChildChild2);
        Assert.assertEquals("Count of changes", 5, testChangeType.size());
        // we have "ADDED" to the list
        Assert.assertEquals("1. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(0));
        // we have "UPDATED" of the parent property
        Assert.assertEquals("2. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(1));
        // we have "ADDED" to the list
        Assert.assertEquals("3. we have \"ADDED\" to the list", ChangeType.ADDED, testChangeType.get(2));
        // we have "UPDATED" of the parent property
        Assert.assertEquals("4. we have \"UPDATED\" of the parent property", ChangeType.UPDATED, testChangeType.get(3));
        // we have "ADDED" from the parent tag
        Assert.assertEquals("5. we have \"ADDED\" from the parent tag", ChangeType.ADDED, testChangeType.get(4));
        
        // and now try on the original child list
        testChangeType.clear();
        System.out.println("testChildListenerReuse: setName of child #1");
        firstChild.setName("DUMMY");
        Assert.assertEquals("Attribue has changed", ChangeType.UPDATED, testChangeType.get(0));
    }
}
