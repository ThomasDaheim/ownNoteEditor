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

import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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
    
    private static enum ButtonType {
        ClickButton,
        ToggleButton
    };
    
    private static final String UNDO_COMMAND = "undo";
    private static final String REDO_COMMAND = "redo";
    ButtonBase undoEdit = null;
    ButtonBase redoEdit = null;

    private WebView mWebView;
    private WebEngine mWebEngine;
    private WebPage mWebPage;
    private ToolBar mTopToolBar;
    private ToolBar mBottomToolBar;    
    
    private HTMLEditor myHTMLEditor = null;
    
    private RawViewer rawViewer =null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private String noteText = "";
    
    private OwnNoteHTMLEditor() {
        super();
    }
    
    public OwnNoteHTMLEditor(final HTMLEditor htmlEditor, final OwnNoteEditor editor) {
        super();
        myHTMLEditor = htmlEditor;
        myEditor = editor;

        mWebView = (WebView) myHTMLEditor.lookup(WEB_VIEW);
        mWebEngine = mWebView.getEngine();
        mWebPage = Accessor.getPageFor(mWebEngine);
        
        rawViewer = new RawViewer();
        
        // delay setup of editor - things are not available at startup...
        Platform.runLater(() -> {
            initHTMLEditor();
        });  
    }
    
    public void setNoteText(final String text) {
        myHTMLEditor.setHtmlText(wrapText(text));
        noteText = getNoteText();
        
        setUndoRedo();
    }
    
    private String wrapText(final String text) {
        // chance to add, whatever we would like to have...
        return String.format(
                "<!DOCTYPE html>\r\n"
                + "<html>\r\n"
                + "  <head>\r\n"
                + "    <script>\r\n"
                + "    </script>\r\n"
                + "  </head>\r\n"
                + "  <body>%s</body>\r\n"
                + "</html>", text);
    }

    public String getNoteText() {
        String noteHtml = myHTMLEditor.getHtmlText();
        
        Document doc = Jsoup.parse(noteHtml);
        // Issue #55: seems we now also get another font tag that we don't want to have: "<span style="font-family: 'Segoe UI';">"
        doc.getElementsByAttributeValueStarting("style", "font-family").unwrap();
        // get rid of "<font face="Segoe UI">" tags - see bug report https://bugs.openjdk.java.net/browse/JDK-8133833
        // TF, 20160724: match only font + face since we also want to allow foreground / background colors
        doc.getElementsByAttributeValue("face", "Segoe UI").unwrap();
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
        mTopToolBar = (ToolBar) myHTMLEditor.lookup(TOP_TOOLBAR);
        mBottomToolBar = (ToolBar) myHTMLEditor.lookup(BOTTOM_TOOLBAR);
        
        // TF, 20160724: why should we remove that editor feature?
        // remove: foreground & background control
        // hideNode(myHTMLEditor, ".html-editor-foreground", 1);
        // hideNode(myHTMLEditor, ".html-editor-background", 1);
        
        // remove: font type & font size control - the 2nd and 3rd control with "font-menu-button" style class
        hideNode(myHTMLEditor, ".font-menu-button", 2);
        hideNode(myHTMLEditor, ".font-menu-button", 3);
        
        // add: insert link & picture controls
        addNoteEditorControls();
        
        // issue #40
        // https://stackoverflow.com/questions/20773249/javafx-htmleditor-doesnt-take-all-free-size-on-the-container
        GridPane.setHgrow(mWebView, Priority.ALWAYS);
        GridPane.setVgrow(mWebView, Priority.ALWAYS);        
        
        // issue #41 - add CTRL+S to button and not to context menu
        // // add a context menu for saving
        // final MenuItem saveMenu = new MenuItem("Save");
        // saveMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        // saveMenu.setOnAction((ActionEvent event) -> {
        //     saveNote();
        // });
        // if (myHTMLEditor.getContextMenu() != null) {
        //     myHTMLEditor.getContextMenu().getItems().add(saveMenu);
        // } else {
        //     final ContextMenu newContextMenu = new ContextMenu();
        //     newContextMenu.getItems().add(saveMenu);
        //     // TODO: fix 2 context menus - but how
        //     myHTMLEditor.setContextMenu(newContextMenu);
        // }
        
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
        ObservableList<String> clickButtonStyles = null;
        Node node = mTopToolBar.lookup(".html-editor-cut");
        if (node != null && node instanceof Button) {
            clickButtonStyles = ((Button) node).getStyleClass();
            // not the own button style, please
            clickButtonStyles.removeAll("html-editor-cut");
        }
        ObservableList<String> toggleButtonStyles = null;
        node = mTopToolBar.lookup(".html-editor-bold");
        if (node != null && node instanceof Button) {
            toggleButtonStyles = ((Button) node).getStyleClass();
            // not the own button style, please
            toggleButtonStyles.removeAll("html-editor-bold");
        }

        // add separator
        mBottomToolBar.getItems().add(new Separator());
        
        // add button to insert link
        final ButtonBase insertLink = 
                    createEditorButton(ButtonType.ClickButton, "link.png", "Insert Link", "html-editor-insertlink", clickButtonStyles);
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
                    insertHtmlAfterCursor(hrefString);
                }
            }
            myHTMLEditor.requestFocus();
        });
        mBottomToolBar.getItems().add(insertLink);

        // add button to insert image
        final ButtonBase insertImage = 
                createEditorButton(ButtonType.ClickButton, "insertimage.gif", "Insert Image", "html-editor-insertimage", clickButtonStyles);
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
            myHTMLEditor.requestFocus();
        });
        mBottomToolBar.getItems().add(insertImage);

        // add separator
        mBottomToolBar.getItems().add(new Separator());

        // add source button
        final ToggleButton viewSource = 
                (ToggleButton) createEditorButton(ButtonType.ToggleButton, "source.png", "View Source", "html-editor-bold", toggleButtonStyles);
        viewSource.setOnAction((ActionEvent arg0) -> {
            if (viewSource.isSelected()) {
                rawViewer.show();
                rawViewer.setText(getNoteText());
            } else {
                rawViewer.hide();
            }
        });
        // uncheck button when window gets closed
        rawViewer.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                viewSource.setSelected(false);
            }
        });        
        mBottomToolBar.getItems().add(viewSource);

        // add separator
        mBottomToolBar.getItems().add(new Separator());

        // Issue #12 - add print button
        // https://stackoverflow.com/questions/28847757/how-to-display-print-dialog-in-java-fx-and-print-node
        final ButtonBase printNote = 
                createEditorButton(ButtonType.ClickButton, "print.png", "Print Note", "html-editor-print", clickButtonStyles);
        printNote.setOnAction((ActionEvent arg0) -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(myHTMLEditor.getScene().getWindow())){
                myHTMLEditor.print(job);
                job.endJob();
            }
            myHTMLEditor.requestFocus();
        });
        mBottomToolBar.getItems().add(printNote);

        // add separator
        mBottomToolBar.getItems().add(new Separator());

        // add save button
        final ButtonBase saveNote = 
                createEditorButton(ButtonType.ClickButton, "save.png", "Save Note", "html-editor-save", clickButtonStyles);
        saveNote.setOnAction((ActionEvent arg0) -> {
            saveNote();
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        mBottomToolBar.getItems().add(saveNote);
        // issue #41 - add CTRL+S to button and not to context menu
        saveNote.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> {
            saveNote.fire();
        });

        // issue #56: add undo & redo button
        // they should go at the beginning of toolbar #1 - so we need to remove everything first...
        List<Node> toolbarItems = new ArrayList<Node>(mTopToolBar.getItems());
        mTopToolBar.getItems().clear();

        // add undo button
        undoEdit = createEditorButton(ButtonType.ClickButton, "undo.png", "Undo", "html-editor-undo", clickButtonStyles);
        undoEdit.setOnAction((ActionEvent arg0) -> {
            if (mWebPage.queryCommandEnabled(UNDO_COMMAND)) {
                final String curText = getNoteText();
                mWebPage.executeCommand(UNDO_COMMAND, null);
                // in some cases the first call do UNDO doesn't change anything, only the second... maybe one day I'll find a fix
                if (curText.equals(getNoteText())) {
                    mWebPage.executeCommand(UNDO_COMMAND, null);
                }
            }
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        mTopToolBar.getItems().add(undoEdit);
        undoEdit.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), () -> {
            undoEdit.fire();
        });
        
        // add redo button
        redoEdit = createEditorButton(ButtonType.ClickButton, "redo.png", "Redo", "html-editor-redo", clickButtonStyles);
        redoEdit.setOnAction((ActionEvent arg0) -> {
            if (mWebPage.queryCommandEnabled(REDO_COMMAND)) {
                final String curText = getNoteText();
                mWebPage.executeCommand(REDO_COMMAND, null);
                // in some cases the first call do UNDO doesn't change anything, only the second... maybe one day I'll find a fix
                if (curText.equals(getNoteText())) {
                    mWebPage.executeCommand(REDO_COMMAND, null);
                }
            }
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        mTopToolBar.getItems().add(redoEdit);
        redoEdit.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), () -> {
            redoEdit.fire();
        });

        // add separator
        mTopToolBar.getItems().add(new Separator());

        // and now once again all other controls
        mTopToolBar.getItems().addAll(toolbarItems);
        
        // TODO: this only catches keys typed but not formatting via buttons - no good idea how this can be monitored
        // set undo / redo based on command state
        myHTMLEditor.addEventHandler(Event.ANY, new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                setUndoRedo();
                
                // update raw viewer if visible
                if (rawViewer.isShowing()) {
                    rawViewer.setText(getNoteText());
                }
            }
        });
    }
    
    private void setUndoRedo() {
        if (undoEdit != null && redoEdit != null) {
            undoEdit.setDisable(!mWebPage.queryCommandEnabled(UNDO_COMMAND));
            redoEdit.setDisable(!mWebPage.queryCommandEnabled(REDO_COMMAND));
        }
    }
    
    private ButtonBase createEditorButton(
            final ButtonType buttonType, 
            final String iconName, 
            final String tooltipText, 
            final String cssStyleClass, 
            final ObservableList<String> buttonStyles) {
        final ImageView graphic =
                new ImageView(new Image(OwnNoteEditor.class.getResourceAsStream("/" + iconName), 20, 20, true, true));

        ButtonBase newButton;
        switch (buttonType) {
            case ClickButton: 
                newButton = new Button("", graphic);
                break;

            case ToggleButton: 
                newButton = new ToggleButton("", graphic);
                break;

            default: 
                newButton = new Button("", graphic);
                break;
        }

        newButton.setTooltip(new Tooltip(tooltipText));
        newButton.getStyleClass().add(cssStyleClass);
        if (buttonStyles != null) {
            newButton.getStyleClass().addAll(buttonStyles);
        }
        
        return newButton;
    }

    @SuppressWarnings("unchecked")
    private void saveNote() {
        assert (myEditor != null);

        final NoteData curNote = (NoteData) getUserData();
        // we might not have selected a note yet... accelerator always works :-(
        if (curNote != null) {
            if (myEditor.saveNoteWrapper(
                    curNote.getGroupName(), 
                    curNote.getNoteName(), 
                    getNoteText())) {
                hasBeenSaved();
            }
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
        mWebEngine.executeScript(script);
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
    
    // internal class for raw viewer
    private class RawViewer {
        private final Stage stage;
        private final TextArea htmlCode;
        
        public RawViewer() {
            https://docs.oracle.com/javafx/2/ui_controls/editor.htm
            stage = new Stage();

            htmlCode = new TextArea();
            htmlCode.setWrapText(true);
            htmlCode.setEditable(false);

            final StackPane root = new StackPane();
            root.setPadding(new Insets(1, 1, 1, 1));
            root.getChildren().add(htmlCode);

            final Scene scene = new Scene(new Group(), 400, 400);
            scene.setRoot(root);

            stage.setScene(scene);
        }
        
        public void setText(final String text) {
            htmlCode.setText(text);
        }
        
        public void show() {
            stage.show();
        }
        
        public void hide() {
            stage.hide();
        }
        
        public boolean isShowing() {
            return stage.isShowing();
        }
        
        public void setOnCloseRequest(final EventHandler<WindowEvent> eventHandler) {
            stage.setOnCloseRequest(eventHandler);        
        }
    }
}
