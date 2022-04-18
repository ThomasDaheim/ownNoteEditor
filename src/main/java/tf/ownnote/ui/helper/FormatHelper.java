/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.helper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.function.Predicate;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import tf.helper.javafx.TooltipHelper;

/**
 * Helper for formating and sorting special values in OwnNoteTableColumn
 * 
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class FormatHelper {
    private final static FormatHelper INSTANCE = new FormatHelper();
    
    private final Comparator<String> fileTimeComparator;
    
    private enum TimeIntervals {
        now("Right now"),
        second("second"),
        minute("minute"),
        hour("hour"),
        day("day"),
        month("month"),
        year("year");
        
        private final String name;
        
        private TimeIntervals(final String s) {
            name = s;
        }

        public boolean equalsName(final String otherName) {
            return (otherName == null) ? false : name.equals(otherName);
        }

        @Override
        public String toString() {
           return this.name;
        }        
        
        public static TimeIntervals fromString(String text) {
            if (text != null) {
                // might end in "s" due to plural... lets check & remove
                if (text.length() > 0 && text.charAt(text.length()-1)=='s') {
                    text = text.substring(0, text.length()-1);
                }
                
                for (TimeIntervals b : TimeIntervals.values()) {
                    if (text.equalsIgnoreCase(b.name)) {
                        return b;
                    }
                }
            }
            return null;
        }
    }
    
    private FormatHelper() {
        super();
        
        // see commit in issue #42
        fileTimeComparator = (String arg0, String arg1) -> {
            // ownNote says "Right now", "x secs ago", "x minutes ago", , "x hours ago", "x days ago", "x months ago", "x years ago"
            
            // equals?
            if (arg0.equals(arg1))
                return 0;
            
            // right now tops everything
            if (arg0.equals(TimeIntervals.now.toString()))
                return -1;
            if (arg1.equals(TimeIntervals.now.toString()))
                return 1;
            
            // split into value and unit - right now has already been excluded :-)
            final String[] parts0 = arg0.split(" ");
            final TimeIntervals unit0 = TimeIntervals.fromString(parts0[1]);
            
            final String[] parts1 = arg1.split(" ");
            final TimeIntervals unit1 = TimeIntervals.fromString(parts1[1]);
            
            final int unit = unit0.compareTo(unit1);

            if (unit == 0) {
                final int value0 = Integer.parseInt(parts0[0]);
                final int value1 = Integer.parseInt(parts1[0]);

                // for equal units compare values
                return Integer.compare(value0, value1);
            } else {
                // return unit comparison (is based on order of declaration of enum values!)
                return unit;
            }
        };
    }

    public static FormatHelper getInstance() {
        return INSTANCE;
    }
    
    // format file modified time as ownNote does
    public String formatFileTime(final LocalDateTime filetime) {
        assert filetime != null;
        
        String result = "";
        
        // ownNote says "now", "x secs ago", "x minutes ago", , "x hours ago", "x days ago", "x months ago", "x years ago"
        
        final LocalDateTime curtime = LocalDateTime.now();
        // start with longest interval and work your way down...
        result = stringFromDifference(ChronoUnit.YEARS.between(filetime, curtime), TimeIntervals.year.toString());
        if (result.isEmpty()) {
            result = stringFromDifference(ChronoUnit.MONTHS.between(filetime, curtime), TimeIntervals.month.toString());
            if (result.isEmpty()) {
                result = stringFromDifference(ChronoUnit.DAYS.between(filetime, curtime), TimeIntervals.day.toString());
                if (result.isEmpty()) {
                    result = stringFromDifference(ChronoUnit.HOURS.between(filetime, curtime), TimeIntervals.hour.toString());
                    if (result.isEmpty()) {
                        result = stringFromDifference(ChronoUnit.MINUTES.between(filetime, curtime), TimeIntervals.minute.toString());
                        if (result.isEmpty()) {
                            result = stringFromDifference(ChronoUnit.SECONDS.between(filetime, curtime), TimeIntervals.second.toString());
                            if (result.isEmpty()) {
                                result = TimeIntervals.now.toString();
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    private String stringFromDifference(final long difference, final String unit) {
        assert unit != null;
        
        String result = "";
        
        if (difference > 0) {
            if (difference == 1.0) {
                result = "1 " + unit + " ago";
            } else {
                result = String.valueOf(difference) + " " + unit + "s ago";
            }
        }
        
        return result;
    }
    
    // and here the matching comparator
    public Comparator<String> getFileTimeComparator() {
        return fileTimeComparator;
    }
    
    public void initNoteGroupNameTextField(final TextField textField) {
        // https://stackoverflow.com/a/54552791
        // https://stackoverflow.com/a/49918923
        // https://stackoverflow.com/a/45201446
        // to check for illegal chars in note & group names
        // use more restrictive windows rules to make sure notes can be stored anywhere

        // TFE, 20220417: added ~ as char to hae it as group name separator
        textField.setTextFormatter(new TextFormatter<>(change ->
            (change.getControlNewText().matches("([^\u0001-\u001f<>:\"/\\\\|?*\u007f]*~)?")) ? change : null));

        final Tooltip t = new Tooltip();
        final StringBuilder tooltext = new StringBuilder();
        tooltext.append("Chars not allowed:\n");
        tooltext.append("<>,\"/\\|?*~ and chars 00-31");
        t.setText(tooltext.toString());
        t.getStyleClass().addAll("nametooltip");
        TooltipHelper.updateTooltipBehavior(t, 0, 10000, 0, true);

        Tooltip.install(textField, t);
    }
    
    public void initTagNameTextField(final TextField textField, final Predicate<String> isValueAllowed) {
        // https://stackoverflow.com/a/54552791
        // https://stackoverflow.com/a/49918923
        // https://stackoverflow.com/a/45201446
        // to check for illegal chars in note & group names
        // use more restrictive windows rules to make sure notes can be stored anywhere

        textField.setTextFormatter(new TextFormatter<>(change ->
            (isValueAllowed.test(change.getControlNewText()) ? change : null)));

        final Tooltip t = new Tooltip("No duplicate tag names allowed");
        t.getStyleClass().addAll("nametooltip");
        TooltipHelper.updateTooltipBehavior(t, 0, 10000, 0, true);

        Tooltip.install(textField, t);
    }
}
