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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
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
import javafx.scene.control.MenuBar;
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
 * 
 * Tries to read the following info out of the manifest file 
 * 
    Manifest-Version: 1.0
    Built-By: thomas
    Build-Timestamp: 2019-08-23T19:51:19.835+0200
    Created-By: Gradle 4.10.2
    Build-Jdk: 11.0.2 (Oracle Corporation 11.0.2+9)
    Build-OS: Windows 10 amd64 10.0
    App-Name: GPXEditor
    App-Version: 4.0
    App-URL: https://github.com/ThomasDaheim/GPXEditor
 * 
 * that has been put in via build.gradle 
 * 
    jar {
        manifest {
            attributes(
                'Built-By'       : System.properties['user.name'],
                'Build-Timestamp': new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()),
                'Created-By'     : "Gradle ${gradle.gradleVersion}",
                'Build-Jdk'      : "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.properties['java.vm.version']})",
                'Build-OS'       : "${System.properties['os.name']} ${System.properties['os.arch']} ${System.properties['os.version']}",
                'App-Name'       : "GPXEditor",
                'App-Version'    : "4.0",
                'App-URL'        : "https://github.com/ThomasDaheim/GPXEditor"
            )
        }
    }
 * 
 * If not found, the parameters "app*" are used as fallback.
*/
public class AboutMenu {
    private final static AboutMenu INSTANCE = new AboutMenu();
    
    private static final String MENU_ICON = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QAAKqNIzIAAAAJcEhZcwAADdcAAA3XAUIom3gAAAAHdElNRQfiChwRMw7JEGHfAAAAqklEQVQoz6XQr06CYRgF8B9/3GjQbejGCEaKSKER7AYj3ABjcCNegndhsBBIRBLJRnAjfI3hY/kUZe+bOOU5O+fsbOfhHI/WVm5k0PAphHk+UAhfOrK4t3DrEjQM1XLmtVeFME2ZFRN7IYTxSa7+splnLyXfpBpqeBJC4SrVcMQALB1SAXgA77kNLUchDLV1U4F+uWFpZ/Qj1v8EmuUNPR+phjtbbyb///gN164oLmafnJcAAAAldEVYdGRhdGU6Y3JlYXRlADIwMTgtMTAtMjhUMTc6NTE6MTQrMDE6MDD/wi67AAAAJXRFWHRkYXRlOm1vZGlmeQAyMDE4LTEwLTI4VDE3OjUxOjE0KzAxOjAwjp+WBwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAAASUVORK5CYII=";
    private final Image menuIcon;
    
    private static final String UNKNOWN = "unknown";
    
    private AboutMenu() {
        super();
        
        menuIcon = SwingFXUtils.toFXImage(decodeToImage(MENU_ICON), null);
    }
    
    public static AboutMenu getInstance() {
        return INSTANCE;
    }
    
    public void addAboutMenu(final Window window, final Menu menu, final String appName, final String appVersion, final String appURL) {
        // add menu to menuBar
        menu.getItems().add(createMenuItem(window, appName, appVersion, appURL, new MenuItem(), false));
    }
    
    public void addAboutMenu(final Window window, final MenuBar menuBar, final String appName, final String appVersion, final String appURL) {
        // add menu to menuBar
        menuBar.getMenus().add((Menu) createMenuItem(window, appName, appVersion, appURL, new Menu(), true));
    };
    
    private MenuItem createMenuItem(final Window window, final String appName, final String appVersion, final String appURL, final MenuItem menuItem, final boolean useImage) {
        // create alert from data with OK button
        final Alert alert = createAboutAlert(window, appName, appVersion, appURL);
        
        if (useImage) {
            // create menu, add image and onclick handler to show alert
            final ImageView menuImage = new ImageView();
            menuImage.setImage(menuIcon);
            menuImage.setPreserveRatio(true);
            menuImage.setFitHeight(12);

            // setOnMouseClicked works only on Label, not directly on ImageView...
            final Label menuLabel = new Label("");
            menuLabel.setGraphic(menuImage);
            menuLabel.setOnMouseClicked((event) -> {
                alert.showAndWait();
            });

            menuItem.setGraphic(menuLabel);
        } else {
            menuItem.setText("About");
        }
        
        // make double sure we get the click :-)
        menuItem.setOnAction((t) -> {
            alert.showAndWait();
        });

        return menuItem;
    }
    
    private Alert createAboutAlert(final Window window, final String inAppName, final String inAppVersion, final String inAppURL) {
        String appName = inAppName;
        String appVersion = inAppVersion;
        String appURL = inAppURL;

        String builtBy = UNKNOWN;
        String buildTimestamp = UNKNOWN;
        String createdBy = UNKNOWN;
        String buildJdk = UNKNOWN;
        String buildOS = UNKNOWN;
        
        // TFE, 20190823: try to read data from manifest file
        try {
	    final JarFile jar = JarFileLoader.jarFileOf(AboutMenu.class);
//            System.out.println("jar: " + jar);
            if (jar != null) {
                final Manifest manifest = jar.getManifest();
                final Attributes manifestContents = manifest.getMainAttributes();

                appName = setFromAttributesIfNotNull(appName, manifestContents, "App-Name");
                appVersion = setFromAttributesIfNotNull(appVersion, manifestContents, "App-Version");
                appURL = setFromAttributesIfNotNull(appName, manifestContents, "App-URL");

                builtBy = setFromAttributesIfNotNull(builtBy, manifestContents, "Built-By");
                buildTimestamp = setFromAttributesIfNotNull(buildTimestamp, manifestContents, "Build-Timestamp");
                createdBy = setFromAttributesIfNotNull(createdBy, manifestContents, "Created-By");
                buildJdk = setFromAttributesIfNotNull(buildJdk, manifestContents, "Build-Jdk");
                buildOS = setFromAttributesIfNotNull(buildOS, manifestContents, "Build-OS");
            }
        } catch (IOException ex) {
            Logger.getLogger(AboutMenu.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // create alert from data with OK button
        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(appName);
        alert.setHeaderText(appName + " " + appVersion);
        alert.initOwner(window);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        // show link as clickable
        // http://bekwam.blogspot.com/2015/07/dont-just-tell-me-show-me-with-custom.html
        final GridPane pane = new GridPane();
        
        int rowNum = 0;
        final Label lbl1 = new Label("This software is provided under the \"The 3-Clause BSD License\"." + "\n\n");
        pane.add(lbl1, 0, rowNum, 2, 1);
        
        rowNum++;
        final Label lbl2 = new Label("See ");
        pane.add(lbl2, 0, rowNum, 1, 1);

        final Hyperlink link = new Hyperlink(appURL);
        link.setOnAction((ActionEvent event) -> {
            final HostServices hostServices = (HostServices) alert.getOwner().getScene().getWindow().getProperties().get("hostServices");
            if (hostServices != null) {
                hostServices.showDocument(link.getText());
            }
            alert.close();
        });
        pane.add(link, 1, rowNum, 1, 1);

        rowNum++;
        final Label lbl3 = new Label("for more information.");
        pane.add(lbl3, 0, rowNum, 2, 1);
        
        rowNum++;
        final String javaVersion = System.getProperty("java.version");
        final String javafxVersion = System.getProperty("javafx.version");
        final Label lbl4 = new Label("\nRunning on Java " + javaVersion + " with JavaFX " + javafxVersion + ".");
        pane.add(lbl4, 0, rowNum, 2, 1);
        
        rowNum++;
        final Label lbl5 = new Label(
                "\nBuilt by:\t\t\t" + builtBy + 
                "\nBuild timestamp:\t" + buildTimestamp + 
                "\nBuild JDK:\t\t\t" + buildJdk + 
                "\nBuild OS:\t\t\t" + buildOS + 
                "\nCreated by:\t\t" + createdBy);
        pane.add(lbl5, 0, rowNum, 2, 1);
        
        alert.getDialogPane().contentProperty().set(pane);
        
        return alert;
    }
    
    private static String setFromAttributesIfNotNull(final String defaultValue, final Attributes manifestContents, final String attrName) {
        String result = manifestContents.getValue(attrName);
        
        if (result == null) {
            result = defaultValue;
        }
        
        return result;
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
