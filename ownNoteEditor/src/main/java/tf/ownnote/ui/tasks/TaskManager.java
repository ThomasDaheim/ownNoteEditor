/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.tasks;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import tf.ownnote.ui.commentdata.CommentDataMapper;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.main.OwnNoteEditor;
import tf.ownnote.ui.notes.Note;

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
    
    private final static String TASK_DIR = File.separator + "MetaData";
    private final static String TASK_FILE = TASK_DIR + File.separator + "task_info.xml";

    private final static Pattern COMMENT_PATTERN = Pattern.compile("\\<!--.*--\\>");

    // TFE, 20201216: speed up searching in long notes
    private final static Pattern TASK_PATTERN = Pattern.compile(TaskData.ANY_BOXES, Pattern.LITERAL);
    
    public static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    
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
        final Set<Note> taskNotes = OwnNoteFileManager.getInstance().getNotesWithText(TaskData.ANY_BOXES);
        
        for (Note note : taskNotes) {
            final String noteContent = OwnNoteFileManager.getInstance().readNote(note, false).getNoteFileContent();

            taskList.addAll(tasksFromNote(note, noteContent));
        }
    }
    
    // noteContent as separate parm since it could be called from change within the editor before save
    public List<TaskData> tasksFromNote(final Note note, final String noteContent) {
//        System.out.println("tasksFromNote started: " + Instant.now());
        final List<TaskData> result = new ArrayList<>();

        // iterate over all matches and create TaskData items
        final List<Integer> textPossssss = findAllOccurences(noteContent);
        for (int textPos : textPossssss) {
//            System.out.println("  task found: " + Instant.now());
            result.add(new TaskData(note, noteContent, textPos));
//            System.out.println("  task added: " + Instant.now());
        }
        
//        System.out.println("tasksFromNote completed: " + Instant.now());
        return result;
    }
    
    private List<Integer> findAllOccurences(final String text) {
        final List<Integer> result = new LinkedList<>();
        
        if (text.isEmpty()) {
            return result;
        }
        
        final Matcher matcher = TASK_PATTERN.matcher(text);
        while (matcher.find()) {
            result.add(matcher.start());
        }
        
//        int index = 0;
//        while (true) {
//            index = text.indexOf(tofind, index);
//            if (index != -1) {
//                result.add(index);
//                // works here under the assumption that we don't search for e.g. "AA" in "AAA"
//                index += tofind.length();
//            } else {
//                break;
//            }
//        }
        
        return result;
    }
    
    public ObservableList<TaskData> getTaskList() {
        if (taskList == null) {
            taskList = FXCollections.<TaskData>observableArrayList();
            // lazy loading
            initTaskList();
        }
        
        return taskList;
    }
    
    public void resetTaskList() {
        taskList = null;
    }
    
    public void saveTaskList() {
    }

    @Override
    public boolean processFileChange(WatchEvent.Kind<?> eventKind, Path filePath) {
        if (inFileChange) {
            return true;
        }

        final Note curNote = myEditor.getEditedNote();
        if (curNote != null && OwnNoteFileManager.getInstance().buildNoteName(curNote).equals(filePath.getFileName().toString())) {
            return true;
        }
        
        Platform.runLater(() -> {
            inFileChange = true;
            // only act for files not currently shown - that will come via FileContentChange...
            if (StandardWatchEventKinds.ENTRY_DELETE.equals(eventKind) || StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                // file with tasks deleted -> remove tasks from own list
                taskList.removeIf((t) -> {
                    return OwnNoteFileManager.getInstance().buildNoteName(t.getNote()).equals(filePath.getFileName().toString());
                });
            }
            if (StandardWatchEventKinds.ENTRY_CREATE.equals(eventKind) || StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                // new file -> scan for tasks and update own list (similar to #2)
                final Note note = OwnNoteFileManager.getInstance().getNote(filePath.getFileName().toString());
                // TFE, 20201027: make sure we don't try to work on temp files which have been deleted in the meantime...
                if (note != null) {
                    final String noteContent = OwnNoteFileManager.getInstance().readNote(note, false).getNoteFileContent();
                    taskList.addAll(tasksFromNote(note, noteContent));
                }
            }
            // modify is delete + add :-)

            inFileChange = false;
        });
        
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final Note note, final String oldContent, final String newContent) {
//        System.out.println("processFileContentChange: " + changeType + ", " + note.getNoteValueName() + ", " + oldContent + ", " + newContent + ".");
        if (inFileChange) {
            return true;
        }
        
//        System.out.println("processFileContentChange started: " + Instant.now());

        inFileChange = true;
        if (FileContentChangeType.CONTENT_CHANGED.equals(changeType)) {
            // rescan text for tasks and update tasklist accordingly
            final List<TaskData> newTasks = tasksFromNote(note, newContent);
//            System.out.println(" newTasks found: " + Instant.now());
            final List<TaskData> oldTasks = taskList.stream().filter((t) -> {
                return t.getNote().equals(note);
            }).collect(Collectors.toList());
//            System.out.println(" oldTasks found: " + Instant.now());
            
            // compare old a new to minimize change impact on observable list
            // 1: same description = only pos & selected might have changed
            // takes care of all changes before & after task
            for (TaskData newTask: new ArrayList<>(newTasks)) {
                // TFE, 20210120: lets use id if we find it :-)
                Optional<TaskData> oldnew = oldTasks.stream().filter((t) -> {
                    return t.getId().equals(newTask.getId());
                }).findFirst();

                // fallback: find by text
                if (oldnew.isEmpty()) {
                    oldnew = oldTasks.stream().filter((t) -> {
                        return t.getDescription().equals(newTask.getDescription());
                    }).findFirst();
                }
                
                if (oldnew.isPresent()) {
                    oldnew.get().setTextPos(newTask.getTextPos());
                    oldnew.get().setCompleted(newTask.isCompleted());
                    
                    // nothing more to be done here
                    newTasks.remove(newTask);
                    oldTasks.remove(oldnew.get());
                }
            }
//            System.out.println(" same description checked: " + Instant.now());
            
            // 2. same position but different description = description & selected might have changed
            // takes care of all changes inside task
            for (TaskData newTask: new ArrayList<>(newTasks)) {
                Optional<TaskData> oldnew = oldTasks.stream().filter((t) -> {
                    return (t.getId().equals(newTask.getId()) || t.getTextPos() == newTask.getTextPos());
                }).findFirst();
                
                if (oldnew.isPresent()) {
                    oldnew.get().setCompleted(newTask.isCompleted());
                    // TFE, 20210119: we also have raw text now as well!
                    oldnew.get().setRawText(newTask.getRawText());
                    // set escapedText and description as well
                    oldnew.get().setHtmlText(newTask.getHtmlText());
                    
                    // nothing more to be done here
                    newTasks.remove(newTask);
                    oldTasks.remove(oldnew.get());
                }
            }
//            System.out.println(" same position checked: " + Instant.now());
            
            // 3. what is left? add & delete of tasks
            taskList.removeAll(oldTasks);
            taskList.addAll(newTasks);
        } else {
            // TFE, 20210122: use the content change way to send this back
            // two complex logic implementations are too much to control & test
            
//            // checkbox might not be start of innerHtml, whereas TaskData description is only the part after checkbox...
//            // FFFFUUUUCCCCKKKK innerHtml sends back <input type="checkbox" checked="checked"> instead of <input type="checkbox" checked="checked" />
//            // TFE, 20201103: better safe than sorry and check for both variants of valid html
//            String oldHtmlText = null;
//            int checkIndex = oldContent.indexOf(TaskData.UNCHECKED_BOXES_2);
//            if (checkIndex > -1) {
//                oldHtmlText = oldContent.substring(checkIndex + TaskData.UNCHECKED_BOXES_2.length());
//            } else {
//                checkIndex = oldContent.indexOf(TaskData.UNCHECKED_BOXES_1);
//                if (checkIndex > -1) {
//                    oldHtmlText = oldContent.substring(checkIndex + TaskData.UNCHECKED_BOXES_1.length());
//                }
//            }
//            if (oldHtmlText == null) {
//                checkIndex = oldContent.indexOf(TaskData.CHECKED_BOXES_2);
//                if (checkIndex > -1) {
//                    oldHtmlText = oldContent.substring(checkIndex + TaskData.CHECKED_BOXES_2.length());
//                } else {
//                    checkIndex = oldContent.indexOf(TaskData.CHECKED_BOXES_1);
//                    if (checkIndex > -1) {
//                        oldHtmlText = oldContent.substring(checkIndex + TaskData.CHECKED_BOXES_1.length());
//                    }
//                }
//            }
//            if (oldHtmlText == null) {
//                System.err.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
//                return true;
//            }
//
//            TaskData changedTask = null;
//            // TFE, 20210120: lets use id if we find it :-)
//            if (CommentDataMapper.containsCommentWithData(oldHtmlText)) {
//                final TaskData oldTask = new TaskData();
//                CommentDataMapper.getInstance().fromComment(oldTask, oldHtmlText);
//                
//                for (TaskData task : taskList) {
//                    if (task.getNote().equals(note) && task.getId().equals(oldTask.getId())) {
//                        changedTask = task;
//                        break;
//                    }
//                }
//            }
//            
//            // fallback: find by text
//            if (changedTask == null) {
//                // TFE, 20201216: this must be done in sync with TaskData changes to make sure text match works
//                oldHtmlText = OwnNoteHTMLEditor.stripHtmlTags(oldHtmlText);
//
//                for (TaskData task : taskList) {
//                    if (task.getNote().equals(note) && task.getEscapedText().equals(oldHtmlText)) {
//                        changedTask = task;
//                        break;
//                    }
//                }
//            }
//
//            Boolean newCompleted = null;
//            if (newContent.contains(TaskData.UNCHECKED_BOXES_2)) {
//                newCompleted = false;
//            } else if (newContent.contains(TaskData.CHECKED_BOXES_2)) {
//                newCompleted = true;
//            }
//            if (newCompleted == null) {
//                System.err.println("Something went wrong with task completion change!" + note + ", " + oldContent + ", " + newContent);
//                return true;
//            }
//
//            if (changedTask != null) {
//                // checkbox must have changed
//                if (changedTask.isCompleted() && !newCompleted) {
//                    changedTask.setCompleted(false);
//                } else if (!changedTask.isCompleted() && newCompleted) {
//                    changedTask.setCompleted(true);
//                }
//                // TFE, 20210119: we also have raw text now as well!
//                changedTask.setRawText(newContent);
//            }
            System.err.println("We shouldn't have ended up here! " + note + ", " + oldContent + ", " + newContent);
        }
            
        inFileChange = false;

//        System.out.println("processFileContentChange ended: " + Instant.now());
        
        return true;
    }
    
    public void processTaskCompletedChanged(final TaskData task) {
        if (inFileChange) {
            return;
        }
        
        inFileChange = true;

//        System.out.println("processTaskCompletedChanged for: " + task);
        myEditor.selectNoteAndToggleCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId(), task.isCompleted());

        inFileChange = false;
    }
    
    public boolean processTaskStatusChanged(final TaskData task, final TaskData.TaskStatus newStatus, final boolean selectNote) {
        boolean result = true;

        // things are more tricky if task completed changes with status...
        final boolean completedChanged = (task.isCompleted() != newStatus.isCompleted());
        
        // if we should select the note OR if its anyways the one shown in the editor
        if (selectNote || task.getNote().equals(myEditor.getEditedNote())) {
            if (completedChanged) {
                // below changes the status implicetly - but wrong :-) e.g. Done -> Open independent of drag position on kanban board
                myEditor.selectNoteAndToggleCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId(), newStatus.isCompleted());
            } else {
                myEditor.selectNoteAndCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId());
            }
            task.setTaskStatus(newStatus);
        } else {
            // handling of read & save note / select note
            OwnNoteFileManager.getInstance().readNote(task.getNote(), false);

            if (completedChanged) {
                // tricky, we need to change note text content as well
                String content = task.getNote().getNoteFileContent();
                final String noteTaskText = content.substring(task.getTextPos(), task.getTextPos() + task.getRawText().length());
                String newNoteTaskText;
                if (task.isCompleted()) {
                    newNoteTaskText = noteTaskText.replace(TaskData.UNCHECKED_BOXES_1, TaskData.CHECKED_BOXES_2).replace(TaskData.UNCHECKED_BOXES_2, TaskData.CHECKED_BOXES_2);
                } else {
                    newNoteTaskText = noteTaskText.replace(TaskData.CHECKED_BOXES_1, TaskData.UNCHECKED_BOXES_2).replace(TaskData.CHECKED_BOXES_2, TaskData.UNCHECKED_BOXES_2);
                }
                
                content = content.substring(0, task.getTextPos()-1) + newNoteTaskText + content.substring(task.getTextPos() + task.getRawText().length());
                
                // update all tasks in file since positions will have changed
                processFileContentChange(FileContentChangeType.CONTENT_CHANGED, task.getNote(), task.getNote().getNoteFileContent(), content);

                // set back content - also to editor content for next editing of note
                task.getNote().setNoteFileContent(content);
                if (task.getNote().getNoteEditorContent() != null) {
                    task.getNote().setNoteEditorContent(content);
                }
            }
            task.setTaskStatus(newStatus);

            result = OwnNoteFileManager.getInstance().saveNote(task.getNote());
        }
        
        return result;
    }
    
    public boolean archiveCompletedTasks(final Set<TaskData> tasks) {
//        System.out.println("archiveCompletedTasks");
        boolean result = true;
        
        // iterate over notes to avoid multiple saveNote() calls
        final Set<Note> notes = tasks.stream().map((t) -> {
            return t.getNote();
        }). distinct().collect(Collectors.toSet());
        
        // replace all checkboxes with \u2611 - as is done in html editor for current node
        for (Note note : notes) {
            // sort in reverse textpos order since we're modifying the content
            final Set<TaskData> noteTasks = tasks.stream().filter((t) -> {
                return note.equals(t.getNote());
            }).sorted((o1, o2) -> {
                return -Integer.compare(o1.getTextPos(), o2.getTextPos());
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            
            if (note.equals(myEditor.getEditedNote())) {
                // the currently edited note - let htmleditor do the work
                // this changes all instances without further checking!
                myEditor.replaceCheckedBoxes();
            } else {
                OwnNoteFileManager.getInstance().readNote(note, false);
                String content = note.getNoteFileContent();
                
                
                // TFE, 20210118: lets changed all like for the edited note
                // ignore passed list of tasks for the moment
                // if this feature is ever needed see code below for individual changes
                content = replaceCheckedBoxes(content);

                // iterate over tasks and change each individually - maybe needed in the future
//                for (TaskData task: noteTasks) {
//                    final String noteTaskText = content.substring(task.getTextPos(), task.getTextPos() + task.getRawText().length());
//                    final String newNoteTaskText = noteTaskText.replace(TaskData.CHECKED_BOXES_1, TaskData.ARCHIVED_BOX).replace(TaskData.CHECKED_BOXES_2, TaskData.ARCHIVED_BOX);
////                    System.out.println("  noteTaskText: \"" + noteTaskText + "\"");
////                    System.out.println("  newNoteTaskText: \"" + newNoteTaskText + "\"");
//
//                    content = content.substring(0, task.getTextPos()) + newNoteTaskText + content.substring(task.getTextPos() + task.getRawText().length());
//                }
                
                // update all tasks in file since positions will have changed - this also removes the tasks from the list
                processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, note.getNoteFileContent(), content);

                // set back content - also to editor content for next editing of note
                note.setNoteFileContent(content);
                if (note.getNoteEditorContent() != null) {
                    note.setNoteEditorContent(content);
                }

                if (!OwnNoteFileManager.getInstance().saveNote(note)) {
                    result = false;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public boolean inFileChange() {
        return inFileChange;
    }
    
    public TaskCount getTaskCount(final Note note) {
        // first all tasks for this note
        final List<TaskData> noteTasks = getTaskList().stream().filter((t) -> {
            return t.getNote().equals(note);
        }).collect(Collectors.toList());
        
        final long closedTasks = noteTasks.stream().filter((t) -> {
            return t.isCompleted();
        }).count();
        
        return new TaskCount(noteTasks.size() - closedTasks, closedTasks);
    }
    
    public TaskData taskForId(final String taskId) {
        final Optional<TaskData> result = getTaskList().stream().filter((t) -> {
            return t.getId().equals(taskId);
        }).findFirst();
        
        if (result.isPresent()) {
            return result.get();
        } else {
            return null;
        }
    }
    
    public List<TaskData> tasksForNote(final Note note) {
        return getTaskList().stream().filter((t) -> {
            return t.getNote().equals(note);
        }).collect(Collectors.toList());
    }
    
    public void setTaskDataInNote(final Note note) {
//        System.out.println("setTaskDataInNote");

        String content = "";
        content = note.getNoteEditorContent();
        if (content == null) {
            content = note.getNoteFileContent();
        }

        // need to go through tasks from end to start of note - otherwise textpos gets messed up...
        final List<TaskData> tasks = tasksForNote(note).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        Collections.reverse(tasks);
        
        for (TaskData task : tasks) {
            // replace text after checkbox and insert/replace taskid
            final String newHtmlComment = task.toHtmlComment();
            // find text including checkbox - might be empty after the fact...
            int startOfTaskText = content.indexOf(task.getRawText(), task.getTextPos());
            if (startOfTaskText > -1) {
                // now shift to after the checkbox
                startOfTaskText += (task.getRawText().length() - task.getHtmlText().length());
                final String noteTaskText = content.substring(startOfTaskText, startOfTaskText + task.getHtmlText().length());
//                System.out.println("  noteTaskText: \"" + noteTaskText + "\"");

                if (!noteTaskText.startsWith(newHtmlComment)) {
                    // id not found or id changed!
                    final String newNoteTaskText = newHtmlComment + COMMENT_PATTERN.matcher(noteTaskText).replaceAll("");
//                    System.out.println("  newNoteTaskText: \"" + newNoteTaskText + "\"");

    //                System.out.println("new note text: " + newNoteTaskText);
                    content = content.substring(0, startOfTaskText) + newNoteTaskText + content.substring(startOfTaskText + noteTaskText.length());
                }
            } else {
                System.out.println("Task with text \"" + task.getRawText() + "\" starting @" + task.getTextPos() + " no longer found in \"" + content + "\"!");
            }
        }
        
        // set back to the value we have read above
        if (note.getNoteEditorContent() != null) {
            note.setNoteEditorContent(content);
        } else {
            note.setNoteFileContent(content);
        }
    }

    public static String replaceCheckedBoxes(final String content) {
        return content.replace(TaskData.CHECKED_BOXES_1, TaskData.ARCHIVED_BOX).replace(TaskData.CHECKED_BOXES_2, TaskData.ARCHIVED_BOX);
    }
    
    public static String replaceCheckmarks(final String content) {
        return content.replace(TaskData.ARCHIVED_BOX, TaskData.CHECKED_BOXES_2);
    }
}
