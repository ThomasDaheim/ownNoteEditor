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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditorManager extends Application {

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    private OwnNoteEditor controller;
    
    private Stage myStage = null;
    
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
  
        //System.out.println(System.getProperty("javafx.version"));
        //Map <String,String> myParms = getParameters().getNamed();
        //for (Map.Entry<String, String> entry : myParms.entrySet())
        //{
        //    System.out.println(entry.getKey() + "/" + entry.getValue());
        //}        
        
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
            pane =(BorderPane) fxmlLoader.load();
            
            myStage.setScene(new Scene(pane, recentWindowWidth, recentWindowHeigth));
            
            myStage.setTitle("OwnNote Editor"); 
            myStage.getIcons().add(new Image(OwnNoteEditorManager.class.getResourceAsStream("OwnNoteEditorManager.png")));
            myStage.getScene().getStylesheets().add(OwnNoteEditorManager.class.getResource("/tf/ownnote/ui/css/ownnote.css").toExternalForm());
            
            // set passed parameters for later use
            controller = fxmlLoader.getController();
            controller.setParameters(); 
            
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteEditorManager.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1); 
        }
        myStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();

        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTWINDOWWIDTH, String.valueOf(myStage.getScene().getWidth()));
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTWINDOWHEIGTH, String.valueOf(myStage.getScene().getHeight()));
        
        if (controller != null) {
            controller.stop();
        }
    }
    
}
