/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.commons.text.StringEscapeUtils;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * A task in an ownnote.
 * Basically the text of a checkbox item + the checkbox. 
 * Along with status (from checkbox) and reference to a note.
 * 
 * @author thomas
 */
public class TaskData {
    private BooleanProperty isCompleted = new SimpleBooleanProperty();
    private String myDescription;
    private int myTextPos;
    
    private NoteData myNote = null;
    
    private TaskData() {
    }
    
    public TaskData(final NoteData note, final int textPos) {
        if (note == null) {
            throw new IllegalArgumentException("NoteData is null");
        }

        myNote = note;
        myTextPos = textPos;
        
        // parse htmlText into completed and find description
        parseHtmlText(textPos);
    }
    
    private void parseHtmlText(final int textPos) {
        if (textPos < 0) {
            throw new IllegalArgumentException("TextPos can't be smaller than 0: " + textPos);
        }
        
        // we are only interested in text from the starting position til end of line
        String noteText = OwnNoteFileManager.getInstance().readNote(myNote).substring(myTextPos);
        
        if (!noteText.startsWith(OwnNoteEditor.ANY_BOXES)) {
            throw new IllegalArgumentException("Text not starting with checkbox pattern: " + noteText);
        }
        
        // easy part: completed = checked
        if (noteText.startsWith(OwnNoteEditor.CHECKED_BOXES)) {
            isCompleted.setValue(Boolean.TRUE);
            noteText = noteText.substring(OwnNoteEditor.CHECKED_BOXES.length());
        } else {
            isCompleted.setValue(Boolean.FALSE);
            noteText = noteText.substring(OwnNoteEditor.UNCHECKED_BOXES.length());
        }
        
        // now find text til next end of line - but please without any html tags
        // tricky without end - text from readAllBytes can contain anything...
        int newlinePos = noteText.indexOf(System.lineSeparator());
        if (newlinePos == -1) {
            newlinePos = noteText.indexOf("\n");
        }
        if (newlinePos > -1) {
            noteText = noteText.substring(0, newlinePos);
        }
        
        // TFE, 20191211: remove html tags BUT convert </p> to </p> + line break
        noteText = noteText.replaceAll("\\<.*?\\>", "");
        // convert all &uml; back to &
        myDescription = StringEscapeUtils.unescapeHtml4(noteText);
    }
    
    public BooleanProperty isCompletedProperty() {
        return isCompleted;
    }
    
    public boolean isCompleted() {
        return isCompleted.getValue();
    }
    
    public String getDescription() {
        return myDescription;
    }
    
    public NoteData getNoteData() {
        return myNote;
    }
    
    public int getTextPos() {
        return myTextPos;
    }
    
    @Override
    public String toString() {
        return getDescription();
    }
}
