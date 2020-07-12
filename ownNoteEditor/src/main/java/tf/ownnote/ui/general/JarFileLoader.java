/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.ownnote.ui.general;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author Unterwegs
 */
public class JarFileLoader {
    /**
     * Based on 
     * https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/util/Classes.java
     * but extended to handle windows and IDE as well
     * 
     * @param klass To find jar file for
     * @return  Returns the jar file from which the given class is loaded; or null if no such jar file can be located.
     */
    public static JarFile jarFileOf(final Class<?> klass) {
        URL url = klass.getResource(
            "/" + klass.getName().replace('.', '/') + ".class");
        if (url == null)
            return null;
        
        String s = url.getFile();

        int beginIndex = s.indexOf("file:/") + "file:/".length();
        int endIndex = s.indexOf(".jar!");
        
        final boolean insideIDE = (endIndex == -1);
        
        String f = null;
        if (insideIDE) {
            // TFE, 20190823: we might be running in the editor... below works for netbeans under windows
            // so add some simple checks - not sure how this looks in any other possible IDE
            beginIndex = 1;
            endIndex = s.indexOf("build/") + "build/".length();
            if (endIndex == -1)
                return null;

            f = s.substring(beginIndex, endIndex);
            
            // determine jar name from path... its the part before the "build/"
            final String[] fElements = f.split("/");
            if (fElements.length < 2)
                return null;
            final String jarName = fElements[fElements.length-2];
            
            f += "libs/" + jarName + "*.jar";
        } else {
            endIndex += ".jar".length();
            f = s.substring(beginIndex, endIndex);
        }
        if (f == null) 
            return null;

        // TFE, 20190823: replace spaces in filename - at least under windows
        f = f.replaceAll("%20", "\\ ");
        
        File file = null;
        if (insideIDE) {
            // scan for first match - name of jar file can contain a version number!
            // get directory where to search
            final String jarDir = FilenameUtils.getFullPath(f);
            final String jarName = FilenameUtils.getName(f);
            
            final File directory = new File(jarDir);
            final Collection<File> jarFiles = FileUtils.listFiles(directory, new WildcardFileFilter(jarName), null);
            
            if (jarFiles.isEmpty()) {
                return null;
            } else {
                file = jarFiles.iterator().next();
            }
            
        } else {
            file = new File(f);
        }

        try {
            return file.getAbsoluteFile().exists() ? new JarFile(file) : null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }    
}
