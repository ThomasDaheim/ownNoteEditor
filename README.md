# ownNoteEditor
JavaFX editor for the ownNote files locally stored in ownCloud folder

This is (so far) mainly a self-learn project to understand how JavaFX works. Uploaded here since it might be useful/helpful for others.

Aim was to mimic the behaviour of the ownNote web client using JavaFX and working directly on the owNote files in a local ownCloud directory. This code does NOT update any of the SQL tables used by owNote - but so far this doesn't seem to impact anything.

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
