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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteHTMLEditor {
    private static final String TOP_TOOLBAR = ".top-toolbar";
    private static final String BOTTOM_TOOLBAR = ".bottom-toolbar";
    private static final String WEB_VIEW = ".web-view";

    private WebView mWebView;
    private ToolBar mTopToolBar;
    private ToolBar mBottomToolBar;    
    
    
    private HTMLEditor myHTMLEditor = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private String noteText = "";
    
    private OwnNoteHTMLEditor() {
        super();
    }
    
    public OwnNoteHTMLEditor(final HTMLEditor htmlEditor) {
        super();
        
        myHTMLEditor = htmlEditor;
        
        // delay setup of editor - things are not available at startup...
        Platform.runLater(() -> {
            initHTMLEditor();
        });  
    }
    
    public void setNoteText(final String text) {
        myHTMLEditor.setHtmlText(text);
        noteText = getNoteText();
    }

    public String getNoteText() {
        String noteHtml = myHTMLEditor.getHtmlText();
        
        Document doc = Jsoup.parse(noteHtml);
        // get rid of "<font face="Segoe UI">" tags - see bug report https://bugs.openjdk.java.net/browse/JDK-8133833
        doc.getElementsByTag("font").unwrap();
        // only store content in <body>
        noteHtml = doc.select("body").html();
        
        return noteHtml;
    }
    
    public boolean hasChanged() {
        boolean result = false;
        
        final String newNoteText = getNoteText();
        
        if (noteText == null) {
            result = (newNoteText != null);
        } else {
            result = !noteText.equals(newNoteText);
        }
        
        return result;
    }
    
    public void hasBeenSaved() {
        // re-set stored text to current text to re-start change tracking
        noteText = getNoteText();
    }

    public void setEditor(final OwnNoteEditor editor) {
        myEditor = editor;
    }
    
    public void checkForNameChange(final String oldGroupName, final String newGroupName) {
        checkForNameChange(oldGroupName, newGroupName, "", "");
    }

    @SuppressWarnings("unchecked")
    public void checkForNameChange(final String oldGroupName, final String newGroupName, final String oldNoteName, final String newNoteName) {
        assert (oldGroupName != null);
        assert (newGroupName != null);
        assert (oldNoteName != null);
        assert (newNoteName != null);
        
        if (getUserData() != null) {
            final NoteData editNote =
                    new NoteData((Map<String, String>) getUserData());
            
            boolean changed = false;

            // check for group name change
            if (!oldGroupName.equals(newGroupName)) {
                editNote.setGroupName(newGroupName);
                changed = true;
            }
            
            // check for note name change
            if (!oldNoteName.equals(newNoteName)) {
                editNote.setNoteName(newNoteName);
                changed = true;
            }
            
            if (changed) {
                // we did it! now do the house keeping...
                setUserData(editNote);
                // System.out.printf("User data updated\n");
            }
        }
    }

    private void initHTMLEditor() {
        mWebView = (WebView) myHTMLEditor.lookup(WEB_VIEW);
        mTopToolBar = (ToolBar) myHTMLEditor.lookup(TOP_TOOLBAR);
        mBottomToolBar = (ToolBar) myHTMLEditor.lookup(BOTTOM_TOOLBAR);
        
        // remove: foreground & background control
        hideNode(myHTMLEditor, ".html-editor-foreground", 1);
        hideNode(myHTMLEditor, ".html-editor-background", 1);
        // remove: font type & font size control - the 2nd and 3rd control with "font-menu-button" style class
        hideNode(myHTMLEditor, ".font-menu-button", 2);
        hideNode(myHTMLEditor, ".font-menu-button", 3);
        // add: insert link & picture controls
        addNoteEditorControls();
        // add: undo & redo button, back button
        
        // add a context menu for saving
        final MenuItem saveMenu = new MenuItem("Save");
        saveMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveMenu.setOnAction((ActionEvent event) -> {
            saveNote();
        });
        if (myHTMLEditor.getContextMenu() != null) {
            myHTMLEditor.getContextMenu().getItems().add(saveMenu);
        } else {
            final ContextMenu newContextMenu = new ContextMenu();
            newContextMenu.getItems().add(saveMenu);
            // TODO: fix 2 context menus - but how
            myHTMLEditor.setContextMenu(newContextMenu);
        }
        
        // TODO: add a drop handler, related to issue #31
        // mWebView.setOnDragDropped((DragEvent event) -> {
        //     Dragboard db = event.getDragboard();
        //     boolean success = false;
        //     if (db.hasString()) {
        //         String filePath = null;
        //         for (File file:db.getFiles()) {
        //             filePath = file.getAbsolutePath();
        //             System.out.println(filePath);
        //         }
        //         success = true;
        //     }
        //     event.setDropCompleted(success);
        //     
        //     event.consume();
        // });
    }
    
    private static void hideNode(final Node startNode, final String lookupString, final int occurence) {
        final Set<Node> nodes = startNode.lookupAll(lookupString);
        if (nodes != null && nodes.size() >= occurence) {
            // no simple way to ge nth member of set :-(
            Iterator<Node> itr = nodes.iterator();
            Node node = null;
            for(int i = 0; itr.hasNext() && i<occurence; i++) {
                node = itr.next();
            }
            if (node != null) {
                node.setVisible(false);
                node.setManaged(false);
            }
        }
    }

    private void addNoteEditorControls() {
        // update edit - but only once
        if (mTopToolBar.lookup(".html-editor-insertlink") != null) {
            return;
        }

        // copy styles from other buttons in toolbar
        ObservableList<String> buttonStyles = null;
        Node node = mTopToolBar.lookup(".html-editor-cut");
        if (node != null && node instanceof Button) {
            buttonStyles = ((Button) node).getStyleClass();
            // not the own button style, please
            buttonStyles.removeAll("html-editor-cut");
        }

        // add button to insert link
        ImageView graphic =
                new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/link.png"), 22, 22, true, true));
        final Button insertLink = new Button("", graphic);
        insertLink.getStyleClass().add("html-editor-insertlink");
        if (buttonStyles != null) {
            insertLink.getStyleClass().addAll(buttonStyles);
        }
        insertLink.setOnAction((ActionEvent t) -> {
            LinkDialog linkDialog = new LinkDialog();
            if (linkDialog.showAndWait()) {
                // dialog has been ended with OK - now check if values are fine
                if (!linkDialog.getLinkUrl().isEmpty() && !linkDialog.getLinkText().isEmpty()) {
                    final String hrefString =
                            "<a href=\"" +
                            linkDialog.getLinkUrl().trim() +
                            "\" title=\"" +
                            linkDialog.getLinkTitle().trim() +
                            "\" target=\"" +
                            // decide between _self and _blank on the fly
                            linkDialog.getWindowMode()+
                            "\">" +
                            linkDialog.getLinkText().trim() + "</a>";
                    myHTMLEditor.setHtmlText(myHTMLEditor.getHtmlText() + hrefString);
                }
            }
        });
        mTopToolBar.getItems().add(insertLink);

        // add button to insert image
        graphic = 
                new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/insertimage.gif"), 22, 22, true, true));
        final Button insertImage = new Button("", graphic);
        insertImage.getStyleClass().add("html-editor-insertimage");
        if (buttonStyles != null) {
            insertImage.getStyleClass().addAll(buttonStyles);
        }
        insertImage.setOnAction((ActionEvent arg0) -> {
            final List<String> extFilter = Arrays.asList("*.jpg", "*.png", "*.gif");
            final List<String> extValues = Arrays.asList("jpg", "png", "gif");

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Embed an image");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pictures", extFilter));
            final File selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                if (extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                    // we really have selected a picture - now add it
                    // issue #31: we shouldn't add the link but the image data instead!
                    // see https://github.com/dipu-bd/CustomControlFX/blob/master/CustomHTMLEditor/src/org/sandsoft/components/htmleditor/CustomHTMLEditor.java
                    importDataFile(selectedFile);
                }                        
            }
        });
        mTopToolBar.getItems().add(insertImage);

        // add separator
        mTopToolBar.getItems().add(new Separator());

        // Issue #12 - add print button
        // https://stackoverflow.com/questions/28847757/how-to-display-print-dialog-in-java-fx-and-print-node
        graphic =
                new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/print.png"), 22, 22, true, true));
        final Button printNote = new Button("", graphic);
        printNote.getStyleClass().add("html-editor-print");
        if (buttonStyles != null) {
            printNote.getStyleClass().addAll(buttonStyles);
        }
        printNote.setOnAction((ActionEvent arg0) -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(myHTMLEditor.getScene().getWindow())){
                myHTMLEditor.print(job);
                job.endJob();
            }
        });
        mTopToolBar.getItems().add(printNote);

        // add separator
        mTopToolBar.getItems().add(new Separator());

        // add save button
        graphic =
                new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/tf/ownnote/ui/css/save.png"), 22, 22, true, true));
        final Button saveNote = new Button("", graphic);
        saveNote.getStyleClass().add("html-editor-print");
        if (buttonStyles != null) {
            saveNote.getStyleClass().addAll(buttonStyles);
        }
        saveNote.setOnAction((ActionEvent arg0) -> {
            saveNote();
        });
        mTopToolBar.getItems().add(saveNote);
    }

    @SuppressWarnings("unchecked")
    private void saveNote() {
        assert (myEditor != null);

        final NoteData curNote = (NoteData) getUserData();
        if (myEditor.saveNoteWrapper(
                curNote.getGroupName(), 
                curNote.getNoteName(), 
                getNoteText())) {
            hasBeenSaved();
        }
    }
    
    /**
     * Inserts HTML data after the current cursor. If anything is selected, they
     * get replaced with new HTML data.
     *
     * @param html HTML data to insert.
     */
    private void insertHtmlAfterCursor(String html) {
        //replace invalid chars
        html = html.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        //get script
        String script = String.format(
                "(function(html) {"
                + "  var sel, range;"
                + "  if (window.getSelection) {"
                + "    sel = window.getSelection();"
                + "    if (sel.getRangeAt && sel.rangeCount) {"
                + "      range = sel.getRangeAt(0);"
                + "      range.deleteContents();"
                + "      var el = document.createElement(\"div\");"
                + "      el.innerHTML = html;"
                + "      var frag = document.createDocumentFragment(),"
                + "        node, lastNode;"
                + "      while ((node = el.firstChild)) {"
                + "        lastNode = frag.appendChild(node);"
                + "      }"
                + "      range.insertNode(frag);"
                + "      if (lastNode) {"
                + "        range = range.cloneRange();"
                + "        range.setStartAfter(lastNode);"
                + "        range.collapse(true);"
                + "        sel.removeAllRanges();"
                + "        sel.addRange(range);"
                + "      }"
                + "    }"
                + "  }"
                + "  else if (document.selection && "
                + "           document.selection.type != \"Control\") {"
                + "    document.selection.createRange().pasteHTML(html);"
                + "  }"
                + "})(\"%s\");", html);
        //execute script
        mWebView.getEngine().executeScript(script);
    }
    
    /**
     * Imports an image file.
     *
     * @param file Image file.
     */
    private void importDataFile(File file) {
        try {
            //check if file is too big
            if (file.length() > 1024 * 1024) {
                throw new VerifyError("File is too big.");
            }
            //get mime type of the file
            String type = java.nio.file.Files.probeContentType(file.toPath());
            //get html content
            byte[] data = org.apache.commons.io.FileUtils.readFileToByteArray(file);
            String base64data = java.util.Base64.getEncoder().encodeToString(data);
            String htmlData = String.format(
                    "<img src='data:%s;base64,%s' type='%s' />",
                    type, base64data, type);
            //insert html
            insertHtmlAfterCursor(htmlData);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /* Required getter and setter methods are forwarded to internal TableView */

    public void setDisable(final boolean b) {
        myHTMLEditor.setDisable(b);
    }

    public void setVisible(final boolean b) {
        myHTMLEditor.setVisible(b);
    }
    
    public Object getUserData() {
        return myHTMLEditor.getUserData();
    }

    public void setUserData(final Object data) {
        myHTMLEditor.setUserData(data);
    }
    
    public BooleanProperty visibleProperty() {
        return myHTMLEditor.visibleProperty();
    }
}
