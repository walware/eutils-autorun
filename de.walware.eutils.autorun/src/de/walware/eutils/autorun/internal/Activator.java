/*******************************************************************************
 * Copyright (c) 2007 WalWare.de/Stephan Wahlbrink (http://www.walware.de).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.eutils.autorun.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "de.walware.eutils.autorun"; //$NON-NLS-1$
	
	public static final String PREFKEY_AUTORUN_ENABLED = "enabled"; //$NON-NLS-1$
	public static final String PREFKEY_AUTORUN_CONFIG_ID = "config.id"; //$NON-NLS-1$
	public static final String PREFKEY_AUTORUN_MODE_ID = "mode.id"; //$NON-NLS-1$
	
	
	private static Activator gPlugin;
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return gPlugin;
	}
	
	
	public Activator() {
	}
	
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		gPlugin = this;
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		gPlugin = null;
		super.stop(context);
	}
	
}
