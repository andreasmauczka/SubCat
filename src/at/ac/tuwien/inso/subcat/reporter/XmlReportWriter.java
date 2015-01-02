/* XmlReporterWriter.java
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

import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Project;


public class XmlReportWriter extends ReportWriter {
	private XMLStreamWriter writer;
	private String[] names;

	@Override
	public String getTypeName () {
		return ".xml";
	}

	@Override
	public String getLabel () {
		return "XML";
	}

	@Override
	public void init (Project project, Settings settings, String outputPath) throws ReporterException {
		try {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			writer = factory.createXMLStreamWriter(new FileWriter(outputPath));
		} catch (XMLStreamException e) {
			throw new ReporterException (e);
		} catch (IOException e) {
			throw new ReporterException (e);
		}
	}

	@Override
	public void writeHeader (String[] names) throws ReporterException {
		assert (names != null);

		try {
			writer.writeStartDocument ();
			writer.writeStartElement ("report");
		} catch (XMLStreamException e) {
			throw new ReporterException (e);
		}
	}

	@Override
	public void writeSet (String[] data) throws ReporterException {
		assert (names != null && this.names != null);
		assert (names.length == data.length);
		
		try {
			writer.writeStartElement ("column");
			for (int i = 0; i < data.length; i++) {
				String text = data[i];
				String label = names[i];
	
				writer.writeStartElement ("entry");
				writer.writeAttribute ("column", label);
				writer.writeCharacters (text);
				writer.writeEndElement ();
			}
			writer.writeEndElement ();
		} catch (XMLStreamException e) {
			throw new ReporterException (e);
		}
	}

	@Override
	public void writeFooter (String[] names) throws ReporterException {
		assert (names != null && this.names != null);

		try {
			writer.writeEndElement ();
			writer.writeEndDocument ();
		} catch (XMLStreamException e) {
			throw new ReporterException (e);
		}
	}

	
	@Override
	public void cleanup () throws ReporterException {
		try {
			if (writer != null) {
					writer.close ();
			}
		} catch (XMLStreamException e) {
			throw new ReporterException (e);
		}
	}
}
