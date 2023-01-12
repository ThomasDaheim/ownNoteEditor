/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.notes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.ShowAlerts;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagsEditor;
import tf.ownnote.ui.tasks.TaskCount;
import tf.ownnote.ui.tasks.TaskManager;

/**
 * Show and edit metadata of a note:
 * 1) Authors: show only, last one selected 
 * 2) Tags: show & edit in a fancy way
 *    - show each tag as label with cross to remove
 *    - show label "add or create" that open small dialog with the list of known tags and a create option
 * 3) Attachments: show with option to add & delete
 * 4) Links: show with option to click
 * 5) Tasks: show counts (open / closed / total)
 * @author thomas
 */
public class NoteMetaDataEditor {
    private final static String LINK_FROM = "\u2190 ";
    private final static String LINK_TO = "\u2192 ";
            
    private HBox myHBox;
    
    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
    private HostServices myHostServices = null;
    
    private final ComboBox<String> versions = new ComboBox<>();
    private final MenuBar attachments = new MenuBar();
    private final MenuItem addAttachment = new MenuItem("Add attachment");
    private Label taskstxt;
    private final FlowPane tagsBox = new FlowPane();
    
    // TFE, 20230105: we now alos have notes between links
    private final MenuBar links = new MenuBar();
    
    private Note editorNote;

    // TFE, 20210305: make sure we only attach listener once and it can be removed afterwards
    private SetChangeListener<TagData> tagListener;
    private ListChangeListener<String> attachListener;
    private SetChangeListener<Note> linkedListener;
    private SetChangeListener<Note> linkingListener;
    
    private NoteMetaDataEditor() {
        super();
    }
    
    public NoteMetaDataEditor(final HBox hBox, final OwnNoteEditor editor) {
        super();

        myEditor = editor;
        
        myHBox = hBox;
        
        myHostServices = (HostServices) myEditor.getWindow().getProperties().get("hostServices");
        
        tagListener = new SetChangeListener<>() {
            @Override
            public void onChanged(SetChangeListener.Change<? extends TagData> change) {
                if (change.wasRemoved()) {
                    removeTagLabel(change.getElementRemoved());
                }
                if (change.wasAdded()) {
                    addTagLabel(change.getElementAdded());
                }
            }
        };
        
        // change listener as well
        attachListener = new ListChangeListener<>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (String attach : change.getRemoved()) {
                            removeAttachMenu(attach);
                        }
                    }
                    if (change.wasAdded()) {
                        for (String attach : change.getAddedSubList()) {
                            addAttachMenu(attach);
                        }
                    }
                }
            }
        };

        linkedListener = new SetChangeListener<>() {
            @Override
            public void onChanged(SetChangeListener.Change<? extends Note> change) {
                buildLinkMenus();
            }
        };
        
        linkingListener = new SetChangeListener<>() {
            @Override
            public void onChanged(SetChangeListener.Change<? extends Note> change) {
                buildLinkMenus();
            }
        };
        
        initEditor();
    }
    
    private void initEditor() {
//        myHBox.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        myHBox.setFillHeight(true);
        myHBox.setAlignment(Pos.CENTER_LEFT);
        
        final Label authorsLbl = new Label("Authors:");
        HBox.setMargin(authorsLbl, AbstractStage.INSET_SMALL);
        
        // https://stackoverflow.com/a/32373721
        versions.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item);
                    setDisable(true);
                }
            }
        });
        versions.setVisibleRowCount(4);
        versions.setMaxWidth(200.0);

        final Label tagsLbl = new Label("Tags:");
        HBox.setMargin(tagsLbl, AbstractStage.INSET_SMALL);
        
        tagsBox.getStyleClass().add("tagsBox");
        tagsBox.setAlignment(Pos.CENTER_LEFT);
        tagsBox.setPadding(new Insets(0, 2, 0, 2));

        // wrap into a scrollpane in case of too many tags...
        final ScrollPane tagsScroll = new ScrollPane(tagsBox);
        tagsScroll.getStyleClass().add("tagsPane");
        tagsScroll.setMaxWidth(300.0);
        tagsScroll.setFitToHeight(true);
        tagsScroll.setFitToWidth(true);
        tagsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        final Button tagsButton = new Button("+");
        tagsButton.setOnAction((t) -> {
            TagsEditor.getInstance().editTags(editorNote.getMetaData());
        });

        final Menu attachmentsMenu = new Menu("Attachments");
        attachmentsMenu.getStyleClass().add("metadata-menu");
        attachmentsMenu.getStyleClass().add("menu-as-list");
        attachments.getMenus().add(attachmentsMenu);
        attachments.getStyleClass().add("metadata-menu");
        attachments.getStyleClass().add("menu-as-list");
        
        addAttachment.setUserData("+");
        addAttachment.getStyleClass().add("menu-as-list");
        addAttachment.setOnAction((t) -> {
            addAttachment();
        });
        attachments.getMenus().get(0).getItems().setAll(addAttachment);
        HBox.setMargin(attachments, AbstractStage.INSET_SMALL);
        
        // TFE, 20230105: links are here!
        final Menu linksMenu = new Menu("Links");
        linksMenu.getStyleClass().add("metadata-menu");
        linksMenu.getStyleClass().add("menu-as-list");

        links.getMenus().add(linksMenu);
        links.getStyleClass().add("metadata-menu");
        links.getStyleClass().add("menu-as-list");
        HBox.setMargin(links, AbstractStage.INSET_SMALL);

        final Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);

        final Label tasksLbl = new Label("Tasks:");
        HBox.setMargin(tasksLbl, AbstractStage.INSET_SMALL);

        Tooltip t = new Tooltip("Open / Closed / Total");
        taskstxt = new Label("");
        taskstxt.setTooltip(t);
        HBox.setMargin(taskstxt, new Insets(0, 8, 0, 0));
        
        myHBox.getChildren().addAll(authorsLbl, versions, tagsLbl, tagsScroll, tagsButton, attachments, links, region1, tasksLbl, taskstxt);
    }
    
    public void editNote(final Note note) {
        if (editorNote != null) {
            // remove listeners from old note
            editorNote.getMetaData().getTags().removeListener(tagListener);
            editorNote.getMetaData().getAttachments().removeListener(attachListener);
            editorNote.getMetaData().getLinkingNotes().removeListener(linkedListener);
            editorNote.getMetaData().getLinkedNotes().removeListener(linkingListener);
        }
        
        versions.getItems().clear();
        tagsBox.getChildren().clear();
        attachments.getMenus().get(0).getItems().setAll(addAttachment);

        // TFE, 20220608: clean up in any case!
        if (note == null) {
            return;
        }
        
        editorNote = note;

        // set labels & fields with note data
        if (editorNote.getMetaData().getVersions().isEmpty()) {
            editorNote.getMetaData().addVersion(new NoteVersion(System.getProperty("user.name"), LocalDateTime.now()));
        }
        final List<String> versionList = editorNote.getMetaData().getVersions().stream().map((t) -> {
                return NoteVersion.toHtmlString(t);
            }).collect(Collectors.toList());
        Collections.reverse(versionList);
        versions.getItems().setAll(versionList);
        versions.getSelectionModel().selectFirst();
        
        for (TagData tag : editorNote.getMetaData().getTags()) {
            addTagLabel(tag);
        }
        
        editorNote.getMetaData().getTags().addListener(tagListener);
        
        // fill attachments menu
        for (String attach : editorNote.getMetaData().getAttachments()) {
            addAttachMenu(attach);
        }
        
        editorNote.getMetaData().getAttachments().addListener(attachListener);
        
        // fill links menu
        buildLinkMenus();
        
        editorNote.getMetaData().getLinkingNotes().addListener(linkedListener);
        editorNote.getMetaData().getLinkedNotes().addListener(linkingListener);
        
        final TaskCount taskCount = TaskManager.getInstance().getTaskCount(note);
        taskstxt.setText(taskCount.getCount(TaskCount.TaskType.OPEN) + " / " + taskCount.getCount(TaskCount.TaskType.CLOSED) + " / " + taskCount.getCount(TaskCount.TaskType.TOTAL));
    }
    
    private void addTagLabel(final TagData tag) {
        final Node tagLabel = getTagLabel(tag);
        FlowPane.setMargin(tagLabel, new Insets(0, 0, 0, 4));

        tagsBox.getChildren().add(tagLabel);
    }
    
    private void removeTagLabel(final TagData tag) {
        final List<Node> tagNodes = tagsBox.getChildren().stream().filter((t) -> {
            return ((TagData) t.getUserData()).equals(tag);
        }).collect(Collectors.toList());
        
        tagsBox.getChildren().removeAll(tagNodes);
    }
    
    public boolean hasChanged() {
        return editorNote == null ? false : editorNote.getMetaData().hasUnsavedChanges();
    }
    
    public void hasBeenSaved() {
        // re-init since new version...
        editNote(editorNote);
    }
    
    private Node getTagLabel(final TagData tag) {
        final HBox result = new HBox();
        result.getStyleClass().add("tagLabel");
        result.setAlignment(Pos.CENTER_LEFT);
        result.setPadding(new Insets(0, 2, 0, 2));
        result.setUserData(tag);

        final Label tagLabel = new Label("");
        tagLabel.textProperty().bind(tag.nameProperty());
        
        // add "remove" "button"
        final Label removeTag = new Label("X");
        removeTag.getStyleClass().add("removeButton");
        removeTag.setAlignment(Pos.CENTER);
        removeTag.setTextAlignment(TextAlignment.CENTER);
        removeTag.setContentDisplay(ContentDisplay.CENTER);
        HBox.setMargin(removeTag, new Insets(0, 0, 0, 4));
        
        removeTag.setOnMouseClicked((t) -> {
            // get rid of this tag in the note and of the node in the pane...
            editorNote.getMetaData().getTags().remove(tag);
            // refresh notes list - we might have removed a tag that is used for notes selection
            myEditor.refilterNotesList();
        });
        
        result.getChildren().addAll(tagLabel, removeTag);

        return result;
    }

    private void addAttachMenu(final String attach) {
        final CustomMenuItem menu = new CustomMenuItem();
        menu.getStyleClass().add("menu-as-list");
        menu.setUserData(attach);
        
        final HBox result = new HBox();
        result.setAlignment(Pos.CENTER_LEFT);
        result.setPadding(new Insets(0, 2, 0, 2));

        final Label tagLabel = new Label(attach);
        
        // add "remove" "button"
        final Label removeAttach = new Label("X");
        removeAttach.getStyleClass().add("removeButton");
        removeAttach.setAlignment(Pos.CENTER);
        removeAttach.setTextAlignment(TextAlignment.CENTER);
        removeAttach.setContentDisplay(ContentDisplay.CENTER);
        HBox.setMargin(removeAttach, new Insets(0, 0, 0, 4));
        
        removeAttach.setOnMouseClicked((t) -> {
            // get rid of this attachment in the note and in the Attachments folder...
            final String fileName = OwnNoteFileManager.getInstance().getNotesPath() + NoteMetaData.ATTACHMENTS_DIR + File.separator + attach;
            final File file = new File(fileName);

            // TODO: check if used in some other note
            if (FileUtils.deleteQuietly(file)) {
                editorNote.getMetaData().getAttachments().remove(attach);
            }
        });
        
        result.getChildren().addAll(tagLabel, removeAttach);
        
        menu.setContent(result);
        menu.setOnAction((t) -> {
            // open attachment with standard os handler
            if (myHostServices != null) {
                myHostServices.showDocument(OwnNoteFileManager.getInstance().getNotesPath() + NoteMetaData.ATTACHMENTS_DIR + File.separator + attach);
            }
        });
        
        // last entry is "Add attachment"
        attachments.getMenus().get(0).getItems().add(attachments.getMenus().get(0).getItems().size() - 1, menu);
    }

    private void removeAttachMenu(final String attach) {
        final List<MenuItem> menuList = attachments.getMenus().get(0).getItems().stream().filter((t) -> {
            return ((String) t.getUserData()).equals(attach);
        }).collect(Collectors.toList());
        
        attachments.getMenus().get(0).getItems().removeAll(menuList);
    }

    private void addAttachment() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select attachment");
        final File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                FileUtils.forceMkdir(new File(OwnNoteFileManager.getInstance().getNotesPath() + NoteMetaData.ATTACHMENTS_DIR));
            } catch (IOException ex) {
                Logger.getLogger(NoteMetaDataEditor.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            final String fileName = NoteMetaData.getAttachmentPath() + selectedFile.getName();
            final File file = new File(fileName);
            if (file.exists()) {
                // need to copy file to attachments subdir (if not already there)
                final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.RIGHT);
                Optional<ButtonType> doAction = 
                        ShowAlerts.getInstance().showAlert(
                                Alert.AlertType.WARNING,
                                "Warning",
                                "An attachment with same name already exists.",
                                selectedFile.getPath(),
                                buttonOK);
            }

            try {
                FileUtils.copyFile(selectedFile, file);
                editorNote.getMetaData().getAttachments().add(file.getName());
                attachments.getMenus().get(0).show();
            } catch (IOException ex) {
                Logger.getLogger(NoteMetaDataEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void buildLinkMenus() {
        // start from scratch: clear and add all links and backlinks
        links.getMenus().get(0).getItems().clear();
        
        for (Note note : editorNote.getMetaData().getLinkedNotes()) {
            links.getMenus().get(0).getItems().add(buildLinkMenu(LINK_TO, note.getNoteFileName()));
        }
        
        for (Note note : editorNote.getMetaData().getLinkingNotes()) {
            links.getMenus().get(0).getItems().add(buildLinkMenu(LINK_FROM, note.getNoteFileName()));
        }
        
        // show hide based on number of menu items
        links.setVisible(!links.getMenus().get(0).getItems().isEmpty());
        links.setDisable(links.getMenus().get(0).getItems().isEmpty());
    }
    
    private CustomMenuItem buildLinkMenu(final String direction, final String noteName) {
        final CustomMenuItem menu = new CustomMenuItem(new Label(direction + noteName));
        menu.getStyleClass().add("menu-as-list");
        menu.setUserData(noteName);
        menu.setOnAction((t) -> {
            myEditor.selectNote(noteName);
        });

        return menu;
    }
}
