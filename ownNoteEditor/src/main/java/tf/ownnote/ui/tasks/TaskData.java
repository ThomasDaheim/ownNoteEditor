/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.text.StringEscapeUtils;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.NoteData;

/**
 * A task in an ownnote.
 * Basically the text of a checkbox item + the checkbox. 
 * Along with status (from checkbox) and reference to a note.
 * 
 * @author thomas
 */
public class TaskData {
    private final BooleanProperty isCompleted = new SimpleBooleanProperty();
    private StringProperty myDescription = new SimpleStringProperty();
    private String myHtmlText;
    private int myTextPos;
    
    private NoteData myNote = null;
    
    private TaskData() {
    }
    
    public TaskData(final NoteData note, final String noteContent, final int textPos) {
        if (note == null) {
            throw new IllegalArgumentException("NoteData is null");
        }

        myNote = note;
        myTextPos = textPos;
        
        // parse htmlText into completed and find description
        parseHtmlText(noteContent, textPos);
    }
    
    private void parseHtmlText(final String noteContent, final int textPos) {
        if (textPos < 0) {
            throw new IllegalArgumentException("TextPos can't be smaller than 0: " + textPos);
        }
        
        // we are only interested in text from the starting position til end of line
        String noteText = noteContent.substring(myTextPos);
        
        if (!noteText.startsWith(OwnNoteEditor.ANY_BOXES)) {
            throw new IllegalArgumentException("Text not starting with checkbox pattern: " + noteText);
        }
        
        // find text til next end of line - but please without any html tags
        // tricky without end - text from readAllBytes can contain anything...
        int newlinePos = noteText.indexOf(System.lineSeparator());
        if (newlinePos == -1) {
            newlinePos = noteText.indexOf("\n");
        }
        if (newlinePos > -1) {
            noteText = noteText.substring(0, newlinePos);
        }
        
        // easy part: completed = checked
        if (noteText.startsWith(OwnNoteEditor.CHECKED_BOXES_1)) {
            isCompleted.setValue(Boolean.TRUE);
            noteText = noteText.substring(OwnNoteEditor.CHECKED_BOXES_1.length());
        } else if (noteText.startsWith(OwnNoteEditor.CHECKED_BOXES_2)) {
            isCompleted.setValue(Boolean.TRUE);
            noteText = noteText.substring(OwnNoteEditor.CHECKED_BOXES_2.length());
        } else if (noteText.startsWith(OwnNoteEditor.UNCHECKED_BOXES_1)) {
            isCompleted.setValue(Boolean.FALSE);
            noteText = noteText.substring(OwnNoteEditor.UNCHECKED_BOXES_1.length());
        } else if (noteText.startsWith(OwnNoteEditor.UNCHECKED_BOXES_2)) {
            isCompleted.setValue(Boolean.FALSE);
            noteText = noteText.substring(OwnNoteEditor.UNCHECKED_BOXES_2.length());
        } else {
            System.err.println("Something is wrong here with text: " + noteText);
        }
        
        // end of the line is nice - but only if no other checkbox in the line...
        if (noteText.contains(OwnNoteEditor.ANY_BOXES)) {
            noteText = noteText.substring(0, noteText.indexOf(OwnNoteEditor.ANY_BOXES));
        }
        
        // TFE, 20191211: remove html tags BUT convert </p> to </p> + line break
        noteText = noteText.replaceAll("\\<.*?\\>", "");

        // html text is the "raw" thing
        myHtmlText = noteText;
        // convert all &uml; back to &
        myDescription.setValue(StringEscapeUtils.unescapeHtml4(myHtmlText));
    }
    
    public BooleanProperty isCompletedProperty() {
        return isCompleted;
    }
    
    public boolean isCompleted() {
        return isCompleted.getValue();
    }
    
    public void setCompleted(final boolean complete) {
        isCompleted.setValue(complete);
    }
    
    public StringProperty descriptionProperty() {
        return myDescription;
    }
    
    public String getDescription() {
        return myDescription.getValue();
    }
    
    public String getHtmlText() {
        return myHtmlText;
    }
    
    public void setHtmlText(final String desc) {
        myHtmlText = desc;
        
        myDescription.setValue(StringEscapeUtils.unescapeHtml4(myHtmlText));
    }
    
    public String getFullTaskText() {
        if (isCompleted()) {
            return OwnNoteEditor.CHECKED_BOXES_1 + myHtmlText;
        } else {
            return OwnNoteEditor.UNCHECKED_BOXES_1 + myHtmlText;
        }
    }
    
    public NoteData getNoteData() {
        return myNote;
    }
    
    public int getTextPos() {
        return myTextPos;
    }
    
    public void setTextPos(final int pos) {
        myTextPos = pos;
    }
    
    @Override
    public String toString() {
        return getDescription();
    }
}
