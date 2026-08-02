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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataExtractorTest {

    @Test
    void testExtractNameGroup() {
        DataExtractor extractor = new DataExtractor("[Group1~Group2] MyFile", "<html></html>");
        assertEquals("[Group1~Group2] MyFile", extractor.getName());
        Map<String, List<String>> map = extractor.getDataMap();
        assertTrue(map.containsKey("Group"));
        assertEquals(List.of("Group1/Group2"), map.get("Group"));
    }

    @Test
    void testExtractNameWithoutGroup() {
        DataExtractor extractor = new DataExtractor("MyFile", "<html></html>");
        Map<String, List<String>> map = extractor.getDataMap();
        assertTrue(map.containsKey("Group"));
        assertEquals(List.of(""), map.get("Group"));
    }

    @Test
    void testNullHtmlAndName() {
        DataExtractor extractor = new DataExtractor(null, null);
        assertNull(extractor.getName());
        assertTrue(extractor.getDataMap().isEmpty());
    }

    @Test
    void testDataMapParsingWithCompressedHtml() {
        String html = "<!doctype html><html><body><!-- data=\"eNptjsFOwzAMhl+lyj1R4iRtZ6nS4MBpx8LO0TBt0dpVjQc3np0EtnWHSbZl+5N//2Ge32iJw2lqRKmckFJ+/c+xEdyfxhC3xildKdBgCjBoLfoNIl6hX6FGW6HRGaoPOkemZWvKGzcWXYlWr8calHYZQgEWwaN1d8rVCgF1jf4OQn2RLbMnrxE22TuHLvluU/1pKbL86zh1I0E6fgBypjDGPObyuw9c0DFS1j/0YYnEjXhtX2SdN4E5HPqRJk6Pn2mYuqfdwHykPQ3Lu/qcO/ELiQxrgA==\" --></body></html>";
        DataExtractor extractor = new DataExtractor("[Tag1~Tag2] Note", html);

        Map<String, List<String>> dataMap = extractor.getDataMap();
        assertFalse(dataMap.isEmpty());
        assertTrue(dataMap.containsKey("Group"));
        assertEquals(List.of("Tag1/Tag2"), dataMap.get("Group"));
    }
}
