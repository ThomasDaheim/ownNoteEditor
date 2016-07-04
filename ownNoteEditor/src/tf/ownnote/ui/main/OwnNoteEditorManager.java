/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.main;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditorManager extends Application {

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    private OwnNoteEditor controller;
    
    private Stage myStage = null;
    
    private double myStageX;
    private double myStageY;
    
    private java.awt.SystemTray tray = null;
    private java.awt.TrayIcon trayIcon = null;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(OwnNoteEditorManager.class, args);
    }
  
    /**
     * 
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {
        // store stage for later use
        myStage = primaryStage;

        FXMLLoader fxmlLoader = null;
        BorderPane pane = null;
        try {
            // now we have three kinds of parameters :-(
            // 1) named: name, value pairs from jnlp
            // 2) unnamed: values only from jnlp
            // 3) raw: good, old command line parameters
            // http://java-buddy.blogspot.de/2014/02/get-parametersarguments-in-javafx.html

            // for now just use raw parameters since the code as lready there for this :-)
            // let some one else deal with the command line parameters
            Parameters myParams = getParameters();
            if ((myParams.getRaw() != null) && !myParams.getRaw().isEmpty()) {
                OwnNoteEditorManager.parameters.init(myParams.getRaw().toArray(new String[0]));
            } else {
                OwnNoteEditorManager.parameters.init(null);
            }

            // issue #30: store width and height of window as well - but here so that the scene can be created accordingly
            Double recentWindowWidth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTWINDOWWIDTH, "1200"));
            Double recentWindowHeigth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTWINDOWHEIGTH, "600"));
            
            fxmlLoader = new FXMLLoader(OwnNoteEditorManager.class.getResource("OwnNoteEditor.fxml"));
            pane = (BorderPane) fxmlLoader.load();
            
            myStage.setScene(new Scene(pane, recentWindowWidth, recentWindowHeigth));

            myStage.setTitle("OwnNote Editor"); 
            myStage.getIcons().clear();
            myStage.getIcons().add(new Image(OwnNoteEditorManager.class.getResourceAsStream("/tf/ownnote/ui/main/OwnNoteEditorManager.png")));
            myStage.getScene().getStylesheets().add(OwnNoteEditorManager.class.getResource("/tf/ownnote/ui/css/ownnote.css").toExternalForm());
            
            // TF, 20160620: suppress warnings from css parsing for "-fx-font-weight" - not correctly implemented in the css parrser for javafx 8...
            // Logging.getCSSLogger().setLevel(PlatformLogger.Level.SEVERE);
            
            // set passed parameters for later use
            controller = fxmlLoader.getController();
            controller.setParameters(); 
            
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteEditorManager.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1); 
        }
        
        // TF, 20160619: minimmize to System Tray
        // https://gist.github.com/jewelsea/e231e89e8d36ef4e5d8a
        
        // https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);

        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
        
        // listen to minimize event
        // https://stackoverflow.com/questions/10233066/how-to-attach-event-handler-to-javafx-stage-window-minimize-button
        myStage.iconifiedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue != null && newValue) {
                hideStage();
            }
        });        
        
        // TF, 20160619: stop() is called on minimize as well - we only want to do closeStage() once
        // also, with Platform.setImplicitExit(false) we need to shut down things ourselves
        myStage.setOnCloseRequest((WindowEvent t) -> {
            closeStage();
        });
        
        myStage.show();

        // track changes of x/y pos for later use when coming back from tray
        myStage.xProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null && newValue.doubleValue() > 0) {
                myStageX = newValue.doubleValue();
            }
        }); 
        myStage.yProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null && newValue.doubleValue() > 0) {
                myStageY = newValue.doubleValue();
            }
        }); 

        // center on screen
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        myStage.setX((primScreenBounds.getWidth() - myStage.getWidth()) / 2); 
        myStage.setY((primScreenBounds.getHeight() - myStage.getHeight()) / 2);
    }
    
    /**
     * Sets up a system tray icon for the application.
     */
    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // nothing to do if no system tray support...
            if (!java.awt.SystemTray.isSupported()) {
                return;
            }

            tray = java.awt.SystemTray.getSystemTray();

            // set up a system tray icon from app icon
            final java.awt.Image image = SwingFXUtils.fromFXImage(myStage.getIcons().get(0), null);
            // resize to allowed proportions...
            // https://stackoverflow.com/questions/12287137/system-tray-icon-looks-distorted
            int trayIconWidth = new java.awt.TrayIcon(image).getSize().width;
            trayIcon = new java.awt.TrayIcon(image.getScaledInstance(trayIconWidth, -1, java.awt.Image.SCALE_SMOOTH));

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener((ActionEvent event) -> Platform.runLater(this::showStage));

            // if the user selects the default menu item (which includes the app name), 
            // show the main app stage.
            final java.awt.MenuItem openItem = new java.awt.MenuItem("Restore");
            openItem.addActionListener((ActionEvent event) -> Platform.runLater(this::showStage));

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            final java.awt.Font defaultFont = java.awt.Font.decode(null);
            final java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            final java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener((ActionEvent event) -> Platform.runLater(this::closeStage));

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);
            
            // show on double click on trayIcon

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (java.awt.AWTException ex) {
            Logger.getLogger(OwnNoteEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Shows the application stage and ensures that it is brought to the front of all stages.
     */
    private void showStage() {
        if (myStage != null) {
            myStage.setIconified(false);
            myStage.show();
            restoreStagePos();
            
            myStage.toFront();
        }
    }
    
    private void hideStage() {
        if (myStage != null) {
            myStage.hide();
        }
    }

    public void closeStage() {
        try {
            super.stop();
        } catch (Exception ex) {
            Logger.getLogger(OwnNoteEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTWINDOWWIDTH, String.valueOf(myStage.getScene().getWidth()));
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTWINDOWHEIGTH, String.valueOf(myStage.getScene().getHeight()));
        
        if (controller != null) {
            controller.stop();
        }
        
        if(tray!= null && trayIcon != null) {
            tray.remove(trayIcon);
        }
                    
        Platform.exit();
    }
    
    private void restoreStagePos() {
        myStage.setX(myStageX);
        myStage.setY(myStageY);
    }
}
