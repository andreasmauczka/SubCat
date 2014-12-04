/* ParagraphNode.java
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


public class ParagraphNode<T> extends ContentNode<T> {
	private int paragraphSeparatorSize;
	private String content;

	public ParagraphNode (String content, int paragraphSeparatorSize) {
		assert (content != null);
		assert (paragraphSeparatorSize >= 0);

		this.content = content;
		this.paragraphSeparatorSize = paragraphSeparatorSize;
	}

	@Override
	public void accept (ContentNodeVisitor<T> visitor) {
		assert (visitor != null);

		visitor.visitParagraph (this);
	}

	public String getContent () {
		return content;
	}
	
	public int getParagraphSeparatorSize () {
		return paragraphSeparatorSize;
	}
}
