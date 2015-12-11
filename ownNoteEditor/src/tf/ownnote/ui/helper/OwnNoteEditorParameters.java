package tf.ownnote.ui.helper;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OwnNoteEditorParameters {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static OwnNoteEditorParameters INSTANCE = new OwnNoteEditorParameters();

    // list of command line parameters we can understand
    public static enum CmdOps {
        ownCloudDir
    };

    // value for owncloud path, if set
    private String ownCloudDir = null;
    
    private OwnNoteEditorParameters() {
        // Exists only to defeat instantiation.
    }

    public static OwnNoteEditorParameters getInstance() {
        return INSTANCE;
    }
    
    public void init(final String [ ] args) {
        // thats all options we can handle
        Options options = new Options();
        options.addOption(
                OwnNoteEditorParameters.CmdOps.ownCloudDir.toString(), 
                OwnNoteEditorParameters.CmdOps.ownCloudDir.toString(), 
                true, 
                "Path of ownCloud Notes directory");

        // lets parse them by code from other people
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine command = parser.parse(options, args);
            
            // get path to owncloud notes directory
            if (command.hasOption(OwnNoteEditorParameters.CmdOps.ownCloudDir.toString())) {
                ownCloudDir = command.getOptionValue(OwnNoteEditorParameters.CmdOps.ownCloudDir.toString());
            }
        } catch (ParseException ex) {
            Logger.getLogger(OwnNoteEditorParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getOwnCloudDir() {
        return ownCloudDir;
    }

    public void setOwnCloudDir(final String ownCloudDir) {
        this.ownCloudDir = ownCloudDir;
    }
}
