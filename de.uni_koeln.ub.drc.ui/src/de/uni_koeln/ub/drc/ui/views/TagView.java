/**************************************************************************************************
 * Copyright (c) 2011 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import java.util.Date;

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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
import de.uni_koeln.ub.drc.data.Annotation;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.data.Word;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;
import de.uni_koeln.ub.drc.ui.views.TagViewModel.TagViewContentProvider;
import de.uni_koeln.ub.drc.ui.views.TagViewModel.TagViewLabelProvider;

/**
 * View containing tag details for the currently selected word.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public final class TagView extends ViewPart {

	/**
	 * The class / TagView ID
	 */
	public static final String ID = TagView.class.getName().toLowerCase();

	private Word word;
	private TableViewer viewer;
	private Page page;
	private Button addTag;
	private Label label;

	/**
	 * @param parent
	 *            The parent composite for this part
	 */
	@Override
	public void createPartControl(Composite parent) {
		initTableViewer(parent);
		addNewTagBar(parent);
		GridLayoutFactory.fillDefaults().generateLayout(parent);
		attachSelectionListener();
	}

	@Override
	public void setFocus() {
	}

	/**
	 * @param word
	 *            The selected word
	 */
	public void setWord(final Word word) {
		this.word = word;
		updateLabel();
		setTableInput();
	}

	private void attachSelectionListener() {
		ISelectionService selectionService = (ISelectionService) getSite()
				.getService(ISelectionService.class);
		selectionService.addSelectionListener(new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				if (structuredSelection.getFirstElement() instanceof Page) {
					Page page = (Page) structuredSelection.getFirstElement();
					if (page != null) {
						setPage(page);
						setTableInput();
					}

				}
			}
		});
	}

	private void setPage(Page page) {
		this.page = page;
	}

	private void addNewTagBar(Composite parent) {
		Composite bottomComposite = new Composite(parent, SWT.NONE);
		bottomComposite.setLayout(new GridLayout(4, false));
		insertAddTagButton(bottomComposite);
	}

	private void insertAddTagButton(Composite bottomComposite) {
		label = new Label(bottomComposite, SWT.NONE);
		updateLabel();
		final Combo tagFieldKey = new Combo(bottomComposite, SWT.READ_ONLY);
		tagFieldKey
				.setItems(new String[] { "POS", "SEM", "LEM", "SIC", "ETC" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
		final Text tagFieldVal = new Text(bottomComposite, SWT.BORDER);
		addTag = new Button(bottomComposite, SWT.PUSH | SWT.FLAT);
		addTag.setEnabled(word != null);
		addTag.setToolTipText(Messages.get().AddAnnotationTo);
		addTag.setImage(SessionContextSingleton.getInstance().loadImage(
				"icons/add.gif")); //$NON-NLS-1$
		SelectionListener listener = new SelectionListener() {
			@Override
			// on button click
			public void widgetSelected(SelectionEvent e) {
				addTag(tagFieldKey, tagFieldVal);
			}

			@Override
			// on enter in text
			public void widgetDefaultSelected(SelectionEvent e) {
				addTag(tagFieldKey, tagFieldVal);
			}

			private void addTag(final Combo key, final Text val) {
				String inputKey = key.getText();
				String inputVal = val.getText();
				if (inputKey != null && inputKey.trim().length() != 0
						&& inputVal != null && inputVal.trim().length() != 0) {
					word.annotations().$plus$eq(
							new Annotation(inputKey, inputVal,
									SessionContextSingleton.getInstance()
											.getCurrentUser().id(), System
											.currentTimeMillis()));
					page.saveToDb(SessionContextSingleton.getInstance()
							.getCurrentUser().collection(),
							SessionContextSingleton.getInstance().db());
					setTableInput();
					key.setText(""); //$NON-NLS-1$
					val.setText(""); //$NON-NLS-1$
				}
			}
		};
		addTag.addSelectionListener(listener);
		tagFieldKey.addSelectionListener(listener);
		tagFieldVal.addSelectionListener(listener);
	}

	private void updateLabel() {
		if (addTag != null)
			addTag.setEnabled(this.word != null);
		if (label != null) {
			label.setText(String.format(
					Messages.get().AddAnnotationTo + " '%s'", //$NON-NLS-1$
					word == null ? "*" + Messages.get().NoWordSelected + "*" : word //$NON-NLS-1$//$NON-NLS-2$
									.history().top().form()));
			label.getParent().pack();
		}
	}

	private void initTableViewer(final Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		initTable();
		viewer.setContentProvider(new TagViewContentProvider());
		viewer.setLabelProvider(new TagViewLabelProvider());
		setTableInput();
	}

	private void initTable() {
		final int[] columns = new int[] { 150, 150, 350, 250 };
		createColumn(Messages.get().Key, columns[0], viewer);
		createColumn(Messages.get().Value, columns[1], viewer);
		createColumn(Messages.get().User, columns[2], viewer);
		createColumn(Messages.get().Date, columns[3], viewer);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	TableViewerColumn createColumn(final String name, final int width,
			final TableViewer viewer) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(name);
		column.getColumn().setWidth(width);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		return column;
	}

	private void setTableInput() {
		if (word != null) {
			TableHelper.clearWidgets(viewer.getTable());
			viewer.setInput(TagViewModel.CONTENT.getDetails(word));
		}
		addLinks();
	}

	private void addLinks() {
		Table table = viewer.getTable();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			final TableItem item = items[i];
			final String author = ((Annotation) item.getData()).user();
			Link link = TableHelper.insertLink(viewer.getTable(), item, author,
					2);
			item.setData(new Widget[] { link });
		}

	}

}

/**
 * Model, content and label providers for the {@link TagView}.
 * 
 * @author Fabian Steeg (fsteeg)
 */
final class TagViewModel {
	public static final TagViewModel CONTENT = new TagViewModel();

	public Annotation[] getDetails(final Word word) {
		return JavaConversions.asJavaList(word.annotations()).toArray(
				new Annotation[0]);
	}

	static final class TagViewContentProvider implements
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
			if (newInput != null) {
				Annotation[] newTags = (Annotation[]) newInput;
				for (int i = 0; i < newTags.length; i++) {
					viewer.setData(i + "", newTags[i]); //$NON-NLS-1$
				}
			}
		}
	}

	static final class TagViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			Annotation tag = (Annotation) element;
			switch (columnIndex) {
			case 0:
				return tag.key();
			case 1:
				return tag.value();
			case 2:
				return WordViewModel.WordViewLabelProvider.userDetails(tag
						.user());
			case 3:
				return new Date(tag.date()).toString();
			default:
				return null;
			}
		}

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}
	}
}
