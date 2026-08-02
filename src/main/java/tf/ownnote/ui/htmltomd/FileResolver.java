/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.htmltomd;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileResolver {

    public static List<Path> resolveFiles(String... patterns) {
        List<Path> resolvedFiles = new ArrayList<>();
        if (patterns == null) {
            return resolvedFiles;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pattern.contains("*") || pattern.contains("?")) {
                int lastSep = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
                Path parent = (lastSep != -1) ? Paths.get(pattern.substring(0, lastSep)) : Paths.get(".");
                String globPattern = (lastSep != -1) ? pattern.substring(lastSep + 1) : pattern;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, globPattern)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            resolvedFiles.add(entry);
                        }
                    }
                } catch (IOException ex) {
                    // Ignoriere Fehler beim Verzeichniszugriff
                }
            } else {
                resolvedFiles.add(Paths.get(pattern));
            }
        }
        return resolvedFiles;
    }
}
