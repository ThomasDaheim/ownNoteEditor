/*
 * Copyright (c) 2014ff Thomas Feuster
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
package tf.ownnote.ui.general;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.HostServices;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import javax.imageio.ImageIO;

/**
 *
 * @author thomas
 */
public class AboutMenu {
    private final static AboutMenu INSTANCE = new AboutMenu();
    
    private static final String MENU_ICON = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QAAKqNIzIAAAAJcEhZcwAADdcAAA3XAUIom3gAAAAHdElNRQfiChwRMw7JEGHfAAAAqklEQVQoz6XQr06CYRgF8B9/3GjQbejGCEaKSKER7AYj3ABjcCNegndhsBBIRBLJRnAjfI3hY/kUZe+bOOU5O+fsbOfhHI/WVm5k0PAphHk+UAhfOrK4t3DrEjQM1XLmtVeFME2ZFRN7IYTxSa7+splnLyXfpBpqeBJC4SrVcMQALB1SAXgA77kNLUchDLV1U4F+uWFpZ/Qj1v8EmuUNPR+phjtbbyb///gN164oLmafnJcAAAAldEVYdGRhdGU6Y3JlYXRlADIwMTgtMTAtMjhUMTc6NTE6MTQrMDE6MDD/wi67AAAAJXRFWHRkYXRlOm1vZGlmeQAyMDE4LTEwLTI4VDE3OjUxOjE0KzAxOjAwjp+WBwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAAASUVORK5CYII=";
    private final Image menuIcon;
    
    private AboutMenu() {
        super();
        
        menuIcon = SwingFXUtils.toFXImage(decodeToImage(MENU_ICON), null);
    }
    
    public static AboutMenu getInstance() {
        return INSTANCE;
    }
    
    // TFE, 20181028: can't get the event to fire to show alert...
//    public void addAboutMenu(final MenuBar menuBar, final String appName, final String version, final String link) {
//        // create alert from data with OK button
//        final Alert myAlert = new Alert(AlertType.INFORMATION);
//        myAlert.setTitle(appName);
//        myAlert.setHeaderText(appName + " " + version);
//        myAlert.setContentText("This software is provided under the \"The 3-Clause BSD License\"" + "\n" + "See " + link + " for source code.");
//        
//        // create menu, add image and onclick handler to show alert
//        final ImageView menuImage = new ImageView();
//        menuImage.setImage(menuIcon);
//        menuImage.setPreserveRatio(true);
//        menuImage.setFitHeight(12);
//        menuImage.setOnMouseClicked((event) -> {
//            myAlert.showAndWait();
//        });
//
//        final Menu myMenu = new Menu();
//        myMenu.setGraphic(menuImage);
//        myMenu.setOnShown((event) -> {
//            System.out.println("setOnShown");
//            myAlert.showAndWait();
//        });
//        myMenu.setOnShowing((event) -> {
//            System.out.println("setOnShowing");
//            myAlert.showAndWait();
//        });
//        //myMenu.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> {
//        myMenu.addEventHandler(EventType.ROOT, (event) -> {
//            System.out.println("addEventHandler: " + event.getEventType().toString());
////            if (MouseButton.PRIMARY.equals(event.getButton())) {
////                alert.showAndWait();
////            }
//        });
//        
//        // add menu to menuBar
//        menuBar.getMenus().add(myMenu);
//    };
    
    public void setAboutMenuImage(final Menu menu) {
        menu.setText("");
        
        final ImageView menuImage = new ImageView();
        menuImage.setImage(menuIcon);
        menuImage.setPreserveRatio(true);
        menuImage.setFitHeight(12); 
        menu.setGraphic(menuImage);
    }
    
    public void setAboutAlert(final Window window, final MenuItem menuItem, final String appName, final String appVersion, final String sourceURL) {
        // create alert from data with OK button
        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(appName);
        alert.setHeaderText(appName + " " + appVersion);
        alert.initOwner(window);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        // show link as clickable
        // http://bekwam.blogspot.com/2015/07/dont-just-tell-me-show-me-with-custom.html
        final GridPane pane = new GridPane();
        
        final Label lbl1 = new Label("This software is provided under the \"The 3-Clause BSD License\"" + "\n\n");
        pane.add(lbl1, 0, 0, 2, 1);
        
        final Label lbl2 = new Label("See ");
        pane.add(lbl2, 0, 1, 1, 1);

        final Hyperlink link = new Hyperlink(sourceURL);
        link.setOnAction((ActionEvent event) -> {
            final HostServices hostServices = (HostServices) alert.getOwner().getScene().getWindow().getProperties().get("hostServices");
            if (hostServices != null) {
                hostServices.showDocument(link.getText());
            }
            alert.close();
        });
        pane.add(link, 1, 1, 1, 1);

        final Label lbl3 = new Label("for more information.");
        pane.add(lbl3, 0, 2, 2, 1);
        
        alert.getDialogPane().contentProperty().set(pane);
        
        menuItem.setOnAction((event) -> {
            System.out.println("setOnAction");
            alert.showAndWait();
        });
    }
    
    private static BufferedImage decodeToImage(String imageString) {
        BufferedImage image = null;
        byte[] imageByte = Base64.getDecoder().decode(imageString);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageByte)) {
            image = ImageIO.read(bis);
        } catch (IOException ex) {
            Logger.getLogger(AboutMenu.class.getName()).log(Level.SEVERE, null, ex);
        }
        return image;
    }
}
