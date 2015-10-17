/* CommitBugInterlinkingTask.java
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

package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.Model;


public class CommitBugInterlinkingTask extends PostProcessorTask {
	private final Set<String> bugKeywords = new HashSet<String> ();

	private final static String regexPara = "\r?\n([ |\t]*\r?\n)+";
	private final static Pattern pPara = Pattern.compile (regexPara);

	private final static String regexTokenNorm = "^[^a-zA-Z0-9]*|[^a-zA-Z0-9]*$";
	private final static Pattern pTokenNorm = Pattern.compile (regexTokenNorm);


	private enum TokenType {
		WORD,
		BUG_KEYWORD,
		NUMERIC,
		LINK
	}

	private static class Token {
		public final TokenType type;
		public final String valueStr;
		public final int valueInt;

		public Token (TokenType type, String value) {
			assert (value != null);
			
			this.type = type;
			this.valueStr = value;
			this.valueInt = -1;
		}

		public Token (TokenType type, int value) {
			assert (value > 0);

			this.type = type;
			this.valueStr = null;
			this.valueInt = value;
		}

		@Override
		public String toString () {
			if (valueStr == null) {
				return type + ":\t'" + valueInt + "'";
			} else {
				return type + ":\t'" + valueStr + "'";
			}
		}
	}

	public CommitBugInterlinkingTask () {
		super (PostProcessorTask.COMMIT|PostProcessorTask.BEGIN|PostProcessorTask.END);

		bugKeywords.add ("bug");
		bugKeywords.add ("bugs");
		bugKeywords.add ("defect");
		bugKeywords.add ("defects");
		bugKeywords.add ("problem");
		bugKeywords.add ("problems");

		bugKeywords.add ("resolve");
		bugKeywords.add ("resolves");
		bugKeywords.add ("resolved");
		bugKeywords.add ("resolving");

		bugKeywords.add ("close");
		bugKeywords.add ("closes");
		bugKeywords.add ("closed");
		bugKeywords.add ("closing");

		bugKeywords.add ("fix");
		bugKeywords.add ("fixes");
		bugKeywords.add ("fixed");
		bugKeywords.add ("fixing");

		bugKeywords.add ("reopen");
		bugKeywords.add ("reopens");
		bugKeywords.add ("reopened");
		bugKeywords.add ("reopening");
		bugKeywords.add ("re-open");
		bugKeywords.add ("re-opens");
		bugKeywords.add ("re-opened");
		bugKeywords.add ("re-opening");
	}

	private Token getTokenType (String _str) {
		String str = pTokenNorm.matcher (_str).replaceAll ("");
		str = str.toLowerCase ();
		
		if (bugKeywords.contains (str)) {
			return new Token (TokenType.BUG_KEYWORD, str);
		}

		final String linkEvidence = "/show_bug.cgi?id=";
		int pos = str.indexOf (linkEvidence);
		if (pos >= 0) {
			int idStart = pos + linkEvidence.length ();
			int idEnd = idStart;
			while (idEnd < str.length () && Character.isDigit (str.charAt (idEnd))) {
				idEnd++;
			}

			try {
				int numeric = Integer.parseInt (str.substring (idStart, idEnd));
				return new Token (TokenType.LINK, numeric);
			} catch (NumberFormatException e) {
				return new Token (TokenType.WORD, _str);
			}
		}

		try {
			if (str.length () >= 4 && str.length () <= 6) {
				int numeric = Integer.parseInt (str);
				return new Token (TokenType.NUMERIC, numeric);
			}
		} catch (NumberFormatException e) {
			// Word
		}

		return new Token (TokenType.WORD, _str);
	}

	private void semanticLvl (Commit commit, Set<Integer> added, int bugId, int certainty, Model model) throws SQLException {
		assert (added != null);
		assert (certainty >= 0);
		assert (bugId > 0);
		
		if (certainty <= 1) {
			// Not enough evidence
			return ;
		}

		if (added.contains (bugId)) {
			// Already registered
			return ;
		}

		Bug bug = model.getBug (commit.getProject (), bugId);
		if (bug != null) {
			model.addBugfixCommit (commit, bug);
			added.add (bugId);
		}
	}

	private void processParagraph (Commit commit, Set<Integer> added, String content, Model model) throws SQLException {
		assert (content != null);
		assert (added != null);
		
		StringTokenizer st = new StringTokenizer (content);
		int wordScore = 0;

		while (st.hasMoreTokens ()) {
			Token token = getTokenType (st.nextToken ());
	
			switch (token.type) {
			case LINK:
				semanticLvl (commit, added, token.valueInt, Integer.MAX_VALUE, model);
				break;
	
			case BUG_KEYWORD:
				wordScore++;
				break;
	
			case NUMERIC:
				semanticLvl (commit, added, token.valueInt, wordScore + 1, model);
				break;
	
			case WORD:
				// Do nothing
				break;
			}
		}
	}
	
	private void processCommitMessage (Commit commit, Model model) throws SQLException {
		assert (commit != null);

		Set<Integer> added = new HashSet<Integer> ();
		for (String para : pPara.split (commit.getTitle ())) {
			this.processParagraph (commit, added, para, model);
		}
	}

	

	@Override
	public void begin (PostProcessor processor) throws PostProcessorException {
		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();
			model.removeBugfixCommits (processor.getProject ());
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		} finally {
			if (model != null) {
				model.close ();
			}
		}
	}
	
	@Override
	public void commit (PostProcessor processor, Commit commit) throws PostProcessorException {
		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();			
			processCommitMessage (commit, model);
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		} finally {
			if (model != null) {
				model.close ();
			}
		}
	}
	
	@Override
	public void end (PostProcessor processor) throws PostProcessorException {
	}
	
	@Override
	public String getName () {
		return "commit-bug-interlinking";
	}
}
