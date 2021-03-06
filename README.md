# IntelliJ Idea Plugin for ANTLR v4

An [IntelliJ](https://www.jetbrains.com/idea/) 12.1.x, 13.x plugin for ANTLR v4 ([plugin source at github](https://github.com/antlr/antlr4)).

[Plugin page at intellij](http://plugins.jetbrains.com/plugin/7358?pr=idea)

## History
* 1.0.2:
    * Update to 4.2.1 ANTLR and rm hack needed for bug in 4.2
* 1.0.1:
    * Fixed [#40](https://github.com/antlr/intellij-plugin-v4/issues/40)
    * Fixed [#36](https://github.com/antlr/intellij-plugin-v4/issues/36)
* 1.0
    * Has ANTLR 4.2 release
    * Works with Intellij 13.x, 12.1.x (no longer works with 12.0.x)
    * Color preferences pane
* 1.0a5:
	* Update to use latest ANTLR 4.2 from parrt/antlr4 (close to 4.2 final)
	* wasn't showing grammar name. weird.
	* Added "sort by rule type" to put parser then lexer rules in nav window
	* Added live parse tree preview; type text into left editor pane of
	  tool window. Tree appears in right.
	* Multiple grammar files got error messages mixed up between documents.
* 1.0a4:
	* finds tokenVocab option for code gen
	* if there is a tokenVocab option, don't warn about implicit tokens.
	* shortcut conflicted with grammar-kit plugin.
* 1.0a3:
	* generates code. Uses package dir if @header { package x.y.z; } action present.
	* generates in <root>/gen/package/YourGrammarRecognizer.java
	* Shortcut (ctrl-shift-G / meta-shift-G) but it's in Tools menu, popups.
	* Code completion for tokens, rule names.
* 1.0a2:
	* goto-declaration
	* ANTLR itself processes grammar and semantic errors get highlighted.
	* find usages
	* rename tokens, rules
* 1.0a1:
 	* syntax-aware editing, highlighting, structure view

## Screenshots

### Java grammar view
![Java grammar view](images/java-grammar.png)

### Find usages
![Find usages](images/findusages.png)

### Code completion
![Code completion](images/completion.png)

### Live parse tree preview

You can test any rule in the (parser) grammar.  Right click on rule in grammar
or navigator to "Test ANTLR Rule".  Changing grammar and saving, updates
parse tree. It works with combined grammars and separated but separated
must be in same directory and named XParser.g4 and XLexer.g4.
No raw Java actions are executed obviously during interpretation in
live preview.

[![Live parse preview](http://img.youtube.com/vi/h60VapD1rOo/0.jpg)](//www.youtube.com/embed/h60VapD1rOo)

![Live preview](images/live-preview.png)
![Live preview](images/live-preview-error.png)

### Color preferences

![Live preview](images/color-prefs.png)

