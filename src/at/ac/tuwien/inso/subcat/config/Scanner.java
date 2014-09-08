/* Scanner.java
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

package at.ac.tuwien.inso.subcat.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class Scanner {
	private File file;
	
	private byte[] str;
	private int line;
	private int column;

	private int lineStart;
	private int pos;
	
	
	public Scanner (File file) throws IOException {
		this (file, null);
	}
	
	public Scanner (File file, byte[] content) throws IOException {
		assert (file != null);
		
		if (content == null) {
			this.str = readFile (file);
		} else {
			this.str = content;
		}
		this.file = file;
		
		line = 0;
		column = 0;

		lineStart = 0;
		pos = 0;
	}

	public Token nextToken () throws ScannerException {
		skipSpace ();
		
		SourcePos start = getCurrentPos (0);

		if (pos >= str.length) {
			return new Token (TokenType.EOF, start, start, "");
		}

		// IDs & Keywords:
		if ((str[pos] >= 'a' && str[pos] <= 'z') 
				|| (str[pos] >= 'A' && str[pos] <= 'Z')
				|| str[pos] == '_') {

			return nextIdOrKeyword (start);
		}

		switch (str[pos]) {
		case '=':
			column++;
			pos++;
			
			return new Token (TokenType.ASSIGN, start, getCurrentPos (-1), "=");

		case '+':
			column++;
			pos++;
			
			return new Token (TokenType.PLUS, start, getCurrentPos (-1), "+");

		// Brackets:
		case '{':
			column++;
			pos++;
			return new Token (TokenType.OPEN_BRACE, start, getCurrentPos (-1), "{");

		case '}':
			column++;
			pos++;
			return new Token (TokenType.CLOSE_BRACE, start, getCurrentPos (-1), "}");

			
		// Separators:
		case ';':
			column++;
			pos++;
			return new Token (TokenType.SEMICOLON, start, getCurrentPos (-1), ";");


		// Literals:
		case '"':
			column++;
			pos++;

			int startIndex = pos;
			
			while (pos < str.length - 1 && str[pos] != '"') {
				if (str[pos] == '\n') {
					line++;
					column = 0;
					lineStart = pos + 1;
				}
				column++; // TODO: UTF8
				pos++;
			}

			if (pos == str.length - 1) {
				throw new ScannerException ("syntax error: Missing \"", start, start);
			}

			byte[] idArr = Arrays.copyOfRange (str, startIndex, pos);
			String idStr = new String (idArr);
			
			column++;
			pos++;
			
			return new Token (TokenType.STRING, start, getCurrentPos (-1), idStr);
		}
			
		
		// Unexpeced Chars:
		throw new ScannerException ("lexical error: Unexpected character", start, start);
	}

	private Token nextIdOrKeyword (SourcePos start) {
		int startIndex = pos;
		
		// Checked in nextToken ()
		column++;
		pos++;

		while ((str[pos] >= 'a' && str[pos] <= 'z') 
				|| (str[pos] >= 'A' && str[pos] <= 'Z')
				|| (str[pos] >= '0' && str[pos] <= '9')
				|| str[pos] == '_') {

			column++;
			pos++;
		}

		byte[] idArr = Arrays.copyOfRange (str, startIndex, pos);
		String idStr = new String (idArr);

		if (idStr.equals("DistributionCharts")) {
			return new Token (TokenType.DISTRIBUTION_CHARTS, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals("DistributionOption")) {
			return new Token (TokenType.DISTRIBUTION_CHARTS_OPTION, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals("Filter")) {
			return new Token (TokenType.FILTER, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals("Attributes")) {
			return new Token (TokenType.ATTRIBUTES, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals("Attribute")) {
			return new Token (TokenType.ATTRIBUTE, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("TrendCharts")) {
			return new Token (TokenType.TREND_CHARTS, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("TrendChart")) {
			return new Token (TokenType.TREND_CHART, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("DropDown")) {
			return new Token (TokenType.DROP_DOWN, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("OptionList")) {
			return new Token (TokenType.OPTION_LIST, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("PieCharts")) {
			return new Token (TokenType.PIE_CHARTS, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("PieChart")) {
			return new Token (TokenType.PIE_CHART, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("ShowTotal")) {
			return new Token (TokenType.SHOW_TOTAL, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("Query")) {
			return new Token (TokenType.QUERY, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("DataQuery")) {
			return new Token (TokenType.DATA_QUERY, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("VarName")) {
			return new Token (TokenType.VAR_NAME, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("Name")) {
			return new Token (TokenType.NAME, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("ProjectView")) {
			return new Token (TokenType.PROJECT_VIEW, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("UserView")) {
			return new Token (TokenType.USER_VIEW, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("TeamView")) {
			return new Token (TokenType.TEAM_VIEW, start, getCurrentPos (-1), idStr);
		}
		if (idStr.equals ("true") || idStr.equals ("false")) {
			return new Token (TokenType.BOOLEAN, start, getCurrentPos (-1), idStr);
		}
		
		return new Token (TokenType.ID, start, getCurrentPos (-1), idStr);
	}

	
	//
	// Space & Comments:
	//
	
	private void skipSpace () throws ScannerException {
		while (skipWhitespace () || skipComment ()) {
		}
	}
	
	private boolean skipWhitespace () {
		boolean hasWhitespace = false;

		while (pos < str.length) {
			switch (str[pos]) {
			case ' ':
			case '\t':
				hasWhitespace = true;
				column++;
				pos++;
				break;

			case '\n':
				hasWhitespace = true;
				column = 1;
				line++;
				pos++;
				lineStart = pos;
				break;

			default:
				return hasWhitespace;
			}
		}

		return hasWhitespace;
	}

	private boolean skipComment () throws ScannerException {
		if (pos > str.length - 2 || str[pos] != '/') {
			return false;
		}

		// Single line comment:
		if (str[pos + 1] == '/') {
			column += 2;
			pos += 2;

			while (str[pos] != '\n') {
				pos++;
			}
			
			return true;
		}

		// Multiline Comment:
		if (str[pos + 1] == '*') {
			SourcePos start = getCurrentPos (0);

			column += 2;
			pos += 2;

			while (pos < str.length - 1 && (str[pos] != '*' || str[pos + 1] != '/')) {
				if (str[pos] == '\n') {
					line++;
					column = 0;
					lineStart = pos + 1;
				}
				column++; // TODO: UTF8
				pos++;
			}
			
			if (pos == str.length - 1) {
				throw new ScannerException ("syntax error: Missing */", start,
						new SourcePos (start.getFile (), start.getLine (), start.getLineStart(), start.getColumn ())
					);
			}
			
			column += 2;
			pos += 2;
			
			return true;
		}

		return false;
	}
	

	//
	// Helper:
	//
	
	private SourcePos getCurrentPos (int colOffset) {
		return new SourcePos (file, line, lineStart, column + colOffset);
	}

	private static byte[] readFile (File file) throws IOException {
		byte[] buffer = new byte[(int) file.length()];

		InputStream ios = new FileInputStream (file);
		if (ios.read(buffer) == -1) {
			ios.close ();
			throw new IOException("Unexpected EOF while reading the whole file");
		}

		ios.close ();

		return buffer;
	}

	
	//
	// Test Method:
	//

	/*
	public static void main (String[] args) {
		try {
			Scanner scanner = new Scanner (new File ("/dummy/file"), ("\n\n"
					+ "  \"asdf\"  ;\n"
					+ "").getBytes());

			Token token = null;
			while ((token = scanner.nextToken()).getType() != TokenType.EOF) {
				System.out.println (token.toString ());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ScannerException e) {
			e.printStackTrace();
		}
	}
	*/
}
