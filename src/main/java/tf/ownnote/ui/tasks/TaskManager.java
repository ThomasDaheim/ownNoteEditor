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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import tf.helper.javafx.calendarview.CalendarView;
import tf.ownnote.ui.helper.FileContentChangeType;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IFileContentChangeSubscriber;
import tf.ownnote.ui.helper.OwnNoteFileManager;
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
    
    public static final PseudoClass TASK_COMPLETED = PseudoClass.getPseudoClass("completed");

    // TFE, 20210511: color task data based on distance to due date - if any
    public static final PseudoClass TASK_OVERDUE = PseudoClass.getPseudoClass("overdue");
    public static final PseudoClass TASK_UPCOMING = PseudoClass.getPseudoClass("upcoming");
    public static final PseudoClass TASK_LONGTIME = PseudoClass.getPseudoClass("longtime");
    public static final PseudoClass TASK_ANYTIME = PseudoClass.getPseudoClass("anytime");

    // TFE, 20210527: similar is needed for events as well
    public static final CalendarView.DateStyle EVENT_OVERDUE = CalendarView.DateStyle.STYLE_1;
    public static final CalendarView.DateStyle EVENT_UPCOMING = CalendarView.DateStyle.STYLE_2;
    public static final CalendarView.DateStyle EVENT_LONGTIME = CalendarView.DateStyle.STYLE_3;
    public static final CalendarView.DateStyle EVENT_ANYTIME = CalendarView.DateStyle.STYLE_4;
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 
    
    // callback to OwnNoteEditor
    private OwnNoteEditor myEditor;
    
    // TFE, 20210527: use property exatractors for all properties
    private final ObservableList<TaskData> taskList = 
            FXCollections.<TaskData>observableArrayList(p -> new Observable[]{
                p.descriptionProperty(), 
                p.dueDateProperty(), 
                p.isCompletedProperty(),
                p.getTags(),
                p.taskPriorityProperty(), 
                p.taskStatusProperty()});
    private boolean taskListInitialized = false;
    
    private boolean inFileChange = false;
    private boolean inStatusChange = false;
    
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
            initNoteTasks(note);
        }
    }
    
    private void initNoteTasks(final Note note) {
        final String noteContent = OwnNoteFileManager.getInstance().readNote(note, false).getNoteFileContent();

        final Set<TaskData> tasks = tasksForNoteAndContent(note, noteContent);
        note.getMetaData().setTasks(tasks);
        taskList.addAll(tasks);
    }
    
    // noteContent as separate parm since it could be called from change within the editor before save
    protected Set<TaskData> tasksForNoteAndContent(final Note note, final String noteContent) {
//        System.out.println("tasksFromNote started: " + Instant.now());
        final Set<TaskData> result = new HashSet<>();

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
        if (!taskListInitialized) {
            // lazy loading
            initTaskList();
            
            taskListInitialized = true;
        }
        
        return taskList;
    }
    
    public void resetTaskList() {
        taskList.clear();
        taskListInitialized = false;
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
                    initNoteTasks(note);
                }
            }
            // modify is delete + add :-)

            inFileChange = false;
        });
        
        return true;
    }

    @Override
    public boolean processFileContentChange(final FileContentChangeType changeType, final Note note, final String oldContent, final String newContent) {
//        System.out.println("processFileContentChange: " + changeType + ", " + note.getNoteName()+ ", \n\"" + oldContent + "\n\", \n\"" + newContent + "\".");
        if (inFileChange) {
            return true;
        }
        
//        System.out.println("processFileContentChange started: " + Instant.now());

        inFileChange = true;
        if (FileContentChangeType.CONTENT_CHANGED.equals(changeType)) {
            // rescan text for tasks and update tasklist accordingly
            final Set<TaskData> newTasks = tasksForNoteAndContent(note, newContent);
//            System.out.println(" newTasks found: " + Instant.now());
//            for (TaskData newTask: new ArrayList<>(newTasks)) {
//                System.out.println("newTask: " + newTask.getId() + ", " + newTask.getEventDescription());
//            }
            final Set<TaskData> oldTasks = tasksForNote(note);
//            System.out.println(" oldTasks found: " + Instant.now());
//            for (TaskData oldTask: new ArrayList<>(oldTasks)) {
//                System.out.println("oldTask: " + oldTask.getId() + ", " + oldTask.getEventDescription());
//            }
            
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
                    final TaskData oldTask = oldnew.get();
                    oldTask.setTextPos(newTask.getTextPos());
                    oldTask.setCompleted(newTask.isCompleted());
                    // TFE, 20210119: we also have raw text now as well!
                    oldTask.setRawText(newTask.getRawText());
                    // set escapedText and description as well
                    oldTask.setHtmlText(newTask.getHtmlText());
                    
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
                    // TFE, 20220712: id already checked under #1
                    return (t.getTextPos() == newTask.getTextPos());
                }).findFirst();
                
                if (oldnew.isPresent()) {
                    final TaskData oldTask = oldnew.get();
                    oldTask.setCompleted(newTask.isCompleted());
                    // TFE, 20210119: we also have raw text now as well!
                    oldTask.setRawText(newTask.getRawText());
                    // set escapedText and description as well
                    oldTask.setHtmlText(newTask.getHtmlText());
                    
                    // nothing more to be done here
                    newTasks.remove(newTask);
                    oldTasks.remove(oldTask);
                }
            }
//            System.out.println(" same position checked: " + Instant.now());
            
            // 3. what is left? add & delete of tasks
            note.getMetaData().getTasks().removeAll(oldTasks);
            note.getMetaData().getTasks().addAll(newTasks);
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
        if (isProcessing()) {
            return;
        }
        
        inStatusChange = true;

//        System.out.println("processTaskCompletedChanged for: " + task);
        myEditor.selectNoteAndToggleCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId(), task.isCompleted());

        inStatusChange = false;
    }
    
    public boolean processTaskStatusChanged(final TaskData task, final TaskData.TaskStatus newStatus, final boolean selectNote, final boolean suppressMessages) {
        if (isProcessing()) {
            return true;
        }
        if (task.getTaskStatus().equals(newStatus)) {
            // nothing to do...
            return true;
        }

        boolean result = true;

        inStatusChange = true;
        // things are more tricky if task completed changes with status...
        final boolean completedChanged = (task.isCompleted() != newStatus.isCompleted());
        
        // if we should select the note OR if its anyways the one shown in the editor
        if (selectNote || task.getNote().equals(myEditor.getEditedNote())) {
            if (completedChanged) {
                // below changes the status implicitly - but wrong :-) e.g. Done -> Open independent of drag position on kanban board
                myEditor.selectNoteAndToggleCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId(), newStatus.isCompleted());
            } else {
                myEditor.selectNoteAndCheckBox(task.getNote(), task.getTextPos(), task.getDescription(), task.getId());
            }
            task.setTaskStatus(newStatus);
        } else {
            // handling of read & save note / select note
            // who could we end up if the note of the task hasn't been read???
//            OwnNoteFileManager.getInstance().readNote(task.getNote(), false);
            if (task.getNote().getNoteFileContent() == null) {
                System.err.println("Task status changed without note loaded! Task: " + task.getHtmlText() + ", Note: " + task.getNote().getNoteFileName());
                return false;
            }

            // tricky, we need to change note text content as well
            String content = task.getNote().getNoteFileContent();
            final String noteTaskText = content.substring(task.getTextPos(), task.getTextPos() + task.getRawText().length());
            final int noteTaskLength = task.getRawText().length();
            final String noteHtmlComment = task.toHtmlComment();

            task.setTaskStatus(newStatus);
            String newNoteTaskText = noteTaskText;
            if (completedChanged) {
                // replace with current checkbox
                if (task.isCompleted()) {
                    newNoteTaskText = newNoteTaskText.replace(TaskData.UNCHECKED_BOXES_1, TaskData.CHECKED_BOXES_2).replace(TaskData.UNCHECKED_BOXES_2, TaskData.CHECKED_BOXES_2);
                } else {
                    newNoteTaskText = newNoteTaskText.replace(TaskData.CHECKED_BOXES_1, TaskData.UNCHECKED_BOXES_2).replace(TaskData.CHECKED_BOXES_2, TaskData.UNCHECKED_BOXES_2);
                }
            }
            // html comment has changed
            newNoteTaskText = newNoteTaskText.replace(noteHtmlComment, task.toHtmlComment());

            content = content.substring(0, task.getTextPos()) + newNoteTaskText + content.substring(task.getTextPos() + noteTaskLength);

            // update all tasks in file since positions will have changed
            processFileContentChange(FileContentChangeType.CONTENT_CHANGED, task.getNote(), task.getNote().getNoteFileContent(), content);

            // set back content - also to editor content for next editing of note
            task.getNote().setNoteFileContent(content);
            if (task.getNote().getNoteEditorContent() != null) {
                task.getNote().setNoteEditorContent(content);
            }

            result = OwnNoteFileManager.getInstance().saveNote(task.getNote(), suppressMessages);
        }
        inStatusChange = false;
        
        return result;
    }
    
    public boolean processTaskDataChanged(final TaskData task, final TaskData.TaskPriority newPrio, final LocalDateTime newDate, final String newComment, final boolean suppressMessages) {
        if (isProcessing()) {
            return true;
        }
        
        boolean result = true;

        boolean hasChanged = false;
        if (!Objects.equals(task.getTaskPriority(), newPrio)) {
            hasChanged = true;
            task.setTaskPriority(newPrio);
        }
        if (!Objects.equals(task.getDueDate(), newDate)) {
            hasChanged = true;
            task.setDueDate(newDate);
        }
        if (!Objects.equals(task.getComment(), newComment)) {
            hasChanged = true;
            task.setComment(newComment);
        }
        
        if (hasChanged) {
            if (task.getNote().equals(myEditor.getEditedNote())) {
                // TFE, 20210512: and now the note has unsaved changes as well...
                task.getNote().setUnsavedChanges(true);
            } else {
                // save note content
                if (task.getNote().getNoteFileContent() == null) {
                    System.err.println("Task status changed without note loaded! Task: " + task.getHtmlText() + ", Note: " + task.getNote().getNoteFileName());
                    return false;
                }

                result = OwnNoteFileManager.getInstance().saveNote(task.getNote(), suppressMessages);
            }
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
        
        final String backupSuffix = "_archive_" + DATE_FORMAT.format(new Date());
        // replace all checkboxes with \u2611 - as is done in html editor for current node
        for (Note note : notes) {
            // do backup in cse of mass-updates
            OwnNoteFileManager.getInstance().backupNote(note, backupSuffix);
            
            // sort in reverse textpos order since we're modifying the content
//            final Set<TaskData> noteTasks = tasks.stream().filter((t) -> {
//                return note.equals(t.getNote());
//            }).sorted((o1, o2) -> {
//                return -Integer.compare(o1.getTextPos(), o2.getTextPos());
//            }).collect(Collectors.toCollection(LinkedHashSet::new));

            System.out.println("Archiving tasks in note: " + note.getNoteFileName());
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

                // suppress messages since we won't find all check boxes anymore
                if (!OwnNoteFileManager.getInstance().saveNote(note, true)) {
                    result = false;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public boolean restoreArchivedTasks() {
//        System.out.println("archiveCompletedTasks");
        boolean result = true;
        
        // iterate over notes to avoid multiple saveNote() calls
        final Set<Note> notes = OwnNoteFileManager.getInstance().getNotesWithText(TaskData.ARCHIVED_BOX);
        
        final String backupSuffix = "_restore_" + DATE_FORMAT.format(new Date());
        // replace all checkboxes with \u2611 - as is done in html editor for current node
        for (Note note : notes) {
            // do backup in case of mass-updates
            OwnNoteFileManager.getInstance().backupNote(note, backupSuffix);
            
            System.out.println("Restoring tasks in note: " + note.getNoteFileName());
            if (note.equals(myEditor.getEditedNote())) {
                // the currently edited note - let htmleditor do the work
                // this changes all instances without further checking!
                myEditor.replaceCheckmarks();
            } else {
                OwnNoteFileManager.getInstance().readNote(note, false);
                String content = note.getNoteFileContent();
                
                content = replaceCheckmarks(content);

                // update all tasks in file since positions will have changed - this also removes the tasks from the list
                processFileContentChange(FileContentChangeType.CONTENT_CHANGED, note, note.getNoteFileContent(), content);

                // set back content - also to editor content for next editing of note
                note.setNoteFileContent(content);
                if (note.getNoteEditorContent() != null) {
                    note.setNoteEditorContent(content);
                }

                // suppress messages since we won't find all check boxes anymore
                if (!OwnNoteFileManager.getInstance().saveNote(note, true)) {
                    result = false;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public boolean isProcessing() {
        return inFileChange || inStatusChange;
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
    
    public Set<TaskData> tasksForNote(final Note note) {
        return getTaskList().stream().filter((t) -> {
            return t.getNote().equals(note);
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    protected boolean resolveDuplicateTaskIds(final List<TaskData> tasks) {
        // find the duplicate ids first
        // https://mkyong.com/java8/java-8-find-duplicate-elements-in-a-stream/
        final Set<String> taskIDs = new HashSet<>();
        final Set<String> duplicates = tasks.stream()
                .filter(n -> !taskIDs.add(n.getId())).map((t) -> {
                    return t.getId();
                })
                .collect(Collectors.toSet());
        // and now find the tasks for the duplicate ids
        for (String duplicateId: duplicates) {
            // find all "other" tasks that have the same id except for the first one
            final Set<TaskData> duplicateTasks = tasks.stream().filter((t) -> {
                return t.getId().equals(duplicateId);
            }).skip(1).collect(Collectors.toSet());

            // we should have at least one match
            assert !duplicateTasks.isEmpty();
            for (TaskData duplicateTask: new ArrayList<>(duplicateTasks)) {
                // update task id
                duplicateTask.randomId();
            }
        }
        
        return true;
    }
    
    public void replaceTaskDataInNote(final Note note, final boolean suppressMessages) {
//        System.out.println("replaceTaskDataInNote: " + note.getNoteName());

        String content = "";
        content = note.getNoteEditorContent();
        if (content == null) {
            content = note.getNoteFileContent();
        }

        // need to go through tasks from end to start of note - otherwise textpos gets messed up...
        final List<TaskData> tasks = tasksForNoteAndContent(note, content).stream().sorted((o1, o2) -> {
            return Integer.compare(o1.getTextPos(), o2.getTextPos());
        }).collect(Collectors.toList());
        Collections.reverse(tasks);
        
        // TFE, 20220712: make sure we have unique task ids
        resolveDuplicateTaskIds(tasks);
        
        for (TaskData task : tasks) {
            // replace text after checkbox and insert/replace taskid
            final String newHtmlComment = task.toHtmlComment();
            // find text including checkbox - might be empty after the fact...
            int startOfTaskText = content.indexOf(task.getRawText());//content.indexOf(task.getRawText(), task.getTextPos());
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
                if (!suppressMessages) {
                    System.err.println("Task with text \"" + task.getRawText() + "\" starting @" + task.getTextPos() + " no longer found in \"" + content + "\"!");
                }
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
    
    public static void setPseudoClassForDueDate(final Node node, final TaskData task) {
        PseudoClass result;

        if (task.isCompleted()) {
            result = TASK_ANYTIME;
        } else {
            final LocalDateTime dueDate = task.getDueDate();
            if (dueDate == null) {
                result = TASK_ANYTIME;
            } else {
                final LocalDateTime now = LocalDateTime.now();
                final int dateDiff = Period.between(now.toLocalDate(), dueDate.toLocalDate()).getDays();
                // TODO: put logic & values into map and/or own class...
                if (dateDiff > 3) {
                    result = TASK_LONGTIME;
                } else if (dateDiff > 0) {
                    result = TASK_UPCOMING;
                } else {
                    result = TASK_OVERDUE;
                }
            }
        }

        node.pseudoClassStateChanged(result, true);
    }
    
    public static void resetPseudoClassForDueDate(final Node node) {
        node.pseudoClassStateChanged(TaskManager.TASK_OVERDUE, false);
        node.pseudoClassStateChanged(TaskManager.TASK_UPCOMING, false);
        node.pseudoClassStateChanged(TaskManager.TASK_LONGTIME, false);
        node.pseudoClassStateChanged(TaskManager.TASK_ANYTIME, false);
    }
    
    public static CalendarView.DateStyle getDateStyleForDueDate(final TaskData task) {
        CalendarView.DateStyle result;

        if (task.isCompleted()) {
            result = EVENT_ANYTIME;
        } else {
            final LocalDateTime dueDate = task.getDueDate();
            if (dueDate == null) {
                result = EVENT_ANYTIME;
            } else {
                final LocalDateTime now = LocalDateTime.now();
                final int dateDiff = Period.between(now.toLocalDate(), dueDate.toLocalDate()).getDays();
                // TODO: put logic & values into map and/or own class...
                if (dateDiff > 3) {
                    result = EVENT_LONGTIME;
                } else if (dateDiff > 0) {
                    result = EVENT_UPCOMING;
                } else {
                    result = EVENT_OVERDUE;
                }
            }
        }
        
        return result;
    }
}
