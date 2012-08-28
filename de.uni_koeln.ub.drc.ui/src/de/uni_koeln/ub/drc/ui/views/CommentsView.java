/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import scala.collection.JavaConversions;
import de.uni_koeln.ub.drc.data.Comment;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;
import de.uni_koeln.ub.drc.ui.util.ViewPartFinder;
import de.uni_koeln.ub.drc.ui.views.WordViewModel.WordViewLabelProvider;

/**
 * View on comments for a page.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public final class CommentsView extends ViewPart {

	/**
	 * The class / CommentsView ID
	 */
	public static final String ID = CommentsView.class.getName().toLowerCase();
	static final String NEW_COMMENT = "new_comment"; //$NON-NLS-1$
	private TableViewer viewer;
	private Page page;
	private Text commentField;

	/**
	 * @param parent
	 *            The parent composite for this part
	 */
	@Override
	public void createPartControl(final Composite parent) {
		initAddCommentBar(parent);
		initTableViewer(parent);
		GridLayoutFactory.fillDefaults().generateLayout(parent);
		attachSelectionListener();
	}

	@Override
	public void setFocus() {
	}

	private void attachSelectionListener() {
		ISelectionService selectionService = (ISelectionService) getSite()
				.getService(ISelectionService.class);
		selectionService.addSelectionListener(new ISelectionListener() {

			@Override
			@SuppressWarnings("unchecked")
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				if (structuredSelection.getFirstElement() instanceof Page) {
					List<Page> pages = structuredSelection.toList();
					if (pages != null && pages.size() > 0) {
						page = pages.get(0);
						setInput();
					}
				}
			}
		});
	}

	/* DI */@SuppressWarnings("unused")
	@PostConstruct
	private void addFocusListener() {
		commentField
				.addFocusListener(new SpecialCharacterView.TextFocusListener(
						commentField));
	}

	private void initAddCommentBar(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		commentField = new Text(comp, SWT.BORDER);
		commentField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button add = new Button(comp, SWT.PUSH | SWT.FLAT);
		add.setImage(SessionContextSingleton.getInstance().loadImage(
				"icons/add.gif")); //$NON-NLS-1$
		add.setToolTipText(Messages.get().AddNewComment);
		SelectionListener listener = new SelectionListener() {
			@Override
			// on button click
			public void widgetSelected(SelectionEvent e) {
				addComment(commentField);
			}

			@Override
			// on enter in text
			public void widgetDefaultSelected(SelectionEvent e) {
				addComment(commentField);
			}

			private void addComment(final Text commentField) {
				String text = commentField.getText().trim();
				if (text.length() > 0) {
					page.comments().$plus$eq(
							new Comment(SessionContextSingleton.getInstance()
									.getCurrentUser().id(), text, System
									.currentTimeMillis()));
					setInput();
					page.saveToDb(SessionContextSingleton.getInstance()
							.getCurrentUser().collection(),
							SessionContextSingleton.getInstance().db());
					commentField.setText(""); //$NON-NLS-1$
					SearchView sv = ViewPartFinder.find(SearchView.class);
					sv.updateTreeViewer();
				}
			}
		};
		add.addSelectionListener(listener);
		commentField.addSelectionListener(listener);
	}

	private void initTableViewer(final Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.BORDER);
		initTable();
		viewer.setContentProvider(new CommentsContentProvider());
		viewer.setLabelProvider(new CommentsLabelProvider());
		setInput();
	}

	private void initTable() {
		final int[] columns = new int[] { 25, 500, 350, 250 };
		createColumn("", columns[0]); //$NON-NLS-1$
		createColumn(Messages.get().Comment, columns[1]);
		createColumn(Messages.get().Author, columns[2]);
		createColumn(Messages.get().Date, columns[3]);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private void createColumn(final String name, final int width) {
		TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.NONE);
		column1.getColumn().setText(name);
		column1.getColumn().setWidth(width);
		column1.getColumn().setResizable(true);
		column1.getColumn().setMoveable(true);
	}

	private void setInput() {
		if (page != null) {
			TableHelper.clearWidgets(viewer.getTable());
			viewer.setInput(JavaConversions.asJavaList(page.comments())
					.toArray(new Comment[] {}));
			addLinks();
		}
	}

	private void addLinks() {
		Table table = viewer.getTable();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			final TableItem item = items[i];
			final String author = ((Comment) item.getData()).user();
			Link link = TableHelper.insertLink(viewer.getTable(), item, author,
					2);
			item.setData(new Widget[] { link });
		}

	}

	private static final class CommentsContentProvider implements
			IStructuredContentProvider {
		@Override
		public Object[] getElements(final Object inputElement) {
			Object[] elements = (Object[]) inputElement;
			return elements;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}
	}

	private static final class CommentsLabelProvider extends LabelProvider
			implements ITableLabelProvider {
		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			Comment comment = (Comment) element;
			switch (columnIndex) {
			case 0:
				return ""; //$NON-NLS-1$
			case 1:
				return comment.text();
			case 2:
				return WordViewLabelProvider.userDetails(comment.user());
			case 3:
				return new Date(comment.date()).toString();
			default:
				return comment.toString();
			}
		}

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			if (columnIndex == 0) {
				return SessionContextSingleton.getInstance().loadImage(
						"icons/write.gif"); //$NON-NLS-1$
			}
			return null;
		}
	}

}
