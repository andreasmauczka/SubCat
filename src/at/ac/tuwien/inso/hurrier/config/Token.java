/* Token.java
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


public class Token {
	private TokenType type;
	private SourcePos start;
	private SourcePos end;
	private String value;
	
	public Token (TokenType type, SourcePos start, SourcePos end, String value) {
		assert (type != null); // TODO: Möglich?
		assert (start != null);
		assert (end != null);
		assert (value != null);
		assert (start.getFile () == end.getFile ());
		
		this.type = type;
		this.start = start;
		this.end = end;
		this.value = value;
	}

	public TokenType getType () {
		return type;
	}

	public SourcePos getStart () {
		return start;
	}

	public SourcePos getEnd () {
		return end;
	}

	public String getValue () {
		return value;
	}

	@Override
	public String toString () {
		return start.getFile () + ": " + start.toString () + "-"
				+ end.toString () + ": Token: " + value;
	}
}
