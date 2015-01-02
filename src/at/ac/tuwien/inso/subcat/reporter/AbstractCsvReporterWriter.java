/* AbstractCsvReporterWriter.java
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

package at.ac.tuwien.inso.subcat.reporter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Project;


public abstract class AbstractCsvReporterWriter extends ReportWriter {
	private PrintWriter writer;
	private String csvTypeName;

	private String valueStart;
	private String valueEnd;
	private String valueSeparator;
	private String rowStart;
	private String rowEnd;

	protected AbstractCsvReporterWriter (String csvTypeName, String valueStart, String valueEnd, String valueSeparator, String rowStart, String rowEnd) {
		assert (csvTypeName != null);
		assert (valueStart != null);
		assert (valueEnd != null);
		assert (valueSeparator != null);
		assert (rowStart != null);
		assert (rowEnd != null);
		
		this.csvTypeName = csvTypeName;
		this.valueStart = valueStart;
		this.valueEnd = valueEnd;
		this.valueSeparator = valueSeparator;
		this.rowStart = rowStart;
		this.rowEnd = rowEnd;
	}
	
	@Override
	public String getTypeName () {
		return ".csv";
	}

	@Override
	public String getLabel () {
		return "CSV - " + this.csvTypeName;
	}

	@Override
	public void init (Project project, Settings settings, String outputPath) throws ReporterException {
		try {
			writer = new PrintWriter(outputPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new ReporterException (e);
		} catch (FileNotFoundException e) {
			throw new ReporterException (e);
		}
	}

	@Override
	public void writeHeader (String[] names) throws ReporterException {
		writeSet (names);
	}
		
	@Override
	public void writeSet (String[] data) throws ReporterException {
		writer.append (rowStart);
		boolean isFirst = true;

		for (String value : data) {
			if (!isFirst) {
				writer.append (valueSeparator);
			}
				
			writer.append (valueStart);
			writer.append (escapeValue (value));
			writer.append (valueEnd);
			isFirst = false;
		}
		writer.append (rowEnd);
	}


	@Override
	public void writeFooter (String[] names) throws ReporterException {
	}

	protected String escapeValue (String value) {
		return value;
	}
	
	@Override
	public void cleanup () {
		if (writer != null) {
			writer.close ();
		}
	}
}
