/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.helper;

import tf.ownnote.ui.notes.Note;

/**
 * Subscriber interface for editor content changes.
 * 
 * Option to break propagation chain by returning FALSE.
 * 
 * @author thomas
 */
public interface IFileContentChangeSubscriber {
    abstract boolean processFileContentChange(final FileContentChangeType changeType, final Note note, final String oldContent, final String newContent);
}
