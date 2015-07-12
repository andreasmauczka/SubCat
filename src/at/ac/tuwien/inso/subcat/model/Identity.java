/* Identity.java
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

package at.ac.tuwien.inso.subcat.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Identity {
	private Pattern patternMailValidator = Pattern.compile ("^([_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*)@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
	private Pattern patternNameFragValidator = Pattern.compile("^[^a-z]*([a-z]*)[^a-z]*$");
	private Pattern patternNameSplitter = Pattern.compile ("[ |\t|\\.|\\-|_|,|:]+");

	private Integer id;
	private Integer identifier;
	private String context;
	private String mail;
	private String name;
	private User user;

	public Identity (Integer id, Integer identifier, String context, String mail, String name, User user) {
		assert (name != null);
		assert (user != null);
		assert (context != null);

		this.id = id;
		this.identifier = identifier;
		this.mail = mail;
		this.name = name;
		this.user = user;
		this.context = context;
	}

	public Integer getId () {
		return id;
	}

	public void setId (Integer id) {
		this.id = id;
	}

	public Integer getIdentifier () {
		return identifier;
	}
	
	public String getMail () {
		return mail;
	}

	public void setMail (String mail) {
		assert (mail != null);

		this.mail = mail;
	}

	public String getName () {
		return name;
	}

	public Set<String> getNameFragments () {
		return getNameFragments (true);
	}

	public Set<String> getNameFragments (boolean includeMail) {
		HashSet<String> set = new HashSet<String> ();
		if (name != null) {
			String[] fragments = patternNameSplitter.split (name.toLowerCase ());
			for (String fragment : fragments) {
				Matcher mm = patternMailValidator.matcher (fragment);
				if (mm.matches ()) {
					if (includeMail) {
						String[] localFragments = patternNameSplitter.split (mm.group (1));
						addAllNameFragments (set, localFragments);
					}
				} else {
					addAllNameFragment (set, fragment);
				}
			}
		}

		if (includeMail && mail != null && mail.length () > 0) {
			int endIndex = mail.indexOf ('@');
			String local;
			if (endIndex >= 0) {
				local = mail.substring (0, endIndex);
			} else {
				local = mail;
			}
			local = local.toLowerCase ();
			String[] fragments = patternNameSplitter.split (local);
			addAllNameFragments (set, fragments);
		}
		
		return set;
	}

	private void addAllNameFragments (Collection<String> coll, String[] arr) {
		assert (coll != null);

		if (arr == null) {
			return ;
		}
		
		for (String str : arr) {
			addAllNameFragment (coll, str);
		}
	}

	private void addAllNameFragment (Collection<String> coll, String str) {
		assert (coll != null);

	   	Matcher m = patternNameFragValidator.matcher (str);
	   	if (m.find ()) {
	   		coll.add (m.group (1));
	   	}
	}
	
	
	public void setName (String name) {
		assert (name != null);

		this.name = name;
	}

	public User getUser () {
		return user;
	}

	public void setUser (User user) {
		assert (user != null);

		this.user = user;
	}

	@Override
	public String toString () {
		return "Identity [id=" + id + ", mail=" + mail + ", name=" + name
				+ ", user=" + user + "]";
	}
	
	public boolean equals (Identity obj) {
		if (id == null || obj.id == null) {
			return obj == this;
		}

		return id.equals (obj.id);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof Identity) {
			return equals ((Identity) obj);
		}

		return super.equals (obj);
	}

	public String getContext () {
		return context;
	}
}
