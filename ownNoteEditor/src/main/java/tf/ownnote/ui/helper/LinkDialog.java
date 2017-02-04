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
package tf.ownnote.ui.helper;

import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class LinkDialog {
    public static final String TARGET_NONE = "_self";
    public static final String TARGET_NEW = "_blank";

    private class Result {
        public String linkurl = "";
        public String linktext = "";
        public String linktitle = "";
        public String windowmode = "";
        
        Result() {
        }
        
        Result(final Result fromResult) {
            assert fromResult != null;
            
            this.linkurl = fromResult.linkurl;
            this.linktext = fromResult.linktext;
            this.linktitle = fromResult.linktitle;
            this.windowmode = fromResult.windowmode;
        }
    }
    
    private Dialog<Result> myDialog = null;
    private Result myResult = null;
    
    public LinkDialog() {
        super();
        
        initMyDialog();
    }
    
    public boolean showAndWait() {
        Optional<Result> thisresult = myDialog.showAndWait();
        
        if (thisresult.isPresent()) {
            myResult = new Result(thisresult.get());
        }
        
        return thisresult.isPresent();
    }
    
    public String getLinkUrl() {
        assert myResult != null;
        
        return myResult.linkurl;
    }
    public String getLinkText() {
        assert myResult != null;
        
        return myResult.linktext;
    }
    public String getLinkTitle() {
        assert myResult != null;
        
        return myResult.linktitle;
    } 
    public String getWindowMode() {
        assert myResult != null;
        
        return myResult.windowmode;
    }
    
    private void initMyDialog() {
        myDialog = new Dialog<Result>();

        // now setup the dialog
        // Header    Insert link
        // textfield Url
        // textfield Text to display
        // textfield Title
        // combobox  Target (None, New window)
        // buttons   OK, Cancel
        
        // Set header and buttons
        myDialog.setTitle("Insert link");
        myDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // add fields & description
        TextField linkurlField = new TextField();
        linkurlField.setPromptText("Url");
        linkurlField.getStyleClass().add("linkurlField");
        TextField linktextField = new TextField();
        linktextField.setPromptText("Text");
        linktextField.getStyleClass().add("linktextField");
        TextField linktitleField = new TextField();
        linktitleField.setPromptText("Title");
        linktitleField.getStyleClass().add("linktitleField");

        ComboBox<String> windowmodeCombo = new ComboBox<String>();
        windowmodeCombo.getItems().clear();
        windowmodeCombo.getItems().add("None");
        windowmodeCombo.getItems().add("New window");
        windowmodeCombo.getSelectionModel().select(0);
        
        grid.add(new Label("Url:"), 0, 0);
        grid.add(linkurlField, 1, 0);
        grid.add(new Label("Text to display:"), 0, 1);
        grid.add(linktextField, 1, 1);
        grid.add(new Label("Title:"), 0, 2);
        grid.add(linktitleField, 1, 2);
        grid.add(new Label("Target:"), 0, 3);
        grid.add(windowmodeCombo, 1, 3);
        
        myDialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> linkurlField.requestFocus());
        
        myDialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Result thisResult = new Result();
                thisResult.linkurl = linktextField.getText();
                thisResult.linktext = linktextField.getText();
                thisResult.linktitle = linktitleField.getText();
                
                
                switch (windowmodeCombo.getSelectionModel().getSelectedIndex()) {
                    case 0:
                        thisResult.windowmode = LinkDialog.TARGET_NONE;
                        break;
                    case 1:
                        thisResult.windowmode = LinkDialog.TARGET_NEW;
                        break;
                }
                
                return thisResult;
            }
            return null;
        });
    }

}
