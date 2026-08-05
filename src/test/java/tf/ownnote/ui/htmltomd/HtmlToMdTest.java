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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlToMdTest {

    @Test
    void testGetAndSetHtmlFileName() {
        HtmlToMd converter = new HtmlToMd("initial.html");
        assertEquals("initial.html", converter.getHtmlFileName());

        converter.setHtmlFileName("updated.html");
        assertEquals("updated.html", converter.getHtmlFileName());
    }

    @Test
    void testGetMarkdownFileName() {
        HtmlToMd converter = new HtmlToMd("test.html");
        assertEquals("test.md", converter.getMarkdownFileName());

        converter.setHtmlFileName("path/to/file.html");
        assertEquals("path/to/file.md", converter.getMarkdownFileName());

        converter.setHtmlFileName("fileWithoutExtension");
        assertEquals("fileWithoutExtension.md", converter.getMarkdownFileName());

        converter.setHtmlFileName("dir.name/file");
        assertEquals("dir.name/file.md", converter.getMarkdownFileName());

        converter.setHtmlFileName("[Test] test.html");
        assertEquals(" test.md", converter.getMarkdownFileName());
    }

    @Test
    void testGetMarkdownFileNameThrowsWhenNull() {
        HtmlToMd converter = new HtmlToMd();
        assertThrows(IllegalStateException.class, converter::getMarkdownFileName);
    }

    @Test
    void testConvertToMarkdown() {
        HtmlToMd converter = new HtmlToMd();
        assertNull(converter.convertToMarkdown(null));

        String html = "<h1>Title</h1><p>Hello world</p>";
        String markdown = converter.convertToMarkdown(html);
        assertNotNull(markdown);
        assertTrue(markdown.contains("Title"));
        assertTrue(markdown.contains("Hello world"));
    }

    @Test
    void testReadAndSaveFile(@TempDir Path tempDir) throws IOException {
        Path htmlFile = tempDir.resolve("sample.html");
        Files.writeString(htmlFile, "<h1>Test Header</h1>");

        HtmlToMd converter = new HtmlToMd(htmlFile.toString());
        String readContent = converter.readHtmlFile();
        assertEquals("<h1>Test Header</h1>", readContent);

        converter.saveMarkdownFile();

        Path mdFile = tempDir.resolve("sample.md");
        assertTrue(Files.exists(mdFile));
        String mdContent = Files.readString(mdFile);
        assertTrue(mdContent.contains("Test Header"));
    }

    @Test
    void testResolveFilesWithMultipleArgsAndWildcards(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("a1_b.html"));
        Files.createFile(tempDir.resolve("a2_b.html"));
        Files.createFile(tempDir.resolve("other.html"));

        List<Path> files = HtmlToMd.resolveFiles(
                tempDir.toAbsolutePath() + "/a*_b.html",
                tempDir.resolve("other.html").toString()
        );

        assertEquals(3, files.size());
    }

    @Test
    void testSanitizeMarkdownLinks() {
        HtmlToMd converter = new HtmlToMd();
        String html = "<a href=\"path\\to\\file.html\">Link</a><img src=\"images\\pic.png\" alt=\"Image\">";
        String markdown = converter.convertToMarkdown(html);
        assertNotNull(markdown);
        assertTrue(markdown.contains("[Link](path/to/file.html)"));
        assertTrue(markdown.contains("![Image](images/pic.png)"));
    }

    @Test
    void testSanitizeMarkdownLinksLocalFileSystem() {
        HtmlToMd converter = new HtmlToMd();
        String html = "<a href=\"C:\\path\\to\\file.html\">Local Link</a>";
        String markdown = converter.convertToMarkdown(html);
        assertNotNull(markdown);
        assertTrue(markdown.contains("[Local Link](file:///C:/path/to/file.html)"));
    }

    @Test
    void testConvertToMarkdownWithInlineImage() {
        HtmlToMd converter = new HtmlToMd();
        String html = "<p><img src=\"data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ==\" width=\"100\" height=\"100\"></p>";
        String markdown = converter.convertToMarkdown(html);
        assertNotNull(markdown);
        assertTrue(markdown.contains("![](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ==)"));
    }
}
