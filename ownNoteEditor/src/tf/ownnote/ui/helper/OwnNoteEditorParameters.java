package tf.ownnote.ui.helper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OwnNoteEditorParameters {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static OwnNoteEditorParameters INSTANCE = new OwnNoteEditorParameters();

    // list of command line parameters we can understand
    public static enum CmdOps {
        ownCloudDir,
        lookAndFeel
    };

    public static enum LookAndFeel {
        classic,
        oneNote
    };

    // value for owncloud path, if set
    private String ownCloudDir = null;

    // value for lookAndFeel, if set
    private LookAndFeel lookAndFeel = LookAndFeel.classic;
    
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
                OwnNoteEditorParameters.CmdOps.lookAndFeel.toString(), 
                OwnNoteEditorParameters.CmdOps.lookAndFeel.toString(), 
                true, 
                "Layout to use - \"classic\" or \"onenote\"");
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
                System.out.println("Option ownCloudDir found: " + ownCloudDir);
            }
            
            String laf = "";
            if (command.hasOption(OwnNoteEditorParameters.CmdOps.lookAndFeel.toString())) {
                laf = command.getOptionValue(OwnNoteEditorParameters.CmdOps.lookAndFeel.toString());
            }
            switch (laf) {
                case "classic":
                    System.out.println("Option lookAndFeel found: " + laf);
                    lookAndFeel = LookAndFeel.classic;
                    break;
                case "oneNote":
                    System.out.println("Option lookAndFeel found: " + laf);
                    lookAndFeel = LookAndFeel.oneNote;
                    break;
                default:
                    System.out.println("Value \"" + laf + "\" for option lookAndFeel not recognized, using \"classic\"");
                    lookAndFeel = LookAndFeel.classic;
            }
        } catch (ParseException ex) {
            //Logger.getLogger(OwnNoteEditorParameters.class.getName()).log(Level.SEVERE, null, ex);
            // fix for issue #19: add usage screen in case of incorrect options
            help(options);
        }
    }

    public String getOwnCloudDir() {
        return ownCloudDir;
    }

    public void setOwnCloudDir(final String ownCloudDir) {
        this.ownCloudDir = ownCloudDir;
    }

    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(final LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }
    
    private void help(final Options options) {
        // This prints out some help
        HelpFormatter formater = new HelpFormatter();

        formater.printHelp("OwnNoteEditor", "Valid options are:", options, "Continue using only recognized options");
        //System.exit(0);
    }    
}
