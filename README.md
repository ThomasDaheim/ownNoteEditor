# ownNoteEditor
JavaFX editor for the ownNote files locally stored in ownCloud folder

Aim was to mimic the behaviour of the ownNote web client using JavaFX and working directly on the owNote files in a local ownCloud directory. This code does NOT update any of the SQL tables used by owNote - but so far this doesn't seem to impact anything.

Note on Java 11: After various tweaks to build.gradle this now also runs under Java 11. See e.g. https://github.com/kelemen/netbeans-gradle-project/issues/403 an some of the discussion that where required to get there... Unfortunately, there is one isseu with the TestFX framework when trying to drag & drop notes. Since I wasn't able to fix this I had to disable the "testDragNote()" test step.

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
--lookAndFeel="classic" or "oneNote"
```

Both things can also be changed using the UI.

## create a jar file or a distributable tree on Linux or Windows

```
./gradlew installDist
```

The tree will be in `build/install`.

## create a Mac OS X .app bundle

```
./gradlew createApp
```

The resulting application will be in the directory `build/macApp`.

## create a Mac OS X .dmg file (will only work on Mac OS X)

```
./gradlew createDmg
```

The resulting file will be in the directory `build/distributions`.


## Dependencies

Of course, such a project depends on the results of many others! I've tried to add comments with links to stackoverflow, ... wherever I have re-used the ideas and code of others. In case I have forgotten someone: that was only by accident/incompetency but never intentionally. I'm grateful for anyone that provides his/her results for public use!

Explicit dependencies:

* 'org.slf4j:slf4j-api:1.7.12'
* 'commons-cli:commons-cli:1.3.1'
* 'commons-io:commons-io:2.4'
* 'org.apache.commons:commons-lang3:3.5'
* 'org.testfx:testfx-junit:4.0.+'
* 'org.testfx:testfx-core:4.0.+'
* 'org.junit.jupiter:junit-jupiter-api:5.3.1'
