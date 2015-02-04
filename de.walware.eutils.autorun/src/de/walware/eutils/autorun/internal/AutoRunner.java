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

package de.walware.eutils.autorun.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.statushandlers.StatusManager;

public class AutoRunner extends Job {
	
	
	private final String key;
	
	private final String mode;
	
	
	public AutoRunner(final String key, final String mode) {
		super("Auto Run");
		if (key == null) {
			throw new NullPointerException("key");
		}
		if (mode == null) {
			throw new NullPointerException("mode");
		}
		this.key= key;
		this.mode= mode;
	}
	
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final ILaunchConfiguration config= DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(this.key);
			if (config == null) {
				final IStatus status= new Status(IStatus.WARNING, Activator.PLUGIN_ID, 101,
						"The configured launch configuration for Auto Run could not be loaded.", null);
				StatusManager.getManager().handle(status);
				return Status.OK_STATUS;
			}
			
			config.launch(this.mode, monitor, false, true);
		}
		catch (final CoreException e) {
			final IStatus status= new Status(IStatus.ERROR, Activator.PLUGIN_ID, 102, 
					"An error occured when starting the launch configuration by Auto Run.", e);
			return status;
		}
		return Status.OK_STATUS;
	}
	
}
