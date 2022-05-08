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
 * THIS SOFTWARE IS PROVIDED BY THE VERSIONS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE VERSIONS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.notes;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import tf.ownnote.ui.commentdata.CommentDataMapper;
import tf.ownnote.ui.commentdata.ICommentDataHolder;
import tf.ownnote.ui.commentdata.ICommentDataInfo;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.tags.ITagHolder;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;
import tf.ownnote.ui.tasks.TaskData;

/**
 * Store for note metadata, e.g. author, last modified, tags, ...
 * 
 * @author thomas
 */
public class NoteMetaData implements ICommentDataHolder, ITagHolder {
    public final static String ATTACHMENTS_DIR = File.separator + "Attachments";
    
    private enum UpdateTag {
        LINK,
        UNLINK
    }
    
    // info per available metadata - name & multiplicity
    private static enum CommentDataInfo implements ICommentDataInfo {
        // TFE, 20220506: new meta data app version
        // must be first in the list to be read first - since handling of TAGS depends on version info
        APP_VERSION("appVersion", Multiplicity.SINGLE),
        VERSIONS("versions", Multiplicity.MULTIPLE),
        TAGS("tags", Multiplicity.MULTIPLE),
        CHARSET("charset", Multiplicity.SINGLE),
        ATTACHMENTS("attachments", Multiplicity.MULTIPLE);
        
        private final String dataName;
        private final Multiplicity dataMulti;
        
        private CommentDataInfo (final String name, final Multiplicity multi) {
            dataName = name;
            dataMulti = multi;
        }
        
        @Override
        public String getDataName() {
            return dataName;
        }
        
        @Override
        public Multiplicity getDataMultiplicity() {
            return dataMulti;
        }
    }

    private final ObservableList<NoteVersion> myVersions = FXCollections.<NoteVersion>observableArrayList();
    private final ObservableSet<TagData> myTags = FXCollections.<TagData>observableSet();
    // TFE, 20210228: know thy tasks as well
    private final ObservableSet<TaskData> myTasks = FXCollections.<TaskData>observableSet();
    // TFE, 20201217: add charset to metadata - since we switched to UTF-8 on 17.12.2020 we need to be able to handle old notes
    private Charset myCharset = StandardCharsets.ISO_8859_1;
    // TFE, 20210101: support for note attachments
    private final ObservableList<String> myAttachments = FXCollections.<String>observableArrayList();
    // TFE, 20220430: know thy app version - to find out if we might need to migrate stuff
    private final DoubleProperty myAppVersionProperty = new SimpleDoubleProperty(OwnNoteEditor.AppVersion.NONE.getVersionId());
    
    // TFE, 20210201: know you're own change status
    private final BooleanProperty hasUnsavedChanges = new SimpleBooleanProperty(false);

    private Note myNote;
    
    public NoteMetaData(final Note note) {
        super();
        
        myNote = note;

        // go, tell it to the mountains
        myTags.addListener((SetChangeListener.Change<? extends TagData> change) -> {
            if (change.wasAdded()) {
//                System.out.println("Linking note " + myNote.getNoteName() + " to tag " + change.getElementAdded().getName());
                change.getElementAdded().getLinkedNotes().add(myNote);
                hasUnsavedChanges.set(true);
            }

            if (change.wasRemoved()) {
//                System.out.println("Unlinking note " + myNote.getNoteName() + " from tag " + change.getElementRemoved().getName());
                change.getElementRemoved().getLinkedNotes().remove(myNote);
                hasUnsavedChanges.set(true);
            }
        });

        myVersions.addListener((ListChangeListener.Change<? extends NoteVersion> change) -> {
            hasUnsavedChanges.set(true);
        });
        
        myAttachments.addListener((ListChangeListener.Change<? extends String> change) -> {
            hasUnsavedChanges.set(true);
        });
    }
    
    public boolean isEmpty() {
        return (myVersions.isEmpty() && myTags.isEmpty());
    }

    public ObservableList<NoteVersion> getVersions() {
        return myVersions;
    }

    public void setVersions(final List<NoteVersion> versions) {
        myVersions.clear();
        myVersions.addAll(versions);
    }

    public NoteVersion getVersion() {
        if (!myVersions.isEmpty()) {
            return myVersions.get(myVersions.size()-1);
        } else {
            return null;
        }
    }

    public void addVersion(final NoteVersion version) {
        if (myVersions.isEmpty()) {
            myVersions.add(version);
        } else {
            // keep only one entry per date - otherwise its going to explode
            if (myVersions.get(myVersions.size() - 1).getDate().toLocalDate().equals(version.getDate().toLocalDate())) {
                myVersions.remove(myVersions.size() - 1);
            }
            myVersions.add(version);
        }
    }

    @Override
    public ObservableSet<TagData> getTags() {
        return myTags;
    }

    @Override
    public void setTags(final Set<TagData> tags) {
        myTags.clear();
        myTags.addAll(tags);
    }
    
    private void updateTags(final UpdateTag updateTag) {
        if (myNote == null) return;
        
        for (TagData tag : myTags) {
            switch (updateTag) {
                case LINK:
//                    System.out.println("Linking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    tag.getLinkedNotes().add(myNote);
                    break;
                case UNLINK:
//                    System.out.println("Unlinking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    // TFE; 20201227: for some obscure reason the following doesn't work - don't ask
                    tag.getLinkedNotes().remove(myNote);
                    break;
            }
        }
    }
    
    public ObservableSet<TaskData> getTasks() {
        return myTasks;
    }

    public void setTasks(final Set<TaskData> tasks) {
        myTasks.clear();
        myTasks.addAll(tasks);
    }
    
    public Note getNote() {
        return myNote;
    }
    
    public void setNote(final Note note) {
        assert note != null;
        if (!note.equals(myNote)) {
            updateTags(UpdateTag.UNLINK);
            myNote = note;
            updateTags(UpdateTag.LINK);
        }
    }
    
    public Charset getCharset() {
        return myCharset;
    }

    public void setCharset(final Charset charset) {
        myCharset = charset;
        hasUnsavedChanges.set(true);
    }
    
    public ObservableList<String> getAttachments() {
        return myAttachments;
    }

    public void setAttachments(final List<String> attachments) {
        myAttachments.clear();
        myAttachments.addAll(attachments);
    }
    
    public Double getAppVersion() {
        return myAppVersionProperty.get();
    }
    
    public void setAppVersion(final double version) {
        myAppVersionProperty.set(version);
    }
    
    public DoubleProperty appVersionProperty() {
        return myAppVersionProperty;
    }

    public static boolean hasMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return false;
        }
        
        final String contentString = htmlString.split("\n")[0];
        return CommentDataMapper.isCommentWithData(contentString);
    }
    
    public static String removeMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return "";
        }
        
        String result = htmlString;
        if (hasMetaDataContent(htmlString)) {
            final int endPos = htmlString.indexOf("\n");
            // TFE, 20210113: special case: only one line with metadata!
            if (endPos != -1 && endPos < htmlString.length()) {
                result = htmlString.substring(endPos+1);
            } else {
                result = "";
            }
        }
        
        return result;
    }
    
    public static NoteMetaData fromHtmlComment(final Note note, final String htmlString) {
        final NoteMetaData result = new NoteMetaData(note);

        // parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // authors="xyz" tags="a:::b:::c"
        
        if (htmlString != null && hasMetaDataContent(htmlString)) {
            CommentDataMapper.getInstance().fromComment(result, htmlString);
            // no changes for "newborn" metadata
            result.setUnsavedChanges(false);
        }
        
        return result;
    }
    
    public static String toHtmlComment(final NoteMetaData data) {
        if (data == null) {
            return "";
        }

        return CommentDataMapper.getInstance().toComment(data) + "\n";
    }
    
    @Override
    public ICommentDataInfo[] getCommentDataInfo() {
        return CommentDataInfo.values();
    }

    @Override
    public void setFromString(ICommentDataInfo name, String value) {
        if (CommentDataInfo.CHARSET.equals(name)) {
            setCharset(Charset.forName(value));
        } else if (CommentDataInfo.APP_VERSION.equals(name)) {
            setAppVersion(Double.valueOf(value));
        }
    }

    @Override
    public void setFromList(ICommentDataInfo name, List<String> values) {
        if (CommentDataInfo.VERSIONS.equals(name)) {
            final List<NoteVersion> versions = new LinkedList<>();
            for (String value : values) {
                versions.add(NoteVersion.fromHtmlString(value));
            }
            setVersions(versions);
        } else if (CommentDataInfo.TAGS.equals(name)) {
            if (OwnNoteEditor.AppVersion.V6_1.isHigherAppVersionThan(getAppVersion())) {
                setTags(TagManager.getInstance().tagsForNames(new HashSet<>(values), null, true));
            } else {
                // new way of doing things with external names
                setTags(TagManager.getInstance().tagsForExternalNames(new HashSet<>(values), null, true));
            }
        } else if (CommentDataInfo.ATTACHMENTS.equals(name)) {
            setAttachments(values);
        }
    }

    @Override
    public String getAsString(ICommentDataInfo name) {
        if (CommentDataInfo.CHARSET.equals(name)) {
            return getCharset().name();
        } else if (CommentDataInfo.APP_VERSION.equals(name)) {
            return getAppVersion().toString();
        }
        return null;
    }

    @Override
    public List<String> getAsList(ICommentDataInfo name) {
        if (CommentDataInfo.VERSIONS.equals(name)) {
            if (!myVersions.isEmpty()) {
                return myVersions.stream().map((t) -> {
                    return NoteVersion.toHtmlString(t);
                }).collect(Collectors.toList());
            } else {
                return null;
            }
        } else if (CommentDataInfo.TAGS.equals(name)) {
            return myTags.stream().map((t) -> {
                if (OwnNoteEditor.AppVersion.CURRENT.isLowerAppVersionThan(OwnNoteEditor.AppVersion.V6_1)) {
                    // not sure how this might happen - since we invented app version with v6.1
                    System.err.println("Reading note metadata with app version: " + OwnNoteEditor.AppVersion.CURRENT.getVersionId());
                    return t.getName();
                } else {
                    return t.getExternalName();
                }
            }).collect(Collectors.toList());
        } else if (CommentDataInfo.ATTACHMENTS.equals(name)) {
            return myAttachments;
        }
        return null;
    }
    
    public BooleanProperty hasUnsavedChangesProperty() {
        return hasUnsavedChanges;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges.getValue();
    }
    
    public void setUnsavedChanges(final boolean changed) {
        hasUnsavedChanges.setValue(changed);
    }
    
    public static String getAttachmentPath() {
        return OwnNoteFileManager.getInstance().getNotesPath() + ATTACHMENTS_DIR + File.separator;
    }
}
