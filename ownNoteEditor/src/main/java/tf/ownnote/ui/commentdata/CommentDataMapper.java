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
package tf.ownnote.ui.commentdata;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.apache.commons.codec.binary.Base64;

/**
 * Helper for the mapping between html comments and data values.
 * 
 * - deflate / inflate of data to reduce note file size
 * - generic handling of get & set of data from objects
 * 
 * @author thomas
 */
public class CommentDataMapper {
    private final static CommentDataMapper INSTANCE = new CommentDataMapper();
    
    private static final String COMMENT_STRING_PREFIX = "<!-- ";
    private static final String COMMENT_STRING_SUFFIX = " -->";
    public static final String COMMENT_DATA_SEP = "---";
    public static final String COMMENT_VALUES_SEP = ":::";

    private static final Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
    private static final Inflater decompresser = new Inflater();

    private CommentDataMapper() {
        super();
    }
    
    public static CommentDataMapper getInstance() {
        return INSTANCE;
    }
    
    public static boolean isCommentWithData(final String comment) {
        return (comment.startsWith(COMMENT_STRING_PREFIX) && comment.endsWith(COMMENT_STRING_SUFFIX));
    }
    
    public static boolean containsCommentWithData(final String comment) {
        return (comment.startsWith(COMMENT_STRING_PREFIX) && comment.contains(COMMENT_STRING_SUFFIX));
    }
    
    public void fromComment(final ICommentDataHolder dataHolder, final String comment) {
        final String [] data = extractData(comment);
        
        final ICommentDataInfo[] infos = dataHolder.getCommentDataInfo();

        // now we have the name - value pairs
        // split further depending on multiplicity
        for (String nameValue : data) {
            boolean infoFound = false;
            for (ICommentDataInfo info : infos) {
                final String dataName = info.getDataName();
                if (nameValue.startsWith(dataName + "=\"") && nameValue.endsWith("\"")) {
                    // found it! now check & parse for values
                    final String[] values = nameValue.substring(dataName.length()+2, nameValue.length()-1).
                        strip().split(CommentDataMapper.COMMENT_VALUES_SEP);

                    switch (info.getDataMultiplicity()) {
                        case SINGLE:
                            dataHolder.setFromString(info, values[0]);
                            infoFound = true;
                            break;
                        case MULTIPLE:
                            dataHolder.setFromList(info, Arrays.asList(values));
                            infoFound = true;
                            break;
                    }
                }
                if (infoFound) {
                    // done, lets check next data value
                    break;
                }
            }
        }
    }
    
    public String toComment(final ICommentDataHolder dataHolder) {
        final StringBuffer result = new StringBuffer();

        for (ICommentDataInfo info : dataHolder.getCommentDataInfo()) {
            switch (info.getDataMultiplicity()) {
                case SINGLE:
                    final String value = dataHolder.getAsString(info);
                    if (value != null) {
                        if (result.length() > 0) {
                            result.append(CommentDataMapper.COMMENT_DATA_SEP);
                        }
                        result.append(info.getDataName());
                        result.append("=\"");
                        result.append(value);
                        result.append("\"");
                    }
                    break;
                case MULTIPLE:
                    final List<String> values = dataHolder.getAsList(info);
                    if (values != null && !values.isEmpty()) {
                        if (result.length() > 0) {
                            result.append(CommentDataMapper.COMMENT_DATA_SEP);
                        }
                        result.append(info.getDataName());
                        result.append("=\"");
                        result.append(values.stream().collect(Collectors.joining(CommentDataMapper.COMMENT_VALUES_SEP)));
                        result.append("\"");
                    }
                    break;
            }
        }
        
        return finalizeComment(result);
    }
    
    public static String finalizeComment(final StringBuffer stringBuffer) {
        try {
            compresser.reset();
            compresser.setInput(stringBuffer.toString().getBytes("UTF-8"));
            compresser.finish();
            
            final byte[] temp = new byte[32768];
            final int compressedDataLength = compresser.deflate(temp);
            final byte[] output = new byte[compressedDataLength];
            System.arraycopy(temp, 0, output, 0, compressedDataLength);
            final String encodedResult = Base64.encodeBase64String(output);

            // lets compress - if it is really shorter :-)
            if (encodedResult.length() < stringBuffer.length()) {
                stringBuffer.delete(0, stringBuffer.length());
                stringBuffer.append("data");
                stringBuffer.append("=\"");
                stringBuffer.append(encodedResult);
                stringBuffer.append("\"");
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CommentDataMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return CommentDataMapper.COMMENT_STRING_PREFIX + stringBuffer.toString() + CommentDataMapper.COMMENT_STRING_SUFFIX;
    }
    
    public static String [] extractData(final String content) {
        final String contentString = content.split(COMMENT_STRING_SUFFIX)[0] + COMMENT_STRING_SUFFIX;
        String [] data = contentString.substring(COMMENT_STRING_PREFIX.length(), contentString.length()-COMMENT_STRING_SUFFIX.length()).
                strip().split(COMMENT_DATA_SEP);

        // check for "data" first to decompress if required
        boolean dataFound = false;
        String dataString = "";
        for (String nameValue : data) {
            if (nameValue.startsWith("data=\"") && nameValue.endsWith("\"")) {
                final String[] values = nameValue.substring("data".length()+2, nameValue.length()-1).
                    strip().split(COMMENT_VALUES_SEP);

                decompresser.reset();
                final byte[] decoded = Base64.decodeBase64(values[0]);
                decompresser.setInput(decoded, 0, decoded.length);

                final byte[] temp = new byte[32768];
                try {
                    final int resultLength = decompresser.inflate(temp);

                    final byte[] input = new byte[resultLength];
                    System.arraycopy(temp, 0, input, 0, resultLength);

                    dataString = new String(input, "UTF-8");

                    dataFound = true;
                } catch (DataFormatException | UnsupportedEncodingException ex) {
                    Logger.getLogger(CommentDataMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (dataFound) {
            data = dataString.strip().split(COMMENT_DATA_SEP);
        }

        return data;
    }
}
