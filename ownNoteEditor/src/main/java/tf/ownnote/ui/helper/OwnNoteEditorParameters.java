package tf.ownnote.ui.helper;

import java.util.Optional;
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
        groupTabs,
        tagTree
    };

    // value for owncloud path, if set
    private String ownCloudDir = null;

    // value for lookAndFeel, if set
    private LookAndFeel lookAndFeel = null;
    
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
                "Layout to use - <arg> can be \"classic\" or \"oneNote\"");
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
                // System.out.println("Option ownCloudDir found: " + ownCloudDir);
            }
            
            String value = "";
            if (command.hasOption(OwnNoteEditorParameters.CmdOps.lookAndFeel.toString())) {
                value = command.getOptionValue(OwnNoteEditorParameters.CmdOps.lookAndFeel.toString());

                switch (value) {
                    case "classic":
                        // System.out.println("Option lookAndFeel found: " + laf);
                        lookAndFeel = LookAndFeel.classic;
                        break;
                    case "oneNote":
                        // System.out.println("Option lookAndFeel found: " + laf);
                        lookAndFeel = LookAndFeel.groupTabs;
                        break;
                    default:
                        System.out.println("Value \"" + value + "\" for option lookAndFeel not recognized.");
                }
            }
            
        } catch (ParseException|NumberFormatException ex) {
            //Logger.getLogger(OwnNoteEditorParameters.class.getName()).log(Level.SEVERE, null, ex);
            // fix for issue #19: add usage screen in case of incorrect options
            help(options);
        }
    }

    public Optional<String> getOwnCloudDir() {
        return Optional.ofNullable(ownCloudDir);
    }

    public void setOwnCloudDir(final String ownCloudDir) {
        this.ownCloudDir = ownCloudDir;
    }

    public Optional<LookAndFeel> getLookAndFeel() {
        return Optional.ofNullable(lookAndFeel);
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
