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

import com.sun.javafx.scene.control.ContextMenuContent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import netscape.javascript.JSObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;
import tf.ownnote.ui.general.KeyCodesHelper;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteHTMLEditor {
    private static final String CONTEXT_MENU = ".context-menu";
    private static final String RELOAD_PAGE = "Reload page";
    private static final String OPEN_FRAME_NEW_WINDOW = "Open Frame in New Window";
    private static final String OPEN_LINK = "Open Link";
    private static final String OPEN_LINK_NEW_WINDOW = "Open Link in New Window";
    private static final List<String> REMOVE_MENUES =  List.of(RELOAD_PAGE, OPEN_FRAME_NEW_WINDOW, OPEN_LINK, OPEN_LINK_NEW_WINDOW);
    private static final String COPY_SELECTION = "Copy";
    private static final String PASTE_SELECTION = "Paste";

    private WebView myWebView;
    private WebEngine myWebEngine;
    final private OwnNoteHTMLEditor myself = this;
    
    private HostServices myHostServices = null;
    
    final Clipboard myClipboard = Clipboard.getSystemClipboard();
    
    // https://github.com/Namek/TheConsole/blob/5fd635e14d16f2058a557b06ef2c30c71142280a/src/net/namekdev/theconsole/view/ConsoleOutput.xtend
    // have a command queue during the startup phase
    final private BlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
    private boolean editorInitialized = false;
    private boolean setContentDone = false;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private String editorText = "";
    
    // defy garbage collection of callback functions
    // https://stackoverflow.com/a/41908133
    private JavascriptLogger javascriptLogger;
    private EditorCallback editorCallback;
    
    // TFE, 20181002: enums to support drag & drop
    // what do we need to do with a file that is dropped on us?
    private static enum FileCopyMode {
        COPY,
        IMPORT
    }

    // what extensions can we accept?
    private static enum AcceptableFileType {
        TXT("txt", FileCopyMode.COPY),
        HTM("htm", FileCopyMode.COPY),
        HTML("html", FileCopyMode.COPY),
        XML("xml", FileCopyMode.COPY),
        JAVA("java", FileCopyMode.COPY),
        JS("js", FileCopyMode.COPY),
        CSS("css", FileCopyMode.COPY),
        JPG("jpg", FileCopyMode.IMPORT),
        PNG("png", FileCopyMode.IMPORT),
        GIF("gif", FileCopyMode.IMPORT);
        
        private final String extension;
        private final FileCopyMode copyMode;
        
        AcceptableFileType(final String ext, final FileCopyMode cMode) {
            extension = ext;
            copyMode = cMode;
        }
        
        String getExtension() {
            return extension;
        }
        
        FileCopyMode getFileCopyMode() {
            return copyMode;
        }
    }
    
    private OwnNoteHTMLEditor() {
        super();
    }
    
    public OwnNoteHTMLEditor(final WebView webView, final OwnNoteEditor editor) {
        super();

        myEditor = editor;

        myWebView = webView;
        myWebEngine = myWebView.getEngine();
        
        myHostServices = (HostServices) myWebView.getScene().getWindow().getProperties().get("hostServices");

        // delay setup of editor - things are not available at startup...
        Platform.runLater(() -> {
            initWebView();
        });  
    }
    /**
     * Enables Firebug Lite for debugging a webEngine.
     * @param engine the webEngine for which debugging is to be enabled.
     */
    private static void enableFirebug(final WebEngine engine) {
        wrapExecuteScript(engine, "if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}"); 
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
  
    private void initWebView() {
        // issue #40
        // https://stackoverflow.com/questions/20773249/javafx-htmleditor-doesnt-take-all-free-size-on-the-container
        GridPane.setHgrow(myWebView, Priority.ALWAYS);
        GridPane.setVgrow(myWebView, Priority.ALWAYS);
        
        myWebEngine.setJavaScriptEnabled(true);

        // https://stackoverflow.com/questions/10021433/jsexception-in-webview-of-java-fx
        myWebEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED && !editorInitialized) {
                    JSObject window = (JSObject) myWebEngine.executeScript("window");

                    javascriptLogger = new JavascriptLogger();
                    window.setMember("javascriptLogger", javascriptLogger);
                    wrapExecuteScript(myWebEngine, "console.log = function(message)\n" +
                        "{\n" +
                        "    javascriptLogger.log(message);\n" +
                        "};");
                    //myWebEngine.executeScript("console.log(\"Testmessage\");");

                    editorCallback = new EditorCallback();
                    window.setMember("editorCallback", editorCallback);
                    
                    wrapExecuteScript(myWebEngine, "initEditor();");

                    // issue #41 - add CTRL+S to button and not to context menu
                    // add a context menu for saving
                    // http://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine/27047819#27047819
                    myWebView.setOnContextMenuRequested((ContextMenuEvent e) -> {
                        getPopupWindow();
                    });
                    
                    // support for drag & drop
                    // https://stackoverflow.com/a/39118740
                    myWebView.setOnDragOver(myself::handleOnDragOver);
                    myWebView.setOnDragDropped(myself::handleOnDragDropped);
                    
                    // listeners for CTRL+S and CTRL+C
                    myWebView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                        @Override
                        public void handle(KeyEvent event) {
                            if (KeyCodesHelper.KeyCodes.CNTRL_C.match(event)) {
                                copyToClipboard(true);
                            }
                            if (KeyCodesHelper.KeyCodes.CNTRL_S.match(event)) {
                                saveNote();
                            }
                        }
                    });

                    // add debugger to webviewer
                    //enableFirebug(myWebEngine);
                }
            }
        });

        final String editor_script = OwnNoteHTMLEditor.class.getResource("/tinymceEditor.html").toExternalForm();
        myWebView.getEngine().load(editor_script);
    }
    
    //
    // support for drag & drop
    // http://stackoverflow.com/questions/24655437/javafx-webview-html5-draganddrop/39118740#39118740
    //
    
    private void handleOnDragOver(final DragEvent event) {
        final Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            for (File file:db.getFiles()) {
                // check file types against acceptable ones
                if (acceptableDragboard(file) != null) {
                    // one is enough...
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    break;
                }
            }
        } else {
            event.consume();
        }
    }

    private void handleOnDragDropped(final DragEvent event) {
        boolean success = false;

        final Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            for (File file:db.getFiles()) {
                // check file types against acceptable ones
                final AcceptableFileType result = acceptableDragboard(file);
                if (result != null) {
                    // add to note as file type teels us
                    addFileToNote(file, result);
                }
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }
    
    private AcceptableFileType acceptableDragboard(final File file) {
        AcceptableFileType result = null;
        
        for (AcceptableFileType fileType : AcceptableFileType.values()) { 
            if (fileType.getExtension().equals(FilenameUtils.getExtension(file.getName()).toLowerCase())) {
                result = fileType;
                break;
            }
        }
        
        return result;
    }
    
    private void addFileToNote(final File file, final AcceptableFileType fileType) {
        if (FileCopyMode.COPY.equals(fileType.getFileCopyMode())) {
            // read text and insert...
            importTextFile(file);
        } else {
            // a picture... luckily we already know how to deal with that :-)
            importMediaFile(file);
        }
    }
    
    //
    // javascript callbacks
    //
    
    private void initEditorDone() {
        editorInitialized = true;
        
        while (!myQueue.isEmpty()) {
            myQueue.poll().run();
        }
    }

    private void setContentDone() {
        setContentDone = true;
    }

    private void insertMedia() {
        //System.out.println("insertMedia");
        final List<String> extFilter = Arrays.asList("*.jpg", "*.png", "*.gif");
        final List<String> extValues = Arrays.asList("jpg", "png", "gif");

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Embed an image (Size < 1MB)");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Pictures", extFilter));
        final File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            if (extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                // we really have selected a picture - now add it
                // issue #31: we shouldn't add the link but the image data instead!
                // see https://github.com/dipu-bd/CustomControlFX/blob/master/CustomHTMLEditor/src/org/sandsoft/components/htmleditor/CustomHTMLEditor.java
                importMediaFile(selectedFile);
            }                        
        }
    }
    
    /**
     * Imports an image file.
     *
     * @param file Image file.
     */
    private void importMediaFile(File file) {
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
            
            //insert html
            wrapExecuteScript(myWebEngine, "insertMedia('" + type + "', '" + base64data + "');");
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Imports a text file.
     * No conversions done, content is inserted as is
     *
     * @param file Text file.
     */
    private void importTextFile(File file) {
        try {
            //check if file is too big
            if (file.length() > 1024 * 1024) {
                throw new VerifyError("File is too big.");
            }
            
            // read file as text
            String content = new String(Files.readAllBytes(file.toPath()));
            
            content = content.replace("'", "\\'");
            content = content.replace(System.getProperty("line.separator"), "\\n");
            content = content.replace("\n", "\\n");
            content = content.replace("\r", "\\n");
            
            //insert text
            wrapExecuteScript(myWebEngine, "insertText('" + content + "');");
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void printNote() {
        assert (myEditor != null);
        
        // https://docs.oracle.com/javafx/8/webview/printing.htm
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(myWebView.getScene().getWindow())){
            myWebEngine.print(job);
            job.endJob();
        }
        myWebView.requestFocus();
    }
    
    private void saveNote() {
        assert (myEditor != null);

        final NoteData curNote = (NoteData) getUserData();
        // we might not have selected a note yet... accelerator always works :-(
        if (curNote != null) {
            if (myEditor.saveNoteWrapper(curNote.getGroupName(), curNote.getNoteName(), getNoteText())) {
                hasBeenSaved();
            }
        }
    }
    
    private PopupWindow getPopupWindow() {
        final ObservableList<Window> windows = Window.getWindows();

        for (Window window : windows) {
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
                            
                            // check for "Reload page", ... entry and remove it...
                            int index = 0;
                            int copyIndex = -1;
                            List<Node> deleteNodes = new ArrayList<>();
                            for (Node n: itemsContainer.getChildren()) {
                                assert n instanceof ContextMenuContent.MenuItemContainer;
                                
                                final ContextMenuContent.MenuItemContainer item = (ContextMenuContent.MenuItemContainer) n;
                                if (REMOVE_MENUES.contains(item.getItem().getText())) {
                                    deleteNodes.add(n);
                                }
                                
                                // TFE, 20181209: need to monitor "Copy" in order to add copied text to the OS clipboard as well
                                if (COPY_SELECTION.equals(item.getItem().getText())) {
                                    item.getItem().setOnAction((t) -> {
                                        copyToClipboard(true);
                                    });
                                    
                                    copyIndex = index;
                                }
                                
                                index++;
                            }
                            if (copyIndex != -1) {
                                // TFE, 20191211: add option to copy plain text as well
                                final MenuItem copyPlain = new MenuItem("Copy Text");
                                copyPlain.setOnAction((ActionEvent event) -> {
                                    copyToClipboard(false);
                                });

                                // add new item:
                                itemsContainer.getChildren().add(copyIndex+1, cmc.new MenuItemContainer(copyPlain));
                            }
                            if (!deleteNodes.isEmpty()) {
                                itemsContainer.getChildren().removeAll(deleteNodes);
                            }

                            // adding save item
                            final MenuItem saveMenu = new MenuItem("Save");
                            saveMenu.setOnAction((ActionEvent event) -> {
                                saveNote();
                            });
                            // not working... BUT still here to show short cut :-) 
                            // work is done in myWebView.addEventHandler(KeyEvent.KEY_PRESSED...
                            saveMenu.setAccelerator(KeyCodesHelper.KeyCodes.CNTRL_S.getKeyCode());
                            
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
    private void copyToClipboard(final boolean copyFullHTML) {
        Platform.runLater(() -> {
            Object dummy = wrapExecuteScript(myWebEngine, "saveGetSelection();");
            
            assert (dummy instanceof String);
            String selection = (String) dummy;
            
            if (!copyFullHTML) {
                // TFE, 2091211: remove html tags BUT convert </p> to </p> + line break
                selection = selection.replaceAll("\\</p\\>", "</p>" + System.lineSeparator());
                selection = selection.replaceAll("\\<.*?\\>", "");
                // convert all &uml; back to &
                selection = StringEscapeUtils.unescapeHtml4(selection);
            }
            
            final ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(selection);
            clipboardContent.putHtml(selection);
            myClipboard.setContent(clipboardContent);
        });
    }
    
    //
    // javascript callbacks
    //
    
    private void openLinkInDefaultBrowser(final String url) {
        // first load is OK :-)
        if (myHostServices != null && editorInitialized) {
            myHostServices.showDocument(url);
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
        
    public void setNoteText(final String text) {
        Runnable task = () -> {
            //System.out.println("setEditorText " + text);
            setContentDone = false;
            wrapExecuteScript(myWebEngine, "saveSetContent('" + replaceForEditor(text) + "');");
        };
        
        startTask(task);
        editorText = text;
    }
    private String replaceForEditor(final String text) {
        String result = text;

        // https://stackoverflow.com/questions/17802239/jsexception-while-loading-a-file-in-a-codemirror-based-editor-using-java-using-s
        // TFE, 20181030: for tinymce we also need to escape \
        result = result.replace("\\", "\\\\");
        result = result.replace("'", "\\'");
        result = result.replace(System.getProperty("line.separator"), "\\n");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\n");
        
        return result;
    }

    public String getNoteText() {
        editorText = readNoteText();
        return editorText;
    }

    private String readNoteText() {
        String newEditorText = "";

        if (editorInitialized) {
            Object dummy = wrapExecuteScript(myWebEngine, "saveGetContent();");
            
            assert (dummy instanceof String);
            newEditorText = (String) dummy;

            //System.out.println("readEditorText " + newEditorText);
        }

        return newEditorText;
    }

    public boolean hasChanged() {
        boolean result = false;

        // only try to read context when we had the callback from last setcontent() javascript call
        if (setContentDone) {
            final String newEditorText = readNoteText();

            if (editorText == null) {
                result = (newEditorText != null);
            } else {
                result = !editorText.equals(newEditorText);
                // System.out.println("================");
                // System.out.println(editorText);
                // System.out.println("================");
                // System.out.println(newEditorText);
                // System.out.println("================");
                // System.out.println(result);
                // System.out.println("================");
                // System.out.println("");
                
                //if (result) {
                //    System.out.println("editorText: " + editorText);
                //    System.out.println("newEditorText: " + newEditorText);
                //}
            }
        }
        
        return result;
    }
    
    public void hasBeenSaved() {
        // re-set stored text to current text to re-start change tracking
        // TFE, 20180924: avoid multiple reads in save process
        //editorText = getEditorText();
        wrapExecuteScript(myWebEngine, "tinymce.activeEditor.setDirty(false);");
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
            final NoteData editNote = (NoteData) getUserData();
            
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
        myWebView.setDisable(b);
    }

    public void setVisible(final boolean b) {
        myWebView.setVisible(b);
    }
    
    public NoteData getUserData() {
        return (NoteData) myWebView.getUserData();
    }

    public void setUserData(final NoteData data) {
        myWebView.setUserData(data);
    }
    
    public BooleanProperty visibleProperty() {
        return myWebView.visibleProperty();
    }

    // https://stackoverflow.com/questions/28687640/javafx-8-webengine-how-to-get-console-log-from-javascript-to-system-out-in-ja
    public class JavascriptLogger
    {
        public void log(String text) {
            System.out.println(text);
        }
    }
    
    public class EditorCallback
    {
        // loading of the editor has been completed - we can run the queue
        public void initEditorDone() {
            myself.initEditorDone();
        }
        // loading of the content has been completed - we can do ???
        public void setContentDone() {
//            System.out.println("Java: setContentDone() called");
            myself.setContentDone();
//            System.out.println("Java: setContentDone() done");
        }
        public void insertImage() {
//            System.out.println("Java: insertMedia() called");
            myself.insertMedia();
//            System.out.println("Java: insertMedia() done");
        }
        public void printNote() {
//            System.out.println("Java: saveNote() called");
            myself.printNote();
//            System.out.println("Java: saveNote() done");
        }
        public void saveNote() {
//            System.out.println("Java: saveNote() called");
            myself.saveNote();
//            System.out.println("Java: saveNote() done");
        }
        public void openLinkInDefaultBrowser(final String url) {
            myself.openLinkInDefaultBrowser(url);
        }
    }
}
