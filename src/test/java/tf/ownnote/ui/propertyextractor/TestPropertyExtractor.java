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
package tf.ownnote.ui.propertyextractor;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author thomas
 */
public class TestPropertyExtractor {
    private static ListChangeListener<PropertyHolder> testListener;
    private static ListChangeListener<PropertyHolder> testListener2;
    
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
        testListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends PropertyHolder> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        testChangeType.add(ChangeType.REMOVED.setListener("testListener"));
                    }
                    if (change.wasAdded()) {
                        testChangeType.add(ChangeType.ADDED.setListener("testListener"));
                    }
                    if (change.wasUpdated()) {
                        testChangeType.add(ChangeType.UPDATED.setListener("testListener"));
                    }
                }
            }
        };
        
        testListener2 = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends PropertyHolder> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        testChangeType.add(ChangeType.REMOVED.setListener("testListener2"));
                    }
                    if (change.wasAdded()) {
                        testChangeType.add(ChangeType.ADDED.setListener("testListener2"));
                    }
                    if (change.wasUpdated()) {
                        testChangeType.add(ChangeType.UPDATED.setListener("testListener2"));
                    }
                }
            }
        };
        
        testChangeType.addListener((ListChangeListener.Change<? extends ChangeType> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    System.out.println("Change event was removed: " + change.getRemoved() + " for listener " + change.getRemoved().get(0).getListener());
                }
                if (change.wasAdded()) {
                    System.out.println("Change event was added: " + change.getAddedSubList() + " for listener " + change.getAddedSubList().get(0).getListener());
                }
            }
        });
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    public void testPropertyExtractor() {
        final PropertyHolder localRoot = new PropertyHolder("ROOT");
        localRoot.getChildren().addListener(testListener);
        
        final PropertyHolder localChild1 = new PropertyHolder("CHILD_1");
        localChild1.getChildren().addListener(testListener);
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: add of 1 child to root");
        localRoot.getChildren().add(localChild1);
        Assertions.assertEquals(3, testChangeType.size(), "Count of changes");
        // 1. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "1. we have \"ADDED\" to the list");
        // 2. we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(1), "2. we have \"UPDATED\" of the parent property");
        // 3. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(2), "3. we have \"ADDED\" to the list");
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: setName of child #1");
        localChild1.setName("DUMMY");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("DUMMY", localChild1.getName(), "Attribue has changed");
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: resetName of child #1");
        localChild1.setName("CHILD_1");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("CHILD_1", localChild1.getName(), "Attribue has changed");

        // and now for childs of childs
        final PropertyHolder localChildChild1 = new PropertyHolder("CHILD_CHILD_1");
        final PropertyHolder localChildChild2 = new PropertyHolder("CHILD_CHILD_2");
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: addAll of 2 childs to child #1");
        localChild1.getChildren().addAll(localChildChild1, localChildChild2);
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: setName of child child #1");
        localChildChild1.setName("DUMMY");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("DUMMY", localChildChild1.getName(), "Attribue has changed");
        
        testChangeType.clear();
        System.out.println("testPropertyExtractor: resetName of child #1");
        localChildChild1.setName("CHILD_CHILD_1");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("CHILD_CHILD_1", localChildChild1.getName(), "Attribue has changed");
    }
    
    @Test
    public void testChildListener() {
        final PropertyHolder localRoot = new PropertyHolder("ROOT");
        
        localRoot.getChildren().addListener(testListener);
        
        final PropertyHolder localChild1 = new PropertyHolder("CHILD_1");
        final PropertyHolder localChild2 = new PropertyHolder("CHILD_2");
        
        testChangeType.clear();
        System.out.println("testChildListener: addAll of 2 childs to root");
        localRoot.getChildren().addAll(localChild1, localChild2);
        Assertions.assertEquals(5, testChangeType.size(), "Count of changes");
        // 1. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "1. we have \"ADDED\" to the list");
        // 2. we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(1), "2. we have \"UPDATED\" of the parent property");
        // 3. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(2), "3. we have \"ADDED\" to the list");
        // 4. we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(3), "4. we have \"UPDATED\" of the parent property");
        // 5. we have "ADDED" from the parent tag
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(4), "5. we have \"ADDED\" from the parent tag");

        // change attributes
        testChangeType.clear();
        System.out.println("testChildListener: setName of root");
        localRoot.setName("DUMMY");
        // no listener => no change triggered
        Assertions.assertTrue(testChangeType.isEmpty(), "No change listener");

        testChangeType.clear();
        System.out.println("testChildListener: setName of child #1");
        localChild1.setName("DUMMY");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("DUMMY", localChild1.getName(), "Attribue has changed");
    }

    @Test
    public void testChildListenerReuse() {
        final PropertyHolder localRoot = new PropertyHolder("ROOT");
        
        localRoot.getChildren().addListener(testListener);
        
        final PropertyHolder localChild1 = new PropertyHolder("CHILD_1");
        final PropertyHolder localChild2 = new PropertyHolder("CHILD_2");
        
        testChangeType.clear();
        System.out.println("testChildListenerReuse: addAll of 2 childs to root");
        localRoot.getChildren().addAll(localChild1, localChild2);
        Assertions.assertEquals(5, testChangeType.size(), "Count of changes");
        // 1. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "1. we have \"ADDED\" to the list");
        // 2. we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(1), "2. we have \"UPDATED\" of the parent property");
        // 3. we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(2), "3. we have \"ADDED\" to the list");
        // 4. we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(3), "4. we have \"UPDATED\" of the parent property");
        // 5. we have "ADDED" from the parent tag
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(4), "5. we have \"ADDED\" from the parent tag");

        // re-use testListener on first child
        final PropertyHolder firstChild = localRoot.getChildren().get(0);
        firstChild.getChildren().addListener(testListener);

        final PropertyHolder localChildChild1 = new PropertyHolder("CHILD_CHILD_1");
        final PropertyHolder localChildChild2 = new PropertyHolder("CHILD_CHILD_2");
        
        testChangeType.clear();
        System.out.println("testChildListenerReuse: addAll of 2 childs to child #1");
        firstChild.getChildren().addAll(localChildChild1, localChildChild2);
        Assertions.assertEquals(5, testChangeType.size(), "Count of changes");
        // we have "ADDED" to the list
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(0), "1. we have \"ADDED\" to the list");
        // we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(1), "2. we have \"UPDATED\" of the parent property");
        // we have "ADDED" to the list of children of firstChild
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(2), "3. we have \"ADDED\" to the list");
        // we have "UPDATED" of the parent property
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(3), "4. we have \"UPDATED\" of the parent property");
        // we have "ADDED" from the parent tag
        Assertions.assertEquals(ChangeType.ADDED, testChangeType.get(4), "5. we have \"ADDED\" from the parent tag");
        
        // and now try on the original child list
        testChangeType.clear();
        System.out.println("testChildListenerReuse: setName of child #1");
        firstChild.setName("DUMMY");
        Assertions.assertEquals(ChangeType.UPDATED, testChangeType.get(0), "Attribue has changed");
        Assertions.assertEquals("DUMMY", firstChild.getName(), "Attribue has changed");
    }
}
