/*=============================================================================#
 # Copyright (c) 2007-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.eutils.autorun.internal.nostart;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.IStartup;

import de.walware.eutils.autorun.internal.Activator;
import de.walware.eutils.autorun.internal.AutoRunner;


public class AutoRunStartup implements IStartup {
	
	
	public AutoRunStartup() {
	}
	
	
	@Override
	public void earlyStartup() {
		if (Boolean.parseBoolean(System.getProperty("de.walware.eutils.autorun.disable"))) { //$NON-NLS-1$
			return;
		}
		final IPreferencesService preferences= Platform.getPreferencesService();
		if (preferences.getBoolean(Activator.PLUGIN_ID, Activator.ENABLED_PREF_KEY, true, null)) {
			final String key= preferences.getString(Activator.PLUGIN_ID, Activator.LAUNCH_CONFIG_ID_PREF_KEY, null, null);
			if (key != null) {
				final String mode= preferences.getString(Activator.PLUGIN_ID, Activator.LAUNCH_MODE_ID_PREF_KEY, ILaunchManager.RUN_MODE, null);
				new AutoRunner(key, mode).schedule(500);
			}
		}
	}
	
}
