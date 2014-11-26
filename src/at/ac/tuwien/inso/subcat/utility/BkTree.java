/* BkTree.java
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

package at.ac.tuwien.inso.subcat.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;




public class BkTree<V> {

	public static abstract class Foreach<V> {

		public abstract void callback (BkNode<V> node);
	}


	public static abstract class DistanceFunc {

		public abstract int distance (String a, String b);

		public int distance (String[] ar, String[] br) {
			assert (ar != null);
			assert (br != null);
			assert (ar.length > 0);
			assert (br.length > 0);

			int distance = Integer.MAX_VALUE;
			
			for (String a : ar) {
				for (String b : br) {
					int tmp = distance (a, b);
					if (tmp < distance) {
						distance = tmp;
					}
				}
			}

			return distance;
		}
	}

	
	public static class LevensteinFunc extends DistanceFunc {

		// Source: http://rosettacode.org/wiki/Levenshtein_distance
		// GNU Free Documentation License
		@Override
	    public int distance(String a, String b) {
	    	a = a.toLowerCase();
	        b = b.toLowerCase();
	        // i == 0
	        int [] costs = new int [b.length() + 1];
	        for (int j = 0; j < costs.length; j++)
	            costs[j] = j;
	        for (int i = 1; i <= a.length(); i++) {
	            // j == 0; nw = lev(i - 1, j)
	            costs[0] = i;
	            int nw = i - 1;
	            for (int j = 1; j <= b.length(); j++) {
	                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
	                nw = costs[j];
	                costs[j] = cj;
	            }
	        }
	        return costs[b.length()];
	    }
	}


	public static class BkNode<V> {
		public final Map<Integer, BkNode<V>> children;
		public final String key;
		public final ArrayList<V> values;

		public BkNode (String key, V value) {
			this.children = new HashMap<Integer, BkNode<V>> ();
			this.values = new ArrayList<V> ();
			this.values.add (value);
			this.key = key;
		}

		public void add (String newKey, V newValue, DistanceFunc func) {
			int dist = func.distance (key, newKey);
			if (dist == 0) {
				this.values.add (newValue);
				return ;
			}

			BkNode<V> parent = children.get (dist);
			if (parent == null) {
				BkNode<V> node = new BkNode<V> (newKey, newValue);
				children.put (dist, node);
			} else {
				parent.add (newKey, newValue, func);
			}
		}

		public void get (String term, int threshold, DistanceFunc func, Collection<V> collected) {
			int nodeDist = func.distance (term, this.key);

			if (nodeDist <= threshold) {
				collected.addAll (values);
			}

			int maxDist = nodeDist + threshold;
			int minDist = nodeDist - threshold;

			for (Entry<Integer, BkNode<V>> entry : children.entrySet ()) {
				int mapDistance = entry.getKey ();

				if (minDist <= mapDistance && mapDistance <= maxDist) {
					entry.getValue ().get (term, threshold, func, collected);					
				}
			}
		}
		
		public void foreach (Foreach<V> cb) {
			cb.callback (this);

			for (BkNode<V> node : children.values ()) {
				node.foreach (cb);
			}
		}
	}

	public final DistanceFunc distFunc;
	private BkNode<V> root;
	

	public BkTree (DistanceFunc distFunc) {
		assert (distFunc != null);

		this.distFunc = distFunc;
	}

	public void add (String key, V value) {
		assert (key != null);

		if (root == null) {
			root = new BkNode<V> (key, value);
		} else {
			root.add (key, value, distFunc);
		}
	}

	public void add (String[] keys, V value) {
		assert (keys != null);

		for (String k : keys) {
			add (k, value);
		}
	}

	public void get (String term, int threshold, Collection<V> collection) {
		assert (term != null);
		assert (threshold >= 0);
		assert (collection != null);

		if (root != null) {
			root.get (term, threshold, distFunc, collection);
		}
	}

	public void get (String[] terms, int threshold, Collection<V> collection) {
		assert (terms != null);

		for (String term : terms) {
			get (term, threshold, collection);
		}
	}

	
	public List<V> get (String term, int threshold) {
		List<V> output = new LinkedList<V> ();
		get (term, threshold, output);
		return output;
	}

	public List<V> get (String[] terms, int threshold) {
		List<V> output = new LinkedList<V> ();
		get (terms, threshold, output);
		return output;
	}

	public Set<V> getSet (String term, int threshold) {
		Set<V> output = new HashSet<V> ();
		get (term, threshold, output);
		return output;
	}

	public Set<V> getSet (String[] terms, int threshold) {
		Set<V> output = new HashSet<V> ();
		get (terms, threshold, output);
		return output;
	}

    public void foreach (Foreach<V> cb) {
    	if (root != null) {
    		root.foreach (cb);
    	}
    }
}
