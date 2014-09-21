/* VerticalLabel.java
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

package at.ac.tuwien.inso.subcat.ui.widgets;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
 


public class VerticalLabel extends Composite {
	private Label imgLabel;
	private String text;

	private Color foreground;
	private Color background;
	private Font font;

	// SWT.UP, SWT.DOWN
	public VerticalLabel (Composite parent, int style) {
		super (parent, style);

		imgLabel = new Label (this, style);
		
		this.addDisposeListener (new DisposeListener () {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				imgLabel.getImage ().dispose ();
				imgLabel.dispose ();
			}
		});
	}

	public void setText (String text) {
		assert (text != null);

		Display display = Display.getCurrent ();

		// Get the dimensions:
		GC gc = new GC (display);
		gc.setFont (getFont ());
		Point textExtent = gc.textExtent (text);
		gc.dispose ();

		// The new image:
		Image stringImage = new Image (display, textExtent.x, textExtent.y);
		gc = new GC (stringImage);
		gc.setFont (getFont ());
		gc.setForeground (getForeground ());
		gc.setBackground (getBackground ());
		gc.drawText (text, 0, 0);

		// Rotation:
		Image rotatedImage = rotatedImage (stringImage, this.getStyle ());

		gc.dispose ();
		stringImage.dispose ();

	
		imgLabel.setImage (rotatedImage);
		imgLabel.pack ();

		this.text = text;
	}

	public String getText () {
		return text;
	}

	public Font getFont () {
		return (font != null)? font :
			getDisplay ().getSystemFont ();
	}

	public Color getForeground () {
		return (foreground != null)? foreground :
			getDisplay ().getSystemColor (SWT.COLOR_WIDGET_FOREGROUND);
	}

	public Color getBackground () {
		return (background != null)? background :
			getDisplay ().getSystemColor (SWT.COLOR_WIDGET_BACKGROUND);
	}

	private Image rotatedImage (Image image, int style) {
		Display display = Display.getCurrent ();

		ImageData orig = image.getImageData ();
		ImageData res = new ImageData (orig.height, orig.width, orig.depth, orig.palette);
	
		boolean rotateUp = (style & SWT.DOWN) != SWT.DOWN;
	
		for (int sx = 0; sx < orig.width; sx++) {
			for (int sy = 0; sy < orig.height; sy++) {
				int dx = (rotateUp)? sy : orig.height - sy - 1;
				int dy = (rotateUp)? orig.width - sx - 1 : sx;
				res.setPixel(dx, dy, orig.getPixel (sx, sy));
			}
		}

		return new Image (display, res);
	}

	
	//
	// Test Main
	//
	
	/*
	public static void main(String[] args) {
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());

		VerticalLabel label = new VerticalLabel (shell, SWT.NONE);
		label.setText("FOO");

		
	    shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) {
				display.sleep ();
			}
		}
		
		display.dispose();
	}
	*/
}
