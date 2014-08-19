/* SourcePos.java
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

import java.io.File;


public class SourcePos {
	private File file;
	private int line;
	private int column;
	private int lineStart;

	public SourcePos (File file, int line, int lineStart, int column) {
		assert (file != null);

		this.file = file;
		this.line = line;
		this.lineStart = line;
		this.column = column;
	}
	
	public File getFile () {
		return file;
	}

	public int getLine () {
		return line;
	}

	public int getLineStart () {
		return lineStart;
	}

	public int getColumn () {
		return column;
	}

	@Override
	public String toString () {
		return line + ":" + column;
	}
}
