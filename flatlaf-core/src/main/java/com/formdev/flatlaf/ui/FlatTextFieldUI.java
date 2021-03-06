/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import static com.formdev.flatlaf.util.UIScale.scale;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import com.formdev.flatlaf.FlatClientProperties;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JTextField}.
 *
 * <!-- BasicTextFieldUI -->
 *
 * @uiDefault TextField.font					Font
 * @uiDefault TextField.background				Color
 * @uiDefault TextField.foreground				Color	also used if not editable
 * @uiDefault TextField.caretForeground			Color
 * @uiDefault TextField.selectionBackground		Color
 * @uiDefault TextField.selectionForeground		Color
 * @uiDefault TextField.disabledBackground		Color	used if not enabled
 * @uiDefault TextField.inactiveBackground		Color	used if not editable
 * @uiDefault TextField.inactiveForeground		Color	used if not enabled (yes, this is confusing; this should be named disabledForeground)
 * @uiDefault TextField.border					Border
 * @uiDefault TextField.margin					Insets
 * @uiDefault TextField.caretBlinkRate			int		default is 500 milliseconds
 *
 * <!-- FlatTextFieldUI -->
 *
 * @uiDefault Component.focusWidth				int
 * @uiDefault Component.minimumWidth			int
 * @uiDefault Component.isIntelliJTheme			boolean
 * @uiDefault TextField.placeholderForeground	Color
 *
 * @author Karl Tauber
 */
public class FlatTextFieldUI
	extends BasicTextFieldUI
{
	protected int focusWidth;
	protected int minimumWidth;
	protected boolean isIntelliJTheme;
	protected Color placeholderForeground;

	private FocusListener focusListener;

	public static ComponentUI createUI( JComponent c ) {
		return new FlatTextFieldUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();

		String prefix = getPropertyPrefix();
		focusWidth = UIManager.getInt( "Component.focusWidth" );
		minimumWidth = UIManager.getInt( "Component.minimumWidth" );
		isIntelliJTheme = UIManager.getBoolean( "Component.isIntelliJTheme" );
		placeholderForeground = UIManager.getColor( prefix + ".placeholderForeground" );

		LookAndFeel.installProperty( getComponent(), "opaque", focusWidth == 0 );

		MigLayoutVisualPadding.install( getComponent(), focusWidth );
	}

	@Override
	protected void uninstallDefaults() {
		super.uninstallDefaults();

		placeholderForeground = null;

		MigLayoutVisualPadding.uninstall( getComponent() );
	}

	@Override
	protected void installListeners() {
		super.installListeners();

		focusListener = new FlatUIUtils.RepaintFocusListener( getComponent() );
		getComponent().addFocusListener( focusListener );
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();

		getComponent().removeFocusListener( focusListener );
		focusListener = null;
	}

	@Override
	protected void propertyChange( PropertyChangeEvent e ) {
		super.propertyChange( e );

		if( FlatClientProperties.PLACEHOLDER_TEXT.equals( e.getPropertyName() ) )
			getComponent().repaint();
	}

	@Override
	protected void paintSafely( Graphics g ) {
		paintBackground( g, getComponent(), focusWidth, isIntelliJTheme );
		paintPlaceholder( g, getComponent(), placeholderForeground );
		super.paintSafely( g );
	}

	@Override
	protected void paintBackground( Graphics g ) {
		// background is painted elsewhere
	}

	static void paintBackground( Graphics g, JTextComponent c, int focusWidth, boolean isIntelliJTheme ) {
		// do not paint background if:
		//   - not opaque and
		//   - border is not a flat border and
		//   - opaque was explicitly set (to false)
		// (same behaviour as in AquaTextFieldUI)
		if( !c.isOpaque() && !(c.getBorder() instanceof FlatBorder) && FlatUIUtils.hasOpaqueBeenExplicitlySet( c ) )
			return;

		// fill background if opaque to avoid garbage if user sets opaque to true
		if( c.isOpaque() && focusWidth > 0 )
			FlatUIUtils.paintParentBackground( g, c );

		// paint background
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			FlatUIUtils.setRenderingHints( g2 );

			float fFocusWidth = (c.getBorder() instanceof FlatBorder) ? scale( (float) focusWidth ) : 0;

			Color background = c.getBackground();
			g2.setColor( !(background instanceof UIResource)
				? background
				: (isIntelliJTheme && (!c.isEnabled() || !c.isEditable())
					? FlatUIUtils.getParentBackground( c )
					: background) );
			FlatUIUtils.paintComponentBackground( g2, 0, 0, c.getWidth(), c.getHeight(), fFocusWidth, 0 );
		} finally {
			g2.dispose();
		}
	}

	static void paintPlaceholder( Graphics g, JTextComponent c, Color placeholderForeground ) {
		// check whether text component is empty
		if( c.getDocument().getLength() > 0 )
			return;

		// check for JComboBox
		Container parent = c.getParent();
		JComponent jc = (parent instanceof JComboBox) ? (JComboBox<?>) parent : c;

		// get placeholder text
		Object placeholder = jc.getClientProperty( FlatClientProperties.PLACEHOLDER_TEXT );
		if( !(placeholder instanceof String) )
			return;

		// compute placeholder location
		Insets insets = c.getInsets();
		FontMetrics fm = c.getFontMetrics( c.getFont() );
		int x = insets.left;
		int y = insets.top + fm.getAscent() + ((c.getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2);

		// paint placeholder
		g.setColor( placeholderForeground );
		FlatUIUtils.drawString( c, g, (String) placeholder, x, y );
	}

	@Override
	public Dimension getPreferredSize( JComponent c ) {
		return applyMinimumWidth( super.getPreferredSize( c ), c );
	}

	@Override
	public Dimension getMinimumSize( JComponent c ) {
		return applyMinimumWidth( super.getMinimumSize( c ), c );
	}

	private Dimension applyMinimumWidth( Dimension size, JComponent c ) {
		// do not apply minimum width if JTextField.columns is set
		if( c instanceof JTextField && ((JTextField)c).getColumns() > 0 )
			return size;

		Container parent = c.getParent();
		if( parent instanceof JComboBox ||
			parent instanceof JSpinner ||
			(parent != null && parent.getParent() instanceof JSpinner) )
		  return size;

		int minimumWidth = FlatUIUtils.minimumWidth( getComponent(), this.minimumWidth );
		int focusWidth = (c.getBorder() instanceof FlatBorder) ? this.focusWidth : 0;
		size.width = Math.max( size.width, scale( minimumWidth + (focusWidth * 2) ) );
		return size;
	}
}
