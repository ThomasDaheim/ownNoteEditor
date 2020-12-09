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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import tf.ownnote.ui.tags.TagInfo;
import tf.ownnote.ui.tags.TagManager;

/**
 * Store for note metadata, e.g. author, last modified, tags, ...
 * 
 * @author thomas
 */
public class NoteMetaData {
    private static final String META_STRING_PREFIX = "<!-- ";
    private static final String META_STRING_SUFFIX = " -->";
    private static final String META_DATA_SEP = "---";
    private static final String META_VALUES_SEP = ":::";
    
    private static enum Multiplicity {
        SINGLE,
        MULTIPLE
    }
    
    private enum UpdateTag {
        LINK,
        UNLINK
    }

    // info per available metadata - name & multiplicity
    // TODO: add values here as well
    private static enum MetaDataInfo {
        VERSIONS("versions", Multiplicity.MULTIPLE),
        TAGS("tags", Multiplicity.MULTIPLE);
        
        private final String dataName;
        private final Multiplicity dataMulti;
        
        private MetaDataInfo (final String name, final Multiplicity multi) {
            dataName = name;
            dataMulti = multi;
        }
        
        public String getDataName() {
            return dataName;
        }
        
        public Multiplicity getDataMultiplicity() {
            return dataMulti;
        }
    }

    private final ObservableList<NoteVersion> myVersions = FXCollections.<NoteVersion>observableArrayList();
    private final ObservableSet<TagInfo> myTags = FXCollections.<TagInfo>observableSet();
    
    private Note myNote;
    
    public NoteMetaData() {
        super();
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

    public ObservableSet<TagInfo> getTags() {
        return myTags;
    }

    public void setTags(final Set<TagInfo> tags) {
        updateTags(UpdateTag.UNLINK);
        myTags.clear();
        myTags.addAll(tags);
        updateTags(UpdateTag.LINK);
    }
    
    private void updateTags(final UpdateTag updateTag) {
        if (myNote == null) return;
        
        for (TagInfo tag : myTags) {
            switch (updateTag) {
                case LINK:
//                    System.out.println("Linking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    tag.getLinkedNotes().add(myNote);
                    break;
                case UNLINK:
//                    System.out.println("Unlinking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    tag.getLinkedNotes().remove(myNote);
                    break;
            }
        }
    }
    
    public Note getNote() {
        return myNote;
    }
    
    public void setNote(final Note note) {
        updateTags(UpdateTag.UNLINK);
        myNote = note;
        updateTags(UpdateTag.LINK);
    }
    
    public static boolean hasMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return false;
        }
        
        final String contentString = htmlString.split("\n")[0];
        return (contentString.startsWith(META_STRING_PREFIX) && contentString.endsWith(META_STRING_SUFFIX));
    }
    
    public static String removeMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return "";
        }
        
        String result = htmlString;
        if (hasMetaDataContent(htmlString)) {
            final int endPos = htmlString.indexOf("\n");
            if (endPos < htmlString.length()) {
                result = htmlString.substring(endPos+1);
            } else {
                result = "";
            }
        }
        
        return result;
    }
    
    public static NoteMetaData fromHtmlString(final String htmlString) {
        final NoteMetaData result = new NoteMetaData();

        // parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // authors="xyz" tags="a:::b:::c"
        
        if (htmlString != null && hasMetaDataContent(htmlString)) {
            final String contentString = htmlString.split("\n")[0];
            String [] data = contentString.substring(META_STRING_PREFIX.length(), contentString.length()-META_STRING_SUFFIX.length()).
                    strip().split(META_DATA_SEP);

            // now we have the name - value pairs
            // split further depending on multiplicity
            for (String nameValue : data) {
                boolean infoFound = false;
                for (MetaDataInfo info : MetaDataInfo.values()) {
                    final String dataName = info.getDataName();
                    if (nameValue.startsWith(dataName + "=\"") && nameValue.endsWith("\"")) {
                        // found it! now check & parse for values
                        final String[] values = nameValue.substring(dataName.length()+2, nameValue.length()-1).
                            strip().split(META_VALUES_SEP);
                        
                        switch (info) {
                            case VERSIONS:
                                final List<NoteVersion> versions = new LinkedList<>();
                                for (String value : values) {
                                    versions.add(NoteVersion.fromHtmlString(value));
                                }
                                result.setVersions(versions);
                                infoFound = true;
                                break;
                            case TAGS:
                                result.setTags(TagManager.getInstance().tagsForNames(new HashSet<>(Arrays.asList(values))));
                                infoFound = true;
                                break;
                            default:
                        }
                    }
                    if (infoFound) {
                        // done, lets check next data value
                        break;
                    }
                }
            }
        }

        return result;
    }
    
    public static String toHtmlString(final NoteMetaData data) {
        if (data == null) {
            return "";
        }

        String result = "";
        boolean hasData = false;
        
        if (data.getVersion() != null) {
            result += MetaDataInfo.VERSIONS.getDataName() + "=\"" + data.getVersions().stream().map((t) -> {
                return NoteVersion.toHtmlString(t);
            }).collect(Collectors.joining(META_VALUES_SEP)) + "\"";
            hasData = true;
        }
        if (!data.getTags().isEmpty()) {
            if (hasData) {
                result += META_DATA_SEP;
            }
            result += MetaDataInfo.TAGS.getDataName() + "=\"" + data.getTags().stream().map((t) -> {
                return t.getName();
            }).collect(Collectors.joining(META_VALUES_SEP)) + "\"";
            hasData = true;
        }
        if (hasData) {
            result = META_STRING_PREFIX + result + META_STRING_SUFFIX + "\n";
        }
        
        return result;
    }
}
