/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.helper;

/**
 * Subscriber interface for editor content changes.
 * 
 * Option to break propagation chain by returning FALSE.
 * 
 * @author thomas
 */
public interface IFileContentChangeSubscriber {
    abstract boolean processFileContentChange(final FileContentChangeType changeType, final NoteData note, final String oldContent, final String newContent);
}
