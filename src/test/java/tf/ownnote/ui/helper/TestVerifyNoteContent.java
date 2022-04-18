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
package tf.ownnote.ui.helper;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.ownnote.ui.notes.Note;
import tf.ownnote.ui.tags.TagManager;

/**
 *
 * @author thomas
 */
public class TestVerifyNoteContent {
    private Boolean resultBool;
    private String resultErr;
    
    @Before
    public void setUp() {
        OwnNoteFileManager.getInstance().setCallback(null);
        OwnNoteFileManager.getInstance().initNotesPath("src/test/resources/");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testVerifyOK() throws Exception {
        final String errorString = "";
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().tagForGroupName("Test", false), "TestVerify_OK");
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();
        
        doTestNoteFileContent(note, errorString, true);
        doTestNoteContent(content, errorString, true);
    }

    @Test
    public void testVerifyDuplicateComment() throws Exception {
        final String errorString = "Checking of Repeated Metadata failed!\n  Found --><!-- @274\n  Found --><!-- @1327\n";
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().tagForGroupName("Test", false), "TestVerify_DUPL_COM");
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();

        doTestNoteFileContent(note, "[Test] TestVerify_DUPL_COM.htm: " + errorString, false);
        doTestNoteContent(content, errorString, false);
    }

    @Test
    public void testVerifyDuplicateId() throws Exception {
        final String errorString = "Checking of Duplicate Metadata Id failed!\n  Found 9c4fcb5f90af @882\n  Found b6b5f454856d @629\n  Found b659e1dc2728 @551\n";
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().tagForGroupName("Test", false), "TestVerify_DUPL_ID");
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();

        doTestNoteFileContent(note, "[Test] TestVerify_DUPL_ID.htm: " + errorString, false);
        doTestNoteContent(content, errorString, false);
    }

    @Test
    public void testVerifyAdditionalAttributes() throws Exception {
        final String errorString = "Checking of Additional checkbox attributes failed!\n  Found <input type=\"checkbox\" value=\"salami\"> @519\n  Found <input name=\"zutat\" type=\"checkbox\" value=\"salami\"> @953\n  Found <input name=\"zutat\" type=\"checkbox\" value=\"salami\"> @1242\n";
        final Note note = OwnNoteFileManager.getInstance().getNote(TagManager.getInstance().tagForGroupName("Test", false), "TestVerify_ADD_ATTR");
        final String content = OwnNoteFileManager.getInstance().readNote(note, true).getNoteFileContent();

        doTestNoteFileContent(note, "[Test] TestVerify_ADD_ATTR.htm: " + errorString, false);
        doTestNoteContent(content, errorString, false);
    }
    
//    @Test
//    public void testNotPattern() {
//        final Pattern pattern = Pattern.compile("<input[^>]*(?<!type)=\"([^\"]*)\"[^>]*>");
////        final Pattern pattern = Pattern.compile("(?<!type)(=\"[^\"]*\")");
////        final Pattern pattern = Pattern.compile("([^\\s]+)=\"[^\"]+\"");
//
//        String checkString = "<input name=\"zutat\" type=\"checkbox\">";
//        System.out.println("Checking " + checkString);
//        Matcher matcher = pattern.matcher(checkString);
//        while (matcher.find()) {
////            System.out.println("  Found '" +  matcher.group(0) + "' @" + matcher.start(0) + "-" + matcher.end(0));
//            for (int i=1; i<=matcher.groupCount(); i++) {
////                System.out.println("  Found '" +  matcher.group(i) + "' / '" + checkString.substring(matcher.start(i)) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//                System.out.println("  Found '" +  matcher.group(i) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//            }
//        }
//
//        checkString = "<input name=\"zutat\" type=\"checkbox\" value=\"salami\">";
//        System.out.println("Checking " + checkString);
//        matcher = pattern.matcher(checkString);
//        while (matcher.find()) {
////            System.out.println("  Found '" +  matcher.group(0) + "' @" + matcher.start(0) + "-" + matcher.end(0));
//            for (int i=1; i<=matcher.groupCount(); i++) {
////                System.out.println("  Found '" +  matcher.group(i) + "' / '" + checkString.substring(matcher.start(i)) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//                System.out.println("  Found '" +  matcher.group(i) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//            }
//        }
//
//        checkString = "<input type=\"checkbox\" value=\"salami\">";
//        System.out.println("Checking " + checkString);
//        matcher = pattern.matcher(checkString);
//        while (matcher.find()) {
////            System.out.println("  Found '" +  matcher.group(0) + "' @" + matcher.start(0) + "-" + matcher.end(0));
//            for (int i=1; i<=matcher.groupCount(); i++) {
////                System.out.println("  Found '" +  matcher.group(i) + "' / '" + checkString.substring(matcher.start(i)) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//                System.out.println("  Found '" +  matcher.group(i) + "' @" + matcher.start(i) + "-" + matcher.end(i));
//            }
//        }
//    }
    
    private void doTestNoteFileContent(final Note note, final String errorString, final boolean errorBool) throws Exception {
        resultErr = tapSystemErrNormalized(() -> {
            resultBool = VerifyNoteContent.getInstance().verifyNoteFileContent(note);
          });
        Assert.assertTrue(errorBool == resultBool);
        Assert.assertEquals(errorString, resultErr);
    }

    private void doTestNoteContent(final String content, final String errorString, final boolean errorBool) throws Exception {
        resultErr = tapSystemErrNormalized(() -> {
            resultBool = VerifyNoteContent.getInstance().verifyNoteContent(content);
          });
        Assert.assertTrue(errorBool == resultBool);
        Assert.assertEquals(errorString, resultErr);
    }
}
