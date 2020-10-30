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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.notes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

    // info per available metadata - name & multiplicity
    // TODO: add values here as well
    private static enum MetaDataInfo {
        AUTHORS("authors", Multiplicity.MULTIPLE),
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

    private final ObservableList<String> myAuthors = FXCollections.observableArrayList();
    private final ObservableList<String> myTags = FXCollections.observableArrayList();
    
    public NoteMetaData() {
        super();
    }
    
    public boolean isEmpty() {
        return (myAuthors.isEmpty() && myTags.isEmpty());
    }

    public ObservableList<String> getAuthors() {
        return myAuthors;
    }

    public void setAuthors(final List<String> authors) {
        myAuthors.clear();
        myAuthors.addAll(authors);
    }

    public String getAuthor() {
        if (!myAuthors.isEmpty()) {
            return myAuthors.get(myAuthors.size()-1);
        } else {
            return null;
        }
    }

    public void addAuthor(final String author) {
        // add author to history chain if its different from last one
        if (author != null && !author.equals(getAuthor())) {
            myAuthors.add(author);
        }
    }

    public ObservableList<String> getTags() {
        return myTags;
    }

    public void setTags(final List<String> tags) {
        myTags.clear();
        myTags.addAll(tags);
    }
    
    public static NoteMetaData fromHtmlString(final String htmlString) {
        final NoteMetaData result = new NoteMetaData();

        // TODO: parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // author="xyz" tags="a:::b:::c"
        
        if (htmlString != null && htmlString.startsWith(META_STRING_PREFIX) && htmlString.endsWith(META_STRING_SUFFIX)) {
            String [] data = htmlString.substring(META_STRING_PREFIX.length(), htmlString.length()-META_STRING_SUFFIX.length()).
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
                            case AUTHORS:
                                result.setAuthors(Arrays.asList(values));
                                infoFound = true;
                                break;
                            case TAGS:
                                result.setTags(Arrays.asList(values));
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
        String result = "";
        boolean hasData = false;
        
        if (data.getAuthor() != null) {
            result += MetaDataInfo.AUTHORS.getDataName() + "=\"" + data.getAuthors().stream().collect(Collectors.joining(META_VALUES_SEP)) + "\"";
            hasData = true;
        }
        if (!data.getTags().isEmpty()) {
            if (hasData) {
                result += META_DATA_SEP;
            }
            result += MetaDataInfo.TAGS.getDataName() + "=\"" + data.getTags().stream().collect(Collectors.joining(META_VALUES_SEP)) + "\"";
            hasData = true;
        }
        if (hasData) {
            result = META_STRING_PREFIX + result + META_STRING_SUFFIX + "\n";
        }
        
        return result;
    }
}
