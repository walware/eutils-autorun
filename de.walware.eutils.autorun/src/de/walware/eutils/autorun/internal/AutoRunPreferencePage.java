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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationComparator;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;


public class AutoRunPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private Button fEnableButton;
	private TreeViewer fEntryCombo;
	private Button fViewButton;
	
	
	public AutoRunPreferencePage() {
	}
	
	
	public void init(final IWorkbench workbench) {
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		fEnableButton = new Button(composite, SWT.CHECK);
		fEnableButton.setText("Enable &run at startup of:");
		fEnableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		fEntryCombo = new TreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
		fEntryCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		fEntryCombo.setLabelProvider(DebugUITools.newDebugModelPresentation());
		fEntryCombo.setContentProvider(new LaunchConfigurationTreeContentProvider(ILaunchManager.RUN_MODE, getShell()) {
			@Override
			public Object[] getElements(final Object parentElement) {
				final Object[] children = super.getChildren(parentElement);
				final List<Object> filtered = new ArrayList<Object>(children.length);
				for (int i = 0; i < children.length; i++) {
					if (super.hasChildren(children[i])) {
						filtered.add(children[i]);
					}
				}
				return filtered.toArray();
			}
		});
		fEntryCombo.setComparator(new LaunchConfigurationComparator());
		
		fViewButton = new Button(composite, SWT.PUSH);
		fViewButton.setText("View/&Edit...");
		final GridData gd = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
		initializeDialogUnits(fViewButton);
		gd.widthHint = Math.max(fViewButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH));
		fViewButton.setLayoutData(gd);
		fViewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Object element = ((IStructuredSelection) fEntryCombo.getSelection()).getFirstElement();
				if (element instanceof ILaunchConfiguration) {
					DebugUITools.openLaunchConfigurationPropertiesDialog(getShell(), (ILaunchConfiguration) element, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
				}
			}
		});
		
		loadConfig();
		
		return composite;
	}
	
	private void loadConfig() {
		final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration[] launchConfigurations;
		try {
			launchConfigurations = manager.getLaunchConfigurations();
			fEntryCombo.setInput(launchConfigurations);
		} catch (final CoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
					"Error occured while loading launch configurations.", e)); //$NON-NLS-1$
			fEnableButton.setSelection(false);
			fEnableButton.setEnabled(false);
			return;
		}
		
		final IEclipsePreferences node = new InstanceScope().getNode(Activator.PLUGIN_ID);
		fEnableButton.setSelection(node.getBoolean(Activator.PREFKEY_AUTORUN_ENABLED, false));
		final String key = node.get(Activator.PREFKEY_AUTORUN_CONFIG_ID, null);
		try {
			final ILaunchConfiguration config = (key != null && key.length() > 0) ? manager.getLaunchConfiguration(key) : null;
			fEntryCombo.setSelection((config != null) ? new StructuredSelection(config) : new StructuredSelection());
		} catch (final CoreException e) {
		}
	}
	
	private void saveConfig() {
		final IEclipsePreferences node = new InstanceScope().getNode(Activator.PLUGIN_ID);
		node.putBoolean(Activator.PREFKEY_AUTORUN_ENABLED, fEnableButton.getSelection());
		final Object element = ((IStructuredSelection) fEntryCombo.getSelection()).getFirstElement();
		String key = null;
		if (element instanceof ILaunchConfiguration) {
			try {
				key = ((ILaunchConfiguration) element).getMemento();
			} catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured while saving autorun launch configuration", e));
				return;
			}
		}
		if (key != null) {
			node.put(Activator.PREFKEY_AUTORUN_CONFIG_ID, key);
		}
		else {
			node.remove(Activator.PREFKEY_AUTORUN_CONFIG_ID);
		}
		try {
			node.flush();
		} catch (final BackingStoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
					"An error occured while saving autorun launch configuration", e));
		}
	}
	
	
	@Override
	protected void performDefaults() {
		fEnableButton.setSelection(false);
		fEntryCombo.setSelection(new StructuredSelection());
	}
	
	@Override
	public boolean performOk() {
		saveConfig();
		return true;
	}
	
}
