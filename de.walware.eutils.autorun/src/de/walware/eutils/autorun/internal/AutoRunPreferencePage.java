/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationComparator;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;


public class AutoRunPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private ILaunchManager launchManager;
	
	private Button enableButton;
	
	private TreeViewer entryViewer;
	private Button viewButton;
	
	private Label modeLabel;
	private ComboViewer modeViewer;
	
	private Set<String> lastMode;
	
	
	public AutoRunPreferencePage() {
	}
	
	
	@Override
	public void init(final IWorkbench workbench) {
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		this.launchManager= DebugPlugin.getDefault().getLaunchManager();
		
		final Composite composite= new Composite(parent, SWT.NONE);
		{	final GridLayout gd= new GridLayout();
			gd.marginWidth= 0;
			gd.marginHeight= 0;
			gd.numColumns= 3;
			composite.setLayout(gd);
		}
		{	this.enableButton= new Button(composite, SWT.CHECK);
			this.enableButton.setText("Enable &launch at startup of:");
			this.enableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}
		{	this.entryViewer= new TreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
			final Tree tree= this.entryViewer.getTree();
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
			Dialog.applyDialogFont(this.entryViewer.getControl());
			gd.heightHint= tree.getItemHeight() * 10;
			this.entryViewer.getControl().setLayoutData(gd);
			
			this.entryViewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
			this.entryViewer.setContentProvider(new LaunchConfigurationTreeContentProvider(ILaunchManager.RUN_MODE, getShell()) {
				@Override
				public Object[] getElements(final Object parentElement) {
					final Object[] children= super.getChildren(parentElement);
					final List<Object> filtered= new ArrayList<>(children.length);
					for (int i= 0; i < children.length; i++) {
						if (super.hasChildren(children[i])) {
							filtered.add(children[i]);
						}
					}
					return filtered.toArray();
				}
			});
			this.entryViewer.setComparator(new LaunchConfigurationComparator());
		}
		{	this.viewButton= new Button(composite, SWT.PUSH);
			this.viewButton.setText("View/&Edit...");
			final GridData gd= new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
			Dialog.applyDialogFont(this.viewButton);
			gd.widthHint= Math.max(this.viewButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x,
					convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH) );
			this.viewButton.setLayoutData(gd);
			this.viewButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final Object element= ((IStructuredSelection) AutoRunPreferencePage.this.entryViewer.getSelection()).getFirstElement();
					if (element instanceof ILaunchConfiguration) {
						DebugUITools.openLaunchConfigurationPropertiesDialog(getShell(), (ILaunchConfiguration) element, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
					}
				}
			});
		}
		{	this.modeLabel= new Label(composite, SWT.NONE);
			this.modeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
			this.modeLabel.setText("Launch &Mode:");
			
			this.modeViewer= new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			this.modeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			this.modeViewer.setContentProvider(new ArrayContentProvider());
			this.modeViewer.setLabelProvider(new LabelProvider() {
				
				private final StringBuilder fStringBuilder= new StringBuilder();
				
				@Override
				public String getText(final Object element) {
					this.fStringBuilder.setLength(0);
					final Iterator<String> iter= ((Set<String>) element).iterator();
					if (iter.hasNext()) {
						append(AutoRunPreferencePage.this.launchManager.getLaunchMode(iter.next()));
						while (iter.hasNext()) {
							this.fStringBuilder.append("+");
							append(AutoRunPreferencePage.this.launchManager.getLaunchMode(iter.next()));
						}
					}
					return this.fStringBuilder.toString();
				}
				
				private void append(final ILaunchMode mode) {
					final String s= mode.getLabel();
					for (int i= 0; i < s.length(); i++) {
						final char c= s.charAt(i);
						if (c == '&') {
							i++;
							if (i < s.length()) {
								this.fStringBuilder.append(s.charAt(i));
							}
						}
						else {
							this.fStringBuilder.append(c);
						}
					}
				}
			});
			
			this.entryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(final SelectionChangedEvent event) {
					updateModes();
				}
			});
			this.modeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(final SelectionChangedEvent event) {
					final Object element= ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (element != null) {
						AutoRunPreferencePage.this.lastMode= (Set<String>) element;
					}
				}
			});
		}
		
		Dialog.applyDialogFont(composite);
		
		loadConfigs();
		loadPrefs();
		
		return composite;
	}
	
	private void updateModes() {
		final Object element= ((IStructuredSelection) this.entryViewer.getSelection()).getFirstElement();
		if (element instanceof ILaunchConfiguration) {
			try {
				final Set<Set<String>> combinations= ((ILaunchConfiguration) element).getType()
						.getSupportedModeCombinations();
				{	// mixed not yet supported
					final Iterator<Set<String>> iter= combinations.iterator();
					while (iter.hasNext()) {
						if (iter.next().size() != 1) {
							iter.remove();
						}
					}
				}
				final Set<String>[] array= combinations.toArray(new Set[combinations.size()]);
				if (array.length > 0) {
					this.modeViewer.setInput(array);
					if (this.lastMode != null && combinations.contains(this.lastMode)) {
						this.modeViewer.setSelection(new StructuredSelection(this.lastMode));
					}
					else {
						this.modeViewer.setSelection(new StructuredSelection(array[0]));
					}
					this.modeLabel.setEnabled(true);
					this.modeViewer.getControl().setEnabled(true);
					return;
				}
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when loading supported modes for the launch configuration.", e));
			}
		}
		this.modeViewer.setInput(new Set[0]);
		this.modeLabel.setEnabled(false);
		this.modeViewer.getControl().setEnabled(false);
	}
	
	private void loadConfigs() {
		final ILaunchConfiguration[] launchConfigurations;
		try {
			launchConfigurations= this.launchManager.getLaunchConfigurations();
			this.entryViewer.setInput(launchConfigurations);
		}
		catch (final CoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
					"Error occured when loading available launch configurations.", e)); //$NON-NLS-1$
			this.enableButton.setSelection(false);
			this.enableButton.setEnabled(false);
			
			updateModes();
			return;
		}
	}
	
	private void loadPrefs() {
		final IPreferencesService preferences= Platform.getPreferencesService();
		this.enableButton.setSelection(preferences.getBoolean(
				Activator.PLUGIN_ID, Activator.ENABLED_PREF_KEY, true, null ));
		try {
			final String key= preferences.getString(
					Activator.PLUGIN_ID, Activator.LAUNCH_CONFIG_ID_PREF_KEY, null, null );
			final ILaunchConfiguration config= (key != null && key.length() > 0) ?
					this.launchManager.getLaunchConfiguration(key) : null;
			this.entryViewer.setSelection((config != null) ? new StructuredSelection(config) : new StructuredSelection());
		}
		catch (final CoreException e) {
		}
		
		{	final String mode= preferences.getString(
					Activator.PLUGIN_ID, Activator.LAUNCH_MODE_ID_PREF_KEY, null, null );
			if (mode != null) {
				this.lastMode= new HashSet<>(1);
				this.lastMode.add(mode);
			}
		}
		
		updateModes();
	}
	
	private void savePrefs(final boolean flush) {
		final IEclipsePreferences node= InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		
		node.putBoolean(Activator.ENABLED_PREF_KEY, this.enableButton.getSelection());
		
		final Object element= ((IStructuredSelection) this.entryViewer.getSelection()).getFirstElement();
		String key= null;
		if (element instanceof ILaunchConfiguration) {
			try {
				key= ((ILaunchConfiguration) element).getMemento();
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when saving the autorun launch configuration.", e));
				return;
			}
		}
		if (key != null) {
			node.put(Activator.LAUNCH_CONFIG_ID_PREF_KEY, key);
			
			final Set<String> modes= (Set<String>) ((IStructuredSelection) this.modeViewer.getSelection()).getFirstElement();
			if (modes != null && modes.size() == 1) {
				node.put(Activator.LAUNCH_MODE_ID_PREF_KEY, modes.iterator().next());
			}
		}
		else {
			node.remove(Activator.LAUNCH_CONFIG_ID_PREF_KEY);
		}
		
		if (flush) {
			try {
				node.flush();
			}
			catch (final BackingStoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when saving the autorun launch configuration.", e));
			}
		}
	}
	
	
	@Override
	protected void performDefaults() {
		this.enableButton.setSelection(true);
		this.entryViewer.setSelection(new StructuredSelection());
	}
	
	@Override
	protected void performApply() {
		savePrefs(true);
	}
	
	@Override
	public boolean performOk() {
		savePrefs(false);
		return true;
	}
	
}
