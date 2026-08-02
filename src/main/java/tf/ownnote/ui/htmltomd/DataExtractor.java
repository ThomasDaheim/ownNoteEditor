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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class DataExtractor {
    private static final Inflater decompresser = new Inflater();
    private final static List<String> ignoreTags = Arrays.asList("charset", "appVersion", "versions", "tags");

    private final String name;
    private final String html;
    private String extractedGroup = "";
    private final Map<String, List<String>> dataMap;

    public DataExtractor(String name, String html) {
        this.name = name;
        this.html = html;
        this.dataMap = parseDataMap(extractAndDecodeData(this.html));
    }

    public String getName() {
        return name;
    }

    public String getExtractedGroup() {
        return extractedGroup;
    }

    public Map<String, List<String>> getDataMap() {
        return dataMap;
    }

    private Map<String, List<String>> parseDataMap(String data) {
        Map<String, List<String>> map = new HashMap<>();
        if (name != null) {
            int start = name.indexOf('[');
            int end = name.indexOf(']', start + 1);
            if (start != -1 && end > start) {
                String groupContent = name.substring(start + 1, end);
                map.put("Group", Arrays.asList(groupContent.replace("~", "/")));
                extractedGroup = groupContent.replace("~", "/");
            } else {
                map.put("Group", Arrays.asList(""));
            }
        }
        if (data != null && !data.isEmpty()) {
            String[] entries = data.split("---");
            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2);
                // nur die wichtigen Tags parsen
                if (keyValue.length == 2 && !ignoreTags.contains(keyValue[0])) {
                    String key = keyValue[0];
                    String value = keyValue[1].replace("~", "/");
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    List<String> values = new ArrayList<>();
                    if (value.contains(":::")) {
                        values.addAll(Arrays.asList(value.split(":::")));
                    } else {
                        values.add(value);
                    }
                    map.put(key, values);
                }
            }
        }
        return map;
    }

    private synchronized String extractAndDecodeData(String html) {
        if (html == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("<!--.*?data=\"([^\"]*)\".*?-->", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            String base64Data = matcher.group(1);
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

                decompresser.reset();
                decompresser.setInput(decodedBytes, 0, decodedBytes.length);

                final byte[] temp = new byte[32768];
                try {
                    final int resultLength = decompresser.inflate(temp);

                    final byte[] input = new byte[resultLength];
                    System.arraycopy(temp, 0, input, 0, resultLength);

                    return new String(input, StandardCharsets.UTF_8);
                } catch (DataFormatException ex) {
                    // Ignore invalid base64 data
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid base64 data
            }
        }
        return null;
    }
}
