 package tf.ownnote.ui.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import tf.helper.general.GeneralParser;
import tf.helper.general.IPreferencesStore;
import tf.helper.general.ObjectsHelper;
import tf.ownnote.ui.main.OwnNoteEditor;

public enum OwnNoteEditorPreferences implements IPreferencesStore {
    INSTANCE("instance", "", String.class),
    RECENT_OWNCLOUDPATH("recentOwnCloudPath", "", String.class),
    RECENT_LOOKANDFEEL("recentLookAndFeel", OwnNoteEditorParameters.LookAndFeel.groupTabs.name(), OwnNoteEditorParameters.LookAndFeel.class), 
    // issue #30 store percentage for group column width
    RECENT_GROUPTABS_GROUPWIDTH("recentGroupTabsGroupWidth", Double.toString(30.0), Double.class),
    RECENT_TASKLIST_WIDTH("recentTaskListWidth", Double.toString(15.0), Double.class),
    RECENT_TASKLIST_VISIBLE("recentTaskListVisible", Boolean.toString(true), Boolean.class),
    RECENT_WINDOW_WIDTH("recentWindowWidth", Double.toString(1200.0), Double.class),
    RECENT_WINDOW_HEIGTH("recentWindowHeigth", Double.toString(600.0), Double.class),
    RECENT_WINDOW_LEFT("recentWindowLeft", Double.toString(0.0), Double.class),
    RECENT_WINDOW_TOP("recentWindowTop", Double.toString(0.0), Double.class),
    // issue #45 store sort order for tables
    RECENT_NOTESTABLE_SORTORDER("recentNotesTableSortOrder", "recentNotesTableSortOrder", String.class),
    // TFE, 20201205: store everything for tables :-)
    RECENT_NOTESTABLE_SETTINGS("recentNotesTableSettings", "recentNotesTableSettings", String.class),
    // TFE, 20201030: store last edited file
    LAST_EDITED_NOTE("lastEditedNote", "", String.class),
    LAST_EDITED_GROUP("lastEditedGroup", "", String.class),
    // TFE, 20201204: new layout: tag tree as first column
    RECENT_TAGTREE_WIDTH("recentTagTreeWidth", Double.toString(18.3333333), Double.class),
    // TFE, 20210111: store location & size of KANBAN window
    RECENT_KANBAN_WINDOW_WIDTH("recentKanbanWindowWidth", Double.toString(800.0), Double.class),
    RECENT_KANBAN_WINDOW_HEIGTH("recentKanbanWindowHeigth", Double.toString(600.0), Double.class),
    RECENT_KANBAN_WINDOW_LEFT("recentKanbanWindowLeft", Double.toString(Double.NaN), Double.class),
    RECENT_KANBAN_WINDOW_TOP("recentKanbanWindowTop", Double.toString(Double.NaN), Double.class),
    // TFE, 20200907: store tab order
    RECENT_TAB_ORDER("recentTabOrder", "", String.class);
    public static final String PREF_STRING_PREFIX = "[ ";
    public static final String PREF_STRING_SUFFIX = " ]";
    public static final String PREF_STRING_SEP = " ::: ";
    
    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(OwnNoteEditor.class);

    private final String myPrefKey;
    private final String myDefaultValue;
    private final Class myClass;

    private OwnNoteEditorPreferences(final String key, final String defaultValue, final Class classP) {
        myPrefKey = key;
        myDefaultValue = defaultValue;
        myClass = classP;
    }

    public String getAsString() {
        return get(myPrefKey, myDefaultValue);
    }
    
    public <T> T getAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        try {
            return ObjectsHelper.uncheckedCast(GeneralParser.parse(getAsString(), ObjectsHelper.uncheckedCast(myClass)));
        } catch (Exception ex) {
            return getDefaultAsType();
        }
    }
    
    public <T> void put(final T value) {
        put(myPrefKey, value.toString());
    }
    
    public <T> T getDefaultAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        return ObjectsHelper.uncheckedCast(GeneralParser.parse(myDefaultValue, ObjectsHelper.uncheckedCast(myClass)));
    }

    private static String getImpl(final String key, final String defaultValue) {
        String result = defaultValue;

        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    @Override
    public String get(final String key, final String defaultValue) {
        return getImpl(key, defaultValue);
    }

    private static void putImpl(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }
    @Override
    public void put(final String key, final String value) {
        putImpl(key, value);
    }

    public static void clearImpl() {
        try {
            MYPREFERENCES.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void clear() {
        clearImpl();
    }
    
    public static void removeImpl(String key) {
        MYPREFERENCES.remove(key);
    }
    @Override
    public void remove(String key) {
        removeImpl(key);
    }

    public static void exportPreferencesImpl(final OutputStream os) {
        try {
            MYPREFERENCES.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void exportPreferences(final OutputStream os) {
        exportPreferencesImpl(os);
    }

    public void importPreferencesImpl(final InputStream is) {
        try {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void importPreferences(final InputStream is) {
        importPreferencesImpl(is);
    }
}
