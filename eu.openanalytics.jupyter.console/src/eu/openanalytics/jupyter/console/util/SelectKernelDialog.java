/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.util;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.jupyter.wsclient.KernelService.KernelSpec;

public class SelectKernelDialog extends TitleAreaDialog {

	private KernelSpec[] specs;
	private KernelSpec selectedSpec;
	
	private TableViewer tableViewer;
	
	public SelectKernelDialog(Shell parentShell, KernelSpec[] specs) {
		super(parentShell);
		this.specs = specs;
		Arrays.sort(this.specs, new Comparator<KernelSpec>() {
			@Override
			public int compare(KernelSpec o1, KernelSpec o2) {
				return o1.name.compareTo(o2.name);
			}
		});
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Kernelspec");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(area);
		
		tableViewer = new TableViewer(area, SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getTable());
		
		TableViewerColumn colViewer = new TableViewerColumn(tableViewer, SWT.LEFT);
		colViewer.getColumn().setText("Kernel");
		colViewer.getColumn().setWidth(100);
		colViewer.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				KernelSpec spec = (KernelSpec) cell.getElement();
				cell.setText(spec.name);
			}
		});
		
		colViewer = new TableViewerColumn(tableViewer, SWT.LEFT);
		colViewer.getColumn().setText("Name");
		colViewer.getColumn().setWidth(100);
		colViewer.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				KernelSpec spec = (KernelSpec) cell.getElement();
				cell.setText(spec.displayName);
			}
		});
		
		colViewer = new TableViewerColumn(tableViewer, SWT.LEFT);
		colViewer.getColumn().setText("Language");
		colViewer.getColumn().setWidth(100);
		colViewer.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				KernelSpec spec = (KernelSpec) cell.getElement();
				cell.setText(spec.language);
			}
		});
		
		tableViewer.setInput(specs);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection sel = (StructuredSelection) tableViewer.getSelection();
				if (!sel.isEmpty()) selectedSpec = (KernelSpec) sel.getFirstElement();
			}
		});
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		
		setTitle("Select Kernelspec");
		setMessage("Select a kernelspec from the list of available specs below.");
		
		return area;
	}
	
	public KernelSpec getSelectedSpec() {
		return selectedSpec;
	}
}
