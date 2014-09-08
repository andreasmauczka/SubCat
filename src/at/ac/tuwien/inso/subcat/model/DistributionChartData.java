/* DistributionChartData.java
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

package at.ac.tuwien.inso.subcat.model;


public class DistributionChartData {
	private double[][] data; 


	public DistributionChartData (double[][] data) {
		assert (data != null);
		assert (data.length == 12);

		this.data = data;
	}
	
	public double getMean (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][0];
	}

	public double getMedian (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][1];
	}

	public double getQ1 (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][2];
	}

	public double getQ3 (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][3];
	}

	public double getMin (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][4];
	}

	public double getMax (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][5];
	}

	public double getCount (int month) {
		assert (month >= 0 && month <= 11);

		return data[month][6];
	}
}
