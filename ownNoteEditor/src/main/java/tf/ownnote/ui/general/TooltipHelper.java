/*
 * Copyright (c) 2014ff Thomas Feuster
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
package tf.ownnote.ui.general;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 *
 * @author thomas
 */
public class TooltipHelper {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static TooltipHelper INSTANCE = new TooltipHelper();

    private TooltipHelper() {
        // Exists only to defeat instantiation.
    }

    public static TooltipHelper getInstance() {
        return INSTANCE;
    }
    
    // https://stackoverflow.com/a/42759066
    /**
     * Hack allowing to modify the default behavior of the tooltips.
     * @param tooltip
     * @param openDelay The open delay, knowing that by default it is set to 1000.
     * @param visibleDuration The visible duration, knowing that by default it is set to 5000.
     * @param closeDelay The close delay, knowing that by default it is set to 200.
     * @param hideOnExit Indicates whether the tooltip should be hide on exit, 
     * knowing that by default it is set to false.
     */
    public static void updateTooltipBehavior(
            final Tooltip tooltip,
            final double openDelay,
            final double visibleDuration,
            final double closeDelay,
            final boolean hideOnExit) {
        // TFE, 20181005: with java 9 its no longer necessary to use reflection
        // since the required methods of behaviour are now available
        tooltip.setShowDelay(Duration.millis(openDelay));
        tooltip.setHideDelay(Duration.millis(closeDelay));
        tooltip.setShowDuration(Duration.millis(visibleDuration));
        tooltip.setAutoHide(hideOnExit);
        tooltip.setHideOnEscape(hideOnExit);
        tooltip.setAutoFix(true);
//        try {
//            // Get the non public field "BEHAVIOR"
//            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
//            // Make the field accessible to be able to get and set its value
//            fieldBehavior.setAccessible(true);
//            // Get the value of the static field
//            Object objBehavior = fieldBehavior.get(null);
//            // Get the constructor of the private static inner class TooltipBehavior
//            Constructor<?> constructor = objBehavior.getClass().getDeclaredConstructor(
//                Duration.class, Duration.class, Duration.class, boolean.class
//            );
//            // Make the constructor accessible to be able to invoke it
//            constructor.setAccessible(true);
//            // Create a new instance of the private static inner class TooltipBehavior
//            Object tooltipBehavior = constructor.newInstance(
//                new Duration(openDelay), new Duration(visibleDuration),
//                new Duration(closeDelay), hideOnExit
//            );
//            // Set the new instance of TooltipBehavior
//            fieldBehavior.set(null, tooltipBehavior);
//        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchFieldException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
//            throw new IllegalStateException(e);
//        }
    }
}
