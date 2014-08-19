/* TokenType.java
 *
 * Copyright (C) 2014  Brosch Florian
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
 * 	Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.hurrier.config;


public enum TokenType {
	EOF,			// \0

	ASSIGN,			// =
	PLUS,			// +

	OPEN_BRACE,		// {
	CLOSE_BRACE,	// }
	
	SEMICOLON,		// ;

	ID,				// <id>
	TAB,			// Tab
	PROJECT_VIEW,	// ProjectView
	USER_VIEW,		// UserView
	TEAM_VIEW,		// TeamView
	VAR_NAME,		// VarName

	PIE_CHARTS,		// PieCharts
	PIE_CHART,		// PieCHart
	TREND_CHARTS,	// TrendCharts
	TREND_CHART,	// TrendChart
	SHOW_TOTAL,		// ShowTotal
	DROP_DOWN,		// DropDown
	OPTION_LIST,	// OptionList
	DISTRIBUTION_CHARTS, 		// DistributionCharts
	DISTRIBUTION_CHARTS_OPTION,	// DistributionOption
	FILTER,						// Filter
	ATTRIBUTES,		// Attributes
	ATTRIBUTE,		// Attribute

	NAME,			// Name
	QUERY,			// Query
	DATA_QUERY,		// DataQuery
	
	STRING,			// "<whatever>"
	BOOLEAN,		// true, false
}
