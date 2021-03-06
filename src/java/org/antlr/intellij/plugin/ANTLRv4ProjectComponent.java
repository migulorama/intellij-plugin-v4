package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.antlr.intellij.plugin.preview.ParseTreePanel;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ANTLRv4ProjectComponent implements ProjectComponent {
	public ParseTreePanel treePanel;
	public ConsoleView console;
	public Project project;

	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.ANTLRv4ProjectComponent");

	public ANTLRv4ProjectComponent(Project project) {
		this.project = project;
	}

	public static ANTLRv4ProjectComponent getInstance(Project project) {
		ANTLRv4ProjectComponent pc = project.getComponent(ANTLRv4ProjectComponent.class);
		return pc;
	}

/* doesn't work if file is not in a source dir of a project i think.
	public static Project getProjectForFile(VirtualFile virtualFile) {
		Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
		Project project = null;
		for (int i = 0; i < openProjects.length; i++) {
			Project p = openProjects[i];
			ProjectFileIndex fileIndex = ProjectRootManager.getInstance(p).getFileIndex();
			if ( fileIndex.isInContent(virtualFile) ) {
				project = p;
			}
		}
		return project;
	}
	 */

	public ParseTreePanel getTreeViewPanel() {
		return treePanel;
	}

	public ConsoleView getConsole() {
		return console;
	}

	// -------------------------------------

	@Override
	public void initComponent() {
	}

	@Override
	public void projectOpened() {
		treePanel = new ParseTreePanel();
		TextConsoleBuilderFactory consoleBuidlerFactory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = consoleBuidlerFactory.createBuilder(project);

		console = consoleBuilder.getConsole();
	}

	@Override
	public void projectClosed() {
	}

	@Override
	public void disposeComponent() {
	}

	@NotNull
	@Override
	public String getComponentName() {
		return "antlr.ProjectComponent";
	}

//	private ToolWindow getToolWindow()
//	{
//		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(_project);
//		ToolWindow toolWindow = toolWindowManager.getToolWindow(ParseTreeWindowFactory.ID);
//		if ( toolWindow!=null ) {
//			return toolWindow;
//		}
//		else {
//			return toolWindowManager.registerToolWindow(ID_TOOL_WINDOW,
//														_viewerPanel,
//														ToolWindowAnchor.RIGHT);
//		}
//	}
//
//	private boolean isToolWindowRegistered()
//	{
//		return ToolWindowManager.getInstance(_project).getToolWindow(ID_TOOL_WINDOW) != null;
//	}

	public static Object[] parseText(ParseTreePanel parseTreePanel,
									 String inputText,
									 String grammarFileName,
									 String startRule)
		throws IOException
	{
		if (!new File(grammarFileName).exists()) {
			return null;
		}

		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		MyANTLRToolListener listener = new MyANTLRToolListener(antlr);
		antlr.addListener(listener);

		String combinedGrammarFileName = null;
		String lexerGrammarFileName = null;
		String parserGrammarFileName = null;

		Grammar g = antlr.loadGrammar(grammarFileName); // load to examine it
		// examine's Grammar AST from v4 itself;
		// hence use ANTLRParser.X not ANTLRv4Parser from this plugin
		switch ( g.getType() ) {
			case ANTLRParser.PARSER :
				parserGrammarFileName = grammarFileName;
				int i = grammarFileName.indexOf("Parser");
				if ( i>=0 ) {
					lexerGrammarFileName = grammarFileName.substring(0, i) + "Lexer.g4";
				}
				break;
			case ANTLRParser.LEXER :
				lexerGrammarFileName = grammarFileName;
				int i2 = grammarFileName.indexOf("Lexer");
				if ( i2>=0 ) {
					parserGrammarFileName = grammarFileName.substring(0, i2) + "Parser.g4";
				}
				break;
			case ANTLRParser.COMBINED :
				combinedGrammarFileName = grammarFileName;
				lexerGrammarFileName = grammarFileName+"Lexer";
				parserGrammarFileName = grammarFileName+"Parser";
				break;
		}

		if ( lexerGrammarFileName==null ) {
			LOG.error("Can't compute lexer file name from "+grammarFileName, (Throwable)null);
			return null;
		}
		if ( parserGrammarFileName==null ) {
			LOG.error("Can't compute parser file name from "+grammarFileName, (Throwable)null);
			return null;
		}

		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		if ( combinedGrammarFileName!=null ) {
			// already loaded above
			if ( listener.grammarErrorMessage!=null ) {
				return null;
			}
			lexEngine = g.createLexerInterpreter(input);
		}
		else {
			LexerGrammar lg = null;
			try {
				lg = (LexerGrammar)Grammar.load(lexerGrammarFileName);
			}
			catch (ClassCastException cce) {
				LOG.error("File " + lexerGrammarFileName + " isn't a lexer grammar", cce);
			}
			if ( listener.grammarErrorMessage!=null ) {
				return null;
			}
			g = loadGrammar(antlr, parserGrammarFileName, lg);
			lexEngine = lg.createLexerInterpreter(input);
		}

		final JTextArea console = parseTreePanel.getConsole();
		final MyConsoleErrorListener syntaxErrorListener = new MyConsoleErrorListener();
		Object[] result = new Object[2];

		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		ParserInterpreter parser = g.createParserInterpreter(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(syntaxErrorListener);
		Rule start = g.getRule(startRule);
		if ( start==null ) {
			return null; // can't find start rule
		}
		ParseTree t = parser.parse(start.index);

		console.setText(syntaxErrorListener.syntaxError);
		if ( t!=null ) {
			return new Object[] {parser, t};
		}
		return null;
	}

	/** Same as loadGrammar(fileName) except import vocab from existing lexer */
	public static Grammar loadGrammar(Tool tool, String fileName, LexerGrammar lexerGrammar) {
		GrammarRootAST grammarRootAST = tool.parseGrammar(fileName);
		final Grammar g = tool.createGrammar(grammarRootAST);
		g.fileName = fileName;
		g.importVocab(lexerGrammar);
		tool.process(g, false);
		return g;
	}

	static class MyANTLRToolListener extends DefaultToolListener {
		public String grammarErrorMessage;
		public MyANTLRToolListener(Tool tool) { super(tool); }

		@Override
		public void error(ANTLRMessage msg) {
//			super.error(msg);
			ST msgST = tool.errMgr.getMessageTemplate(msg);
			grammarErrorMessage = msgST.render();
			if (tool.errMgr.formatWantsSingleLineMessage()) {
				grammarErrorMessage = grammarErrorMessage.replace('\n', ' ');
			}
		}
	}

	/** Traps parser interpreter syntax errors */
	static class MyConsoleErrorListener extends ConsoleErrorListener {
		public String syntaxError="";
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
								@Nullable Object offendingSymbol,
								int line, int charPositionInLine, String msg,
								@Nullable RecognitionException e)
		{
//			super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			syntaxError = "line " + line + ":" + charPositionInLine + " " + msg;
		}
	}
}
