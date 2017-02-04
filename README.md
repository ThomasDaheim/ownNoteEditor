# ownNoteEditor
JavaFX editor for the ownNote files locally stored in ownCloud folder

Aim was to mimic the behaviour of the ownNote web client using JavaFX and working directly on the owNote files in a local ownCloud directory. This code does NOT update any of the SQL tables used by owNote - but so far this doesn't seem to impact anything.

V 2.1: Added TestFX test harness

* Complete test harnbess for the "oneNote" look & feel using TestFX. NOTE: Currently, there is an issue with the tests under NetBeans since under gradle 
the editor isn't started in the foreground and therefore keys & mouse events from the TestFX tests aren't sent to the correct window.
However, under maven it works without issues...
* Now showing number of notes as part of the tab header
* Entries in the notes ist colored as their corresponding tab. So when you select the "All" group you'll get a very colorful notes list...


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
