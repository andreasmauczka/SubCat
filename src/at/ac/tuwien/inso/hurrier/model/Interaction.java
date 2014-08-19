/* Interaction.java
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

package at.ac.tuwien.inso.hurrier.model;

import java.util.Date;


public class Interaction {
	private Integer id;
	private User from;
	private User to;
	private int quotes;
	private boolean closed;
	private float pos;
	private float neg;
	private Date date;
	
	public Interaction (Integer id, User from, User to, boolean closed,
			int quotes, float pos, float neg, Date date) {
		assert (from != to);
		assert (from != null);
		assert (to != null);
		assert (date != null);
		assert (quotes >= 0);

		this.id = id;
		this.from = from;
		this.to = to;
		this.quotes = quotes;
		this.pos = pos;
		this.neg = neg;
		this.date = date;
		this.closed = closed;
	}

	public User getFrom () {
		return from;
	}

	public void setFrom (User from) {
		assert (from != to);
		assert (from != null);

		this.from = from;
	}

	public User getTo () {
		return to;
	}

	public void setTo (User to) {
		assert (from != to);
		assert (to != null);

		this.to = to;
	}

	public float getPos () {
		return pos;
	}

	public void setPos (float pos) {
		this.pos = pos;
	}

	public float getNeg () {
		return neg;
	}

	public void setNeg (float neg) {
		this.neg = neg;
	}

	public void setQuotes (int quotes) {
		assert (quotes >= 0);

		this.quotes = quotes;
	}

	public int getQuotes() {
		return quotes;
	}

	public void setDate (Date date) {
		assert (date != null);

		this.date = date;
	}

	public Date getDate () {
		return date;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Integer getId () {
		return id;
	}

	public void setClosed (boolean closed) {
		this.closed = closed;
	}

	public boolean isClosed () {
		return closed;
	}

	@Override
	public String toString () {
		return "Interaction [id=" + id + " from=" + from + ", to=" + to + ", quotes=" + quotes
				+ ", pos=" + pos + ", neg=" + neg + ", date=" + date + " closed=" + closed + "]";
	}

	public boolean equals (Interaction obj) {
		if (id == null || obj.id == null) {
			return this == obj;
		}
		
		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Interaction) {
			return equals ((Interaction) obj);
		}
		
		return super.equals (obj);
	}
}
