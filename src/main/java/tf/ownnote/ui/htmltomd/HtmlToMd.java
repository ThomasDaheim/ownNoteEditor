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

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HtmlToMd {
    private String htmlFileName;

    public HtmlToMd() {
    }

    public HtmlToMd(String htmlFileName) {
        this.htmlFileName = htmlFileName;
    }

    public String getHtmlFileName() {
        return htmlFileName;
    }

    public void setHtmlFileName(String htmlFileName) {
        this.htmlFileName = htmlFileName;
    }

    public String readHtmlFile() throws IOException {
        if (htmlFileName == null) {
            throw new IllegalStateException("HTML file name is not set");
        }
        return new String(Files.readAllBytes(Paths.get(htmlFileName)), StandardCharsets.UTF_8);
    }

    public String convertToMarkdown(String html) {
        if (html == null) {
            return null;
        }

        String name = null;
        if (htmlFileName != null) {
            int lastSep = Math.max(htmlFileName.lastIndexOf('/'), htmlFileName.lastIndexOf('\\'));
            String fileName = (lastSep != -1) ? htmlFileName.substring(lastSep + 1) : htmlFileName;
            int lastDot = fileName.lastIndexOf('.');
            name = (lastDot != -1) ? fileName.substring(0, lastDot) : fileName;
        }

        DataExtractor dataExtractor = new DataExtractor(name, html);

        String htmlToConvert = html;
        if (!html.toLowerCase().contains("<body")) {
            htmlToConvert = "<body>" + html + "</body>";
        }
        MutableDataSet options = new MutableDataSet();
        options.set(FlexmarkHtmlConverter.OUTPUT_UNKNOWN_TAGS, false);
        options.set(FlexmarkHtmlConverter.BR_AS_EXTRA_BLANK_LINES, false);
        options.set(FlexmarkHtmlConverter.RENDER_COMMENTS, false);
        String markdown = FlexmarkHtmlConverter.builder(options).build().convert(htmlToConvert);
        markdown = LinkSanitizer.sanitizeMarkdownLinks(markdown);

        if (!dataExtractor.getDataMap().entrySet().isEmpty()) {
            StringBuilder sb = new StringBuilder("---");
            for (Map.Entry<String, List<String>> entry : dataExtractor.getDataMap().entrySet()) {
                sb.append("\n").append(entry.getKey()).append(": ");
                if (entry.getValue().size() == 1) {
                    sb.append(entry.getValue().get(0));
                } else {
                    // lets yaml!
                    sb.append(String.join("\n  - ", entry.getValue()));
                }
            }
            sb.append("\nTags: ").append(dataExtractor.getExtractedGroup());
            return sb.append("\n---\n").append(markdown).toString();
        }
        return markdown;
    }

    public String convertToMarkdown() throws IOException {
        return convertToMarkdown(readHtmlFile());
    }

    public String getMarkdownFileName() {
        if (htmlFileName == null) {
            throw new IllegalStateException("HTML file name is not set");
        }
        String fileName = htmlFileName.replaceAll("\\[[^\\]]*\\]", "");
        int lastDot = fileName.lastIndexOf('.');
        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastDot > lastSep) {
            return fileName.substring(0, lastDot) + ".md";
        }
        return fileName + ".md";
    }

    public void saveMarkdownFile(String markdown) throws IOException {
        String mdFileName = getMarkdownFileName();
        Files.write(Paths.get(mdFileName), markdown.getBytes(StandardCharsets.UTF_8));
    }

    public void saveMarkdownFile() throws IOException {
        saveMarkdownFile(convertToMarkdown());
    }

    public static List<Path> resolveFiles(String... patterns) {
        return FileResolver.resolveFiles(patterns);
    }

    public static void main(String[] args) {
        List<Path> filesToProcess = new ArrayList<>();
        if (args.length > 0 && !args[0].isBlank()) {
            filesToProcess = resolveFiles(args);
        } else {
            System.out.print("Bitte den Namen der HTML-Datei eingeben: ");
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if (!input.isBlank()) {
                    filesToProcess = resolveFiles(input);
                }
            }
        }

        System.out.println("Converting " + filesToProcess.size() + " files");
        for (Path file : filesToProcess) {
            System.out.println("  Converting " + file.toString());
            HtmlToMd converter = new HtmlToMd(file.toString());
            try {
                converter.saveMarkdownFile();
            } catch (IOException ex) {
                System.getLogger(HtmlToMd.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }
}
