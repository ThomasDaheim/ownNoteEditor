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
package tf.ownnote.ui.notes;

import java.util.HashMap;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class GroupData extends HashMap<String,String> {

    // to reference the columns for groups table
    private enum GroupMapKey {
        groupName,
        groupDelete,
        groupCount;
    }
        
    public static final String ALL_GROUPS = "All";
    public static final String NOT_GROUPED = "Not grouped";
    public static final String NEW_GROUP = "New group";
    
    public GroupData() {
        super();
    }
    
    public GroupData(final GroupData dataRow) {
        super(dataRow);
    }
    
    public static String getGroupDataName(final int i) {
        return GroupMapKey.values()[i].name();
    }
    
    public String getGroupName() {
        return get(GroupMapKey.groupName.name());
    }

    public void setGroupName(final String groupName) {
        put(GroupMapKey.groupName.name(), groupName);
    }

    public String getGroupDelete() {
        return get(GroupMapKey.groupDelete.name());
    }

    public void setGroupDelete(final String groupDelete) {
        put(GroupMapKey.groupDelete.name(), groupDelete);
    }

    public String getGroupCount() {
        return get(GroupMapKey.groupCount.name());
    }

    public void setGroupCount(final String groupCount) {
        put(GroupMapKey.groupCount.name(), groupCount);
    }
}
