/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.helper;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * Subscriber interface for file changes.
 * 
 * Option to break propagation chain by returning FALSE.
 * 
 * @author thomas
 */
public interface IFileChangeSubscriber {
    abstract boolean processFileChange(final WatchEvent.Kind<?> eventKind, final Path filePath);
}
