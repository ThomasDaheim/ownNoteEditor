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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.TableColumn;

/**
 * Store pairs of "tablecolumn text", "tablecolumn sorttype"
 * values for easier management of persisting tableview sorting.
 * 
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class TableSortHelper extends HashMap<String,String> {
    
    public TableSortHelper() {
        super();
    }
    
    public TableSortHelper(final TableSortHelper sortData) {
        super(sortData);
    }
    
    public TableSortHelper(final Map<String,String> sortData) {
        super(sortData);
    }
    
    public TableSortHelper(final List<TableColumn<Map<String, String>,?>> sortColumns) {
        super();

        // loop through list and extract
        // column text & sort type
        
        final Map<String, String> sortData = new HashMap<String, String>();
        for (TableColumn<Map<String, String>,?> sortColumn : sortColumns) {
            sortData.put(sortColumn.getText(), sortColumn.getSortType().toString());
        }
        
        this.putAll(sortData);
    }
    
    public Map<String,String> getAsMap() {
        return this;
    }
    
    public final List<TableColumn<Map<String, String>,?>> toTableColumnList(final List<TableColumn<Map<String, String>,?>> columns) {
        final List<TableColumn<Map<String, String>,?>> tableColumnList = new ArrayList<TableColumn<Map<String, String>,?>>();
        
        // loop through myself and extract "text" & "sorttype"
        // find column with name and set sort type & add to observable list
        for (Map.Entry<String, String> entry : this.entrySet())
        {
            for (TableColumn<Map<String, String>,?> column : columns)
            {
                if (entry.getKey().equals(column.getText())) {
                    column.setSortType(TableColumn.SortType.valueOf(entry.getValue()));
                    tableColumnList.add(column);
                }
            }
        }

        return tableColumnList;
    }
    
    /** Write the object to a Base64 string. */
    public static String toString(final TableSortHelper sortData) {
        StringBuilder stringBuilder = new StringBuilder();

        // keep things readable... don't try to serialize the map
        // https://dzone.com/articles/two-ways-convert-java-map
        Map<String,String> map = sortData.getAsMap();
        for (String key : map.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();        
        
        /*
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream( baos );
            oos.writeObject( sortData ); 
            oos.close();
            baos.close();
            
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(TableSortHelper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (oos != null)
                    oos.close();
                if (baos != null)
                    baos.close();
            } catch (IOException ex) {
                Logger.getLogger(TableSortHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return "";
        */
    }

    /** Read the object from Base64 string. */
    public static TableSortHelper fromString( final String input ) {
        Map<String, String> map = new HashMap<String, String>();

        // keep things readable... don't try to serialize the map
        if (input.length() > 0) {
            String[] nameValuePairs = input.split("&");
            for (String nameValuePair : nameValuePairs) {
                String[] nameValue = nameValuePair.split("=");
                try {
                    map.put(URLDecoder.decode(nameValue[0], "UTF-8"), nameValue.length > 1 ? URLDecoder.decode(
                    nameValue[1], "UTF-8") : "");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("This method requires UTF-8 encoding support", e);
                }
            }
        }

        return new TableSortHelper(map);
    
        /*
        ObjectInputStream ois = null;
        try {
            byte [] data = Base64.getDecoder().decode( s );
            ois = new ObjectInputStream( 
                    new ByteArrayInputStream(  data ) );
            TableSortHelper o = (TableSortHelper) ois.readObject();
            ois.close();
            
            return o;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(TableSortHelper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (ois != null)
                    ois.close();
            } catch (IOException ex) {
                Logger.getLogger(TableSortHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return new TableSortHelper();
        */
    }
}
