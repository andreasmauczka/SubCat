/* Helper.java
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

package at.ac.tuwien.inso.subcat.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


public class Helper {

	public static void setLabelStyle (Label label, int style) {
		assert (label != null);

		FontData fontData = label.getFont().getFontData()[0];
		FontData newFontData = new FontData(fontData.getName(), fontData.getHeight(), style);
		Font font = new Font (label.getDisplay (), newFontData);
		label.setFont (font);
	}

	public static void separator (Composite parent, int size) {
		assert (parent != null);

		Label lblDd = new Label (parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_SOLID);
		lblDd.setLayoutData (new GridData (SWT.FILL, SWT.FILL, false, false, size, 1));
		lblDd.setBackground (new org.eclipse.swt.graphics.Color (null, 0, 0, 0));
	}
}
