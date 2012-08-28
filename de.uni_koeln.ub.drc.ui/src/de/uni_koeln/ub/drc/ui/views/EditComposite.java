/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import scala.collection.JavaConversions;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.data.Word;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.CSSSWTConstantsHelper;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;
import de.uni_koeln.ub.drc.ui.facades.TextHelper;
import de.uni_koeln.ub.drc.ui.util.ViewPartFinder;

/**
 * Composite holding the edit area. Used by the {@link EditView}.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public class EditComposite extends Composite {

	private Composite parent;
	private Page page;
	private boolean commitChanges = false;
	private List<Text> words;
	private List<Composite> lines = new ArrayList<Composite>();
	private Label status;
	private EditView editView;

	/**
	 * @param editView
	 *            The parent edit view for this composite
	 * @param style
	 *            The style bits for this composite
	 */
	public EditComposite(final EditView editView, final int style) {
		super(editView.sc, style);
		this.editView = editView;
		this.parent = editView.sc;
		this.status = editView.label;
		parent.getShell().setBackgroundMode(SWT.INHERIT_DEFAULT);
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);
		commitChanges = true;
		addWrapOnResizeListener(parent);
		setCssName(this);
	}

	/**
	 * @return The text widgets representing the words in the current page
	 */
	public List<Text> getWords() {
		return words;
	}

	/**
	 * @param page
	 *            Update the content of this composite with the given page
	 */
	public void update(final Page page) {
		this.page = page;
		updatePage(parent);
	}

	Page getPage() {
		return page;
	}

	private void addWrapOnResizeListener(final Composite parent) {
		parent.addControlListener(new ControlListener() {
			@Override
			public void controlResized(final ControlEvent e) {
				for (Composite line : lines) {
					if (!line.isDisposed()) {
						setLineLayout(line);
					}
				}
			}

			@Override
			public void controlMoved(final ControlEvent e) {
			}
		});
	}

	private void updatePage(final Composite parent) {
		words = addTextFrom(page, this);
	}

	private List<Text> addTextFrom(final Page page, final Composite c) {
		if (lines != null) {
			for (Composite line : lines) {
				line.dispose();
			}
		}
		List<Text> list = new ArrayList<Text>();
		this.page = page;
		Composite lineComposite = new Composite(c, SWT.NONE);
		setLineLayout(lineComposite);
		if (lines != null)
			lines.add(lineComposite);
		for (Word word : JavaConversions.asJavaIterable(page.words())) {
			if (word.original().equals(Page.ParagraphMarker())) {
				lineComposite = new Composite(c, SWT.NONE);
				setLineLayout(lineComposite);
				lines.add(lineComposite);
			} else {
				Text text = new Text(lineComposite, SWT.NONE);
				setCssName(text);
				text.setText(TextHelper.fixForDisplay(word.history().top()
						.form()));
				handleEmptyText(text);
				// option: word.isPossibleError() ? UNCHECKED : DEFAULT
				text.setForeground(parent.getDisplay().getSystemColor(DEFAULT));
				text.setData(Word.class.toString(), word);
				text.setData(Page.class.toString(), page);
				addListeners(text);
				text.setEditable(!word.isLocked() && !page.done());
				list.add(text);
			}
		}
		this.layout();
		return list;
	}

	private void setCssName(final Control control) {
		control.setData(CSSSWTConstantsHelper.getCSS(), "editComposite"); //$NON-NLS-1$
	}

	private void setLineLayout(final Composite lineComposite) {
		setCssName(lineComposite);
		RowLayout layout = new RowLayout();
		GridData data = new GridData();
		int scrollBarSize = ((Integer) UIManager.get("ScrollBar.width")); //$NON-NLS-1$
		data.widthHint = lineComposite.computeSize(getSize().x, getSize().y).x
				- scrollBarSize;
		lineComposite.setLayoutData(data);
		lineComposite.setLayout(layout);
	}

	private void addListeners(final Text text) {
		addFocusListener(text);
		addModifyListener(text);
	}

	private Text prev;
	final static int ACTIVE = SWT.COLOR_DARK_GREEN;
	final static int DUBIOUS = SWT.COLOR_RED;
	final static int DEFAULT = SWT.COLOR_BLACK;
	final static int UNCHECKED = SWT.COLOR_BLACK; // no coloring for now

	/**
	 * @return The previous text widget
	 */
	public Text getPrev() {
		return prev;
	}

	private void addModifyListener(final Text text) {
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				User user = SessionContextSingleton.getInstance()
						.getCurrentUser();
				user.latestPage_$eq(page.id());
				user.latestWord_$eq(words.indexOf(text));
				/*
				 * Reset any warning color during editing (we check when focus
				 * is lost, see below):
				 */
				text.setForeground(text.getDisplay().getSystemColor(ACTIVE));
				if (commitChanges) {
					editView.setDirty(true);
				}
				handleEmptyText(text);
				text.pack(true);
				text.getParent().layout();
				/*
				 * Workaround: on Windows, when adding text at the end of the
				 * widget, text at the beginning is pushed out of the widget and
				 * not visible - so we jump to beginning and then back:
				 */
				int pos = text.getCaretPosition();
				text.setSelection(0);
				text.setSelection(pos);
			}

		});
	}

	private void handleEmptyText(final Text text) {
		if (text.getText().length() == 0) {
			text.setText("\u2026"); // ellipsis //$NON-NLS-1$
		}
	}

	private void addFocusListener(final Text text) {
		text.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				prev = text; // remember so we can clear only when new focus
				// gained, not when lost
				// FIXME: Wrong layout of '&' in rap
				checkWordValidity(text);
			}

			private void checkWordValidity(final Text text) {
				String current = text.getText();
				Word word = (Word) text.getData(Word.class.toString());
				String reference = word.history().top().form();
				if (current.length() != reference.length()
						|| (current.contains(" ") && !reference.contains(" "))) { //$NON-NLS-1$ //$NON-NLS-2$
					text.setForeground(text.getDisplay()
							.getSystemColor(DUBIOUS));
					setMessage(Messages.get().YourRecentEdit);
				} else {
					status.setText(""); //$NON-NLS-1$
				}
			}

			@Override
			public void focusGained(final FocusEvent e) {
				text.clearSelection(); // use only our persistent marking below
				Word word = (Word) text.getData(Word.class.toString());
				if (word.isLocked()) {
					setMessage(String.format(
							Messages.get().Entry
									+ " '%s' " + Messages.get().IsLocked, text.getText())); //$NON-NLS-1$
				}
				text.setEditable(!word.isLocked() && !page.done());
				ViewPartFinder.find(CheckView.class).setSelection(text);
				ViewPartFinder.find(SpecialCharacterView.class).setText(text);
				ViewPartFinder.find(WordView.class).selectedWord(word, text);
				ViewPartFinder.find(TagView.class).setWord(word);
				text.setToolTipText(word.formattedHistory());
				if (prev != null
						&& !prev.isDisposed()
						&& !prev.getForeground().equals(
								text.getDisplay().getSystemColor(DUBIOUS))) {
					prev.setForeground(text.getDisplay()
							.getSystemColor(DEFAULT));
				}
				text.setForeground(text.getDisplay().getSystemColor(ACTIVE));
			}

			private void setMessage(String t) {
				status.setText(t);
				status.setForeground(status.getDisplay()
						.getSystemColor(DUBIOUS));
			}
		});
	}
}
