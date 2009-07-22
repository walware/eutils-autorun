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

package de.walware.eclipseutils.autorun.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.statushandlers.StatusManager;

public class AutoRunner extends Job {
	
	
	private String fKey;
	
	
	public AutoRunner(final String key) {
		super("Auto Run");
		fKey = key;
	}
	
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final ILaunchConfiguration config = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(fKey);
			if (config == null) {
				StatusManager.getManager().handle(new Status(IStatus.WARNING, Activator.PLUGIN_ID, 101, 
						"The configured autorun launch configuration could not loaded.", null));
				return Status.OK_STATUS;
			}
			
			config.launch(ILaunchManager.RUN_MODE, monitor, false, true);
		} catch (final CoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 102, 
					"An error occured while running autorun launch configuration.", e));
		}
		return Status.OK_STATUS;
	}
	
}
