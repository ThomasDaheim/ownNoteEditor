package tf.ownnote.ui.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import tf.helper.general.IPreferencesStore;
import tf.ownnote.ui.main.OwnNoteEditor;

public class OwnNoteEditorPreferences implements IPreferencesStore {
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
    
    @Override
    public String get(final String key, final String defaultValue) {
        String result = defaultValue;
        
        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    @Override
    public void put(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }

    @Override
    public void clear() {
        try {
            MYPREFERENCES.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void remove(String key) {
        MYPREFERENCES.remove(key);
    }

    @Override
    public void exportSubtree(final OutputStream os) {
        try {
            MYPREFERENCES.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void importPreferences(final InputStream is) {
        try {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            Logger.getLogger(OwnNoteEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
