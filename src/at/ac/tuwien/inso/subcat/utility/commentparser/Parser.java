/* Parser.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.utility.commentparser;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public class Parser<T> {
	private final static String regexReview = "^Review of attachment ([0-9]+)";
	private final static Pattern pReview= Pattern.compile (regexReview);

	private final static String regexQuote = "(\\(In reply to comment #([0-9]+)\\))?((\n|^)>.*)+";
	private final static Pattern pQuote= Pattern.compile (regexQuote);

	private final static String regexPara = "\n\\s*(\n\\s*)+";
	private final static Pattern pPara = Pattern.compile (regexPara);

	private final static String regexNoramlise = "(\n| |\t)+";
	private final static Pattern pNorm = Pattern.compile (regexNoramlise);

	private final static String regexCleanFstQuote = "^>( |\t)*";
	private final static Pattern pCleanFstQuote = Pattern.compile (regexCleanFstQuote, Pattern.MULTILINE);

	private final static String regexSubQuote = "^(>.*|\\(In reply to comment #[0-9]+\\))$";
	private final static Pattern pCleanSubQuote = Pattern.compile (regexSubQuote, Pattern.MULTILINE);

	private final static String regexQuoteSplitter = "\n\\s*(\n\\s*)+";
	private final static Pattern pQuoteSplitter = Pattern.compile (regexQuoteSplitter);

	private final static String regexGLibMessages = "((\\s*^\\*\\*\\s+\\(.+:[0-9]+\\):\\s+[A-Za-z0-9-]+\\s+\\*\\*:\\s+[a-zA-Z_0-9]+:.*$\n?)+)";
	private final static Pattern pGLibMessages = Pattern.compile (regexGLibMessages, Pattern.MULTILINE);

	private final static String cliRegex = "^((\\[[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+ (~|[a-zA-Z0-9_-]+)\\])|[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+:(~|[a-zA-Z0-0_-]+))(\\$|#).*";
	private final static Pattern pCli= Pattern.compile (cliRegex);

	private final static  String lstRegex = "^[ |\t]*(\\*|-|\\(([0-9]+|[a-z]+|[A-Z]+)\\)|([0-9]+|[a-z]+|[A-Z]+)(\\)|\\.)).*$";
	private final static Pattern pLst= Pattern.compile (lstRegex);


	private final static int PARA_MAX_NEWLINES = 10;
	

	public CommentNode<T> parse (String content) {
		int patchId = -1;

		Matcher m = pReview.matcher (content);
		if (m.find () && m.group (1) != null) {
			patchId = Integer.parseInt (m.group (1));
			content = content.substring (m.end ());
		}
		
		List<ContentNode<T>> ast = new LinkedList<ContentNode<T>> ();
		parseQuotes (ast, content);

		return new CommentNode<T> (ast, patchId);
	}
	
	private void parseQuotes (List<ContentNode<T>> ast, String comment) {
		Matcher m = pQuote.matcher (comment);
		
		int lastEnd = 0;
		while (m.find ()) {
			if (lastEnd >= 0 && lastEnd != m.start ()) {
				parseParagraphs (ast, comment.substring (lastEnd, m.start ()));
			}
			
			String content = m.group (0);
			String idStr = m.group (2);
			int id = (idStr != null)? Integer.parseInt (idStr) : -1;

			Matcher cfm = pCleanFstQuote.matcher (content);
			content = cfm.replaceAll ("");

			Matcher csm = pCleanSubQuote.matcher (content);
			content = csm.replaceAll ("");

			String[] contentFragments = pQuoteSplitter.split (content.trim ());
			String[] cleanedFragments = new String[contentFragments.length];
			for (int i = 0; i < contentFragments.length; i++) {
				Matcher normM = pNorm.matcher (contentFragments[i]);
				cleanedFragments[i] = normM.replaceAll (" ");
			}

			ast.add (new QuoteNode<T> (cleanedFragments, id));
			lastEnd = m.end ();
		}

		if (lastEnd != comment.length ()) {
			String frag = comment.substring (lastEnd, comment.length ());
			if (frag.trim ().length () > 0) {
				parseGLibMessages (ast, frag);
			}
		}
	}

	private void parseGLibMessages (List<ContentNode<T>> ast, String commentFragment) {
		Matcher gm = pGLibMessages.matcher (commentFragment);

		int lastEnd = 0;
		while (gm.find ()) {
			if (lastEnd != gm.start ()) {
				parseParagraphs (ast, commentFragment.substring (lastEnd, gm.start ()));
			}

			ast.add (new ArtefactNode<T> (gm.group (1)));
			lastEnd = gm.end ();
		}

		if (lastEnd != commentFragment.length ()) {
			String frag = commentFragment.substring (lastEnd, commentFragment.length ());
			if (frag.trim ().length () > 0) {
				parseParagraphs (ast, frag);
			}
		}
	}

	private void parseParagraphs (List<ContentNode<T>> ast, String commentFragment) {
		Matcher pm = pPara.matcher (commentFragment);

		int lastEnd = 0;
		while (pm.find ()) {
			if (lastEnd != pm.start ()) {
				int count = StringUtils.countMatches (pm.group (0), "\n") - 1;
				parseParagraph (ast, commentFragment.substring (lastEnd, pm.start ()), count);
			}

			lastEnd = pm.end ();
		}

		if (lastEnd != commentFragment.length ()) {
			String frag = commentFragment.substring (lastEnd, commentFragment.length ());
			if (frag.trim ().length () > 0) {
				parseParagraph (ast, frag, 0);
			}
		}
	}

	private void parseParagraph (List<ContentNode<T>> ast, String para, int paragraphSeparatorSize) {
		if (paragraphIsArtefact (para)) {
			ast.add (new ArtefactNode<T> (para));
			return ;
		}

		Matcher normM = pNorm.matcher (para);
		String paraOut = normM.replaceAll (" ");

		if (paraOut.length () != 0) {
			ast.add (new ParagraphNode<T> (paraOut, para, paragraphSeparatorSize));
		}
	}

	private boolean paragraphIsArtefact (String paragraph) {
		int listElements = 0;
		int spacedLinesTabs = 0;
		int spacedLines = 0;
		int make = 0;
		int breakEnd = 0;
		int bash = 0;

		String[] lines = paragraph.split ("\n");

		if (lines.length > PARA_MAX_NEWLINES) {
			return true;
		}
		
		for (String line : lines) {
			if (line.length () > 0) {
				switch (line.charAt (0)) {
				case '\t':
					spacedLinesTabs++;
					break;

				case ' ':
					spacedLines++;
					break;

				case 'm':
					if (line.startsWith ("make[")) {
						make++;
					}
					break;

				case '$':
					if (line.length () > 1 && line.charAt (1) == ' ') {
						bash++;
					}
					break;
				}

				
				if (pLst.matcher (line).matches ()) {
					listElements++;
				} else if (pCli.matcher (line).matches ()) {
					bash++;
				}
			}

			if (line.endsWith ("\\")) {
				breakEnd++;
			}
		}

		if (breakEnd > 0 || bash > 0 || make > 0) {			
			return true;
		} else if (spacedLines > 0 || spacedLinesTabs > 0) {
			return (listElements == 0);
		}

		return false;
	}

	/*
	public static void main (String[] args) {
		Parser<String> parser = new Parser<String> ();
		CommentNode<String> ast = parser.parse (""
		+ "Review of attachment 272566 [details]:\n"
		+ "\n"
		+ "Hey Richard,\n"
		+ "\n"
		+ "Our great Luca Bruno asked me to review your patch. It looks good so far.\n"
		+ "However, I still think we should add indentation fixes.\n"
		+ "\n"
		+ "\n"
		+ "Every documentation comment line should start with a *. Valadoc catches all\n"
		+ "missing stars. There is no way to break the comment by simply removing leading\n"
		+ "whitespaces/tabs and replacing them with the correct amount of tabs.\n"
		+ "\n"
		+ "::: vala/valacodewriter.vala\n"
		+ "@@ +173,3 @@\n"
		+ "+                    } else {\n"
		+ "+                        Report.warning(comment.source_reference, \"Comment\n"
		+ "describes namespace, that was already described by another comment.\");\n"
		+ "+                        Report.warning(first_reference, \"Previous comment was\n"
		+ "here.\");\n"
		+ "\n"
		+ "The second message should not be a warning. Please use Report.note () instead.\n"

		/*
		+ "Actually, this is incorrect - those were two different arbitrary orders.\n"
		+ "\n"
		+ "(In reply to comment #2)\n"
		+ "> (In reply to comment #1)\n"
		+ "> > It appears to follow the order of the files passed in to the valadoc command;\n"
		+ "> asdasdf\n"
		+ "> > but valadoc should sort them by their final class names.\n"
		+ "> \n"
		+ "> Interestingly, the valadoc native doclet runs into the same problem if the\n"
		+ "> files are passed in explicitly (in an arbitrary order), but not if they're\n"
		+ ">\n"
		+ "> described by \"*.vala\" (which somehow gets expanded in alphanumeric order for\n"
		+ "> the native doclet but not for the other doclets).\n"
		* /
		);
		
		ast.accept (new ContentNodeVisitor<String> () {
			public void visitComment (CommentNode<String> comment) {
				System.out.println ("--------------");
				comment.acceptChildren (this);
			}

			public void visitQuote (QuoteNode<String> quote) {
				System.out.println ("QUOTE");
			}

			public void visitParagraph (ParagraphNode<String> para) {
				System.out.println ("PARA " + para.getParagraphSeparatorSize ());
				System.out.println (" (>)" + para.getContent ());
			}
			
			public void visitArtefactNode (ArtefactNode<String> artefact) {
				System.out.println ("ARTEFACT (" + artefact.getContent ().length () + ")");
			}
		});
	}
	*/
}
