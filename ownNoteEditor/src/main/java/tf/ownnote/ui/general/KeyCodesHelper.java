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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author thomas
 */
public class KeyCodesHelper {
    private final static KeyCodeCombination controlCKey = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    private final static KeyCodeCombination controlVKey = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_ANY);
    private final static KeyCodeCombination controlXKey = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_ANY);
    private final static KeyCodeCombination shiftDeleteKey = new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHIFT_DOWN);
    private final static KeyCodeCombination deleteKey = new KeyCodeCombination(KeyCode.DELETE);
    private final static KeyCodeCombination insertKey = new KeyCodeCombination(KeyCode.INSERT);
    
    private final static KeyCodeCombination controlSKey = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_ANY);

    public static enum KeyCodes {
        CNTRL_C(controlCKey),
        CNTRL_V(controlVKey),
        CNTRL_X(controlXKey),
        SHIFT_DEL(shiftDeleteKey),
        DEL(deleteKey),
        INSERT(insertKey),
        CNTRL_S(controlSKey);
        
        private final KeyCodeCombination keyCode;
        
        private KeyCodes(final KeyCodeCombination key) {
            keyCode = key;
        }

        public KeyCodeCombination getKeyCode() {
            return keyCode;
        }
        
        public boolean match(final KeyEvent event) {
            return keyCode.match(event);
        }
    }
}
