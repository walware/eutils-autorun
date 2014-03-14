/*=============================================================================#
 # Copyright (c) 2007-2014 WalWare.de/Stephan Wahlbrink (http://www.walware.de).
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.eutils.autorun.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationComparator;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;


public class AutoRunPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private ILaunchManager fLaunchManager;
	
	private Button fEnableButton;
	
	private TreeViewer fEntryCombo;
	private Button fViewButton;
	
	private Label fModeLabel;
	private ComboViewer fModeControl;
	
	private Set<String> fLastMode;
	
	
	public AutoRunPreferencePage() {
	}
	
	
	@Override
	public void init(final IWorkbench workbench) {
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		fLaunchManager = DebugPlugin.getDefault().getLaunchManager();
		
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 3;
		composite.setLayout(layout);
		
		fEnableButton = new Button(composite, SWT.CHECK);
		fEnableButton.setText("Enable &run at startup of:");
		fEnableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		{	fEntryCombo = new TreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
			fEntryCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
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
		}
		{	fModeLabel = new Label(composite, SWT.NONE);
			fModeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
			fModeLabel.setText("Launch &Mode:");
			
			fModeControl = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			fModeControl.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			fModeControl.setContentProvider(new ArrayContentProvider());
			fModeControl.setLabelProvider(new LabelProvider() {
				
				private final StringBuilder fStringBuilder = new StringBuilder();
				
				@Override
				public String getText(final Object element) {
					fStringBuilder.setLength(0);
					final Iterator<String> iter = ((Set<String>) element).iterator();
					if (iter.hasNext()) {
						append(fLaunchManager.getLaunchMode(iter.next()));
						while (iter.hasNext()) {
							fStringBuilder.append("+");
							append(fLaunchManager.getLaunchMode(iter.next()));
						}
					}
					return fStringBuilder.toString();
				}
				
				private void append(final ILaunchMode mode) {
					final String s = mode.getLabel();
					for (int i = 0; i < s.length(); i++) {
						final char c = s.charAt(i);
						if (c == '&') {
							i++;
							if (i < s.length()) {
								fStringBuilder.append(s.charAt(i));
							}
						}
						else {
							fStringBuilder.append(c);
						}
					}
				}
			});
			
			fEntryCombo.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(final SelectionChangedEvent event) {
					updateModes();
				}
			});
			fModeControl.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(final SelectionChangedEvent event) {
					final Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (element != null) {
						fLastMode = (Set<String>) element;
					}
				}
			});
		}
		
		loadConfig();
		
		return composite;
	}
	
	private void updateModes() {
		final Object element = ((IStructuredSelection) fEntryCombo.getSelection()).getFirstElement();
		if (element instanceof ILaunchConfiguration) {
			try {
				final Set<Set<String>> combinations = ((ILaunchConfiguration) element).getType()
						.getSupportedModeCombinations();
				{	// mixed not yet supported
					final Iterator<Set<String>> iter = combinations.iterator();
					while (iter.hasNext()) {
						if (iter.next().size() != 1) {
							iter.remove();
						}
					}
				}
				final Set<String>[] array = combinations.toArray(new Set[combinations.size()]);
				if (array.length > 0) {
					fModeControl.setInput(array);
					if (fLastMode != null && combinations.contains(fLastMode)) {
						fModeControl.setSelection(new StructuredSelection(fLastMode));
					}
					else {
						fModeControl.setSelection(new StructuredSelection(array[0]));
					}
					fModeLabel.setEnabled(true);
					fModeControl.getControl().setEnabled(true);
					return;
				}
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when loading supported modes for the launch configuration.", e));
			}
		}
		fModeControl.setInput(new Set[0]);
		fModeLabel.setEnabled(false);
		fModeControl.getControl().setEnabled(false);
	}
	
	private void loadConfig() {
		ILaunchConfiguration[] launchConfigurations;
		try {
			launchConfigurations = fLaunchManager.getLaunchConfigurations();
			fEntryCombo.setInput(launchConfigurations);
		}
		catch (final CoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
					"Error occured when loading available launch configurations.", e)); //$NON-NLS-1$
			fEnableButton.setSelection(false);
			fEnableButton.setEnabled(false);
			
			updateModes();
			return;
		}
		
		final IEclipsePreferences node = new InstanceScope().getNode(Activator.PLUGIN_ID);
		fEnableButton.setSelection(node.getBoolean(Activator.PREFKEY_AUTORUN_ENABLED, false));
		try {
			final String key = node.get(Activator.PREFKEY_AUTORUN_CONFIG_ID, null);
			final ILaunchConfiguration config = (key != null && key.length() > 0) ?
					fLaunchManager.getLaunchConfiguration(key) : null;
			fEntryCombo.setSelection((config != null) ? new StructuredSelection(config) : new StructuredSelection());
		}
		catch (final CoreException e) {
		}
		
		{	final String mode = node.get(Activator.PREFKEY_AUTORUN_MODE_ID, null);
			if (mode != null) {
				fLastMode = new HashSet<String>(1);
				fLastMode.add(mode);
			}
		}
		
		updateModes();
	}
	
	private void saveConfig() {
		final IEclipsePreferences node = new InstanceScope().getNode(Activator.PLUGIN_ID);
		node.putBoolean(Activator.PREFKEY_AUTORUN_ENABLED, fEnableButton.getSelection());
		final Object element = ((IStructuredSelection) fEntryCombo.getSelection()).getFirstElement();
		String key = null;
		if (element instanceof ILaunchConfiguration) {
			try {
				key = ((ILaunchConfiguration) element).getMemento();
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when saving the autorun launch configuration.", e));
				return;
			}
		}
		if (key != null) {
			node.put(Activator.PREFKEY_AUTORUN_CONFIG_ID, key);
			
			final Set<String> modes = (Set<String>) ((IStructuredSelection) fModeControl.getSelection()).getFirstElement();
			if (modes != null && modes.size() == 1) {
				node.put(Activator.PREFKEY_AUTORUN_MODE_ID, modes.iterator().next());
			}
		}
		else {
			node.remove(Activator.PREFKEY_AUTORUN_CONFIG_ID);
		}
		try {
			node.flush();
		}
		catch (final BackingStoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
					"An error occured when saving the autorun launch configuration.", e));
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
