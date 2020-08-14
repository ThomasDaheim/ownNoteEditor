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
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteDirectoryMonitor {
    private ThreadWatcher fileWatcher = null;
    private Thread watchThread = null;
    
    // linked list to maintain order of callbacks
    private List<IFileChangeSubscriber> changeSubscribers = new LinkedList<>();

    public OwnNoteDirectoryMonitor() {
        super();
    }

    public void subscribe(final IFileChangeSubscriber subscriber) {
        if (subscriber == null) {
            return;
        }

        changeSubscribers.add(subscriber);
    }
    
    public void unsubscribe(final IFileChangeSubscriber subscriber) {
        if (subscriber == null) {
            return;
        }
        
        changeSubscribers.remove(subscriber);
    }

    /**
     * Use to end watcher thread
     * If not, it will keep on running until forcefully terminated
     */
    public void stop() {
        // stop the old thread if running
        if (fileWatcher != null && watchThread != null) {
            // System.out.printf("Time %s: Starting to stop\n", myEditor.getCurrentTimeStamp());
            try {
                fileWatcher.disable();
                fileWatcher.terminate();
                watchThread.join();
            } catch (InterruptedException | ClosedWatchServiceException ex) {
                Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
            // System.out.printf("Time %s: Done stopping\n", myEditor.getCurrentTimeStamp());
            
            // System.out.printf("Time %s: Done stopping\n", myEditor.getCurrentTimeStamp());
        }
    }
    
    public void enableMonitor() {
        fileWatcher.enable();
    }
    
    public void disableMonitor() {
        fileWatcher.disable();
    }
    
    public void setDirectoryToMonitor(final String directory) {
        stop();
        
        fileWatcher = new ThreadWatcher(directory, changeSubscribers);
        fileWatcher.enable();
        watchThread = new Thread(fileWatcher, "FileWatcher");
        watchThread.start();
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
        private final List<IFileChangeSubscriber> changeSubscribers;

        /** the watchService that is passed in from above */
        private WatchService watcher = null;
        private Path watchPath = null; 
        private final String watchDir;
        private volatile boolean running = true;
        private volatile boolean enabled = true;

        public ThreadWatcher(final String directory, final List<IFileChangeSubscriber> subscribers) {
            watchDir = directory;
            changeSubscribers = subscribers;
        }
        public void terminate() {
            running = false;
        }
        public void enable() {
            // System.out.printf("Time %s: Starting to enable watcher\n", myEditor.getCurrentTimeStamp());
            disable();

            // initialize path and watcher if not already set
            if (watchPath == null || watcher == null) {
                try {
                    watchPath = Paths.get(watchDir);
                    
                    if (watcher != null) {
                        watcher.close();
                    }
                    watcher = watchPath.getFileSystem().newWatchService();

                    // register a file
                    watchPath.register(watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                } catch (IOException ex) {
                    Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            enabled = true;
            // System.out.printf("Time %s: Enabled watcher\n", myEditor.getCurrentTimeStamp());
        }
        public void disable() {
            // System.out.printf("Time %s: Starting to disable watcher\n", myEditor.getCurrentTimeStamp());
            enabled = false;
            
            // close watcher and unset path and watcher
            if (watchPath != null && watcher != null) {
                try {
                    // give some time to think about that :-)
                    Thread.sleep(10);

                    watcher.close();

                    watcher = null;
                    watchPath = null;
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }

                // System.out.printf("Time %s: Disabled watcher\n", myEditor.getCurrentTimeStamp());
            }
        }
 
        /**
         * In order to implement a file watcher, we loop forever 
         * ensuring requesting to take the next item from the file 
         * watchers queue.
         */
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            WatchKey key = null;
            while(running) {
                if (enabled) {
                    // fix for CPU-load issue - do nothing for a while
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    try {
                        // due to wait loop above the watcher might have been deleted due to notes directory change
                        if (watcher != null) {
                            key = watcher.poll();
                        }
                    } catch (ClosedWatchServiceException ex) {
                        // System.out.printf("Time %s: Exception\n", myEditor.getCurrentTimeStamp());
                        Logger.getLogger(OwnNoteDirectoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
                        key = null;
                    }
                    
                    if (key != null) {
                        // we have a polled event, now we traverse it and
                        // receive all the states from it
                        for (WatchEvent event : key.pollEvents()) {
                            // System.out.printf("Time %s: Received %s event for file: %s\n",
                            //          myEditor.getCurrentTimeStamp(), event.kind(), event.context() );

                            final WatchEvent.Kind<?> eventKind = event.kind();
                            if (eventKind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            final Path filePath = ((WatchEvent<Path>) event).context();

                            // calling all subscribers...
                            for (IFileChangeSubscriber subscriber : changeSubscribers) {
                                if (!subscriber.processFileChange(eventKind, filePath)) {
                                    break;
                                }
                            }
                        }
                        key.reset();
                    }
                }
            }
        }
    }
}
