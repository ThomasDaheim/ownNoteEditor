/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.helper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteDirectoryMonitor {

    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private ThreadWatcher fileWatcher = null;
    private Thread watchThread = null;

    public OwnNoteDirectoryMonitor(final OwnNoteEditor editor) {
        super();
        
        myEditor = editor;
    }
    
    /**
     * Use to end watcher thread
     * If not, it will keep on running until forecfully terminated
     */
    public void stop() {
        // stop the old thread if running
        if (fileWatcher != null && watchThread != null) {
            try {
                fileWatcher.terminate();
                watchThread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void setDirectoryToMonitor(final String directory) {
        try {
            stop();
            
            final Path noteDir = Paths.get(directory);
            final WatchService noteDirWatcher = noteDir.getFileSystem().newWatchService();
            
            // start the file watcher thread below
            fileWatcher = new ThreadWatcher(noteDirWatcher, myEditor);

            watchThread = new Thread(fileWatcher, "FileWatcher");
            watchThread.start();

            // register a file
            noteDir.register(noteDirWatcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException ex) {
            Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * This Runnable is used to constantly attempt to take from the watch 
     * queue, and will receive all events that are registered with the 
     * fileWatcher it is associated. In this sample for simplicity we 
     * just output the kind of event and name of the file affected to 
     * standard out.
     */
    private static class ThreadWatcher implements Runnable {
 
        // callback to OwnNoteEditor required for e.g. delete & rename
        private OwnNoteEditor myEditor= null;

        /** the watchService that is passed in from above */
        private WatchService myWatcher;
        private volatile boolean running = true;

        public ThreadWatcher(final WatchService watcher, final OwnNoteEditor editor) {
            myWatcher = watcher;
            
            myEditor = editor;
        }
        public void terminate() {
            running = false;
        }
 
        /**
         * In order to implement a file watcher, we loop forever 
         * ensuring requesting to take the next item from the file 
         * watchers queue.
         */
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            WatchKey key;
            while(running) {
                key = myWatcher.poll();
                if (key != null) {
                    // we have a polled event, now we traverse it and
                    // receive all the states from it
                    for (WatchEvent event : key.pollEvents()) {
                        // System.out.printf("Time %s: Received %s event for file: %s\n",
                        //         myEditor.getCurrentTimeStamp(), event.kind(), event.context() );

                        final WatchEvent.Kind<?> eventKind = event.kind();
                        if (eventKind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                        }
                        final Path filePath = ((WatchEvent<Path>) event).context();
                        
                        // let my editor handle this...
                        myEditor.processFileChange(eventKind, filePath);
                    }
                    key.reset();
                }
            }
        }
    }
}
