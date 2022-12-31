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
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import javafx.scene.control.Menu;
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
import javafx.stage.Window;
import javax.imageio.ImageIO;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import netscape.javascript.JSObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.unbescape.html.HtmlEscape;
import tf.helper.general.ImageHelper;
import tf.helper.javafx.UsefulKeyCodes;
import tf.ownnote.ui.links.LinkManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteMetaData;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tasks.TaskData;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteHTMLEditor {
    // TFE, 20201216: speed up searching in long notes
    private final static Pattern TAG_PATTERN = Pattern.compile("\\<.*?\\>");
    private final static Pattern IMAGE_PATTERN = Pattern.compile("img src=['\"]data:image");
    private final static String DATA_START = "img src=\"data:image/";
    private final static String BASE64_START = ";base64,";

    // TFE, 20200504: support more than one language here
    private static final String CONTEXT_MENU = ".context-menu";
    private static final List<String> RELOAD_PAGE = List.of("Reload page", "Seite neu laden");
    private static final List<String> OPEN_FRAME_NEW_WINDOW = List.of("Open Frame in New Window", "Frame in neuem Fenster \u00f6ffnen");
    private static final List<String> OPEN_LINK = List.of("Open Link", "Link \u00f6ffnen");
    private static final List<String> OPEN_LINK_NEW_WINDOW = List.of("Open Link in New Window", "Link in neuem Fenster \u00f6ffnen");
    private static final List<List<String>> REMOVE_MENUES =  List.of(RELOAD_PAGE, OPEN_FRAME_NEW_WINDOW, OPEN_LINK, OPEN_LINK_NEW_WINDOW);
    
    private static final List<String> COPY_SELECTION = List.of("Copy", "Kopieren");
    private static final List<String> COPY_TEXT_SELECTION = List.of("Copy as text", "Kopieren als Text");
    private static final List<String> PASTE = List.of("Paste", "Einf\u00fcgen");
    private static final List<String> PASTE_TEXT = List.of("Paste as text", "Einf\u00fcgen als Text");
    private static final List<String> SAVE_NOTE = List.of("Save", "Speichern");
    // TFE, 20210304: add context menu to insert attachment link into note
    private static final List<String> ATTACHMENT_LINK = List.of("Insert attachment link", "Anhang-Link einf\u00fcgen");
    private static final List<String> NOTE_LINK = List.of("Insert note link", "Note-Link einf\u00fcgen");
    private static final List<String> COMPRESS_IMAGES = List.of("Compress images", "Bilder komprimieren");
    private static final List<String> REPLACE_CHECKEDBOXES = List.of("Checked box -> " + TaskData.ARCHIVED_BOX, "Checked Box -> " + TaskData.ARCHIVED_BOX);
    private static final List<String> REPLACE_CHECKMARKS = List.of(TaskData.ARCHIVED_BOX + " -> Checked box", TaskData.ARCHIVED_BOX + " -> Checked Box");

    // complete list of menu items for language check
    private static final List<List<String>> TINYMCE_MENUES =  List.of(COPY_SELECTION, RELOAD_PAGE, OPEN_FRAME_NEW_WINDOW, OPEN_LINK, OPEN_LINK_NEW_WINDOW);
    
    // prefix for links to other notes
    public final static String NOTE_HTML_LINK_TYPE = "file:///";
    
    private int language = -1;

    private WebView myWebView;
    private WebEngine myWebEngine;
    // needed for use as javascriprt callback
    final private OwnNoteHTMLEditor myself = this;
    
    private HostServices myHostServices = null;
    
    final Clipboard myClipboardFx = Clipboard.getSystemClipboard();
    final java.awt.datatransfer.Clipboard myClipboardAwt = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    // https://github.com/Namek/TheConsole/blob/5fd635e14d16f2058a557b06ef2c30c71142280a/src/net/namekdev/theconsole/view/ConsoleOutput.xtend
    // have a command queue during the startup phase
    final private BlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
    private boolean editorInitialized = false;
    private boolean setContentDone = false;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor = null;
    
    private Note editedNote;
    
    // linked list to maintain order of callbacks
    private final List<IFileContentChangeSubscriber> changeSubscribers = new LinkedList<>();
    
    // TFE, 20181002: enums to support drag & drop
    // what do we need to do with a file that is dropped on us?
    private static enum FileCopyMode {
        COPY,
        IMPORT
    }
    
    private boolean isDialogueOpen = false;

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
        
        myHostServices = (HostServices) myEditor.getWindow().getProperties().get("hostServices");

        // delay setup of editor - things are not available at startup...
        Platform.runLater(() -> {
            initWebView();
        });  
    }
    
    public void subscribe(final IFileContentChangeSubscriber subscriber) {
        if (subscriber == null) {
            return;
        }

        changeSubscribers.add(subscriber);
    }
    
    public void unsubscribe(final IFileContentChangeSubscriber subscriber) {
        if (subscriber == null) {
            return;
        }
        
        changeSubscribers.remove(subscriber);
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

                    window.setMember("editorCallback", myself);
                    wrapExecuteScript(myWebEngine, "console.log = function(message)\n" +
                        "{\n" +
                        "    editorCallback.log(message);\n" +
                        "};");
                    //myWebEngine.executeScript("console.log(\"Testmessage\");");
                    
                    wrapExecuteScript(myWebEngine, "initEditor();");

                    // issue #41 - add CTRL+S to button and not to context menu
                    // add a context menu for saving
                    // http://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine/27047819#27047819
                    myWebView.setOnContextMenuRequested((ContextMenuEvent e) -> {
                        initPopupWindow();
                    });
                    
                    // support for drag & drop
                    // https://stackoverflow.com/a/39118740
                    myWebView.setOnDragOver(myself::handleOnDragOver);
                    myWebView.setOnDragDropped(myself::handleOnDragDropped);
                    
                    // listeners for CTRL+S and CTRL+C
                    myWebView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                        @Override
                        public void handle(KeyEvent event) {
                            if (UsefulKeyCodes.CNTRL_C.match(event)) {
                                copyToClipboard(true, true);
                                event.consume();
                            }
                            if (UsefulKeyCodes.SHIFT_DEL.match(event)) {
                                copyToClipboard(false, true);
//                                wrapExecuteScript(myWebEngine, "tinymce.activeEditor.execCommand(\"Cut\");");
                                event.consume();
                            }
                            if (UsefulKeyCodes.CNTRL_X.match(event)) {
                                copyToClipboard(true, true);
                                event.consume();
                            }
                            if (UsefulKeyCodes.CNTRL_S.match(event)) {
                                saveNote();
                                event.consume();
                            }
                        }
                    });
                }
            }
        });
        myWebEngine.setUserStyleSheetLocation(OwnNoteHTMLEditor.class.getResource("/editor.min.css").toString());

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
    
    /**
     * Imports an image file.
     *
     * @param file Image file.
     */
    private void importMediaFile(File file) {
        try {
            //check if file is too big
            // TFE, 20201012: things can be a bit bigger nowadays :-)
            if (file.length() > 10 * 1024 * 1024) {
                throw new VerifyError("File is too big.");
            }
            //get mime type of the file
            final String type = Files.probeContentType(file.toPath());
            //get html content
            final byte[] data = FileUtils.readFileToByteArray(file);
            final String base64data = Base64.encodeBase64String(data);
            
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
    
    public void replaceCheckedBoxes() {
        final String content = TaskManager.replaceCheckedBoxes(getNoteText());
        
        contentChanged(content);
        editNote(editedNote, content, true);
    }
    
    public void replaceCheckmarks() {
        final String content = TaskManager.replaceCheckmarks(getNoteText());
        
        contentChanged(content);
        editNote(editedNote, content, true);
    }
    
    public void replaceNoteLinks(final String oldNoteName, final String newNoteName) {
        final String content = LinkManager.replaceNoteLinks(getNoteText(), oldNoteName, newNoteName);
        
        contentChanged(content);
        editNote(editedNote, content, true);
    }
    
    public void invalidateNoteLinks(final String noteName) {
        final String content = LinkManager.invalidateNoteLinks(getNoteText(), noteName);
        
        contentChanged(content);
        editNote(editedNote, content, true);
    }
    
    private void compressImages() {
        assert (myEditor != null);
        
        String content = getNoteText();

        // 1) find all images in the note
        // "<img src='data:" + mediaType + ";base64," + mediaData + ">"
        final LinkedList<Integer> images = new LinkedList<>();
        final Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            images.add(matcher.start());
        }
        
        if (images.isEmpty()) {
            // no images in this note
            return;
        }
        
        // reverse list since file positions change during replace
        
        Collections.reverse(images); 
        for (int imageStart : images) {
            // 2) create image from base64
            final int imageEnd = content.indexOf(">", imageStart);
            if (imageEnd == -1) {
                // something wrong with this image...
                continue;
            }
            final String imageString = content.substring(imageStart, imageEnd);
//            System.out.println("imageString: starts with " + imageString.subSequence(0, 5) + " ends with " + imageString.substring(imageString.length()-5) + " and has length " + imageString.length());
            
            // extract "data:image/jpeg;base64," content
            int contentStart = DATA_START.length();
            int contentEnd = imageString.indexOf(BASE64_START, contentStart);
            if (contentEnd == -1) {
                // something wrong with this image...
                continue;
            }
            final String imageType = imageString.substring(contentStart, contentEnd);
//            System.out.println("imageType: " + imageType);
            // TODO: only works for png & jpeg
            if ("gif".equals(imageType)) {
                continue;
            }
            
            contentStart = imageString.indexOf(BASE64_START) + BASE64_START.length();
            if (contentStart == -1) {
                // something wrong with this image...
                continue;
            }
            if (imageString.indexOf("\"", contentStart) > 0) {
                contentEnd = imageString.indexOf("\"", contentStart);
            } else {
                contentEnd = imageString.indexOf("'", contentStart);
            }
            if (contentEnd == -1) {
                // something wrong with this image...
                continue;
            }
            final String imageBase64 = imageString.substring(contentStart, contentEnd);
//            System.out.println("imageBase64: starts with " + imageBase64.subSequence(0, 5) + " ends with " + imageBase64.substring(imageBase64.length()-5) + " and has length " + imageBase64.length());

            // extract width="xyz" height="abc" content if available
            Integer imageWidth = -1;
            contentStart = imageString.indexOf("width=\"");
            if (contentStart == -1) {
                contentStart = imageString.indexOf("width='");
            }
            if (contentStart > -1) {
                contentStart += "width='".length();
                contentEnd = imageString.indexOf("\"", contentStart);
                if (contentEnd == -1) {
                    contentEnd = imageString.indexOf("'", contentStart);
                }
                if (contentEnd == -1) {
                    // something wrong with this image...
                    continue;
                }
                imageWidth = Integer.valueOf(imageString.substring(contentStart, contentEnd));
            }
//            System.out.println("imageWidth: " + imageWidth);

            Integer imageHeight = -1;
            contentStart = imageString.indexOf("height=\"");
            if (contentStart == -1) {
                contentStart = imageString.indexOf("height='");
            }
            if (contentStart > -1) {
                contentStart += "height='".length();
                contentEnd = imageString.indexOf("\"", contentStart);
                if (contentEnd == -1) {
                    contentEnd = imageString.indexOf("'", contentStart);
                }
                if (contentEnd == -1) {
                    // something wrong with this image...
                    continue;
                }
                imageHeight = Integer.valueOf(imageString.substring(contentStart, contentEnd));
            }
//            System.out.println("imageHeight: " + imageWidth);

            // 3) compress image based on type images
            final String newImageBas64 = ImageHelper.compressBase64Image(imageBase64, imageType, imageWidth, imageHeight);

            // 4) replace imageString
            String newImageString = DATA_START + imageType + BASE64_START + newImageBas64 + "\"";
            if (imageWidth > -1) {
                newImageString += (" width=\"" + imageWidth + "\"");
            }
            if (imageHeight > -1) {
                newImageString += (" height=\"" + imageHeight + "\"");
            }
            
            if (newImageString.length() < imageString.length()) {
                content = content.replace(imageString, newImageString);
                System.out.println("Replaced " + imageString.length() + " chars of image data with " + newImageString.length() + " chars for image starting @" + imageStart);
            }
        }
        
        contentChanged(content);
        editNote(editedNote, content, true);
    }
    
    private void initPopupWindow() {
        final ObservableList<Window> windows = Window.getWindows();

        for (Window window : windows) {
            if (window instanceof ContextMenu) {
                // TFE, 20210628: work directly on ContextMenu - why all the complex logic below?
                // TFE, 20211101: because you'll get an exception 
                // 
                // Exception in thread "JavaFX Application Thread" java.lang.ClassCastException: 
                // class javafx.scene.control.MenuItem cannot be cast to class com.sun.javafx.webkit.theme.ContextMenuImpl$MenuItemPeer 
                // (javafx.scene.control.MenuItem is in module javafx.controls of loader 'app'; 
                // com.sun.javafx.webkit.theme.ContextMenuImpl$MenuItemPeer is in module javafx.web of loader 'app')
                // 
                // otherwise?!
                if (window.getScene() != null && window.getScene().getRoot() != null) { 
                    final Parent root = window.getScene().getRoot();

                    // this is a bit tricky...
                    // 1) attachMenu only works with ContextMenu.getItems().add
                    // 2) needs to be done upfront otherwise all menu items added via ContextMenuContent.getItemsContainer().getChildren().add() get removed
                    final ContextMenu cm = (ContextMenu) window;
                    // add attachment links - fill menu items for each edited note individually
                    final Menu attachMenu = new Menu(ATTACHMENT_LINK.get(getLanguage()));
                    populateAttachMenu(attachMenu);
                    cm.getItems().add(attachMenu);

                    // add attachment links - fill menu items for each edited note individually
                    final Menu notesMenu = new Menu(NOTE_LINK.get(getLanguage()));
                    populateLinksMenu(notesMenu);
                    cm.getItems().add(notesMenu);

                    // access to context menu content
                    if (!root.getChildrenUnmodifiable().isEmpty()) {
                        final Node popup = root.getChildrenUnmodifiable().get(0);
                        if (popup.lookup(CONTEXT_MENU) != null) {
                            final Node bridge = popup.lookup(CONTEXT_MENU);

                            final ContextMenuContent cmc = (ContextMenuContent)((Parent) bridge).getChildrenUnmodifiable().get(0);

                            final VBox itemsContainer = cmc.getItemsContainer();
                            
                            // TFE, 202100510: loop through once before to determine language
                            for (Node n: itemsContainer.getChildren()) {
                                assert n instanceof ContextMenuContent.MenuItemContainer;
                                
                                final ContextMenuContent.MenuItemContainer item = (ContextMenuContent.MenuItemContainer) n;
                                setLanguage(item.getItem().getText());
                            }
                            
                            // check for "Reload page", ... entry and remove it...
                            int index = 0;
                            int copyIndex = -1;
                            // TFE, 20221106: paste as text as well, please
                            int pasteIndex = -1;
                            List<Node> deleteMenuItems = new ArrayList<>();
                            for (Node n: itemsContainer.getChildren()) {
                                assert n instanceof ContextMenuContent.MenuItemContainer;
                                
                                final ContextMenuContent.MenuItemContainer item = (ContextMenuContent.MenuItemContainer) n;
                                if (removeMenu(item.getItem().getText())) {
                                    deleteMenuItems.add(n);
                                }
                                
                                // TFE, 20181209: need to monitor "Copy" in order to add copied text to the OS clipboard as well
                                if (COPY_SELECTION.contains(item.getItem().getText())) {
                                    item.getItem().setOnAction((t) -> {
                                        copyToClipboard(true, true);
                                    });
                                    
                                    copyIndex = index;
                                    
                                    if (pasteIndex != -1) {
                                        copyIndex++;
                                    }
                                }
                                
                                if (PASTE.contains(item.getItem().getText())) {
                                    pasteIndex = index;
                                    
                                    if (copyIndex != -1) {
                                        pasteIndex++;
                                    }
                                }

                                index++;
                            }
                            
                            if (copyIndex != -1) {
                                // TFE, 20191211: add option to copy plain text as well
                                final MenuItem copyPlain = new MenuItem(COPY_TEXT_SELECTION.get(getLanguage()));
                                copyPlain.setOnAction((ActionEvent event) -> {
                                    copyToClipboard(true, false);
                                });

                                // add new item
                                itemsContainer.getChildren().add(copyIndex+1, cmc.new MenuItemContainer(copyPlain));
                            }

                            if (pasteIndex != -1) {
                                final MenuItem pastePlain = new MenuItem(PASTE_TEXT.get(getLanguage()));
                                pastePlain.setOnAction((ActionEvent event) -> {
                                    pasteFromClipboard(true, false);
                                });

                                // add new item
                                itemsContainer.getChildren().add(pasteIndex+1, cmc.new MenuItemContainer(pastePlain));
                            }

                            if (!deleteMenuItems.isEmpty()) {
                                itemsContainer.getChildren().removeAll(deleteMenuItems);
                            }

                            // adding save item
                            final MenuItem saveMenu = new MenuItem(SAVE_NOTE.get(getLanguage()));
                            saveMenu.setOnAction((ActionEvent event) -> {
                                saveNote();
                            });
                            // not working... BUT still here to show short cut :-) 
                            // work is done in myWebView.addEventHandler(KeyEvent.KEY_PRESSED...
                            saveMenu.setAccelerator(UsefulKeyCodes.CNTRL_S.getKeyCodeCombination());
                            
                            // checkbox -> symbol
                            final MenuItem replaceCheckedBoxesMenu = new MenuItem(REPLACE_CHECKEDBOXES.get(getLanguage()));
                            replaceCheckedBoxesMenu.setOnAction((ActionEvent event) -> {
                                replaceCheckedBoxes();
                            });

                            // symbol -> checkbox
                            final MenuItem replaceCheckmarksMenu = new MenuItem(REPLACE_CHECKMARKS.get(getLanguage()));
                            replaceCheckmarksMenu.setOnAction((ActionEvent event) -> {
                                replaceCheckmarks();
                            });

                            // compress images
                            final MenuItem compressImagesMenu = new MenuItem(COMPRESS_IMAGES.get(getLanguage()));
                            compressImagesMenu.setOnAction((ActionEvent event) -> {
                                compressImages();
                            });

                            // add new items:
                            itemsContainer.getChildren().add(cmc.new MenuItemContainer(saveMenu));
                            itemsContainer.getChildren().add(cmc.new MenuItemContainer(compressImagesMenu));
                            itemsContainer.getChildren().add(cmc.new MenuItemContainer(replaceCheckedBoxesMenu));
                            itemsContainer.getChildren().add(cmc.new MenuItemContainer(replaceCheckmarksMenu));
                        }
                    }
                }
            }
        }
    } 
    
    private int getLanguage() {
        return language == -1 ? 0 : language;
    }
    
    private void setLanguage(final String menuname) {
        if (language != -1) {
            // we only need to determine it once...
            return;
        }
        
        for (List<String> strings : TINYMCE_MENUES) {
            final int nameIndex = strings.indexOf(menuname);
            
            if (nameIndex != -1) {
                language = nameIndex;
                break;
            }
        }
    }

    private boolean removeMenu(final String menuname) {
        boolean result = false;
        
        for (List<String> strings : REMOVE_MENUES) {
            if (strings.indexOf(menuname) != -1) {
                result = true;
                break;
            }
        }
        
        return result;
    }
    
    private void copyToClipboard(final boolean runLater, final boolean copyFullHTML) {
        // tricky... in some cases we want to let tinymce do its work before in others we don't
        if (runLater) {
            Platform.runLater(() -> {
                doCopyToClipboard(copyFullHTML);
            });
        } else {
            doCopyToClipboard(copyFullHTML);
        }
    }
    
    private void doCopyToClipboard(final boolean copyFullHTML) {
        Object dummy = wrapExecuteScript(myWebEngine, "saveGetSelection();");

        assert (dummy instanceof String);
        String selection = (String) dummy;

        if (!copyFullHTML) {
            // TFE, 20210624: we often have space after link before newline - since "SPACE" after paste link makes it a link...
            // we need to remove these since it breaks paste
            selection = selection.replaceAll("(\\</a\\>)&nbsp;(\\</.*\\>)", "$1$2");
            // TFE, 20191211: remove html tags BUT convert </p> to </p> + line break
            selection = selection.replaceAll("\\</p\\>", "</p>" + System.lineSeparator());
            selection = stripHtmlTags(selection);
            // convert all &uml; back to &
            selection = HtmlEscape.unescapeHtml(selection);
        }

        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(selection);
        if (copyFullHTML) {
            // add as html as well to know later on which version to paste...
            clipboardContent.putHtml(selection);
        }
        // set the Fx clipboard to pass it on to the AWT clipboard... probably a better way to do that
        myClipboardFx.setContent(clipboardContent);
    }
    
    private void pasteFromClipboard(final boolean runLater, final boolean pasteFullHTML) {
        // tricky... in some cases we want to let tinymce do its work before in others we don't
        if (runLater) {
            Platform.runLater(() -> {
                doPasteFromClipboard(pasteFullHTML);
            });
        } else {
            doPasteFromClipboard(pasteFullHTML);
        }
    }
    
    private void doPasteFromClipboard(final boolean pasteFullHTML) {
        String selection = "";
        if (myClipboardFx.hasHtml()) {
            // remove all html tags
            selection = myClipboardFx.getHtml().replaceAll("\\<.*?>","");
        } else if (myClipboardFx.hasUrl()) {
            System.out.println(myClipboardFx.getUrl());
        } else if (myClipboardFx.hasRtf()) {
            try {
                final RTFEditorKit rtfParser = new RTFEditorKit();
                final Document document = rtfParser.createDefaultDocument();
                rtfParser.read(new ByteArrayInputStream(myClipboardFx.getRtf().getBytes()), document, 0);
                selection = document.getText(0, document.getLength());
            } catch (IOException | BadLocationException ex) {
                Logger.getLogger(OwnNoteHTMLEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (myClipboardAwt.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            try {
                selection = (String) myClipboardAwt.getData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(OwnNoteHTMLEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // anything left to insert?
        if (!selection.isEmpty()) {
            selection = selection.replaceAll("(\n\r|\r\n|\n|\r)", "<br />");
            wrapExecuteScript(myWebEngine, "insertText('" + replaceForEditor(selection) + "');");
        }
    }

    //
    // javascript callbacks
    //
    
    private void populateAttachMenu(final Menu attachMenu) {
        if (attachMenu == null) {
            return;
        }
        
        attachMenu.getItems().clear();
        
        if (editedNote != null) {
            for (String attachment : editedNote.getMetaData().getAttachments()) {
                final MenuItem attachItem = new MenuItem(attachment);
                attachItem.setOnAction((ActionEvent event) -> {
                    final String fileName = NoteMetaData.getAttachmentPath() + attachment;
                    // insert hyperlink at position
                    // <a href="fileName" target="dummy">attachment</a>
                    // TFE, 20210629: if we don't use the real attachment path, tinymce messes up the url to point to somewhere...
                    wrapExecuteScript(myWebEngine, "insertLinkToNoteAttachment('" + replaceForEditor(fileName) + "', '" + attachment + "');");
                });
                attachMenu.getItems().add(attachItem);
            }
        }
    }
    
    private void populateLinksMenu(final Menu linksMenu) {
        if (linksMenu == null) {
            return;
        }
        
        linksMenu.getItems().clear();
        
        // no link to notes in archive!
        // TFE, 20221228: and why on earth not???
        // lets sort archived groups to the end - thats OK with me
        final LinkedHashSet<Note> notesList = OwnNoteFileManager.getInstance().getNotesList().stream().filter((t) -> {
            return !t.getGroup().isArchivedGroup();
        }).collect(Collectors.toCollection(LinkedHashSet::new));
        notesList.addAll(OwnNoteFileManager.getInstance().getNotesList().stream().filter((t) -> {
            return t.getGroup().isArchivedGroup();
        }).collect(Collectors.toCollection(LinkedHashSet::new)));
        
        for (Note note : notesList) {
            final String noteName = FilenameUtils.getBaseName(note.getNoteFileName());
            final MenuItem noteItem = new MenuItem(noteName);
            noteItem.setOnAction((ActionEvent event) -> {
                wrapExecuteScript(myWebEngine, "insertLinkToNote('" + NOTE_HTML_LINK_TYPE + note.getNoteFileName() + "', '" + noteName + "');");
                // processFileContentChange will update the link lists
            });
            linksMenu.getItems().add(noteItem);
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
        
    public void editNote(final Note note) {
        editNote(note, note != null ? note.getNoteFileContent() : "", false);
    }
        
    private void editNote(final Note note, final String text, final boolean keepCursorPos) {
        setContentDone = false;
        Runnable task = () -> {
            //System.out.println("setEditorText " + text);
            wrapExecuteScript(myWebEngine, "saveSetContent('" + replaceForEditor(text) + "', " + keepCursorPos + ");");
        };
        
        startTask(task);
        editedNote = note;
        if (editedNote != null) {
            editedNote.setNoteEditorContent(text);
        }
    }

    private String replaceForEditor(final String text) {
        String result = text;

        // https://stackoverflow.com/questions/17802239/jsexception-while-loading-a-file-in-a-codemirror-based-editor-using-java-using-s
        // TFE, 20181030: for tinymce we also need to escape \
        result = result.replace("\\", "\\\\")
                       .replace("'", "\\'")
                       .replace(System.lineSeparator(), "\\n")
                       .replace("\n", "\\n")
                       .replace("\r", "\\n");
        
        return result;
    }
    
    public void scrollToCheckBox(final int textPos, final String htmlText, final String taskId) {
        // call tinymce to set the cursor
        // TFE, 20220601: escape check box text for editor - might contain e.g. "'"
        wrapExecuteScript(myWebEngine, "scrollToCheckBox(" + textPos + ", '" + replaceForEditor(htmlText) + "', '" + taskId + "');");
    }
    
    public void scrollToAndToggleCheckBox(final int textPos, final String htmlText, final String taskId, final boolean newStatus) {
        // call tinymce to change the checkbox
        // TFE, 20220601: escape check box text for editor - might contain e.g. "'"
        wrapExecuteScript(myWebEngine, "scrollToAndToggleCheckBox(" + textPos + ", '" + replaceForEditor(htmlText) + "', '" + taskId + "', " + newStatus + ");");
    }

    public String getNoteText() {
        return readNoteText();
    }

    private String readNoteText() {
        String newEditorText = "";

        if (editorInitialized) {
            Object dummy = wrapExecuteScript(myWebEngine, "saveGetContent(" + true + ", " + true + ");");
            
            assert (dummy instanceof String);
            newEditorText = (String) dummy;

            //System.out.println("readEditorText " + newEditorText);
            
            if (editedNote != null) {
                editedNote.setNoteEditorContent(newEditorText);
            }
        }

        return newEditorText;
    }

    public boolean hasChanged() {
        boolean result = false;

        // only try to read context when we had the callback from last setcontent() javascript call
        if (editorInitialized && setContentDone) {
            final String newEditorText = readNoteText();

            if (editedNote == null || editedNote.getNoteFileContent() == null) {
                result = !newEditorText.isEmpty();
            } else {
                // TFE, 20201103: unwrapping checkboxes in js can lead to unescaping of text...
                result = editedNote.hasUnsavedChanges();
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

    public void doNameChange(final TagData oldGroup, final TagData newGroup, final String oldNoteName, final String newNoteName) {
        assert (oldGroup != null);
        assert (newGroup != null);
        assert (oldNoteName != null);
        assert (newNoteName != null);
        
        if (getEditedNote() != null) {
            // TODO: check if the edited note is the one that has been changed!!!
            editedNote.setGroup(newGroup);
            editedNote.setNoteName(newNoteName);
        }
    }

    /* Required getter and setter methods are forwarded to internal TableView */

    public void setDisable(final boolean b) {
        myWebView.setDisable(b);
    }

    public void setVisible(final boolean b) {
        myWebView.setVisible(b);
    }
    
    public Note getEditedNote() {
        return editedNote;
    }
    
    public BooleanProperty visibleProperty() {
        return myWebView.visibleProperty();
    }
    
    public static String stripHtmlTags(final String input) {
        return input.contains("<") ? TAG_PATTERN.matcher(input).replaceAll("") : input;
    }
    
    /**
     * See: https://codereview.stackexchange.com/a/112506
     * Verify if a string contains any HTML characters by comparing its
     * HTML-escaped version with the original.
     * @param input the input String
     * @return boolean  True if the String contains HTML characters
     */
    public static boolean isHtml(final String input) {
        boolean isHtml = false;
        if (input != null) {
            isHtml = (!input.equals(HtmlEscape.escapeHtml4(input)) && !input.equals(HtmlEscape.escapeHtml5(input)));
        }
        return isHtml;
    }    

    //
    // javascript callbacks
    //
    
    public void initEditorDone() {
        editorInitialized = true;
        
        while (!myQueue.isEmpty()) {
            myQueue.poll().run();
        }
    }

    public void setContentDone() {
        setContentDone = true;
    }

    public void insertImage() {
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
    
    public void printNote() {
        assert (myEditor != null);
        
        // https://docs.oracle.com/javafx/8/webview/printing.htm
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(myWebView.getScene().getWindow())){
            myWebEngine.print(job);
            job.endJob();
        }
        myWebView.requestFocus();
    }
    
    public void saveNote() {
        assert (myEditor != null);

        // we might not have selected a note yet... accelerator always works :-(
        if (editedNote != null) {
            editedNote.setNoteEditorContent(getNoteText());
            if (myEditor.saveNote(editedNote)) {
                // TFE, 20210224: update editor content since task metadata might have changed!
                editNote(editedNote, editedNote.getNoteEditorContent(), true);
            }
        }
    }
    
    public void openLinkInDefaultBrowser(final String url, final String attachment, final String note) {
        // first load is OK :-)
        if (editorInitialized) {
            String realUrl = url;
            if ("yes".equals(note)) {
                realUrl = realUrl.substring(NOTE_HTML_LINK_TYPE.length());
                // TFE, 20221126: allow links to other notes
                myEditor.selectNote(realUrl);
            } else {
                if (myHostServices != null) {
                    if ("yes".equals(attachment)) {
                        // replace previous path to attachment folder with current one
                        realUrl = NoteMetaData.getAttachmentPath() + FilenameUtils.getName(url);
                    }
                    myHostServices.showDocument(realUrl);
                }
            }
        }
    }
    
    public String getClipboardContent() {
        // TFE, 20200711: tinyMCE manages an own clipboard. Once something has been copied in tinyMCE the system clipboard is ignored during paste...
        // https://stackoverrun.com/de/q/9359780#39265109
        String result = "";

        // We use the AWT clipboard because the FX implementation delivers funky characters when pasting from e.g. Command Prompt and images
        try {
            if (myClipboardFx.hasHtml()) {
                // TFE, 20210624: change from 20210510 breaks html copy & paste...
                result = myClipboardFx.getHtml();
            } else if (myClipboardAwt.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                result = (String) myClipboardAwt.getData(DataFlavor.stringFlavor);
                if (!isHtml(result)) {
                    result = result.replaceAll("(\n\r|\r\n|\n|\r)", "<br />");
                }
            } else if (myClipboardAwt.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                // TFE, 20201012: allow pasting of images
                // issues with images from javafx clipboard: https://bugs.openjdk.java.net/browse/JDK-8223425
                final BufferedImage img = (BufferedImage) myClipboardAwt.getData(DataFlavor.imageFlavor); 
                final String base64String;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1000)) {
                    ImageIO.write(img, "png", baos);
                    baos.flush();
                    base64String = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
                }  
            
                // duplication of code with javascript method insertMedia - but we can live with that
                result = "<img src='data:" + "image/png" + ";base64," + base64String + "' >";
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
    public void checkBoxChanged(final String htmlBefore, final String htmlAfter) {
        // send change note to all subscribes
        for (IFileContentChangeSubscriber subscriber : changeSubscribers) {
            if (!subscriber.processFileContentChange(FileContentChangeType.CHECKBOX_CHANGED, editedNote, htmlBefore, htmlAfter)) {
                break;
            }
        }
    }
    
    public void contentChanged(final String newContent) {
        if (editedNote == null) {
            return;
        }
        
        final String oldContent = editedNote.getNoteEditorContent();
        if (oldContent.equals(newContent)) {
            // TFE, 20201216: this can happen when checkbox is clicked since input event comes before the 
            // $(editor.getBody()).on("change", ":checkbox", function(el) is called
            return;
        }

        editedNote.setNoteEditorContent(newContent);

        // send change note to all subscribes
        for (IFileContentChangeSubscriber subscriber : changeSubscribers) {
            if (!subscriber.processFileContentChange(FileContentChangeType.CONTENT_CHANGED, editedNote, oldContent, newContent)) {
                break;
            }
        }
    }
    
    public void toggleDialog(final boolean isOpen) {
        isDialogueOpen = isOpen;
        myWebView.setContextMenuEnabled(!isDialogueOpen);
//        System.out.println("A dialog was " + (isOpen ? "opened" : "closed"));
    }
    
    public void log(String text) {
        System.out.println(text);
    }
}
