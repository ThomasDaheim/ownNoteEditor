/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.text.StringEscapeUtils;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * Handler for creation, search, update, sync of tasks with their notes.
 * 
 * Provide observable list of open / completed tasks from all notes.
 * 
 * Handle change notifications from file system or notes editor and update lists.
 * 
 * @author thomas
 */
public class TaskManager implements IFileChangeSubscriber, IFileContentChangeSubscriber {
    private final static TaskManager INSTANCE = new TaskManager();
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    private ObservableList<TaskData> taskList = null;
    
    private boolean inFileChange = false;
    
    private TaskManager() {
        super();
    }
        
    public static TaskManager getInstance() {
        return INSTANCE;
    }

    public void setCallback(final OwnNoteEditor editor) {
        myEditor = editor;

        // now we can register everywhere
        OwnNoteFileManager.getInstance().subscribe(INSTANCE);
        myEditor.getNoteEditor().subscribe(INSTANCE);
    }
    
    private void initTaskList() {
        // find all notes containing checkbox and parse to create TaskData for them
        final List<NoteData> taskNotes = OwnNoteFileManager.getInstance().getNotesWithText(OwnNoteEditor.ANY_BOXES);
        
        for (NoteData note : taskNotes) {
            final String noteContent = OwnNoteFileManager.getInstance().readNote(note);
            
            // iterate over all matches and create TaskData items
            for (int textPos : findAllOccurences(noteContent, OwnNoteEditor.ANY_BOXES)) {
                taskList.add(new TaskData(note, textPos));
            }
        }
    }
    
    public List<Integer> findAllOccurences(final String text, final String tofind) {
        final List<Integer> result = new ArrayList<>();
        
        if (text.isEmpty() || tofind.isEmpty()) {
            return result;
        }
        
        int index = 0;
        while (true) {
            index = text.indexOf(tofind, index);
            if (index != -1) {
                result.add(index);
                // works here under the assumption that we don't search for e.g. "AA" in "AAA"
                index += tofind.length();
            } else {
                break;
            }
        }
        
        return result;
    }
    
    public ObservableList<TaskData> getTaskList() {
        if (taskList == null) {
            taskList = FXCollections.observableArrayList();
            // lazy loading
            initTaskList();
        }
        
        // you can use but not change
        return FXCollections.unmodifiableObservableList(taskList);
    }

    @Override
    public void processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        inFileChange = true;
        // TODO: fill with life and react to the various possibilites of changes:
        // 1) file with tasks deleted -> remove tasks from own list
        // 2) file changed -> rescan for tasks and update own list
        // 3) new file -> scan for tasks and update own list (similar to #2)
        inFileChange = false;
    }

    @Override
    public void processFileContentChange(final FileContentChangeType changeType, final NoteData note, final String oldContent, final String newContent) {
        inFileChange = true;
        // FFFFUUUUCCCCKKKK innerHTML sends back <input type="checkbox" checked="checked"> instead of correct <input type="checkbox" checked="checked" />
        String oldDescription = null;
        if (oldContent.startsWith(OwnNoteEditor.WRONG_UNCHECKED_BOXES)) {
            oldDescription = StringEscapeUtils.unescapeHtml4(oldContent.substring(OwnNoteEditor.WRONG_UNCHECKED_BOXES.length()));
        } else if (oldContent.startsWith(OwnNoteEditor.WRONG_CHECKED_BOXES)) {
            oldDescription = StringEscapeUtils.unescapeHtml4(oldContent.substring(OwnNoteEditor.WRONG_CHECKED_BOXES.length()));
        }
        if (oldDescription == null) {
            System.out.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
            return;
        }
        Boolean newCompleted = null;
        if (newContent.startsWith(OwnNoteEditor.WRONG_UNCHECKED_BOXES)) {
            newCompleted = false;
        } else if (newContent.startsWith(OwnNoteEditor.WRONG_CHECKED_BOXES)) {
            newCompleted = true;
        }
        if (newCompleted == null) {
            System.out.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
            return;
        }
        
        TaskData changedTask = null;
        for (TaskData task : taskList) {
            if (task.getNoteData().equals(note) && task.getDescription().equals(oldDescription)) {
                changedTask = task;
                break;
            }
        }
        
        if (changedTask != null) {
            // checkbox must have changed
            if (changedTask.isCompleted() && !newCompleted) {
                changedTask.setCompleted(false);
            } else if (!changedTask.isCompleted() && newCompleted) {
                changedTask.setCompleted(true);
            }
        }
        inFileChange = false;
    }
    
    public void processTaskCompletedChanged(final TaskData task) {
        if (inFileChange) {
            return;
        }
        
        System.out.println("processTaskCompletedChanged for: " + task);
    }
}
