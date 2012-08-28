/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.Modification;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.data.Word;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;
import de.uni_koeln.ub.drc.ui.views.WordViewModel.WordViewContentProvider;
import de.uni_koeln.ub.drc.ui.views.WordViewModel.WordViewLabelProvider;

/**
 * View containing details for the currently selected word.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public final class WordView extends ViewPart {

	/**
	 * The class / WordView ID
	 */
	public static final String ID = WordView.class.getName().toLowerCase();

	private TableViewer viewer;
	private Page page;
	private Text text;
	private Word word;

	/**
	 * @param parent
	 *            The parent composite for this part
	 */

	@Override
	public void createPartControl(final Composite parent) {
		initTableViewer(parent);
		GridLayoutFactory.fillDefaults().generateLayout(parent);
		attachSelectionListener();
	}

	@Override
	public void setFocus() {
	}

	/**
	 * @param word
	 *            The selected word
	 * @param text
	 *            The selected text widget
	 */
	public void selectedWord(final Word word, final Text text) {
		this.text = text;
		this.word = word;
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
				if (structuredSelection.getFirstElement() instanceof Text) {
					if (text != null
							&& (word = (Word) text.getData(Word.class
									.toString())) != null
							&& (page = (Page) text.getData(Page.class
									.toString())) != null) {
						setText(text);
					}
					setTableInput();
				}
			}
		});
	}

	private void setText(Text text) {
		this.text = text;
	}

	private void initTableViewer(final Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		initTable();
		viewer.setContentProvider(new WordViewContentProvider());
		viewer.setLabelProvider(new WordViewLabelProvider());
		setTableInput();
	}

	private void initTable() {
		final int[] columns = new int[] { 185, 350, 300, 80, 80, 80, 80 };
		createColumn(Messages.get().Form, columns[0], viewer);
		createColumn(Messages.get().Author, columns[1], viewer);
		createColumn(Messages.get().Date, columns[2], viewer);
		createColumn(Messages.get().Votes, columns[3], viewer);
		createColumn(Messages.get().Upvote, columns[4], viewer);
		createColumn(Messages.get().Downvote, columns[5], viewer);
		createColumn(Messages.get().Revert, columns[6], viewer);
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
			viewer.setInput(WordViewModel.CONTENT.getDetails(word));
			addWidgets();
		}
	}

	private void addWidgets() {
		TableItem[] items = viewer.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			final TableItem item = items[i];
			final int index = i;
			final String author = ((Modification) item.getData()).author();
			Link link = TableHelper.insertLink(viewer.getTable(), item, author,
					1);
			Button up = addVoteButton(item, index, Vote.UP, 4);
			Button down = addVoteButton(item, index, Vote.DOWN, 5);
			Button rev = addRevertButton(item, index, 6);
			item.setData(new Widget[] { up, down, rev, link });
		}
	}

	private Button addRevertButton(final TableItem item, final int index,
			int col) {
		final Modification modification = (Modification) viewer.getData(index
				+ ""); //$NON-NLS-1$
		if (!word.history().top().equals(modification)) { // no revert for most
			// recent modification
			Button button = createButton(item, SessionContextSingleton
					.getInstance().loadImage("icons/revert.gif"), //$NON-NLS-1$
					col);
			button.setEnabled(!word.isLocked());
			button.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					/*
					 * If we revert to a previous modification the currently
					 * most recent modification is voted down, and the
					 * modification that we are reverting to is voted up:
					 */
					User currentUser = SessionContextSingleton.getInstance()
							.getCurrentUser();
					vote(word.history().top(), currentUser, Vote.DOWN);
					vote(modification, currentUser, Vote.UP);
					MessageDialog.openInformation(item.getParent().getShell(),
							Messages.get().Reverted, Messages.get().RevertedTo
									+ modification);
					text.setText(modification.form());
				}

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
				}
			});
			return button;
		}
		return null;
	}

	private static enum Vote {
		UP {
			@Override
			void update(Modification mod, User author, User voter) {
				mod.upvote(voter.id());
				voter.hasUpvoted(); // TODO pass to single vote(Vote) method
				author.wasUpvoted();
			}
		},
		DOWN {
			@Override
			void update(Modification mod, User author, User voter) {
				mod.downvote(voter.id());
				voter.hasDownvoted();
				author.wasDownvoted();
			}
		};
		abstract void update(Modification modification, User author, User voter);
	}

	private Button addVoteButton(final TableItem item, final int index,
			final Vote vote, int col) {
		Button button = createButton(
				item,
				vote == Vote.UP ? SessionContextSingleton.getInstance()
						.loadImage("icons/up.gif") : SessionContextSingleton.getInstance().loadImage("icons/down.gif"), col); //$NON-NLS-1$ //$NON-NLS-2$ 
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				Modification modification = (Modification) viewer.getData(index
						+ ""); //$NON-NLS-1$
				if (currentUserMayVote(modification)) {
					vote(modification, SessionContextSingleton.getInstance()
							.getCurrentUser(), vote);
					MessageDialog.openInformation(item.getParent().getShell(),
							Messages.get().Vote + vote, Messages.get().Voted
									+ modification + ": " + vote); //$NON-NLS-1$
					text.setEditable(!word.isLocked());
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
			}
		});
		return button;
	}

	private void vote(Modification modification, User voter, Vote vote) {
		if (!modification.voters().contains(voter.id())) {
			XmlDb db = SessionContextSingleton.getInstance().db();
			XmlDb userDb = SessionContextSingleton.getInstance().getUserDb();
			User author = User.withId(Index.DefaultCollection(), userDb,
					modification.author());
			vote.update(modification, author, voter);
			page.saveToDb(author.collection(), db);
			voter.save(userDb);
			author.save(userDb);
			setTableInput();
		}
	}

	private boolean currentUserMayVote(Modification modification) {
		User user = SessionContextSingleton.getInstance().getCurrentUser();
		if (modification.author().equals(user.id())) {
			MessageDialog.openWarning(viewer.getControl().getShell(),
					Messages.get().CannotVoteForOwnShort,
					Messages.get().CannotVoteForOwnLong);
			return false;
		}
		if (modification.voters().contains(user.id())) {
			MessageDialog.openWarning(viewer.getControl().getShell(),
					Messages.get().CanVoteOnlyOnceShort,
					Messages.get().CanVoteOnlyOnceLong);
			return false;
		}
		return true;
	}

	private Button createButton(final TableItem item, final Image label,
			int columnIndex) {
		TableEditor editor = new TableEditor(viewer.getTable());
		Button button = new Button(viewer.getTable(), SWT.PUSH | SWT.FLAT);
		button.setImage(label);
		button.pack();
		editor.minimumWidth = 15;
		editor.minimumHeight = 15;
		editor.horizontalAlignment = SWT.LEFT;
		editor.setEditor(button, item, columnIndex);
		return button;
	}

}
