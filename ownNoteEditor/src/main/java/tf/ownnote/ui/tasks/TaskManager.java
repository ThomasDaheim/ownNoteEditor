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
        // TODO: fill with life and react to the various possibilites of changes:
        // 1) file with tasks deleted -> remove tasks from own list
        // 2) file changed -> rescan for tasks and update own list
        // 3) new file -> scan for tasks and update own list (similar to #2)
    }

    @Override
    public void processFileContentChange(NoteData note, String newContent) {
        // TODO: fill with life and react to the various possibilites of changes:
        // 1) file changed -> rescan for tasks and update own list - same task as for 2) of processFileChange, only no need to reload note content
    }
}
