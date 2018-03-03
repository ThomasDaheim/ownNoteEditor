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
package tf.ownnote.ui.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author Thomas
 */
public class CodeLoader {
    private final Stage stage;
    private final TextArea codeArea;
    private final ChoiceBox<String> codeType;
    private final Map <String,String> myMap = new LinkedHashMap<>();
    private final TilePane myGrid = new TilePane();
    private final Button insertBtn = new Button();
    private final Button cancelBtn = new Button(cancelLabel);
    
    // not more than 16 languages, please...
    private static final int ROW_COUNT = 16;
    
    private static final String stageTitle = "Insert formatted text";
    private static final String saveLabel = "Insert";
    private static final String replaceLabel = "Replace";
    private static final String cancelLabel = "Cancel";
    private boolean doInsert = false;
    
    public CodeLoader() {
        initGridPane();
                
        codeType = new ChoiceBox<>();
        codeType.getStyleClass().add("codetypeField");
        codeType.getItems().addAll(myMap.keySet());
        codeType.getSelectionModel().clearAndSelect(0);
        
        https://docs.oracle.com/javafx/2/ui_controls/editor.htm
        stage = new Stage();
        stage.setTitle("Insert formatted text");
        stage.initModality(Modality.WINDOW_MODAL);

        codeArea = new TextArea();
        codeArea.getStyleClass().add("codeareaField");
        codeArea.setWrapText(false);
        codeArea.setEditable(true);
        codeArea.setPrefWidth(Double.MAX_VALUE);

        final GridPane root = new GridPane();
        root.setPadding(new Insets(5, 5, 5, 5));
        root.setHgap(5);
        root.setVgap(5);
        
        // 1st row: select language
        root.add(new Label("Language:"), 0, 0);
        root.add(codeType, 1, 0);

        // 2nd row: text area
        root.add(codeArea, 0, 1, 2, 1);
        
        // 3th row: insert / cancel buttons
        insertBtn.getStyleClass().add("codeareaInsert");
        insertBtn.setDefaultButton(true);
        insertBtn.setOnAction((ActionEvent arg0) -> {
            doInsert = true;
            stage.close();
        });
        root.add(insertBtn, 0, 2);
        
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            doInsert = false;
            stage.close();
        });
        root.add(cancelBtn, 1, 2);
        
        
        root.getRowConstraints().add(new RowConstraints());
        final RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        root.getRowConstraints().add(row2);

        final Scene scene = new Scene(new Group(), 600, 600);
        scene.setRoot(root);

        stage.setScene(scene);
    }
    
    private void initGridPane() {
        // style grid
        myGrid.setHgap(2);
        myGrid.setVgap(2);
        myGrid.setPadding(new Insets(2, 2, 2, 2));
        myGrid.setAlignment(Pos.CENTER_LEFT);

        // add content to map & grid
        final int langCount = languages.length;
        for (int index = 0; index < langCount; index++ ) {
            myMap.put(languages[index][1], languages[index][0]);
            myGrid.getChildren().add(new Label(languages[index][1]));
        }
    }

    public String getCode() {
        return codeArea.getText();
    }

    public void setCode(final String newCode) {
        codeArea.setText(newCode);
    }

    public String getLanguage() {
        return myMap.get(codeType.getSelectionModel().getSelectedItem());
    }

    public void setLanguage(final String newLanguage) {
        // select correct entry in choicebox - if its available...
        if (myMap.containsValue(newLanguage)) {
            final List<String> langList = new ArrayList<>(myMap.values());
            codeType.getSelectionModel().clearAndSelect(langList.indexOf(newLanguage));
        } else {
            codeType.getSelectionModel().clearAndSelect(0);
        }
    }

    public boolean getDoInsert() {
        return doInsert;
    }

    public void showAndWait() {
        stage.setTitle("Insert formatted text");
        
        // set button label to insert or replace - based on content of test area
        if ("".equals(codeArea.getText())) {
            insertBtn.setText(saveLabel);
        } else {
            insertBtn.setText(replaceLabel);
        }
        
        stage.showAndWait();
    }
    
    // list of all prism.js selected languages
    String languages [][] = {
        {"markup", "HTML/XML/SVG"},
        {"css", "CSS"},
        {"clike", "C-like"},
        {"javascript", "JavaScript"},
        //{"abap", "ABAP"},
        //{"actionscript", "ActionScript"},
        //{"ada", "Ada"},
        //{"apacheconf", "Apache Configuration"},
        //{"apl", "APL"},
        //{"applescript", "AppleScript"},
        //{"asciidoc", "AsciiDoc"},
        //{"aspnet", "ASP.NET (C#)"},
        //{"autoit", "AutoIt"},
        //{"autohotkey", "AutoHotkey"},
        //{"bash", "Bash"},
        //{"basic", "BASIC"},
        //{"batch", "Batch"},
        //{"bison", "Bison"},
        //{"brainfuck", "Brainfuck"},
        //{"bro", "Bro"},
        //{"c", "C"},
        //{"csharp", "C#"},
        //{"cpp", "C++"},
        //{"coffeescript", "CoffeeScript"},
        //{"crystal", "Crystal"},
        //{"d", "D"},
        //{"dart", "Dart"},
        //{"django", "Django/Jinja2"},
        //{"diff", "Diff"},
        //{"docker", "Docker"},
        //{"eiffel", "Eiffel"},
        //{"elixir", "Elixir"},
        //{"erlang", "Erlang"},
        //{"fsharp", "F#"},
        //{"fortran", "Fortran"},
        //{"gherkin", "Gherkin"},
        //{"git", "Git"},
        //{"glsl", "GLSL"},
        //{"go", "Go"},
        //{"graphql", "GraphQL"},
        //{"groovy", "Groovy"},
        //{"haml", "Haml"},
        //{"handlebars", "Handlebars"},
        //{"haskell", "Haskell"},
        //{"haxe", "Haxe"},
        //{"http", "HTTP"},
        //{"icon", "Icon"},
        //{"inform7", "Inform 7"},
        //{"ini", "Ini"},
        //{"j", "J"},
        //{"jade", "Jade"},
        {"java", "Java"},
        //{"jolie", "Jolie"},
        {"json", "JSON"},
        //{"julia", "Julia"},
        //{"keyman", "Keyman"},
        //{"kotlin", "Kotlin"},
        //{"latex", "LaTeX"},
        //{"less", "Less"},
        //{"livescript", "LiveScript"},
        //{"lolcode", "LOLCODE"},
        //{"lua", "Lua"},
        //{"makefile", "Makefile"},
        {"markdown", "Markdown"},
        //{"matlab", "MATLAB"},
        //{"mel", "MEL"},
        //{"mizar", "Mizar"},
        //{"monkey", "Monkey"},
        //{"nasm", "NASM"},
        //{"nginx", "nginx"},
        //{"nim", "Nim"},
        //{"nix", "Nix"},
        //{"nsis", "NSIS"},
        //{"objectivec", "Objective-C"},
        //{"ocaml", "OCaml"},
        //{"oz", "Oz"},
        //{"parigp", "PARI/GP"},
        //{"parser", "Parser"},
        //{"pascal", "Pascal"},
        //{"perl", "Perl"},
        {"php", "PHP"},
        //{"powershell", "PowerShell"},
        //{"processing", "Processing"},
        //{"prolog", "Prolog"},
        {"properties", ".properties"},
        //{"protobuf", "Protocol Buffers"},
        //{"puppet", "Puppet"},
        //{"pure", "Pure"},
        //{"python", "Python"},
        //{"q", "Q"},
        //{"qore", "Qore"},
        //{"r", "R"},
        //{"jsx", "React JSX"},
        //{"reason", "Reason"},
        //{"rest", "reST (reStructuredText)"},
        //{"rip", "Rip"},
        //{"roboconf", "Roboconf"},
        //{"ruby", "Ruby"},
        //{"rust", "Rust"},
        //{"sas", "SAS"},
        //{"sass", "Sass (Sass)"},
        //{"scss", "Sass (Scss)"},
        //{"scala", "Scala"},
        //{"scheme", "Scheme"},
        //{"smalltalk", "Smalltalk"},
        //{"smarty", "Smarty"},
        {"sql", "SQL"},
        //{"stylus", "Stylus"},
        //{"swift", "Swift"},
        //{"tcl", "Tcl"},
        //{"textile", "Textile"},
        //{"twig", "Twig"},
        //{"typescript", "TypeScript"},
        //{"verilog", "Verilog"},
        //{"vhdl", "VHDL"},
        //{"vim", "vim"},
        {"wiki", "Wiki markup"},
        //{"xojo", "Xojo (REALbasic)"},
        //{"yaml", "YAML"}        
    };
}
