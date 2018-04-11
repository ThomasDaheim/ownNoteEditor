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

import com.sun.javafx.scene.control.skin.ContextMenuContent;
import com.sun.javafx.scene.control.skin.ContextMenuContent.MenuItemContainer;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import netscape.javascript.JSObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteHTMLEditor {
    private static final String TOP_TOOLBAR = ".top-toolbar";
    private static final String BOTTOM_TOOLBAR = ".bottom-toolbar";
    private static final String WEB_VIEW = ".web-view";
    private static final String CONTEXT_MENU = ".context-menu";
    private static final String RELOAD_PAGE = "Reload page";

    private static enum ButtonType {
        ClickButton,
        ToggleButton
    };
    
    private static final String UNDO_COMMAND = "undo";
    private static final String REDO_COMMAND = "redo";
    ButtonBase undoEdit = null;
    ButtonBase redoEdit = null;

    private WebView myWebView;
    private WebEngine myWebEngine;
    private WebPage myWebPage;
    private ToolBar myTopToolBar;
    private ToolBar myBottomToolBar;    
    final private OwnNoteHTMLEditor myself = this;
    
    private HTMLEditor myHTMLEditor = null;

    // https://github.com/Namek/TheConsole/blob/5fd635e14d16f2058a557b06ef2c30c71142280a/src/net/namekdev/theconsole/view/ConsoleOutput.xtend
    // have a command queue during the startup phase
    final private BlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
    private boolean editorInitialized = false;
    
    private RawViewer rawViewer = null;
    private CodeLoader codeLoader = null;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
            
    private String noteText = "";
    
    // allow drag & drop of certain file types
    final List<String> dragExtensions = Arrays.asList("jpg", "png", "gif", "txt");
    final List<String> dragImageExtensions = Arrays.asList("jpg", "png", "gif");
    
    private OwnNoteHTMLEditor() {
        super();
    }
    
    public OwnNoteHTMLEditor(final HTMLEditor htmlEditor, final OwnNoteEditor editor) {
        super();
        myHTMLEditor = htmlEditor;
        myEditor = editor;

        myWebView = (WebView) myHTMLEditor.lookup(WEB_VIEW);
        myWebEngine = myWebView.getEngine();
        myWebPage = Accessor.getPageFor(myWebEngine);
        
        rawViewer = new RawViewer();
        codeLoader = new CodeLoader();
        
        // delay setup of editor - things are not available at startup...
        Platform.runLater(() -> {
            initHTMLEditor();
            loadFrameHTML();
        });  
    }
    
    @SuppressWarnings("unchecked") 
    private void initHTMLEditor() {
        myTopToolBar = (ToolBar) myHTMLEditor.lookup(TOP_TOOLBAR);
        myBottomToolBar = (ToolBar) myHTMLEditor.lookup(BOTTOM_TOOLBAR);
        
        // TF, 20160724: why should we remove that editor feature?
        // remove: foreground & background control
        // hideNode(myHTMLEditor, ".html-editor-foreground", 1);
        // hideNode(myHTMLEditor, ".html-editor-background", 1);
        
        // remove: font type & font size control - the 2nd and 3rd control with "font-menu-button" style class
        // TFE, 20180304: htmleditor throws NPE if no font is set and empty line is added... 
        Node node;
        node = getNode(myHTMLEditor, ".font-menu-button", 2);
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
            if (node instanceof ComboBox) {
                // can't set item now since list is still empty :-(
                // need to wait until its populated...
                final ComboBox box = (ComboBox) node;
                box.itemsProperty().addListener(new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        box.getSelectionModel().selectFirst();
                    }
                });
            }
        }
        node = getNode(myHTMLEditor, ".font-menu-button", 3);
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
            if (node instanceof ComboBox) {
                final ComboBox box = (ComboBox) node;
                box.getSelectionModel().selectFirst();
            }
        }
        
        // add: insert link & picture controls
        addNoteEditorControls();
        
        // issue #40
        // https://stackoverflow.com/questions/20773249/javafx-htmleditor-doesnt-take-all-free-size-on-the-container
        GridPane.setHgrow(myWebView, Priority.ALWAYS);
        GridPane.setVgrow(myWebView, Priority.ALWAYS);        
    }

    private static Node getNode(final Node startNode, final String lookupString, final int occurence) {
        Node result = null;
        
        final Set<Node> nodes = startNode.lookupAll(lookupString);
        if (nodes != null && nodes.size() >= occurence) {
            // no simple way to ge nth member of set :-(
            Iterator<Node> itr = nodes.iterator();
            Node node = null;
            for(int i = 0; itr.hasNext() && i<occurence; i++) {
                node = itr.next();
            }
            if (node != null) {
                result = node;
            }
        }
        return result;
    }

    private void addNoteEditorControls() {
        // update edit - but only once
        if (myTopToolBar.lookup(".html-editor-insertlink") != null) {
            return;
        }

        // copy styles from other buttons in toolbar
        ObservableList<String> clickButtonStyles = null;
        Node node = myTopToolBar.lookup(".html-editor-cut");
        if (node != null && node instanceof Button) {
            clickButtonStyles = ((Button) node).getStyleClass();
            // not the own button style, please
            clickButtonStyles.removeAll("html-editor-cut");
        }
        ObservableList<String> toggleButtonStyles = null;
        node = myTopToolBar.lookup(".html-editor-bold");
        if (node != null && node instanceof Button) {
            toggleButtonStyles = ((Button) node).getStyleClass();
            // not the own button style, please
            toggleButtonStyles.removeAll("html-editor-bold");
        }

        // add separator
        myBottomToolBar.getItems().add(new Separator());
        
        // add button to insert checkbox
        final ButtonBase insertCheckbox = 
                    createEditorButton(ButtonType.ClickButton, MaterialDesignIcon.CHECKBOX_MARKED_OUTLINE, "Insert Checkbox", "html-editor-insertcheckbox", clickButtonStyles);
        insertCheckbox.setOnAction((ActionEvent t) -> {
            insertContentAfterCursor("<input type='checkbox'>");
        });
        
        myBottomToolBar.getItems().add(insertCheckbox);

        // add button to insert table
//        final ButtonBase insertTable = 
//                    createEditorButton(ButtonType.ClickButton, MaterialDesignIcon.TABLE_LARGE, "Insert Table", "html-editor-inserttable", clickButtonStyles);
//        // TODO: add action
//        myBottomToolBar.getItems().add(insertTable);
        
        // add separator
        myBottomToolBar.getItems().add(new Separator());
        
        // add button to insert link
        final ButtonBase insertLink = 
                    createEditorButton(ButtonType.ClickButton, FontAwesomeIcon.LINK, "Insert Link", "html-editor-insertlink", clickButtonStyles);
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
                    insertContentAfterCursor(hrefString);
                }
            }
            myHTMLEditor.requestFocus();
        });
        myBottomToolBar.getItems().add(insertLink);

        // add button to insert image
        final ButtonBase insertImage = 
                createEditorButton(ButtonType.ClickButton, FontAwesomeIcon.IMAGE, "Insert Image", "html-editor-insertimage", clickButtonStyles);
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
        myBottomToolBar.getItems().add(insertImage);
        
        // add button to insert formatted text
        final ButtonBase insertCode = 
                createEditorButton(ButtonType.ClickButton, MaterialDesignIcon.CODE_BRACES, "Insert code sample", "html-editor-insertcode", clickButtonStyles);
        insertCode.setOnAction((ActionEvent arg0) -> {
            // check if the cursor is in a code section - in that case we do replace and not insert!
            String prevLanguage = "";
            String prevCode = "";
            
            final Object result = wrapExecuteScript(myWebEngine, "saveGetCodeContent();");
            if (result != null && result instanceof String) {
                final String resultString = (String) result;
                
                final String [] prevParts = resultString.split("\\|");
                if (prevParts.length == 2) {
                    prevLanguage = prevParts[0];
                    prevCode = prevParts[1];
                }
            }
            
            codeLoader.setCode(unwrapText(prevCode, "code"));
            codeLoader.setLanguage(prevLanguage);
            codeLoader.showAndWait();
            
            if (codeLoader.getDoInsert()) {
                // todo: insert text & type
                final String language = codeLoader.getLanguage();
                final String code = codeLoader.getCode();
                
                String htmlData = String.format(
                        "<pre class='language-%s' contenteditable='false'><code class='language-%s'>%s</code></pre>",
                        language, language, StringEscapeUtils.escapeHtml4(code));
                
                if ("".equals(prevLanguage)) {
                    //insert html
                    insertContentAfterCursor(htmlData);
                } else {
                    replaceHtml(prevCode, htmlData);
                }
                // re-highlight with prism.js
                wrapExecuteScript(myWebEngine, "Prism.highlightAll(true);");
            }

            myHTMLEditor.requestFocus();
        });
        myBottomToolBar.getItems().add(insertCode);


        // add separator
        myBottomToolBar.getItems().add(new Separator());

        // add source button
        final ToggleButton viewSource = 
                (ToggleButton) createEditorButton(ButtonType.ToggleButton, FontAwesomeIcon.CODE, "View Source", "html-editor-bold", toggleButtonStyles);
        viewSource.setOnAction((ActionEvent arg0) -> {
            if (viewSource.isSelected()) {
                rawViewer.show();
                rawViewer.setText(getNoteText());
            } else {
                rawViewer.hide();
            }
        });
        // uncheck button when window gets closed
        rawViewer.setOnCloseRequest((WindowEvent event) -> {
            viewSource.setSelected(false);
        });        
        myBottomToolBar.getItems().add(viewSource);

        // add separator
        myBottomToolBar.getItems().add(new Separator());

        // Issue #12 - add print button
        // https://stackoverflow.com/questions/28847757/how-to-display-print-dialog-in-java-fx-and-print-node
        final ButtonBase printNote = 
                createEditorButton(ButtonType.ClickButton, FontAwesomeIcon.PRINT, "Print Note", "html-editor-print", clickButtonStyles);
        printNote.setOnAction((ActionEvent arg0) -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(myHTMLEditor.getScene().getWindow())){
                myHTMLEditor.print(job);
                job.endJob();
            }
            myHTMLEditor.requestFocus();
        });
        myBottomToolBar.getItems().add(printNote);

        // add separator
        myBottomToolBar.getItems().add(new Separator());

        // add save button
        final ButtonBase saveNote = 
                createEditorButton(ButtonType.ClickButton, FontAwesomeIcon.SAVE, "Save Note", "html-editor-save", clickButtonStyles);
        saveNote.setOnAction((ActionEvent arg0) -> {
            saveNote();
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        myBottomToolBar.getItems().add(saveNote);
        // issue #41 - add CTRL+S to button and not to context menu
        saveNote.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> {
            saveNote.fire();
        });

        // issue #56: add undo & redo button
        // they should go at the beginning of toolbar #1 - so we need to remove everything first...
        List<Node> toolbarItems = new ArrayList<>(myTopToolBar.getItems());
        myTopToolBar.getItems().clear();

        // add undo button
        undoEdit = createEditorButton(ButtonType.ClickButton, MaterialDesignIcon.UNDO, "Undo", "html-editor-undo", clickButtonStyles);
        undoEdit.setOnAction((ActionEvent arg0) -> {
            if (myWebPage.queryCommandEnabled(UNDO_COMMAND)) {
                final String curText = getNoteText();
                myWebPage.executeCommand(UNDO_COMMAND, null);
                // in some cases the first call do UNDO doesn't change anything, only the second... maybe one day I'll find a fix
                if (curText.equals(getNoteText())) {
                    myWebPage.executeCommand(UNDO_COMMAND, null);
                }
            }
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        myTopToolBar.getItems().add(undoEdit);
        undoEdit.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), () -> {
            undoEdit.fire();
        });
        
        // add redo button
        redoEdit = createEditorButton(ButtonType.ClickButton, MaterialDesignIcon.REDO, "Redo", "html-editor-redo", clickButtonStyles);
        redoEdit.setOnAction((ActionEvent arg0) -> {
            if (myWebPage.queryCommandEnabled(REDO_COMMAND)) {
                final String curText = getNoteText();
                myWebPage.executeCommand(REDO_COMMAND, null);
                // in some cases the first call do UNDO doesn't change anything, only the second... maybe one day I'll find a fix
                if (curText.equals(getNoteText())) {
                    myWebPage.executeCommand(REDO_COMMAND, null);
                }
            }
            setUndoRedo();
            myHTMLEditor.requestFocus();
        });
        myTopToolBar.getItems().add(redoEdit);
        redoEdit.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), () -> {
            redoEdit.fire();
        });

        // add separator
        myTopToolBar.getItems().add(new Separator());

        // and now once again all other controls
        myTopToolBar.getItems().addAll(toolbarItems);
        
        // TODO: this only catches keys typed but not formatting via buttons - no good idea how this can be monitored
        // set undo / redo based on command state
        myHTMLEditor.addEventHandler(Event.ANY, (Event event) -> {
            setUndoRedo();
            
            // update raw viewer if visible
            if (rawViewer.isShowing()) {
                rawViewer.setText(getNoteText());
            }
        });
    }
    
    private void setUndoRedo() {
        if (undoEdit != null && redoEdit != null) {
            undoEdit.setDisable(!myWebPage.queryCommandEnabled(UNDO_COMMAND));
            redoEdit.setDisable(!myWebPage.queryCommandEnabled(REDO_COMMAND));
        }
    }
    
    private ButtonBase createEditorButton(
            final ButtonType buttonType, 
            final GlyphIcons iconName, 
            final String tooltipText, 
            final String cssStyleClass, 
            final ObservableList<String> buttonStyles) {
        final Text fontAwesomeIcon = GlyphsDude.createIcon(iconName, "16");

        ButtonBase newButton;
        switch (buttonType) {
            case ClickButton: 
                newButton = new Button("", fontAwesomeIcon);
                break;

            case ToggleButton: 
                newButton = new ToggleButton("", fontAwesomeIcon);
                break;

            default: 
                newButton = new Button("", fontAwesomeIcon);
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
     * Inserts content after the current cursor. If anything is selected, they
     * get replaced with new content.
     *
     * @param content Content to insert.
     */
    private void insertContentAfterCursor(String content) {
        wrapExecuteScript(myWebEngine, "saveInsertContent('" + replaceSpecialChars(content) + "');");
    }

    /**
     * Replace html content with new content.
     *
     * @param prevHtml Html to be replace.
     * @param newHtml Html to replace with.
     */
    private void replaceHtml(final String prevHtml, final String newHtml) {
        //System.out.println("saveReplaceContent('" + replaceSpecialChars(prevHtml) + "', '"+ replaceSpecialChars(newHtml) + "');");
        wrapExecuteScript(myWebEngine, "saveReplaceContent('" + replaceSpecialChars(prevHtml) + "', '"+ replaceSpecialChars(newHtml) + "');");
    }
    
    /**
     * Imports an image file. Encode in base64 before inserting as img tag in the note
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
            final String type = java.nio.file.Files.probeContentType(file.toPath());
            //get html content
            final byte[] data = org.apache.commons.io.FileUtils.readFileToByteArray(file);
            final String base64data = java.util.Base64.getEncoder().encodeToString(data);
            final String htmlData = String.format(
                    "<img src='data:%s;base64,%s' type='%s' />",
                    type, base64data, type);
            //insert html
            insertContentAfterCursor(htmlData);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Imports an text file.
     *
     * @param file Text file.
     */
    private void importTextFile(File file) {
        try {
            //check if file is too big
            if (file.length() > 1024 * 1024) {
                throw new VerifyError("File is too big.");
            }
            // get text content of file
           final String data = org.apache.commons.io.FileUtils.readFileToString(file);
            //insert html
            insertContentAfterCursor(data);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //
    // javascript enabling
    //
    
    /**
     * Enables Firebug Lite for debugging a webEngine.
     * @param engine the webEngine for which debugging is to be enabled.
     */
    private static void enableFirebug(final WebEngine engine) {
        engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}"); 
    }

    /**
     * Add try/catch around any script call.
     * @param engine the webEngine for which debugging is to be enabled.
     * @param script the javascript to execute.
     */
    private static Object wrapExecuteScript(final WebEngine engine, final String script) {
        Object result = null;
        
        try {
            //System.out.println("script: " + script);
            result = engine.executeScript(script);
        } catch (Exception ex) {
            Logger.getLogger(OwnNoteHTMLEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
    private void loadFrameHTML() {
        // https://stackoverflow.com/questions/10021433/jsexception-in-webview-of-java-fx
        myWebEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                // if script html has been loaded don't overwrite it with anything
                if (editorInitialized) {
                    Platform.runLater(() -> {
                        myWebEngine.getLoadWorker().cancel();
                    });
                } else if (newState == Worker.State.SUCCEEDED) {
                    // don't open links in htmleditor but in the default browser
                    // https://stackoverflow.com/questions/36842025/javafx-htmleditor-hyperlinks/36844879#36844879
                    myWebEngine.locationProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                        final HostServices hostServices = (HostServices) myHTMLEditor.getScene().getWindow().getProperties().get("hostServices");
                        // first load is OK :-)
                        if (hostServices != null && editorInitialized) {
                            hostServices.showDocument(newValue);
                        }
                    });                    

                    //System.out.println("Worker.State.SUCCEEDED");
                    //System.out.println(wrapExecuteScript(myWebEngine, "document.documentElement.outerHTML;"));
                    startHTMLEditor();

                    // issue #41 - add CTRL+S to button and not to context menu
                    // add a context menu for saving
                    // http://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine/27047819#27047819
                    myWebView.setOnContextMenuRequested((ContextMenuEvent e) -> {
                        getPopupWindow();
                    });
                    
                    myWebView.setOnDragOver(myself::handleOnDragOver);
                    myWebView.setOnDragDropped(myself::handleOnDragDropped);
                }
            }
        });

        // don't use HTMLEditor.setHtml() - internally it ends up doing webPage.load
        // BUT only webPage.open leads to execution of javascript...
        // AND that is only called from WebEngine.load
        // OH, THE IRONY
        final String editor_script = OwnNoteHTMLEditor.class.getResource("/js/editor.html").toExternalForm();
        myWebEngine.load(editor_script);
    }
    
    private void startHTMLEditor() {
        //enableFirebug(myWebEngine);
        
        JSObject window = (JSObject) wrapExecuteScript(myWebEngine, "window");

        JavascriptLogger javascriptLogger = new JavascriptLogger();
        window.setMember("javascriptLogger", javascriptLogger);
        wrapExecuteScript(myWebEngine, "console.log = function(message)\n" +
            "{\n" +
            "    javascriptLogger.log(message);\n" +
            "};");
        //wrapExecuteScript(myWebEngine, "console.log(\"Testmessage\");");

        initEditorDone();
    }
    
    //
    // drag & drop support + context menu
    //
    
    private void handleOnDragOver(DragEvent e) {
        // TODO: check if above a non-editable element
        Dragboard db = e.getDragboard();
        if (db.hasFiles()) {
            boolean acceptDrag = false;
            // accept images, text files, ...
            for (File dragFile : e.getDragboard().getFiles()) {
                if (dragExtensions.contains(FilenameUtils.getExtension(dragFile.getName()).toLowerCase())) {
                    acceptDrag = true;
                }
            }
            if (acceptDrag) {
                e.acceptTransferModes(TransferMode.COPY);
            } else {
                e.acceptTransferModes(TransferMode.NONE);
            }
        } else {
            e.consume();
        }
    }
    
    private void handleOnDragDropped(DragEvent e) {
        boolean success = false;
        if (e.getDragboard().hasFiles()) {
            // insert file after file
            for (File dragFile : e.getDragboard().getFiles()) {
                if (dragExtensions.contains(FilenameUtils.getExtension(dragFile.getName()).toLowerCase())) {
                    if (dragImageExtensions.contains(FilenameUtils.getExtension(dragFile.getName()).toLowerCase())) {
                        // we have an image...
                        importDataFile(dragFile);
                    } else {
                        // good ol plain text
                        importTextFile(dragFile);
                    }
                }
            }
            success = true;
        }
        e.setDropCompleted(success);
        e.consume();
    }
    
    private PopupWindow getPopupWindow() {
        @SuppressWarnings("deprecation") 
        final Iterator<Window> windows = Window.impl_getWindows();

        while (windows.hasNext()) {
            final Window window = windows.next();

            if (window instanceof ContextMenu) {
                if (window.getScene() != null && window.getScene().getRoot() != null) { 
                    final Parent root = window.getScene().getRoot();

                    // access to context menu content
                    if (!root.getChildrenUnmodifiable().isEmpty()) {
                        Node popup = root.getChildrenUnmodifiable().get(0);
                        if (popup.lookup(CONTEXT_MENU) != null) {
                            final Node bridge = popup.lookup(CONTEXT_MENU);
                            final ContextMenuContent cmc = (ContextMenuContent)((Parent) bridge).getChildrenUnmodifiable().get(0);

                            final VBox itemsContainer = cmc.getItemsContainer();
                            
                            // check for "Reload page" entry and remove it...
                            int indexReload = -1;
                            for (Node n: itemsContainer.getChildren()) {
                                assert n instanceof MenuItemContainer;
                                
                                final MenuItemContainer item = (MenuItemContainer) n;
                                if (RELOAD_PAGE.equals(item.getItem().getText())) {
                                    indexReload = itemsContainer.getChildren().indexOf(n);
                                }
                            }
                            if (indexReload > -1) {
                                itemsContainer.getChildren().remove(indexReload);
                            }

                            // adding save item
                            final MenuItem saveMenu = new MenuItem("Save");
                            saveMenu.setOnAction((ActionEvent event) -> {
                                saveNote();
                            });
                            
                            // add new item:
                            itemsContainer.getChildren().add(cmc.new MenuItemContainer(saveMenu));

                            return (PopupWindow)window;
                        }
                    }
                }
                return null;
            }
        }
        return null;
    }    

    //
    // task handling
    //
    
    private void initEditorDone() {
        editorInitialized = true;
        
        while (!myQueue.isEmpty()) {
            myQueue.poll().run();
        }
    }

    private void startTask(final Runnable task) {
        if (!editorInitialized) {
            myQueue.add(task);
        } else {
            // anything left in the queue has to run first
            while (!myQueue.isEmpty()) {
                myQueue.poll().run();
            }
            
            task.run();
        }
    }
  
    //
    // transformations text in & out
    //
    
    private String replaceSpecialChars(final String text) {
        String result = text;
        result = result
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace(System.getProperty("line.separator"), "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
        return result;
    }
    
    private String unwrapText(final String html, final String htmltag) {
        String result = html;

        final Document doc = Jsoup.parse(html);
        // Issue #55: seems we now also get another font tag that we don't want to have: "<span style="font-family: 'Segoe UI';">"
        doc.getElementsByAttributeValueStarting("style", "font-family").unwrap();
        // get rid of "<font face="Segoe UI">" tags - see bug report https://bugs.openjdk.java.net/browse/JDK-8133833
        // TF, 20160724: match only font + face since we also want to allow foreground / background colors
        doc.getElementsByAttributeValueMatching("face", Pattern.compile(".*")).unwrap();
        // get rid of spans added by prism.js: <span class="token BUT only inside <code class="language-
        final Elements elements = doc.getElementsByAttributeValueMatching("class", "language-*");
        for (Element element : elements) {
            if ("code".equals(element.tag().getName())) {
                element.getElementsByAttributeValueStarting("class", "token").unwrap();
            }
        }

        // only store content in "filter" element - if any
        // replace spaces at the end after </p>
        result = doc.select(htmltag).html().replaceAll("\\s+\\n", "\n");
        
        return result.replaceAll("\\s+\\n", "\n");
    }

    //
    // our offering to the outside world
    //
    
    public String getNoteText() {
        String result = "";

        if (editorInitialized) {
            result = (String) wrapExecuteScript(myWebEngine, "saveGetContent();");
            
            result = unwrapText(result, "body");

            //System.out.println("noteHtml: " + noteHtml);
        }

        return result;
    }
    
    public void setNoteText(final String text) {
        Runnable task = () -> {
            wrapExecuteScript(myWebEngine, "saveSetContent('" + replaceSpecialChars(text) + "');");
            
            noteText = getNoteText();

            setUndoRedo();
       };
        
        // might not yet been executed (still loading) but its still our new text
        noteText = text;
        
        startTask(task);
    }
    
    public boolean hasChanged() {
        boolean result = false;
        
        if (editorInitialized) {
            final String newNoteText = getNoteText();

            if (noteText == null) {
                result = (newNoteText != null);
            } else {
                result = !noteText.equals(newNoteText);
            }
            
            if (result) {
                //System.out.println("noteText: " + noteText);
                //System.out.println("newNoteText: " + newNoteText);
            }
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
    
    // https://stackoverflow.com/questions/28687640/javafx-8-webengine-how-to-get-console-log-from-javascript-to-system-out-in-ja
    public class JavascriptLogger
    {
        public void log(String text) {
            System.out.println(text);
        }
    }
}
