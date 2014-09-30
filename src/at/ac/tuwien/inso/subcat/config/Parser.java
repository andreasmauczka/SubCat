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

package at.ac.tuwien.inso.subcat.config;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


public class Parser {
	private final int BUFFER_SIZE = 32;

	private Scanner scanner;

	private Token[] tokens;
	private int index;
	private int size;


	public Parser () {
		tokens = new Token[BUFFER_SIZE];
	}

	public void parse (Configuration config, File file, byte[] content) throws IOException, ParserException {
		assert (config != null);
		assert (file != null);
		
		scanner = new Scanner (file, content);
		index = -1;
		size = 0;

		nextToken ();

	
		while (getCurrent ().getType () != TokenType.EOF) {
			parseRoot (config);
		}
	}

	public void parse (Configuration config, File file) throws IOException, ParserException {
		parse (config, file, null);
	}


	//
	// Rules:
	//

	private void parseRoot (Configuration config) throws ParserException {
		assert (config != null);

		Token current = getCurrent ();

		switch (current.getType ()) {
		case REPORTER:
			parseReporter (config);
			break;

		case TEAM_VIEW:
			TeamViewConfig teamView = new TeamViewConfig (current.getStart (), current.getEnd ());
			config.setTeamViewConfig (teamView);
			parseView (config, TokenType.TEAM_VIEW, teamView);
			break;

		case USER_VIEW:
			UserViewConfig userView = new UserViewConfig (current.getStart (), current.getEnd ());
			config.setUserViewConfig (userView);
			parseView (config, TokenType.USER_VIEW, userView);
			break;

		case PROJECT_VIEW:
			ProjectViewConfig projView = new ProjectViewConfig (current.getStart (), current.getEnd ());
			config.setProjectViewConfig (projView);
			parseView (config, TokenType.PROJECT_VIEW, projView);
			break;

		default:
			error ("unexpected token " + current.getType () + ", expected: "
				+ TokenType.TEAM_VIEW
				+ "|" + TokenType.USER_VIEW 
				+ "|" + TokenType.PROJECT_VIEW
				+ "|" + TokenType.REPORTER);
			break;
		}
		
		expect (TokenType.SEMICOLON);
	}

	private void parseReporter (Configuration config) throws ParserException {
		assert (config != null);

		List<Requires> requires;
		String name;
		Query query;

		Token current = getCurrent ();
		expect (TokenType.REPORTER);

		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		requires = parseRequires (); 
		name = parseName ();
		query = parseQuery ();

		ExporterConfig expConf = new ExporterConfig (name, query, current.getStart (), current.getEnd ());
		expConf.setRequirements (requires);
		if (!config.addExporterConfig (expConf)) {
			throw new ParserException ("Exporter `" + expConf.getName () + "' already defined.", current.getStart (), current.getEnd ());
		}
		
		expect (TokenType.CLOSE_BRACE);
	}
	
	private void parseView (Configuration config, TokenType tabTokenType, ViewConfig view) throws ParserException {
		assert (config != null);
		assert (tabTokenType != null);
		assert (view != null);

		expect (tabTokenType);

		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		while (!accept (TokenType.CLOSE_BRACE)) {
			parseChart (view);
		}
	}
	
	private void parseChart (ViewConfig view) throws ParserException {
		assert (view != null);

		Token current = getCurrent ();
		
		switch (current.getType ()) {
		case PIE_CHARTS:
			parsePieCharts (view);
			break;
			
		case TREND_CHARTS:
			parseTrendCharts (view);
			break;

		case DISTRIBUTION_CHARTS:
			parseDistributionCharts (view);
			break;

		default:
			error ("unexpected token " + current.getType () + ", expected: "
					+ TokenType.PIE_CHARTS + "|"
					+ TokenType.TREND_CHART + "|"
					+ TokenType.DISTRIBUTION_CHARTS);
			break;
		}

	}

	private void parseDistributionCharts (ViewConfig view) throws ParserException {
		assert (view != null);

		String name;
		List<Requires> requires;

		Token current = getCurrent ();
		expect (TokenType.DISTRIBUTION_CHARTS);
		
		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		requires = parseRequires ();
		name = parseName ();
		DistributionChartConfig distributionChart = new DistributionChartConfig (name, current.getStart (), current.getEnd ());
		view.addChart (distributionChart);
		distributionChart.setRequirements (requires);
		
		while (!accept (TokenType.CLOSE_BRACE)) {
			parseDistributionOption (distributionChart);
		}		

		expect (TokenType.SEMICOLON);
	}
	
	private void parseDistributionOption (DistributionChartConfig distributionChart) throws ParserException {
		assert (distributionChart != null);

		List<Requires> requires;
		String name;

		Token current = getCurrent ();
		expect (TokenType.DISTRIBUTION_CHARTS_OPTION);
		
		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		requires = parseRequires ();
		name = parseName ();
		DistributionChartOptionConfig optionConfig = new DistributionChartOptionConfig (name, current.getStart (), current.getEnd ());
		optionConfig.setRequirements (requires);
		distributionChart.addOption (optionConfig);

		while (getCurrentType () == TokenType.FILTER) {
			parseDistributionFilter (optionConfig);
		}

		parseDistributionAttributes (optionConfig);
		
		expect (TokenType.CLOSE_BRACE);		
		expect (TokenType.SEMICOLON);		
	}

	private void parseDistributionFilter (DistributionChartOptionConfig option) throws ParserException {
		assert (option != null);

		DropDownConfig filter = parseDropDownTemplate (TokenType.FILTER, false);
		option.addFilter (filter);
	}

	private void parseDistributionAttributes (DistributionChartOptionConfig option) throws ParserException {
		assert (option != null);

		Token current = getCurrent ();
		expect (TokenType.ATTRIBUTES);

		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		DistributionAttributesConfig att = new DistributionAttributesConfig (current.getStart (), current.getEnd ());
		option.setAttributes (att);
		
		while (!accept (TokenType.CLOSE_BRACE)) {
			parseDistributionAttribute (att);
		}

		expect (TokenType.SEMICOLON);		
	}

	private void parseDistributionAttribute (DistributionAttributesConfig option) throws ParserException {
		assert (option != null);

		List<Requires> requires;
		String name;
		Query query;

		Token current = getCurrent ();
		expect (TokenType.ATTRIBUTE);
		
		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		requires = parseRequires ();
		name = parseName ();
		query = parseQuery ();

		DistributionAttributeConfig conf = new DistributionAttributeConfig (name, query, current.getStart (), current.getEnd ());
		conf.setRequirements (requires);
		option.add (conf);

		expect (TokenType.CLOSE_BRACE);		
		expect (TokenType.SEMICOLON);		
	}
	
	private void parsePieCharts (ViewConfig view) throws ParserException {
		assert (view != null);

		List<Requires> requires;
		String name;

		Token current = getCurrent ();
		expect (TokenType.PIE_CHARTS);

		expect (TokenType.ASSIGN);
		expect (TokenType.OPEN_BRACE);

		requires = parseRequires ();
		name = parseName ();
		PieChartGroupConfig chartGroup = new PieChartGroupConfig (name, current.getStart (), current.getEnd ());
		chartGroup.setRequirements (requires);
		view.addChart(chartGroup);

		while (!accept (TokenType.CLOSE_BRACE)) {
			parsePieChart (chartGroup);
		}		

		expect (TokenType.SEMICOLON);
	}
	
	private void parsePieChart (PieChartGroupConfig chartGroup) throws ParserException {
		assert (chartGroup != null);

		List<Requires> requiers;
		String name;
		Query query;
		boolean showTotal;
		
		Token current = getCurrent ();
		expect (TokenType.PIE_CHART);
		expect (TokenType.ASSIGN);

		expect (TokenType.OPEN_BRACE);
		requiers = parseRequires ();
		name = parseName ();
		query = parseQuery ();

		expect (TokenType.SHOW_TOTAL);
		expect (TokenType.ASSIGN);
		showTotal = parseBoolean ();
		expect (TokenType.SEMICOLON);

		expect (TokenType.CLOSE_BRACE);

		
		expect (TokenType.SEMICOLON);

		PieChartConfig chart = new PieChartConfig (name, query, showTotal, current.getStart (), current.getEnd ());
		chart.setRequirements (requiers);
		chartGroup.addChart (chart);
	}

	private void parseTrendCharts (ViewConfig view) throws ParserException {
		assert (view != null);

		List<Requires> requires;
		String name;

		Token current = getCurrent ();
		expect (TokenType.TREND_CHARTS);
		expect (TokenType.ASSIGN);

		expect (TokenType.OPEN_BRACE);
		requires = parseRequires ();
		name = parseName ();

		TrendChartGroupConfig chart = new TrendChartGroupConfig (name, current.getStart (), current.getEnd ());
		chart.setRequirements (requires);
		view.addChart (chart);

		while (!accept (TokenType.CLOSE_BRACE)) {
			parseTrendChart (chart);
		}

		expect (TokenType.SEMICOLON);
	}

	private void parseTrendChart (TrendChartGroupConfig parent) throws ParserException {
		assert (parent != null);

		List<Requires> requires;
		String name;

		Token current = getCurrent ();
		expect (TokenType.TREND_CHART);
		expect (TokenType.ASSIGN);

		expect (TokenType.OPEN_BRACE);
		requires = parseRequires ();
		name = parseName ();

		TrendChartConfig chart = new TrendChartConfig (name, current.getStart (), current.getEnd ());
		chart.setRequirements (requires);
		parent.addChart (chart);

		current = getCurrent ();
		do {
			parseTrendChartPlotConfig (chart);
			current = getCurrent ();
		} while (current.getType () == TokenType.DROP_DOWN);

		parseOptionList (chart);

		expect (TokenType.CLOSE_BRACE);
		expect (TokenType.SEMICOLON);		
	}

	private void parseOptionList (OptionListOwner parent) throws ParserException {
		assert (parent != null);

		OptionListConfig optionList = parseGenericOptionList (TokenType.OPTION_LIST);
		parent.setOptionList (optionList);
	}

	private OptionListConfig parseGenericOptionList (TokenType startToken) throws ParserException {
		assert (startToken != null);

		String varName;
		Query query;

		Token current = getCurrent ();
		expect (startToken);
		expect (TokenType.ASSIGN);

		expect (TokenType.OPEN_BRACE);
		varName = parseVarName ();
		query = parseQuery ();

		expect (TokenType.CLOSE_BRACE);
		expect (TokenType.SEMICOLON);

		OptionListConfig optionList = new OptionListConfig (varName, query, current.getStart (), current.getEnd ());
		return optionList;
	}
	
	private void parseTrendChartPlotConfig (TrendChartConfig parent) throws ParserException {
		assert (parent != null);

		TrendChartPlotConfig config = (TrendChartPlotConfig) parseDropDownTemplate (TokenType.DROP_DOWN, true);
		parent.addDropDown (config);
	}
	
	private DropDownConfig parseDropDownTemplate (TokenType startToken, boolean isTrendChartPlotConfig) throws ParserException {
		assert (startToken != null);

		List<Requires> requires;
		String varName;
		Query query;
		Query dataQuery;

		Token current = getCurrent ();
		expect (startToken);
		expect (TokenType.ASSIGN);

		expect (TokenType.OPEN_BRACE);
		requires = parseRequires ();
		varName = parseVarName ();
		query = parseQuery ();

		DropDownConfig dropDown;
		if (isTrendChartPlotConfig == true) {
			dataQuery = parseQuery (TokenType.DATA_QUERY);
			dropDown = new TrendChartPlotConfig (varName, query, dataQuery, current.getStart (), current.getEnd ());
		} else {
			dropDown = new DropDownConfig (varName, query, current.getStart (), current.getEnd ());
		}
		dropDown.setRequirements (requires);

		expect (TokenType.CLOSE_BRACE);
		expect (TokenType.SEMICOLON);
		
		return dropDown;
	}
	
	private Query parseQuery () throws ParserException {
		return parseQuery (TokenType.QUERY);
	}
	
	private Query parseQuery (TokenType queryToken) throws ParserException {
		assert (queryToken != null);

		Query query;
		
		
		Token current = getCurrent ();
		query = new Query (current.getStart (), current.getEnd ());
		
		expect (queryToken);
		expect (TokenType.ASSIGN);

		do {
			current = getCurrent ();
			switch (current.getType ()) {
			case STRING:
				query.appendString (current.getValue (), current.getStart (), current.getEnd ());
				nextToken ();
				break;

			case ID:
				query.appendVariable (current.getValue (), current.getStart (), current.getEnd ());
				nextToken ();
				break;

			default:
				error ("expected " + TokenType.STRING + " or " + TokenType.ID);
				break;
			}
		} while (accept (TokenType.PLUS));

		expect (TokenType.SEMICOLON);
		return query;
	}
	
	private String parseName () throws ParserException {
		expect (TokenType.NAME);
		expect (TokenType.ASSIGN);
		String str = parseString ();
		expect (TokenType.SEMICOLON);
		return str;
	}
	
	private String parseVarName () throws ParserException {
		expect (TokenType.VAR_NAME);
		expect (TokenType.ASSIGN);
		String str = parseId ();
		expect (TokenType.SEMICOLON);
		return str;
	}

	private String parseString () throws ParserException {
		Token token = getCurrent ();
		expect (TokenType.STRING);
		return token.getValue ();
	}

	private String parseId () throws ParserException {
		Token token = getCurrent ();
		expect (TokenType.ID);
		return token.getValue ();
	}

	private boolean parseBoolean () throws ParserException {
		Token token = getCurrent ();
		expect (TokenType.BOOLEAN);
		return token.getValue ().equals ("true");
	}

	private List<Requires> parseRequires () throws ParserException {
		LinkedList<Requires> requires = new LinkedList<Requires> ();
		Token token = getCurrent ();

		while (accept (TokenType.REQUIRES)) {
			HashSet<String> flags = new HashSet<String> ();

			expect (TokenType.OPEN_PARENTHESIS);
			do {
				String flag = parseId ();
				flags.add (flag);
			} while (accept (TokenType.BAR));
			expect (TokenType.CLOSE_PARENTHESIS);
			Requires requirement = new Requires (token.getStart (), getCurrent ().getEnd ());
			requirement.setRequirements (flags);
			requires.add (requirement);
			expect (TokenType.SEMICOLON);
		}

		return requires;
	}


	//
	// Helper:
	//

	private boolean nextToken () throws ParserException {
		try {
			index = (index + 1) % BUFFER_SIZE;
			size--;
	
			if (size <= 0) {
				Token token = scanner.nextToken ();
				tokens[index] = token;
				size = 1;
			}
	
			return (tokens[index].getType() != TokenType.EOF);
		} catch (ScannerException e) {
			throw new ParserException (e);
		}
	}

	private TokenType getCurrentType () {
		return tokens[index].getType ();
	}

	private Token getCurrent () {
		return tokens[index];
	}

	private boolean accept (TokenType type) throws ParserException {
		assert (type != null);

		if (getCurrentType () == type) {
			nextToken ();
			return true;
		}
		return false;
	}

	private boolean expect (TokenType type) throws ParserException {
		assert (type != null);

		if (accept (type)) {
			return true;
		}

		error ("expected " + type.toString() + ", got " + getCurrentType ().toString ());
		assert (false);
		return false;
	}

	private void error (String msg) throws ParserException {
		assert (msg != null);

		Token curr = getCurrent ();
		throw new ParserException (
			curr.getStart() + "-" + curr.getEnd() + ": " +
			"syntax error: " + msg, curr.getStart (), curr.getEnd ());
	}



	//
	// Test Method:
	//
	
	/*
	public static void main (String[] args) {
		Configuration config = new Configuration ();
		Parser parser = new Parser ();
		String content = "\n"
				+ "ProjectView = { \n"
				+ " DistributionCharts = {\n"
				+ "  Name = \"Distributions\";\n"
				+ "  DistributionOption = {\n"
				+ "	  Name = \"Bugs\";\n"
				+ "	  Filter = {\n"
				+ "    VarName = id;\n"
				+ "	   Query = \"\";\n"
				+ "	  };\n"
				+ "   Filter = {\n"
				+ "    VarName = id;\n"
				+ "    Query = \"\";\n"
				+ "	  };\n"
				+ "   Attributes = {\n"
				+ "    VarName = id;\n"
				+ "    Query = \"\";\n"
				+ "   };\n"
				+ "	 };\n"
				+ "	};\n"
				+ "};\n"
				+ "\n";

		try {
			parser.parse (config, new File ("/foo/bar/baz"), content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			System.out.println(e.getStart() + "." + e.getEnd());
			e.printStackTrace();
		}	
	}
	*/
}