/* Query.java
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class Query extends ConfigNode {
	private static abstract class Segment extends ConfigNode {

		public Segment (SourcePos start, SourcePos end) {
			super (start, end);
		}

		// TODO: rename to buildQuery
		public abstract String queryBuilder (Set<String> vars, List<VariableSegment> variables) throws SemanticException;
	}

	public static class StringSegment extends Segment {
		private String segment;

		public StringSegment (String segment, SourcePos start, SourcePos end) {
			super (start, end);
			assert (segment != null);
			
			this.segment = segment;
		}

		public String queryBuilder (Set<String> vars, List<VariableSegment> variables) throws SemanticException {
			assert (vars != null);

			return segment;
		}

		@Override
		public void accept (ConfigVisitor visitor) {
			assert (visitor != null);

			visitor.visitStringSegment (this);
		}

		@Override
		public void acceptChildren (ConfigVisitor visitor) {
			super.acceptChildren (visitor);
		}
	}

	public static class VariableSegment extends Segment {
		private String varName;
		
		public VariableSegment (String varName, SourcePos start, SourcePos end) {
			super(start, end);
			assert (varName != null);
			
			this.varName = varName;
		}

		public String queryBuilder (Set<String> vars, List<VariableSegment> variables) throws SemanticException {
			assert (vars != null);

			if (!vars.contains (varName)) {
				throw new SemanticException ("error: could not resolve `" + varName + "'", getStart (), getEnd ());
			}

			variables.add (this);
			
			return " ? ";
		}

		public String getName () {
			return varName;
		}

		@Override
		public void accept (ConfigVisitor visitor) {
			visitor.visitVariableSegment (this);
		}

		@Override
		public void acceptChildren (ConfigVisitor visitor) {
			super.acceptChildren (visitor);
		}
	}


	private List<Segment> segments;
	
	public Query(SourcePos start, SourcePos end) {
		super (start, end);

		segments = new LinkedList<Segment> ();
	}

	public void appendString (String str, SourcePos start, SourcePos end) {
		Segment seg = new StringSegment (str, start, end);
		segments.add (seg);
	}

	public void appendVariable (String str, SourcePos start, SourcePos end) {
		Segment seg = new VariableSegment (str, start, end);
		segments.add (seg);
	}

	public String getQuery (Set<String> vars, List<VariableSegment> variables) throws SemanticException {
		assert (vars != null);
		
		StringBuilder builder = new StringBuilder ();
		
		for (Segment seg : segments) {
			builder.append (seg.queryBuilder (vars, variables));
		}

		return builder.toString ();
	}

	public List<Segment> getSegments () {
		return segments;
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitQuery (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);

		for (Segment seg : segments) {
			seg.accept (visitor);
		}
	}
}
