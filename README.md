# ownNoteEditor
JavaFX editor for the ownNote files locally stored in ownCloud/nextCloud folder.

Aim was to mimic the behaviour of the former ownNote web client using JavaFX and working directly on the ownNote files in a local directory. This code does NOT update any of the SQL tables used by ownNote.

* Note on v6.0: After the "classic" look & feel from ownNotes has been removed it might actually be time to rename the whole project to something like "htmlNoteEditor". But we have enough changes for one major release...
* Note on v5.0: A lot has happened since the initial version of the editor. ownNotes / nextNotes project for NextCloud seems to have died and there is no need anymore to try to mimic their behaviour and notes / groups handling. Therefore, its time for some re-design :-)
* Note on Java 11: After various tweaks to build.gradle this now also runs under Java 11. See e.g. https://github.com/kelemen/netbeans-gradle-project/issues/403 an some of the discussion that where required to get there... Unfortunately, there is one issue with the TestFX framework when trying to drag & drop notes. Since I wasn't able to fix this I had to disable the "testDragNote()" test step.

V 6.2

* Links to other notes
* Internal clean-up of FXML structure

V 6.1

* Upgraded tinymce to 5.10.1
* "Archiving" of notes: moving to the same group name but under new special group "Archive"
* Paste as Text
* Various updates and fixes to deal with hierarchical group names (as required for Archiving)
* Various bugfixes

V 6.0: Getting rid of old stuff

* Removed "classic" look & feel
* Changed all group handling to use tags
* Show icons for groups in tag tree and tab views, highlight based on distance to due date
* Show note count for tag tree view
* Tasks can have tags
* Editor to change icon and color for tags
* Calendar for KANBAN board incl. drag & drop to set due dates
* Bugfixes, small features: store recent note per group, insert link to attachments in notes, ...

Important: Once in a while I can see log messages that indicate mess-ups when changing task status in text & tasklist. But so far the note text has never been mixed up...

V 5.2: Fixes

* Mark tasks for current note in **bold** in tasklist
* Bugfix changes in TaskBoard & TaskCard not updated in all places
* Bugfix scrollbars in note editor
* Bugfix showing new attachment multiple timestamp
* Bugfix no color for new group tags

V 5.1: KANBAN is here

* Compress task data in note if too long
* Show completed tasks as strikethrough
* Taskdata editor
* KANBAN board for tasks with basic functions (drag & drop, archive / restore)
* Verification of note content to find any messups from the code
* Show unsaved changes as italic note name
* Bugfixes & performance updates

V 5.0: Styling up for Christmas...

* A new look & feel using tags: TagTree! Instead of having groups as tabs above we now have the complete tag tree to the left to select notes based on any tags
* Groups are a special form of tags - since they also show up in the filename. (Maybe that'll change in the future - once I have gained some more experience with the new look)
* Add tags via drag & drop to tag tree
* Option "Archive" closed tasks by replacing the checkbox with a symbol - to speed up things once a lot of tasks have been used
* Option to compress images to reduce note file size
* As usual: various bugfixes and speed improvments
* NOTE: As part of the refactoring the name of the look & feel has changed - that doesn't act well with stored preferences. So first time things will default to "classic" ownNote look & feel

V 4.8: Tags!

* Rudimentary tags support: tags can be added to notes and are stored as html-comments in the note; initial support to add & maintain tags; per day on author & timestamp is remembered
* Switch to JMetro theme
* Remember last edited not and load on start
* Improve opening in previous position on multi-monitors
* Various bugfixes incl. for test classes

V 4.7: Tasks!

* Task support: checkboxes = tasks, are listed in separat view to the right, can be checked / unchecked / selected from there
* Bugfix paste image from clipboard

V 4.6: Fixes

* Search for unchecked items
* PASTE always uses context of system clipboard
* Cntrl+C now defaults to copy as plain text
* Cntrl+C works in code view window
* Support for English & German OS when replacing menu entries

V 4.5: Check names

* Ensure that note & group names are valid filenames under WINDOWS & UNIX
* Added option to copy selection as plain text
* Allowed changing of note and group names CaSe only

V 4.4: Updated TinyMCE

* Updated TinyMCE to 4.9.1
* Fixed links in help dialog

V 4.3: Find in files

* Added option to find in files instead of note names
* Added test case for filtering

V 4.2: Java11

* Changes for Java11

V 4.1: Filter notes

* Added a filter for note names
* Added About menu

V 4.0: Lets go TinyMCE

* Switched from prism.js to TinyMCE as notes editor (thanks to a lot of experience collected while working on GPXEditor...)
* Synched toolbar items with nextnote
* added drag & drop support for text files and images

Note on Java 10: Minor updates where made to make this run under Java 10. In OwnNoteHTMLEditor a non-exported API from com.sun.javafx.scene.control.ContextMenuContent is used to delete unwanted ContextMenu entries like "Reload page". To make this work under Java 10 as well the trick from https://stackoverflow.com/a/47265102 had to be used in the compile options.


V 3.1: Minor changes

* Using FontAwesome for button images


V 3.0: All You Ever Wanted

* Switched from JavaFX HTMLEditor to prism.js as notes editor; add/remove html code during load and save of notes
* Enable features from prism.js like syntax coloring, ...
* Insert links, code, images, checkboxes
* View source code


V 2.1: Added TestFX test harness

* Complete test harness for the "oneNote" look & feel using TestFX
* Now showing number of notes as part of the tab header
* Entries in the notes list colored as their corresponding tab. So when you select the "All" group you'll get a very colorful notes list...


V 2.0: Various improvements have been added:

* New option to change look & feel to something similar to OneNote, incl. drag & drop of notes, reordering group tabs, ...
* The ownCloud directory is monitored for changes and group & notes lists are updated in case of changes
* Bugfixes

See https://github.com/Fmstrat/ownnote for ownNote.

DISCLAIMER: This has been tested randomly with my ownNote files. Use at your own risk!

## run & try

Make sure you have Java 8 SDK installed.

You can try to run and use this application by

* cloning this repo to you harddisk
* go to the "ownNoteEditor" subdirectory
* type `./gradlew run`.

## Parameters

If you want to, you can reference your locally synced notes dir directly with a parameter like

```
--ownCloudDir="C:\owncloudpath\Notes"
```

To change to look and feel you can use

```
--lookAndFeel="classic" / "groupTabs" / "tagTree"
```

Both things can also be changed using the UI.

## create a jar file or a distributable tree on Linux or Windows

```
./gradlew installDist
```

The tree will be in `build/install`.

## create a Mac OS X .app bundle

NOTE: Not sure if this still works...

```
./gradlew createApp
```

The resulting application will be in the directory `build/macApp`.

## create a Mac OS X .dmg file (will only work on Mac OS X)

NOTE: Not sure if this still works...

```
./gradlew createDmg
```

The resulting file will be in the directory `build/distributions`.


## Dependencies

Of course, such a project depends on the results of many others! I've tried to add comments with links to stackoverflow, ... wherever I have re-used the ideas and code of others. In case I have forgotten someone: that was only by accident/incompetency but never intentionally. I'm grateful for anyone that provides his/her results for public use!

Explicit dependencies:

* tf.JavaHelper:JavaHelper:1.15 https://github.com/ThomasDaheim/JavaHelper, not available via maven <- any help appreciated on how to best include as sub/meta/... repository

* 'commons-cli:commons-cli:1.5.0'
* 'commons-io:commons-io:2.11.0'
* 'org.apache.commons:commons-lang3:3.12.0'
* 'org.apache.commons:commons-text:1.10.0'
* 'commons-codec:commons-codec:1.15'
* 'com.thoughtworks.xstream:xstream:1.4.19'
* 'org.unbescape:unbescape:1.1.6.RELEASE'
* 'org.jfxtras:jfxtras-controls:1.4.19'
* 'org.jfxtras:jmetro:11.6.16'
* 'org.controlsfx:1.4.19'
* 'de.jensd:fontawesomefx:8.9'

* 'org.testfx:testfx-junit:4.0.+'
* 'org.testfx:testfx-core:4.0.+'
* 'org.junit.jupiter:junit-jupiter-api:5.9.1'
* 'com.github.stefanbirkner:system-lambda:3.2.1'