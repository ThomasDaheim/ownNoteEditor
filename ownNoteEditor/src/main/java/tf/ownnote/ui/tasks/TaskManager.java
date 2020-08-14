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
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

            taskList.addAll(tasksFromNote(note, noteContent));
        }
    }
    
    // noteContent as separate parm since it could be called from change withint editor before save
    public List<TaskData> tasksFromNote(final NoteData note, final String noteContent) {
        final List<TaskData> result = new ArrayList<>();

        // iterate over all matches and create TaskData items
        for (int textPos : findAllOccurences(noteContent, OwnNoteEditor.ANY_BOXES)) {
            result.add(new TaskData(note, noteContent, textPos));
        }
        
        return result;
    }
    
    private List<Integer> findAllOccurences(final String text, final String tofind) {
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
    
    public void resetTaskList() {
        taskList = null;
    }

    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        inFileChange = true;
        // TODO: fill with life and react to the various possibilites of changes:
        // 1) file with tasks deleted -> remove tasks from own list
        // 2) new file -> scan for tasks and update own list (similar to #2)
        // 3) file was changed on disk -> rescan and compare
        inFileChange = false;
        
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final NoteData note, final String oldContent, final String newContent) {
//        System.out.println("processFileContentChange: " + changeType + ", " + note + ", " + oldContent + ", " + newContent.substring(0, 20));

        inFileChange = true;
        if (FileContentChangeType.CONTENT_CHANGED.equals(changeType)) {
            // rescan text for tasks and update tasklist accordingly
            final List<TaskData> newTasks = tasksFromNote(note, newContent);
            final List<TaskData> oldTasks = taskList.stream().filter((t) -> {
                return t.getNoteData().getGroupName().equals(note.getGroupName()) && t.getNoteData().getNoteName().equals(note.getNoteName());
            }).collect(Collectors.toList());
            
            // compare old a new to minimize change impact on observable list
            // 1: same description = only pos & selected might have changed
            // takes care of all changes before & after task
            for (TaskData newTask: new ArrayList<>(newTasks)) {
                Optional<TaskData> oldnew = oldTasks.stream().filter((t) -> {
                    return t.getDescription().equals(newTask.getDescription());
                }).findFirst();
                
                if (oldnew.isPresent()) {
                    oldnew.get().setTextPos(newTask.getTextPos());
                    oldnew.get().setCompleted(newTask.isCompleted());
                    
                    // nothing more to be done here
                    newTasks.remove(newTask);
                    oldTasks.remove(oldnew.get());
                }
            }
            
            // 2. same position but different description = description & selected might have changed
            // takes care of all changes inside task
            for (TaskData newTask: new ArrayList<>(newTasks)) {
                Optional<TaskData> oldnew = oldTasks.stream().filter((t) -> {
                    return t.getTextPos() == newTask.getTextPos();
                }).findFirst();
                
                if (oldnew.isPresent()) {
                    oldnew.get().setCompleted(newTask.isCompleted());
                    oldnew.get().setDescription(newTask.getDescription());
                    
                    // nothing more to be done here
                    newTasks.remove(newTask);
                    oldTasks.remove(oldnew.get());
                }
            }
            
            // 3. what is left? add & delete of tasks
            taskList.removeAll(oldTasks);
            taskList.addAll(newTasks);
        } else {
            // nothing to do - FileContentChangeType.CONTENT_CHANGED covers all
//            // checkbox might not be start of innerHtml, whereas TaskData description is only the part after checkbox...
//            // FFFFUUUUCCCCKKKK innerHtml sends back <input type="checkbox" checked="checked"> instead of correct <input type="checkbox" checked="checked" />
//            String oldDescription = null;
//            int checkIndex = oldContent.indexOf(OwnNoteEditor.WRONG_UNCHECKED_BOXES);
//            if (checkIndex > -1) {
//                oldDescription = StringEscapeUtils.unescapeHtml4(oldContent.substring(checkIndex + OwnNoteEditor.WRONG_UNCHECKED_BOXES.length()));
//            }
//            checkIndex = oldContent.indexOf(OwnNoteEditor.WRONG_CHECKED_BOXES);
//            if (checkIndex > -1) {
//                oldDescription = StringEscapeUtils.unescapeHtml4(oldContent.substring(checkIndex + OwnNoteEditor.WRONG_CHECKED_BOXES.length()));
//            }
//            if (oldDescription == null) {
//                System.out.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
//                return;
//            }
//            Boolean newCompleted = null;
//            if (newContent.contains(OwnNoteEditor.WRONG_UNCHECKED_BOXES)) {
//                newCompleted = false;
//            } else if (newContent.contains(OwnNoteEditor.WRONG_CHECKED_BOXES)) {
//                newCompleted = true;
//            }
//            if (newCompleted == null) {
//                System.out.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
//                return;
//            }
//
//            TaskData changedTask = null;
//            for (TaskData task : taskList) {
//                if (task.getNoteData().equals(note) && task.getDescription().equals(oldDescription)) {
//                    changedTask = task;
//                    break;
//                }
//            }
//
//            if (changedTask != null) {
//                // checkbox must have changed
//                if (changedTask.isCompleted() && !newCompleted) {
//                    changedTask.setCompleted(false);
//                } else if (!changedTask.isCompleted() && newCompleted) {
//                    changedTask.setCompleted(true);
//                }
//            }
        }
            
        inFileChange = false;
        
        return true;
    }
    
    public void processTaskCompletedChanged(final TaskData task) {
        if (inFileChange) {
            return;
        }
        
        System.out.println("processTaskCompletedChanged for: " + task);
    }
    
    public boolean inFileChange() {
        return inFileChange;
    }
}