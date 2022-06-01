/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.apache.commons.lang3.RandomStringUtils;
import org.unbescape.html.HtmlEscape;
import tf.helper.javafx.calendarview.CalendarView;
import tf.helper.javafx.calendarview.ICalendarEvent;
import tf.ownnote.ui.commentdata.CommentDataMapper;
import tf.ownnote.ui.commentdata.ICommentDataHolder;
import tf.ownnote.ui.commentdata.ICommentDataInfo;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.ITagHolder;
import tf.ownnote.ui.tags.TagData;
import tf.ownnote.ui.tags.TagManager;

/**
 * A task in a note.
 * Basically the text of a checkbox item + the checkbox. 
 * Along with status (from checkbox) and reference to a note.
 * 
 * @author thomas
 */
public class TaskData implements ICommentDataHolder, ITagHolder, ICalendarEvent {
    // TFE, 20200712: add search of unchecked boxes
    // TFE, 20201103: actual both variants of html are valid and need to be supported equally
    public final static String UNCHECKED_BOXES_1 = "<input type=\"checkbox\" />";
    public final static String CHECKED_BOXES_1 = "<input type=\"checkbox\" checked=\"checked\" />";
    public final static String UNCHECKED_BOXES_2 = "<input type=\"checkbox\">";
    public final static String CHECKED_BOXES_2 = "<input type=\"checkbox\" checked=\"checked\">";
    public final static String ANY_BOXES = "<input type=\"checkbox\"";
    public final static String ARCHIVED_BOX = "\u2611";

    // info per available metadata - name & multiplicity
    public static enum CommentDataInfo implements ICommentDataInfo {
        ID("id", Multiplicity.SINGLE),
        STATUS("status", Multiplicity.SINGLE),
        PRIO("prio", Multiplicity.SINGLE),
        DUE_DATE("dueDate", Multiplicity.SINGLE),
        COMMENT("comment", Multiplicity.SINGLE),
        TAGS("tags", Multiplicity.MULTIPLE);
        
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
        
        public boolean isCompleted() {
            return DONE.equals(this);
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
    private String myRawText;
    private String myHtmlText;
    private String myEscapedText;
    private int myTextPos;
    private final ObjectProperty<LocalDateTime> myDueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> myEventDate = new SimpleObjectProperty<>();
    private String myComment = null;
    private final ObjectProperty<TaskStatus> myStatus = new SimpleObjectProperty<>(TaskStatus.OPEN);
    private final ObjectProperty<TaskPriority> myPriority = new SimpleObjectProperty<>(TaskPriority.LOW);
    
    // TFE, 20201230: initialized here to always have a value but can be overwritten from parsed noteContent
    private String myId = RandomStringUtils.random(12, "0123456789abcdef"); 

    // TFE, 20210308: tasks can have their own tags!
    private final ObservableSet<TagData> myTags = FXCollections.<TagData>observableSet();
    
    private Note myNote = null;
    
    private boolean inStatusChange = false;

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
        
        // tricky, two properties listening to each other...
        isCompleted.addListener((ov, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && !inStatusChange) {
                inStatusChange = true;
                setTaskStatus(isCompleted() ? TaskStatus.DONE : TaskStatus.maxOpenStatus(TaskStatus.OPEN, myStatus.get()));
                myNote.setUnsavedChanges(true);
                inStatusChange = false;
            }
        });
        inStatusChange = true;
        // init status if no saved value in note text...
        setTaskStatus(isCompleted() ? TaskStatus.DONE : TaskStatus.maxOpenStatus(TaskStatus.OPEN, myStatus.get()));
        inStatusChange = false;
        
        myStatus.addListener((ov, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && !inStatusChange) {
                inStatusChange = true;
                setCompleted(newValue.isCompleted());
                myNote.setUnsavedChanges(true);
                inStatusChange = false;
            }
        });

        // go, tell it to the mountains
        myTags.addListener((SetChangeListener.Change<? extends TagData> change) -> {
            // can happen e.g. when using constructor fromHtmlComment()
            if (myNote == null) {
                return;
            }
            
            if (change.wasAdded()) {
//                System.out.println("Linking note " + myNote.getNoteName() + " to tag " + change.getElementAdded().getName());
                change.getElementAdded().getLinkedNotes().add(myNote);
                myNote.setUnsavedChanges(true);
            }

            if (change.wasRemoved()) {
//                System.out.println("Unlinking note " + myNote.getNoteName() + " from tag " + change.getElementRemoved().getName());
                change.getElementRemoved().getLinkedNotes().remove(myNote);
                myNote.setUnsavedChanges(true);
            }
        });
        
        // we also need the date as localdate for ICalenderEvent
        if (myDueDate.get() == null) {
            myEventDate.set(null);
        } else {
            myEventDate.set(myDueDate.get().toLocalDate());
        }
        myDueDate.addListener((ov, oldValue, newValue) -> {
            if (newValue == null) {
                myEventDate.set(null);
            } else {
                myEventDate.set(newValue.toLocalDate());
            }
        });
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
//        System.out.println("      newline pos found: " + newlinePos + " @" + Instant.now());
        
        // we are only interested in text from the starting position til end of line
        String noteText = noteContent.substring(myTextPos, newlinePos);
//        System.out.println("      cut to newline: " + noteText + " @"  + Instant.now());
        
        if (!noteText.startsWith(ANY_BOXES)) {
            throw new IllegalArgumentException("Text not starting with checkbox pattern: " + noteText);
        }
        
        // easy part: completed = checked
        String checkBoxText = "";
        if (noteText.startsWith(CHECKED_BOXES_1)) {
            isCompleted.setValue(Boolean.TRUE);
            noteText = noteText.substring(CHECKED_BOXES_1.length());
            checkBoxText = CHECKED_BOXES_1;
        } else if (noteText.startsWith(CHECKED_BOXES_2)) {
            isCompleted.setValue(Boolean.TRUE);
            noteText = noteText.substring(CHECKED_BOXES_2.length());
            checkBoxText = CHECKED_BOXES_2;
        } else if (noteText.startsWith(UNCHECKED_BOXES_1)) {
            isCompleted.setValue(Boolean.FALSE);
            noteText = noteText.substring(UNCHECKED_BOXES_1.length());
            checkBoxText = UNCHECKED_BOXES_1;
        } else if (noteText.startsWith(UNCHECKED_BOXES_2)) {
            isCompleted.setValue(Boolean.FALSE);
            noteText = noteText.substring(UNCHECKED_BOXES_2.length());
            checkBoxText = UNCHECKED_BOXES_2;
        } else {
            System.err.println("Something is wrong here with text: " + noteText);
        }
//        System.out.println("      completed parsed: " + noteText + " @" + Instant.now());
        
        // end of the line is nice - but only if no other checkbox in the line...
        if (noteText.contains(ANY_BOXES)) {
            noteText = noteText.substring(0, noteText.indexOf(ANY_BOXES));
//        System.out.println("      cut to checkbox: " + noteText + " @" + Instant.now());
        }

        fromHtmlComment(noteText);
        
        // TFE, 20210118: store rawtext including checkbox tag as well
        myRawText = checkBoxText + noteText;
        // html text is the "raw" thing - including htmls tags, they might be temporary from tinyMCE
        myHtmlText = noteText;

        // TFE, 20191211: remove html tags BUT convert </p> to </p> + line break
//        System.out.println("noteText before strip: " + noteText);
        noteText = OwnNoteHTMLEditor.stripHtmlTags(noteText);
//        System.out.println("noteText after strip: " + noteText);
//        System.out.println("      html tags stripped: " + noteText + " @" + Instant.now());

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
    
    public String getRawText() {
        return myRawText;
    }
    
    public void setRawText(final String text) {
        // TODO: should somehow also set html text...
        myRawText = text;
    }
    
    public String getHtmlText() {
        return myHtmlText;
    }
    
    public String getFullHtmlText() {
        return isCompleted() ? CHECKED_BOXES_2 + myHtmlText : UNCHECKED_BOXES_2 + myHtmlText;
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

    public ObjectProperty<LocalDateTime> dueDateProperty() {
        return myDueDate;
    }

    public LocalDateTime getDueDate() {
        return myDueDate.get();
    }

    public void setDueDate(final LocalDateTime dueDate) {
        myDueDate.set(dueDate);
    }

    public String getComment() {
        return myComment;
    }
    
    public void setComment(final String text) {
        myComment = text;
    }
    
    public ObjectProperty<TaskStatus> taskStatusProperty() {
        return myStatus;
    }

    public TaskStatus getTaskStatus() {
        return myStatus.get();
    }

    public void setTaskStatus(final TaskStatus status) {
        myStatus.set(status);
    }

    public ObjectProperty<TaskPriority> taskPriorityProperty() {
        return myPriority;
    }

    public TaskPriority getTaskPriority() {
        return myPriority.get();
    }

    public void setTaskPriority(final TaskPriority prio) {
        myPriority.set(prio);
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
    
    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public ICommentDataInfo[] getCommentDataInfo() {
        return CommentDataInfo.values();
    }
    
    @Override
    public String getDataHolderInfo() {
        return "TaskData: " + myNote.getNoteFileName() + ", " + myDescription.get();
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
        } else if (CommentDataInfo.COMMENT.equals(name)) {
            myComment = value;
        }
    }

    @Override
    public void setFromList(ICommentDataInfo name, List<String> values) {
        if (CommentDataInfo.TAGS.equals(name)) {
            if (OwnNoteEditor.AppVersion.V6_1.isHigherAppVersionThan(myNote.getMetaData().getAppVersion())) {
                setTags(TagManager.getInstance().tagsForNames(new HashSet<>(values), null, true));
            } else {
                // new way of doing things with external names
                setTags(TagManager.getInstance().tagsForExternalNames(new HashSet<>(values), null, true));
            }
        }
    }

    @Override
    public String getAsString(ICommentDataInfo name) {
        if (CommentDataInfo.ID.equals(name)) {
            return myId;
        } else if (CommentDataInfo.PRIO.equals(name)) {
            return getTaskPriority().name();
        } else if (CommentDataInfo.STATUS.equals(name)) {
            return getTaskStatus().name();
        } else if (CommentDataInfo.DUE_DATE.equals(name)) {
            return getDueDate() != null ? OwnNoteEditor.DATE_TIME_FORMATTER.format(getDueDate()) : null;
        } else if (CommentDataInfo.COMMENT.equals(name)) {
            return myComment;
        }
        return null;
    }

    @Override
    public List<String> getAsList(ICommentDataInfo name) {
        if (CommentDataInfo.TAGS.equals(name)) {
            return myTags.stream().map((t) -> {
                if (OwnNoteEditor.AppVersion.CURRENT.isLowerAppVersionThan(OwnNoteEditor.AppVersion.V6_1)) {
                    // not sure how this might happen - since we invented app version with v6.1
                    System.err.println("Reading note metadata with app version: " + OwnNoteEditor.AppVersion.CURRENT.getVersionId());
                    return t.getName();
                } else {
                    return t.getExternalName();
                }
            }).collect(Collectors.toList());
        }
        return null;
    }
    
    // things to do to be a good CalendarEvent

    @Override
    public ObjectProperty<LocalDate> getStartDate() {
        return myEventDate;
    }

    @Override
    public ObjectProperty<LocalDate> getEndDate() {
        return myEventDate;
    }

    @Override
    public ObjectProperty<CalendarView.DateStyle> getStyle() {
        return new SimpleObjectProperty<>(TaskManager.getDateStyleForDueDate(this));
    }

    @Override
    public StringProperty getEventDescription() {
        return myDescription;
    }
}
