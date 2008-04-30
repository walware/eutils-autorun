/*******************************************************************************
 * Copyright (c) 2007 WalWare.de/Stephan Wahlbrink (http://www.walware.de).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.eclipseutils.autorun.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.walware.eclipseutils.autorun";
	
	public static final String PREFKEY_AUTORUN_ENABLED = "enabled";
	public static final String PREFKEY_AUTORUN_CONFIG_ID = "config.id";

	
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


	public void start(BundleContext context) throws Exception {
		super.start(context);
		gPlugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		gPlugin = null;
		super.stop(context);
	}

}
