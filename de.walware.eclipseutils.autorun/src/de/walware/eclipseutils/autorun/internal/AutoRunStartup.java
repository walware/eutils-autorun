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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IStartup;


public class AutoRunStartup implements IStartup {

	public void earlyStartup() {
		IEclipsePreferences node = new InstanceScope().getNode(Activator.PLUGIN_ID);
		if (node.getBoolean(Activator.PREFKEY_AUTORUN_ENABLED, false)) {
			String key = node.get(Activator.PREFKEY_AUTORUN_CONFIG_ID, null);
			if (key != null) {
				new AutoRunner(key).schedule(500);
			}
		}
	}

}
