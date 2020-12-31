/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringEscapeUtils;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.notes.NoteMetaData;

/**
 * A task in a note.
 * Basically the text of a checkbox item + the checkbox. 
 * Along with status (from checkbox) and reference to a note.
 * 
 * @author thomas
 */
public class TaskData {
    private static final String META_STRING_PREFIX = "<!-- ";
    private static final String META_STRING_SUFFIX = " -->";
    private static final String META_DATA_SEP = "---";
    private static final String META_VALUES_SEP = ":::";

    private static final Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
    private static final Inflater decompresser = new Inflater();

    private static enum Multiplicity {
        SINGLE,
        MULTIPLE
    }
    
    // info per available metadata - name & multiplicity
    private static enum MetaDataInfo {
        ID("id", Multiplicity.SINGLE),
        STATUS("status", Multiplicity.SINGLE),
        PRIO("prio", Multiplicity.SINGLE),
        DUE_DATE("dueDate", Multiplicity.SINGLE);
        
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
        myDescription.setValue(StringEscapeUtils.unescapeHtml4(myEscapedText));
//        System.out.println("    parseHtmlText completed: " + Instant.now());
    }
    
    private void fromHtmlComment(final String noteContent) {
        // parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // id="xyz"
        
        //System.out.println("parseTaskId for: " + noteContent);
        if (noteContent.startsWith(META_STRING_PREFIX) || noteContent.contains(META_STRING_SUFFIX)) {
            final String contentString = noteContent.split(META_STRING_SUFFIX)[0] + META_STRING_SUFFIX;
            String [] data = contentString.substring(META_STRING_PREFIX.length(), contentString.length()-META_STRING_SUFFIX.length()).
                    strip().split(META_DATA_SEP);
            
            // check for "data" first to decompress if required
            boolean dataFound = false;
            String dataString = "";
            for (String nameValue : data) {
                if (nameValue.startsWith("data=\"") && nameValue.endsWith("\"")) {
                    final String[] values = nameValue.substring("data".length()+2, nameValue.length()-1).
                        strip().split(META_VALUES_SEP);

                    decompresser.reset();
                    final byte[] decoded = Base64.decodeBase64(values[0]);
                    decompresser.setInput(decoded, 0, decoded.length);

                    final byte[] temp = new byte[32768];
                    try {
                        final int resultLength = decompresser.inflate(temp);

                        final byte[] input = new byte[resultLength];
                        System.arraycopy(temp, 0, input, 0, resultLength);
                        
                        dataString = new String(input, "UTF-8");
                        
                        dataFound = true;
                    } catch (DataFormatException | UnsupportedEncodingException ex) {
                        Logger.getLogger(NoteMetaData.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (dataFound) {
                data = dataString.strip().split(META_DATA_SEP);
            }

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
                            case ID:
                                myId = values[0];
                                infoFound = true;
                                break;
                            case STATUS:
                                myStatus = TaskStatus.valueOf(values[0]);
                                infoFound = true;
                                break;
                            case PRIO:
                                myPriority = TaskPriority.valueOf(values[0]);
                                infoFound = true;
                                break;
                            case DUE_DATE:
                                myDueDate = LocalDateTime.parse(values[0], OwnNoteEditor.DATE_TIME_FORMATTER);
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
    }
    
    public String toHtmlComment() {
        final StringBuffer result = new StringBuffer();
        
        result.append(MetaDataInfo.ID.getDataName());
        result.append("=\"");
        result.append(myId);
        result.append("\"");

        result.append(META_DATA_SEP);
        result.append(MetaDataInfo.STATUS.getDataName());
        result.append("=\"");
        result.append(myStatus.name());
        result.append("\"");

        result.append(META_DATA_SEP);
        result.append(MetaDataInfo.PRIO.getDataName());
        result.append("=\"");
        result.append(myPriority.name());
        result.append("\"");
        
        if (myDueDate != null) {
            result.append(META_DATA_SEP);
            result.append(MetaDataInfo.DUE_DATE.getDataName());
            result.append("=\"");
            result.append(OwnNoteEditor.DATE_TIME_FORMATTER.format(myDueDate));
            result.append("\"");
        }

        try {
            compresser.reset();
            compresser.setInput(result.toString().getBytes("UTF-8"));
            compresser.finish();
            
            final byte[] temp = new byte[32768];
            final int compressedDataLength = compresser.deflate(temp);
            final byte[] output = new byte[compressedDataLength];
            System.arraycopy(temp, 0, output, 0, compressedDataLength);
            final String encodedResult = Base64.encodeBase64String(output);

            // lets compress - if it is really shorter :-)
            if (encodedResult.length() < result.length()) {
                result.delete(0, result.length());
                result.append("data");
                result.append("=\"");
                result.append(encodedResult);
                result.append("\"");
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NoteMetaData.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return META_STRING_PREFIX + result.toString() + META_STRING_SUFFIX;
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
        
        myDescription.setValue(StringEscapeUtils.unescapeHtml4(myEscapedText));
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
}
