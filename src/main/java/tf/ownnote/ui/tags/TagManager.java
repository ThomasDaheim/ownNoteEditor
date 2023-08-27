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

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import tf.helper.general.ObjectsHelper;
import tf.helper.xstreamfx.FXConverters;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.FileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.INoteCRMDS;
import tf.ownnote.ui.notes.Note;

/**
 * Load & save tag info to XML stored along with notes.
 * // https://nullbeans.com/configuring-xstream-to-convert-java-objects-to-and-from-xml/
 * 
 * @author thomas
 */
public class TagManager implements IFileChangeSubscriber, IFileContentChangeSubscriber, INoteCRMDS {
    private final static TagManager INSTANCE = new TagManager();
    
    private final static String TAG_DIR = File.separator + "MetaData";
    private final static String TAG_FILE = TAG_DIR + File.separator + "tag_info.xml";
    
    public final static String EXTERNAL_NAME_SEPARATOR = "~";

    // available colors for tabs to rotate through
    // issue #36 - have "All" without color
    // TF, 20170122: use colors similar to OneNote - a bit less bright
    //private static final String[] groupColors = { "darkseagreen", "cornflowerblue", "lightsalmon", "gold", "orchid", "cadetblue", "goldenrod", "darkorange", "MediumVioletRed", "lightpink", "skyblue" };
    private static final String[] groupColors = { "#F4A6A6", "#99D0DF", "#F1B87F", "#F2A8D1", "#9FB2E1", "#B4AFDF", "#D4B298", "#C6DA82", "#A2D07F", "#F1B5B5", "#ffb6c1", "#87ceeb" };
    
    // reserved names for tags - can't be moved or edited
    // allow for hierarchy in reserved names to also have ALL, NOT_GROUPED, ARCHIVE here as well...
    public enum ReservedTag {
        Groups("Groups", true, false, null, 0),
        All("All", true, false, Groups, 0),
        NotGrouped("Not grouped", true, false, Groups, 1),
        Archive("Archive", true, true, Groups, -1);
        
        private final String myTagName;
        private final boolean isGroup;
        private final boolean isArchiveGroup;
        private final ReservedTag myParent;
        private final int myPosition;
        private TagData myTag;

        ReservedTag(final String name, final boolean group, final boolean archivegroup, final ReservedTag parent, final int position) {
            myTagName = name;
            isGroup = group;
            isArchiveGroup = archivegroup;
            myParent = parent;
            myPosition = position;
            
            if (isArchiveGroup && !isGroup) {
                System.err.println("Bummer! Archive group and not a group???");
            }
        }

        public String getTagName() {
            return myTagName;
        }
        
        public boolean isGroup() {
            return isGroup;
        }
        
        public boolean isArchiveGroup() {
            return isArchiveGroup;
        }
        
        public ReservedTag getParent() {
            return myParent;
        }
        
        public int getPosition() {
            return myPosition;
        }
        
        public TagData getTag() {
            // TFE, 20230411: We might not yet have loaded the tags! Happens e.g. on first call on new machine...
            TagManager.getInstance().initTags();
            return myTag;
        }

        public void setTag(final TagData tag) {
            myTag = tag;
        }
        
        public static boolean containsName(final String name) {
            return reservedTagNameMap.keySet().contains(name);
        }
        
        public static boolean containsTag(final TagData tag) {
            boolean result = false;
            
            for (ReservedTag value : values()) {
                if (value.getTag().equals(tag)) {
                    result = true;
                    break;
                }
            }
            
            return result;
        }
    }
    private final static Map<String, ReservedTag> reservedTagNameMap = EnumUtils.getEnumMap(ReservedTag.class);
    
    public enum IconSize {
        NORMAL("1.166667em"),
        LARGE("28px");
        
        private final String iconSize;
        
        private IconSize(final String size) {
            iconSize = size;
        }
        
        public final String getSize() {
            return iconSize;
        }
    }
    
    public enum TagCompare {
        BY_NAME,
        BY_EXTERNAL_NAME,
        BY_IDENTITY
    }
    
    private enum LoadingState {
        NOT_LOADED,
        LOADING,
        LOADED;
    }
    private LoadingState loadingState = LoadingState.NOT_LOADED;
    
    protected final static String ROOT_TAG_NAME = "Tags";
    // root of all tags - not saved or loaded
    private final static TagData ROOT_TAG = new TagData(ROOT_TAG_NAME, false, false);
    
    // TFE, 20210405: hold list of group tags with an extractor - to be able to listen to changes of tag content
    private final ObservableList<TagData> groupTags = 
            FXCollections.<TagData>observableArrayList(p -> new Observable[]{p.nameProperty(), p.iconNameProperty(), p.colorNameProperty()});
    
    // we need to keep track of changes in the tag tree to update the group tag list....
    private final ListChangeListener<TagData> tagChildrenListener;
    // know thy listeners - to able to add / remove from added / removed tags
    private final List<ListChangeListener<? super TagData>> tagChildrenChangeListeners = new ArrayList<>();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private TagManager() {
        super();
        // Exists only to defeat instantiation.
        
        tagChildrenListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TagData> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (TagData tag : change.getRemoved()) {
                            // can't check for isGroupsChildTag(tag) anymore since it has already been removed from the parent...
//                            System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was removed from group tags");
                            groupTags.remove(tag);
                        }
                    }
                    if (change.wasAdded()) {
                        for (TagData tag : change.getAddedSubList()) {
                            if (tag.isGroup() && !isGroupsRootTag(tag) && !groupTags.contains(tag)) {
//                                System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was added to group tags");
                                groupTags.add(tag);
                            }
                        }
                    }
                    if (change.wasUpdated()) {
                        for (TagData tag : change.getList().subList(change.getFrom(), change.getTo())) {
//                            System.out.println("Tag " + tag.getId() + ", " + tag.getName() + " was updated");
                        }
                    }
                }
                if (LoadingState.LOADED.equals(loadingState)) {
                    // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
                    setTagTransientData();
                }
            }
        };
        tagChildrenChangeListeners.add(tagChildrenListener);
    }

    public static TagManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        // now we can register everywhere
        FileManager.getInstance().subscribe(INSTANCE);
        myEditor.getNoteEditor().subscribe(INSTANCE);
    }

    private void initTags() {
        if (LoadingState.NOT_LOADED.equals(loadingState)) {
            // lazy loading
            loadTags();

            // we want to listen to everything as well
            doAddAllListener(ROOT_TAG);
        }
    }
    
    public final TagData getRootTag() {
        initTags();
        // you can use but not change
        return ROOT_TAG;
    }

    public ObservableList<TagData> getGroupTags(final boolean useHierarchy) {
        initTags();
        if (!useHierarchy) {
            return ReservedTag.Groups.getTag().getChildren();
        } else {
            return groupTags;
        }
    }
    
    // helper methods to check for All, Not Grouped 
    public static boolean isSpecialGroup(final TagData group) {
        return ((isNotGrouped(group) || ReservedTag.All.getTag().equals(group)));
    }
    
    public static boolean isNotGrouped(final TagData group) {
        return (group == null) || ReservedTag.NotGrouped.getTag().equals(group);
    }
    
    public static boolean isSameGroup(final TagData group1, final TagData group2) {
        assert group1 != null;
        assert group2 != null;
        // either both are equal or both are part of "Not grouped"
        return (group1.equals(group2) || isNotGrouped(group1) && isNotGrouped(group2));
    }
    
    public boolean isValidChangedTagName(final String newTagName, final TagData tag) {
        // combine all checks that need to be done for a changed tag name of an existing tag
        
        boolean result;
        
        if (tag.isGroup()) {
            // tag is group
            // is valid as group name
            // is unique under siblings
            final TagData nameTag = tagForName(newTagName, tag.getParent(), false, false);
            result = (FormatHelper.VALIDNOTEGROUPNAME.test(newTagName) && 
                    // if we have a tag with that name it must be this one
                    (nameTag == null || tag.equals(nameTag)));
        } else {
            // tag is no group
            // is unqiue overall
            final TagData nameTag = tagForName(newTagName, getRootTag(), false, true);
                    // if we have a tag with that name it must be this one
            result = (nameTag == null || tag.equals(nameTag));
        }

        return result;
    }
    
    public boolean isValidNewTagName(final String newTagName, final TagData parent) {
        // combine all checks that need to be done for a new tag name of an new tag
        
        boolean result;
        
        if (parent.isGroup()) {
            // tag is group
            // is valid as group name
            // is unique under siblings
            result = FormatHelper.VALIDNOTEGROUPNAME.test(newTagName);
            if (result) {
                final TagData nameTag = tagForName(newTagName, parent, false, false);
                result = // we must not have a tag with that name with the same parent
                        (nameTag == null || !parent.equals(nameTag.getParent()));
            }
        } else {
            // tag is no group
            // is unqiue overall
            final TagData nameTag = tagForName(newTagName, getRootTag(), false, true);
                    // we must not have a tag with that name AND it musn't be the dragged tag
            result = (nameTag == null);
        }

        return result;
    }
    
    public boolean isValidNewTagParent(final TagData tag, final TagData parent) {
        // combine all checks that need to be done for a new tag name of an new tag
        assert tag != null;
        
        boolean result = true;
        
        final String tagName = tag.getName();
        TagData nameTag = null;
        if (parent.isGroup()) {
            // tag is group
            // is valid as group name
            // is unique under siblings
            result = FormatHelper.VALIDNOTEGROUPNAME.test(tagName);
            if (result) {
                nameTag = tagForName(tagName, parent, false, false);
            }
        } else {
            // tag is no group
            // is unqiue overall
            nameTag = tagForName(tagName, getRootTag(), false, true);
        }
        result = result &&
                // we can't have groups and non-groups mixed
                (tag.isGroup().equals(parent.isGroup())) &&
                // we must not have a tag with that name with the same parent - except for ourselves
                (nameTag == null || tag.equals(nameTag));

        return result;
    }
    
    public void resetTagList() {
        if (LoadingState.LOADED.equals(loadingState)) {
            doRemoveAllListener(ROOT_TAG);
            ROOT_TAG.getChildren().clear();
            loadingState = LoadingState.NOT_LOADED;
        }
    }
    
    private void loadTags() {
        loadingState = LoadingState.LOADING;
        
        final TagData xstreamRoot = new TagData("xstream-root", false, false);
        
        final String fileName = FileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            // load from xml AND from current metadata
            final XStream xstream = new XStream(new PureJavaReflectionProvider(), new DomDriver("UTF-8"));
            xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
            final Class<?>[] classes = new Class[] { 
                TagData.class, 
                ObservableListWrapper.class, 
                ObservableSetWrapper.class, 
                SimpleBooleanProperty.class, 
                SimpleStringProperty.class,
                SimpleObjectProperty.class};
            xstream.allowTypes(classes);

            FXConverters.configure(xstream);

            xstream.alias("taginfo", TagData.class);
            xstream.alias("listProperty", ObservableListWrapper.class);
            xstream.alias("setProperty", ObservableSetWrapper.class);
            xstream.alias("booleanProperty", SimpleBooleanProperty.class);
            xstream.alias("stringProperty", SimpleStringProperty.class);
            xstream.alias("objectProperty", SimpleObjectProperty.class);

            xstream.aliasField("id", TagData.class, "myId");
            xstream.aliasField("name", TagData.class, "nameProperty");
            xstream.aliasField("iconName", TagData.class, "iconNameProperty");
            xstream.aliasField("colorName", TagData.class, "colorNameProperty");
            
            xstream.omitField(TagData.class, "linkedNotes");
            xstream.omitField(TagData.class, "parentProperty");
            // TFE, 20201220: we had that in for a while
            xstream.omitField(TagData.class, "fixedProperty");
            xstream.omitField(TagData.class, "isGroupProperty");
            xstream.omitField(TagData.class, "isArchivedGroupProperty");

            try (
                BufferedInputStream stdin = new BufferedInputStream(new FileInputStream(fileName));
                Reader reader = new InputStreamReader(stdin, "ISO-8859-1");
            ) {
                // TFE, 20210507: can't read into real root tag since xstream deserialization messes something up with the properties
                // and that stops change events from being registered and provided via property extractors
                xstreamRoot.setChildren(ObjectsHelper.uncheckedCast(xstream.fromXML(reader)));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // deep copy tags over to real root
        ROOT_TAG.setChildren(xstreamRoot.cloneMe().getChildren());
        
        // ensure reserved names are fixed
        for (ReservedTag reservedTag : ReservedTag.values()) {
            final String tagName = reservedTag.getTagName();
            TagData tag = tagForName(tagName, reservedTag.getParent() == null ? null : reservedTag.getParent().getTag(), false, false);
            if (tag == null) {
                System.out.println("No tag for reserved name " + tagName + " found. Initializing...");

                // TFE, 20220602: Group has no parent reserved tag
                TagData parent = ROOT_TAG;
                if (reservedTag.getParent() != null) {
                    parent = reservedTag.getParent().getTag();
                }
                tag = createTagWithParent(
                        tagName, 
                        reservedTag.isGroup(), 
                        reservedTag.isArchiveGroup(), 
                        parent);
                if (reservedTag.getPosition() != -1) {
                    parent.getChildren().add(reservedTag.getPosition(), tag);
                } else {
                    parent.getChildren().add(tag);
                }
            }
            // TFE, 20220421: store special tags in the enum and don't use any separate lists...
            reservedTag.setTag(tag);
            // init group flag since not stored in file
            tag.setIsGroup(reservedTag.isGroup);
            
//            if (ReservedTagName.Groups.name().equals(tag.getName())) {
//                ReservedTagName.Groups.setTag(tag);
//                // store for later use
//                groupTags.setAll(tag.getChildren().stream().map((t) -> {
//                    return t.flattened();
//                }).flatMap(Function.identity()).collect(Collectors.toList()));
//                
//                boolean hasAllGroups = false;
//                boolean hasNotGrouped = false;
//                
//                // color my children
//                for (TagData tagChild : tag.getChildren()) {
//                    final String tagChildName = tagChild.getName();
//                    
//                    initGroupTag(tagChild);
//                    
//                    if (ALL_GROUPS_NAME.equals(tagChildName)) {
//                        ALL_GROUPS = tagChild;
//                        hasAllGroups = true;
//                    }
//                    if (NOT_GROUPED_NAME.equals(tagChildName)) {
//                        NOT_GROUPED = tagChild;
//                        hasNotGrouped = true;
//                    }
//                }
//                
//                if (!hasAllGroups) {
//                    // all groups is always the first entry
//                    System.out.println("No tag for group " + ALL_GROUPS_NAME + " found. Adding...");
//                    ALL_GROUPS = createTagBelowParentAtPosition(ALL_GROUPS_NAME, true, 0, tag);
//                }
//                if (!hasNotGrouped) {
//                    // all groups is always the second entry
//                    System.out.println("No tag for group " + NOT_GROUPED_NAME + " found. Adding...");
//                    NOT_GROUPED = createTagBelowParentAtPosition(NOT_GROUPED_NAME, true, 1, tag);
//                }
//            }
        }
        
        // store for later use
        groupTags.setAll(ReservedTag.Groups.getTag().getChildren().stream().map((t) -> {
            return t.flattened();
        }).flatMap(Function.identity()).collect(Collectors.toList()));

        // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
        setTagTransientData();
        
        // TFE, 20220619: delay setting loaded until all change events from the various setParents from setTagTransientData() are done
        // more precise: we should have a counter on the setParent calls here: increase in backlinkParent decrease in tagChildrenListener = new ListChangeListener<>()
        // til we're down to zero before processing change events
        if (Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                loadingState = LoadingState.LOADED;
            });
        } else {
            loadingState = LoadingState.LOADED;
        }
    }
    
    // TFE, 20220404: allow hierarchical group tags - now we need to keep track of each tags position in the hierarchy
    private static void setTagTransientData() {
        // backlink all parents recursively
        backlinkParent(ROOT_TAG);

        ROOT_TAG.setIsGroup(false);
        ROOT_TAG.setIsArchivedGroup(false);
        ROOT_TAG.setLevel(0);
        
        // set all reserved names back to their know values
        // who knows what the ugly world out there has done to our little tags...
        for (ReservedTag tag: ReservedTag.values()) {
            tag.getTag().setIsGroup(tag.isGroup());
            tag.getTag().setIsArchivedGroup(tag.isArchiveGroup());
        }
        
        
        // do the recursion
        setChildTagsTransientData(ROOT_TAG);
    }
    private static void setChildTagsTransientData(final TagData parent) {
        final int level = parent.getLevel() + 1;
        for (TagData tag: parent.getChildren()) {
            // all tags inherit the group status from their parents - except the "Group" and all the other reserved tags...
            if (!ReservedTag.containsName(tag.getName())) {
                tag.setIsGroup(parent.isGroup());
            }
            // all tags inherit the archive group status from their parents - except the "Group" and all the other reserved tags...
            if (!ReservedTag.containsName(tag.getName())) {
                tag.setIsArchivedGroup(parent.isArchivedGroup());
            }
            tag.setLevel(level);
            // do the recursion
            setChildTagsTransientData(tag);
        }
    }

    private static void backlinkParent(final TagData tag) {
        for (TagData childTag : tag.getChildren()) {
            childTag.setParent(tag);
            backlinkParent(childTag);
        }
    }
    
    protected void addListChangeListener(ListChangeListener<? super TagData> ll) {
        tagChildrenChangeListeners.add(ll);
        doAddListChangeListener(getRootTag(), ll);
    }
    private void doAddListChangeListener(final TagData tagRoot, ListChangeListener<? super TagData> ll) {
        // add listener to my children and to the children of my children
//        System.out.println("Adding listener " + ll + " to tag " + tagRoot.getName());
        tagRoot.getChildren().addListener(ll);
        for (TagData tag : tagRoot.getChildren()) {
            doAddListChangeListener(tag, ll);
        }
    }
    private void doAddAllListener(final TagData tagRoot) {
        // add all listeners to the children
        for (ListChangeListener<? super TagData> ll : tagChildrenChangeListeners) {
            doAddListChangeListener(tagRoot, ll);
        }
    }
    
    public void removeListener(ListChangeListener<? super TagData> ll) {
        tagChildrenChangeListeners.remove(ll);
        doRemoveListener(getRootTag(), ll);
    }
    private void doRemoveListener(final TagData tagRoot, ListChangeListener<? super TagData> ll) {
        // remove listener from my children and from the children of my children
//        System.out.println("Removing listener " + ll + " from tag " + tagRoot.getName());
        tagRoot.getChildren().removeListener(ll);
        for (TagData tag : tagRoot.getChildren()) {
            doRemoveListener(tag, ll);
        }
    }
    private void doRemoveAllListener(final TagData tagRoot) {
        // add all listeners to the children
        for (ListChangeListener<? super TagData> ll : tagChildrenChangeListeners) {
            doRemoveListener(tagRoot, ll);
        }
    }
    
    public void processTagNameChange(final TagData tag, final String oldValue, final String newValue) {
        // nothing to do here! we already have renameTag!
    }

    public void saveTags() {
        if (ROOT_TAG.getChildren().isEmpty()) {
            // list hasn't been initialized yet - so don't try to save
            return;
        }

        try {
            FileUtils.forceMkdir(new File(FileManager.getInstance().getNotesPath() + TAG_DIR));
        } catch (IOException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        final String fileName = FileManager.getInstance().getNotesPath() + TAG_FILE;
        final File file = new File(fileName);
        if (file.exists() && (file.isDirectory() || !file.canWrite())) {
            return;
        }
        
        // save to xml
        final XStream xstream = new XStream(new DomDriver("UTF-8"));
        xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
        final Class<?>[] classes = new Class[] { 
            TagData.class, 
            ObservableListWrapper.class, 
            ObservableSetWrapper.class, 
            SimpleBooleanProperty.class, 
            SimpleStringProperty.class,
            SimpleObjectProperty.class};
        xstream.allowTypes(classes);

        FXConverters.configure(xstream);

        xstream.alias("taginfo", TagData.class);
        xstream.alias("listProperty", ObservableListWrapper.class);
        xstream.alias("setProperty", ObservableSetWrapper.class);
        xstream.alias("booleanProperty", SimpleBooleanProperty.class);
        xstream.alias("stringProperty", SimpleStringProperty.class);
        xstream.alias("objectProperty", SimpleObjectProperty.class);

        xstream.aliasField("id", TagData.class, "myId");
        xstream.aliasField("name", TagData.class, "nameProperty");
        xstream.aliasField("iconName", TagData.class, "iconNameProperty");
        xstream.aliasField("colorName", TagData.class, "colorNameProperty");
        
        xstream.omitField(TagData.class, "levelProperty");
        xstream.omitField(TagData.class, "linkedNotes");
        xstream.omitField(TagData.class, "parentProperty");
        xstream.omitField(TagData.class, "isGroupProperty");
        xstream.omitField(TagData.class, "isArchivedGroupProperty");

        try (
            BufferedOutputStream stdout = new BufferedOutputStream(new FileOutputStream(fileName));
            Writer writer = new OutputStreamWriter(stdout, "ISO-8859-1");
        ) {
            PrettyPrintWriter printer = new PrettyPrintWriter(writer, new char[]{'\t'});
            writer.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + System.getProperty("line.separator"));
        
            xstream.marshal(ROOT_TAG.getChildren(), printer);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final Note note, final String oldContent, final String newContent) {
        return true;
    }
    
    public void groupsToTags() {
        // check all notes if they already have a tag with their group name
        // if not add such a tag
        // probably only used once for "migration" to tag metadata
        for (Note note : FileManager.getInstance().getNotesList()) {
            // lets not store a "Not grouped" tag...
            if (!isNotGrouped(note.getGroup())) {
                final TagData groupInfo = note.getGroup();
                if (note.getMetaData().getTags().contains(groupInfo)) {
                    System.out.println("Removing tag " + note.getGroupName() + " from note " + note.getNoteName());
                    FileManager.getInstance().readNote(note, false);
                    note.getMetaData().getTags().remove(groupInfo);
                    FileManager.getInstance().saveNote(note);
                } else {
                    System.out.println("Adding tag " + note.getGroupName() + " to note " + note.getNoteName());
                    FileManager.getInstance().readNote(note, false);
                    note.getMetaData().getTags().add(groupInfo);
                    FileManager.getInstance().saveNote(note);
                }
            }
        }
    }
    
    public TagData groupForName(final String groupName, final boolean createIfNotFound) {
        initTags();
        // start searching under groups only
        return tagForName(groupName.isEmpty() ? ReservedTag.NotGrouped.getTagName() : groupName, ReservedTag.Groups.getTag(), createIfNotFound, false);
    }
    
    public TagData groupForExternalName(final String groupName, final boolean createIfNotFound) {
        initTags();
        // start searching under groups only
        return tagForExternalName(groupName.isEmpty() ? ReservedTag.NotGrouped.getTagName() : groupName, ReservedTag.Groups.getTag(), createIfNotFound);
    }
    
    public TagData tagForExternalName(
            final String tagName, 
            final TagData parent, 
            final boolean createIfNotFound) {
        initTags();
        
        // special case: no group named passed at all
        if (tagName.isEmpty()) {
            return tagForName(ReservedTag.NotGrouped.getTagName(), ReservedTag.Groups.getTag(), createIfNotFound, false);
        }

//        System.out.println("tagForExternalName: " + tagName);
        // respect hierarchy in group names
        final String[] tagNames = tagName.split(EXTERNAL_NAME_SEPARATOR);
        
        TagData startTag = parent;
        if (parent == null) {
            startTag = getRootTag();
        }
        for (String name : tagNames) {
            // skip empty entries in list
            if (!name.isEmpty()) {
//                System.out.println("  looking for: " + name);
                // look only on this level and not all the way down the subtree...
                startTag = tagForName(name, startTag, createIfNotFound, false, TagCompare.BY_NAME);
                if (startTag == null) {
                    // we didn't find one in the hierarchy - so lets get out of here
                    break;
                }
            }
        }
        
        return startTag;
    }

    public Set<TagData> tagsForExternalNames(
            final Set<String> tagNames, 
            final TagData parent, 
            final boolean createIfNotFound) {
        final Set<TagData> result = new HashSet<>();
        
        for (String tagName : tagNames) {
            result.add(tagForExternalName(tagName, parent, createIfNotFound));
        }
        
        return result;
    }

    protected TagData tagForName(
            final String tagName, 
            final TagData parent, 
            final boolean createIfNotFound, 
            final boolean includeHierarchy) {
        // TFE, 20220605: previous default - search by name over the whole tree of siblings
        final Set<TagData> tags = tagsForNames(new HashSet<>(Arrays.asList(tagName)), parent, createIfNotFound, includeHierarchy, TagCompare.BY_NAME);
        
        if (tags.isEmpty()) {
            return null;
        } else {
            return tags.iterator().next();
        }
    }

    protected TagData tagForName(
            final String tagName, 
            final TagData parent, 
            final boolean createIfNotFound, 
            final boolean includeHierarchy,
            // TFE, 20220506: life gets more complicated
            final TagCompare mode) {
        final Set<TagData> tags = tagsForNames(new HashSet<>(Arrays.asList(tagName)), parent, createIfNotFound, includeHierarchy, mode);
        
        if (tags.isEmpty()) {
            return null;
        } else {
            return tags.iterator().next();
        }
    }

    public Set<TagData> tagsForNames(
            final Set<String> tagNames, 
            final TagData parent, 
            final boolean createIfNotFound, 
            final boolean includeHierarchy) {
        return tagsForNames(tagNames, parent, createIfNotFound, includeHierarchy, TagCompare.BY_NAME);
    }

    public Set<TagData> tagsForNames(
            final Set<String> tagNames, 
            final TagData parent, 
            final boolean createIfNotFound, 
            final boolean includeHierarchy,
            // TFE, 20220506: live gets more complicated
            final TagCompare mode) {
        initTags();
        final Set<TagData> result = new HashSet<>();
        
        final TagData realStartTag = (parent != null) ? parent : getRootTag();

        final Set<TagData> flatTags = new HashSet<>();
        // TFE, 20220404: start tag might be the one we're looking for - not necessarily a child
        flatTags.add(realStartTag);

        if (includeHierarchy) {
            // flatten tagslist to set
            // http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/
            // https://stackoverflow.com/a/31992391
            flatTags.addAll(realStartTag.getChildren().stream().map((t) -> {
                return t.flattened();
            }).flatMap(Function.identity()).collect(Collectors.toSet()));
        } else {
            // inly use direct children
            flatTags.addAll(realStartTag.getChildren());
        }

        for (String tagName : tagNames) {
            final Optional<TagData> tag = flatTags.stream().filter((t) -> {
                return compareTags(t, tagName, mode);
            }).findFirst();
            
            if (tag.isPresent()) {
//                System.out.println("    found: " + tagName);
                result.add(tag.get());
            } else {
                // we didn't run into that one before...
                if (createIfNotFound) {
                    if (!isValidNewTagName(tagName, realStartTag)) {
                        System.err.println("Can't create new tag with invalid name: " + tagName);
                    } else {
//                        System.out.println("    creating: " + tagName + " under " + realStartTag.getName());
                        final TagData newTag = createTagWithParent(tagName, realStartTag);
                        realStartTag.getChildren().add(newTag);
                        result.add(newTag);
                        
                        if (LoadingState.LOADING.equals(loadingState)) {
                            // still initializing things - change listern not yet active
                            setChildTagsTransientData(realStartTag);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    public TagData getComplementaryGroup(final TagData tag, final boolean createIfNotFound) {
        assert tag != null;
        assert tag.isGroup();
        assert !ReservedTag.containsTag(tag);
        
        TagData result;
        
        if (!tag.isArchivedGroup()) {
            // we want to have the same name - but under archive BUT with the same hierarchy
            final String compTagName = ReservedTag.Archive.getTagName() + EXTERNAL_NAME_SEPARATOR + tag.getExternalName();
            result = groupForExternalName(compTagName, false);
            if (result == null && createIfNotFound) {
                result = groupForExternalName(compTagName, createIfNotFound);
                if (result != null) {
                    // new archive tag - should look the same
                    result.setIconName(tag.getIconName());
                    result.setColorName(tag.getColorName());
                }
            }
        } else {
            // we want to have the group with the same external name minus the "Archive~" part in the beginning
            String compTagName = tag.getExternalName().replace(ReservedTag.Archive.getTagName() + EXTERNAL_NAME_SEPARATOR, "");
            if (compTagName.isEmpty()) {
                // wasn't attached to a specific group before...
                result = ReservedTag.NotGrouped.getTag();
            } else {
                result = groupForExternalName(compTagName, createIfNotFound);
            }
            
        }
        
        return result;
    }
    
    public boolean renameTag(final TagData tag, final String newName) {
        assert tag != null;
        
        if (tag.getName().equals(newName)) {
            // nothing to do
            return true;
        }
        // can't rename a group to ALL or NOT_GROUPED
        if (tag.isGroup() && isSpecialGroup(tag)) {
            return false;
        }

        boolean result = true;
        final String oldName = tag.getName();

        // if groupTag rename group (and with it note file) as well
        if (tag.isGroup()) {
            final String newGroupName = newName != null ? newName : ReservedTag.NotGrouped.getTagName();
            result = FileManager.getInstance().renameGroup(tag, newGroupName);

            if (result) {
                //check if we just moved the current note in the editor...
                if (myEditor != null && myEditor.getNoteEditor().getEditedNote() != null && myEditor.getNoteEditor().getEditedNote().getGroupName().equals(oldName)) {
                    // TODO: is this still necessary?
//                    myEditor.getNoteEditor().doNameChange(oldName, newGroupName, 
//                            myEditor.getNoteEditor().getEditedNote().getNoteName(), myEditor.getNoteEditor().getEditedNote().getNoteName());
                }
            } else {
                if (newName != null) {
                    // error message - most likely note in new group with same name already exists
                    myEditor.showAlert(Alert.AlertType.ERROR, "Error Dialog", "An error occured while renaming the group.", "A file in the new group has the same name as a file in the old.");
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    myEditor.showAlert(Alert.AlertType.ERROR, "Error Dialog", "An error occured while deleting the group.", "An ungrouped file has the same name as a file in this group.");
                }

                return false;
            }
        }
        
        final List<Note> changedNotes = new ArrayList<>(tag.getLinkedNotes());
        
        // now we have loaded all notes, time to rename/remove the tag...
        if (newName == null) {
            for (Note note : changedNotes) {
                note.getMetaData().getTags().remove(tag);
            }
            // go through tag tree an delete tag
            if (tag.getParent() != null) {
                tag.getParent().getChildren().remove(tag);

                // remove all listeners from the children
                doRemoveAllListener(tag);
            }
        } else {
            tag.setName(newName);
        }
        // save the notes (except for the one currently in the editor
        for (Note note : changedNotes) {
            if (!note.equals(myEditor.getEditedNote())) {
                FileManager.getInstance().saveNote(note);
            } else {
                // tell the world, the note metadata has changed (implicitly)
                // so that any interesting party can listen to the change
                note.setUnsavedChanges(true);
            }
        }
        
        return result;
    }
    
    protected TagData createTagWithParent(final String name, final TagData parent) {
        if (parent != null) {
            return createTagWithParent(name, parent.isGroup(), parent.isArchivedGroup(), parent);
        } else {
            // this should only happen for testing edge cases
            return createTagWithParent(name, false, false, parent);
        }
    }

    protected TagData createTagWithParent(final String name, final boolean isGroup, final boolean isArchivedGroup, final TagData parent) {
        // this should only be used directly for testing edge cases
        // does it already exist?
        TagData result = tagForName(name, parent, false, false);
        
        if (result != null && ((result.isGroup() != isGroup) || (result.isArchivedGroup() != isArchivedGroup))) {
            // tag is there but not what shoud be created!
            Logger.getLogger(TagManager.class.getName()).log(Level.SEVERE, "Trying to create tag {0} that already exists but of wrong type: {1}", new Object[]{name, !isGroup});
            return null;
        }
        if (result == null) {
            result = new TagData(name, isGroup, isArchivedGroup);
            // add all listeners to the children
            doAddAllListener(result);

            // TFE, 20220602: during bootstrap this might be the "Groups" tag itself!
            if (isGroup) {
                initGroupTag(result);
            }
        }

        return result;
    }
    
    public boolean deleteTag(final TagData tag) {
        assert tag != null;
        
        // reuse existing rename method
        return renameTag(tag, null);
    }
    
    private void initGroupTag(final TagData tag) {
        if (tag.getColorName() == null || tag.getColorName().isEmpty()) {
            tag.setColorName(getGroupColor(tag));
        }
    }

    // TF, 20160703: to support coloring of notes table view for individual notes
    // TF, 20170528: determine color from groupname for new colors
    private String getGroupColor(final TagData groupTag) {
        String groupColor = "";

        final int groupIndex = getGroupTags(true).indexOf(groupTag);

        // TF, 20170122: "All" & "Not grouped" have their own colors ("darkgrey", "lightgrey"), rest uses list of colors
        switch (groupIndex) {
            case 0: 
                groupColor = "darkgrey";
                break;
            case 1: 
                groupColor = "lightgrey";
                break;
            case -1:
                // no color found via tag or group - must be new
                groupColor = groupColors[getGroupTags(true).size() % groupColors.length];
                break;
            default: 
                groupColor = groupColors[groupIndex % groupColors.length];
                break;
        }
//                System.out.println("Found group: " + groupTag + " as number: " + groupIndex + " color: " + groupColor);
        
        return groupColor;
    }
    
    public static boolean isGroupsRootTag(final TagData tag) {
        if (ReservedTag.Groups.getTag() == null) {
            return false;
        }
        
        return ReservedTag.Groups.getTag().equals(tag);
    }
    
    public static boolean isArchiveRootTag(final TagData tag) {
        if (ReservedTag.Archive.getTag() == null) {
            return false;
        }
        
        return ReservedTag.Archive.getTag().equals(tag);
    }
    
    public static boolean isEditableTag(final TagData tag) {
        // editable: currently same as NOT fixed
        return !isFixedTag(tag);
    }

    public static boolean isFixedTag(final TagData tag) {
        // fixed: "Groups", "All", "Not grouped", "Archive" tags
        return ReservedTag.containsTag(tag);
    }
    
    public static boolean childAllowed(final TagData tag) {
        // no siblings: All, NotGrouped
        return (!ReservedTag.All.getTag().equals(tag) && !ReservedTag.NotGrouped.getTag().equals(tag));
    }
    
    public boolean compareTagsHierarchy(final TagData tag, final TagData check, final TagCompare compareMode, final boolean includeHierarchy) {
        assert tag != null;
        assert check != null;
        
        boolean result = compareTags(tag, check, compareMode);
        
        if (!result && includeHierarchy) {
            // lets do the hierarchy
            for (TagData child : tag.getChildren()) {
                if (compareTagsHierarchy(child, check, compareMode, includeHierarchy)) {
                    // found it - lets get out here
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
    
    public boolean compareTags(final TagData tag, final TagData check, final TagCompare compareMode) {
        assert tag != null;
        assert check != null;

        boolean result = false;
        
        switch (compareMode) {
            case BY_NAME -> result = tag.getName().equals(check.getName());
            case BY_EXTERNAL_NAME -> result = tag.getExternalName().equals(check.getExternalName());
            case BY_IDENTITY -> result = tag.equals(check);
        }
        
//        System.out.println("Comparing " + group.getId() + ", " + group.getExternalName() + " to " +  check.getId() + ", " + check.getExternalName() + " with result " + result);
        
        return result;
    }
    
    public boolean compareTags(final TagData tag, final String name, final TagCompare compareMode) {
        assert tag != null;
        assert name != null;

        boolean result = false;
        
        switch (compareMode) {
            case BY_NAME -> result = tag.getName().equals(name);
            case BY_EXTERNAL_NAME -> result = tag.getExternalName().equals(name);
            // definitly not the same...
            case BY_IDENTITY -> result = false;
        }
        
//        System.out.println("Comparing " + group.getId() + ", " + group.getExternalName() + " to " +  check.getId() + ", " + check.getExternalName() + " with result " + result);
        
        return result;
    }
    
    @Override
    public boolean createNote(final TagData newGroup, final String newNoteName) {
        // this is called after the fact that a new note has been created
        // so we only need to back-link notes to groups
        final Note newNote = FileManager.getInstance().getNote(newGroup, newNoteName);
        newNote.getGroup().getLinkedNotes().add(newNote);
        ReservedTag.All.getTag().getLinkedNotes().add(newNote);

        return true;
    }

    @Override
    public boolean renameNote(final Note curNote, final String newValue) {
        return true;
    }

    @Override
    public boolean moveNote(final Note curNote, final TagData newGroup) {
        // we have been called after the fact... so the linkedNotes have already been updated with the new group name
        final Note movedNote = FileManager.getInstance().getNote(newGroup, curNote.getNoteName());

        final TagData oldGroup = curNote.getGroup();

        // TFE, 20220424: check if this changes the archiving status!
        if (!oldGroup.isArchivedGroup().equals(movedNote.getGroup().isArchivedGroup())) {
            // TODO: what ever needs to be done here...
            if (!oldGroup.isArchivedGroup()) {
                
            } else {
                
            }
        }

        // TFE, 20201227: allign tags and their note links as well...
        if (movedNote.getMetaData().getTags().contains(oldGroup)) {
            movedNote.getMetaData().getTags().remove(oldGroup);
            movedNote.getMetaData().getTags().add(newGroup);
        } else {
            // need to manually update the linkt tag <-> note - both variants, just to be sure...
            oldGroup.getLinkedNotes().remove(curNote);
            oldGroup.getLinkedNotes().remove(movedNote);
            newGroup.getLinkedNotes().add(movedNote);
        }

        return true;
    }

    @Override
    public boolean deleteNote(Note curNote) {
        groupForName(curNote.getGroupName(), false).getLinkedNotes().remove(curNote);
        ReservedTag.All.getTag().getLinkedNotes().remove(curNote);

        return true;
    }

    @Override
    public boolean saveNote(Note note) {
        return true;
    }
    
    public static Label getIconForName(final String iconName, final IconSize size) {
        Label result; 
        try {
            // TFE, 20210316: can't use GlyphsDude.createIcon since styling of text color isn't working
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.valueOf(iconName), "", size.getSize(), "0px", ContentDisplay.CENTER);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TagData.class.getName()).log(Level.SEVERE, null, ex);
            result = GlyphsDude.createIconLabel(FontAwesomeIcon.BUG, "", size.getSize(), "0px", ContentDisplay.CENTER);
        }
        return result;
    }
    
    public static String getExternalName(final TagData tag) {
        final StringBuffer result = new StringBuffer(tag.getName());
        
        // and now go upwards in the hierarchy til root or "Groups" tag
        TagData parent = tag.getParent();
        for (int i = tag.getLevel(); i > 0; i--) {
            if (parent == null) {
                // TFE, 20230213: occasional exception: Cannot invoke "tf.ownnote.ui.tags.TagData.getName()" because "parent" is null
                // this shouldn't happen! levels says we need to go up the chain but the chain isn't long enough?
                System.err.println("Tag '" + tag.getName() + "' has incorrect parent hierarchy!");
                System.err.println("Expected: " + tag.getLevel() + " parents, chain ended on level " + i);
                System.err.println("Actual tag hierarchy:");
                TagData dump = tag;
                int j = 0;
                do {
                    System.err.println("  Tag " + j + ": '" + dump.getName() + "' on level " + dump.getLevel());

                    dump = dump.getParent();
                    j++;
                } while (dump != null);
            }
            if (isGroupsRootTag(parent)) {
                // we don't need to go all the way up to root...
                break;
            }
            result.insert(0, EXTERNAL_NAME_SEPARATOR);
            result.insert(0, parent.getName());

            parent = parent.getParent();
        }
        
        return result.toString();
    }
    
    public static LinkedList<TagData> getTagHierarchyAsList(final TagData tag) {
        final LinkedList<TagData> result = new LinkedList<>();
        
        // lets start with ourselves
        result.add(tag);
        
        TagData parent = tag.getParent();
        for (int i = tag.getLevel(); i > 0; i--) {
            if (isGroupsRootTag(parent)) {
                // we don't need to go all the way up to root...
                break;
            }
            // add upfront so that order ing is like for external name
            result.add(0, parent);
            
            parent = parent.getParent();
        }

        return result;
    }
    
    // ===========================================================================
    
//    public void checkLinkedNotesCount(final String tagId, final int count) {
//        Optional<TagData> tag = getRootTag().getChildren().stream().map((t) -> {
//            return t.flattened();
//        }).flatMap(Function.identity()).filter((t) -> {
//            return t.getId().equals(tagId);
//        }).findFirst();
//        
//        if (tag.isEmpty()) {
//            System.out.println("Bummer, tag with id " + tagId + " not found!");
//        } else {
//            final int size = tag.get().getLinkedNotes().size();
//            if (size != count) {
//                System.out.println("Bummer, tag with id " + tagId + " has " + size + " linked notes instead of " + count);
//            } else {
//                System.out.println("Bingo, tag with id " + tagId + " has " + count + " linked notes!");
//            }
//        }
//    }
}
