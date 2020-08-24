package tf.ownnote.ui.helper;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import tf.ownnote.ui.main.OwnNoteEditor;

public class OwnNoteEditorPreferences {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static OwnNoteEditorPreferences INSTANCE = new OwnNoteEditorPreferences();

    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(OwnNoteEditor.class);
    public final static String RECENTOWNCLOUDPATH = "recentOwnCloudPath";
    public final static String RECENTLOOKANDFEEL = "recentLookAndFeel";
    // issue #30 store percentage for group column width
    public final static String RECENTCLASSICGROUPWIDTH = "recentClassicGroupWidth";
    public final static String RECENTONENOTEGROUPWIDTH = "recentOneNoteGroupWidth";
    public final static String RECENTTASKLISTWIDTH = "recentTaskListWidth";
    public final static String RECENTWINDOWWIDTH = "recentWindowWidth";
    public final static String RECENTWINDOWHEIGTH = "recentWindowHeigth";
    // issue #45 store sort order for tables
    public final static String RECENTGROUPSTABLESORTORDER = "recentGroupsTableSortOrder";
    public final static String RECENTNOTESTABLESORTORDER = "recentNotesTableSortOrder";
    
    private OwnNoteEditorPreferences() {
        // Exists only to defeat instantiation.
    }

    public static OwnNoteEditorPreferences getInstance() {
        return INSTANCE;
    }
    
    public static String get(final String key, final String defaultValue) {
        String result = defaultValue;
        
        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    public static void put(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }
}
