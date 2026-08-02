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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkSanitizer {

    public static String sanitizeMarkdownLinks(String markdown) {
        if (markdown == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("(!?\\[[^\\]]*\\]\\()([^)]+)(\\))");
        Matcher matcher = pattern.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String url = matcher.group(2).replace('\\', '/');

            if (url.startsWith("file:")) {
                if (!url.startsWith("file:///")) {
                    url = "file:///" + url.substring(5).replaceAll("^/+", "");
                }
            } else if (url.matches("^[a-zA-Z]:.*")) {
                url = "file:///" + url;
            } else if (url.matches("^/[a-zA-Z]:.*")) {
                url = "file:///" + url.substring(1);
            }

            String suffix = matcher.group(3);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + url + suffix));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
