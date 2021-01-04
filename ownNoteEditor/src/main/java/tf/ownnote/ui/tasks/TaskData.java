/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.time.LocalDateTime;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.lang3.RandomStringUtils;
import org.unbescape.html.HtmlEscape;
import tf.ownnote.ui.commentdata.CommentDataMapper;
import tf.ownnote.ui.commentdata.ICommentDataHolder;
import tf.ownnote.ui.commentdata.ICommentDataInfo;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;

/**
 * A task in a note.
 * Basically the text of a checkbox item + the checkbox. 
 * Along with status (from checkbox) and reference to a note.
 * 
 * @author thomas
 */
public class TaskData implements ICommentDataHolder {
    // info per available metadata - name & multiplicity
    public static enum CommentDataInfo implements ICommentDataInfo {
        ID("id", Multiplicity.SINGLE),
        STATUS("status", Multiplicity.SINGLE),
        PRIO("prio", Multiplicity.SINGLE),
        DUE_DATE("dueDate", Multiplicity.SINGLE);
        
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

    public enum TaskStatus {
        OPEN("Open"),
        IN_PROGRESS("In Progress"),
        BLOCKED("Blocked"),
        DONE("Done");
        
        private final String statusName;
        
        TaskStatus(final String name) {
            statusName = name;
        }
        
        @Override
        public String toString() {
            return statusName;
        }
        
        public static TaskStatus maxOpenStatus(final TaskStatus status1, final TaskStatus status2) {
            if (OPEN.equals(status1) && (IN_PROGRESS.equals(status2) || BLOCKED.equals(status2))) {
                return status2;
            }
            if (OPEN.equals(status2) && (IN_PROGRESS.equals(status1) || BLOCKED.equals(status1))) {
                return status1;
            }
            if (IN_PROGRESS.equals(status1) && BLOCKED.equals(status2)) {
                return status2;
            }
            if (IN_PROGRESS.equals(status2) && BLOCKED.equals(status1)) {
                return status1;
            }
            if (DONE.equals(status1) && !DONE.equals(status2)) {
                return status2;
            }
            if (DONE.equals(status2) && !DONE.equals(status1)) {
                return status1;
            }
            // nothing really to compare here - return normal comparison
            return status1.compareTo(status2) < 0 ? status2 : status1;
        }
    }
    
    public enum TaskPriority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High");

        private final String prioName;
        
        TaskPriority(final String prio) {
            prioName = prio;
        }
        
        @Override
        public String toString() {
            return prioName;
        }
        
        public boolean isHigherPrioThen(final TaskPriority other) {
            return (this.compareTo(other) > 0);
        }
        
        public boolean isSamePrioAs(final TaskPriority other) {
            return (this.compareTo(other) == 0);
        }

        public boolean isLowerPrioThen(final TaskPriority other) {
            return (this.compareTo(other) < 0);
        }
    }

    private final BooleanProperty isCompleted = new SimpleBooleanProperty();
    private final StringProperty myDescription = new SimpleStringProperty();
    private String myHtmlText;
    private String myEscapedText;
    private int myTextPos;
    private LocalDateTime myDueDate = null;
    private TaskStatus myStatus = TaskStatus.OPEN;
    private TaskPriority myPriority = TaskPriority.LOW;
    
    // TFE, 20201230: additional attributes are stored in separate meta-data file - link is the unique ID
    // initialized here to always have a value but can be overwritten from parsed noteContent
    private String myId = RandomStringUtils.random(12, "0123456789abcdef"); 
    
    private Note myNote = null;

    private TaskData() {
    }
    
    public TaskData(final Note note, final String noteContent, final int textPos) {
        if (note == null) {
            throw new IllegalArgumentException("Note is null");
        }

        myNote = note;
        myTextPos = textPos;
        
        // parse htmlText into completed and find description
        parseHtmlText(noteContent);
        
        isCompleted.addListener((ov, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                setTaskStatus(isCompleted() ? TaskStatus.DONE : TaskStatus.maxOpenStatus(TaskStatus.OPEN, myStatus));
            }
        });
        setTaskStatus(isCompleted() ? TaskStatus.DONE : TaskStatus.maxOpenStatus(TaskStatus.OPEN, myStatus));
    }
    
    private void parseHtmlText(final String noteContent) {
//        System.out.println("    parseHtmlText started: " + Instant.now());
        if (myTextPos < 0) {
            throw new IllegalArgumentException("TextPos can't be smaller than 0: " + myTextPos);
        }

        // find text til next end of line - but please without any html tags
        // tricky without end - text from readAllBytes can contain anything...
        int newlinePos = noteContent.indexOf(System.lineSeparator(), myTextPos);
        if (newlinePos == -1) {
            newlinePos = noteContent.indexOf("\n", myTextPos);
        }
        if (newlinePos == -1) {
            newlinePos = noteContent.length();
        }
//        System.out.println("      newline pos found: " + Instant.now());
        
        // we are only interested in text from the starting position til end of line
        String noteText = noteContent.substring(myTextPos, newlinePos);
//        System.out.println("      cut to newline: " + Instant.now());
        
        if (!noteText.startsWith(OwnNoteEditor.ANY_BOXES)) {
            throw new IllegalArgumentException("Text not starting with checkbox pattern: " + noteText);
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
//        System.out.println("      completed parsed: " + Instant.now());
        
        // end of the line is nice - but only if no other checkbox in the line...
        if (noteText.contains(OwnNoteEditor.ANY_BOXES)) {
            noteText = noteText.substring(0, noteText.indexOf(OwnNoteEditor.ANY_BOXES));
        }
//        System.out.println("      cut to checkbox: " + Instant.now());

        fromHtmlComment(noteText);
        
        // html text is the "raw" thing - including htmls tags, they might be temporary from tinyMCE
        myHtmlText = noteText;

        // TFE, 20191211: remove html tags BUT convert </p> to </p> + line break
//        System.out.println("noteText before strip: " + noteText);
        noteText = OwnNoteHTMLEditor.stripHtmlTags(noteText);
//        System.out.println("noteText after strip: " + noteText);
//        System.out.println("      html tags stripped: " + Instant.now());

        // escaped text is the "raw" thing - without htmls tags, they might be temporary from tinyMCE
        myEscapedText = noteText;

        // convert all &uml; back to &
        myDescription.setValue(HtmlEscape.unescapeHtml(myEscapedText));
//        System.out.println("    parseHtmlText completed: " + Instant.now());
    }
    
    private void fromHtmlComment(final String noteContent) {
        // parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // id="xyz"
        
        //System.out.println("parseTaskId for: " + noteContent);
        if (CommentDataMapper.containsCommentWithData(noteContent)) {
            CommentDataMapper.getInstance().fromComment(this, noteContent);
        }
    }
    
    public String toHtmlComment() {
        return CommentDataMapper.getInstance().toComment(this);
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
    
    public String getFullHtmlText() {
        return isCompleted() ? OwnNoteEditor.CHECKED_BOXES_2 + myHtmlText : OwnNoteEditor.UNCHECKED_BOXES_2 + myHtmlText;
    }
    
    public void setHtmlText(final String text) {
        myHtmlText = text;
        
        setEscapedText(OwnNoteHTMLEditor.stripHtmlTags(myHtmlText));
    }
    
    public String getEscapedText() {
        return myEscapedText;
    }
    
    public void setEscapedText(final String text) {
        myEscapedText = text;
        
        myDescription.setValue(HtmlEscape.unescapeHtml(myEscapedText));
    }
    
    public Note getNote() {
        return myNote;
    }
    
    public int getTextPos() {
        return myTextPos;
    }
    
    public void setTextPos(final int pos) {
        myTextPos = pos;
    }
    
    public String getId() {
        return myId;
    }
    public LocalDateTime getDueDate() {
        return myDueDate;
    }

    public void setDueDate(final LocalDateTime dueDate) {
        myDueDate = dueDate;
    }

    public TaskStatus getTaskStatus() {
        return myStatus;
    }

    public void setTaskStatus(final TaskStatus status) {
        myStatus = status;
    }

    public TaskPriority getTaskPriority() {
        return myPriority;
    }

    public void setTaskPriority(final TaskPriority prio) {
        myPriority = prio;
    }
    
    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public ICommentDataInfo[] getCommentDataInfo() {
        return CommentDataInfo.values();
    }

    @Override
    public void setFromString(ICommentDataInfo name, String value) {
        if (CommentDataInfo.ID.equals(name)) {
            myId = value;
        } else if (CommentDataInfo.PRIO.equals(name)) {
            setTaskPriority(TaskPriority.valueOf(value));
        } else if (CommentDataInfo.STATUS.equals(name)) {
            setTaskStatus(TaskStatus.valueOf(value));
        } else if (CommentDataInfo.DUE_DATE.equals(name)) {
            setDueDate(LocalDateTime.parse(value, OwnNoteEditor.DATE_TIME_FORMATTER));
        }
    }

    @Override
    public void setFromList(ICommentDataInfo name, List<String> values) {
    }

    @Override
    public String getAsString(ICommentDataInfo name) {
        if (CommentDataInfo.ID.equals(name)) {
            return myId;
        } else if (CommentDataInfo.PRIO.equals(name)) {
            return myPriority.name();
        } else if (CommentDataInfo.STATUS.equals(name)) {
            return myStatus.name();
        } else if (CommentDataInfo.DUE_DATE.equals(name)) {
            return myDueDate != null ? OwnNoteEditor.DATE_TIME_FORMATTER.format(myDueDate) : null;
        }
        return null;
    }

    @Override
    public List<String> getAsList(ICommentDataInfo name) {
        return null;
    }
}
