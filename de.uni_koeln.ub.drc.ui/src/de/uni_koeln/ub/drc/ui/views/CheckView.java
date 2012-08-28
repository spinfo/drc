/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.uni_koeln.ub.drc.data.Box;
import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.data.Word;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * View containing the scanned page used to check the original word while
 * editing.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public final class CheckView extends ViewPart {

	/**
	 * The class / CheckView ID
	 */
	public static final String ID = CheckView.class.getName().toLowerCase();

	private Composite parent;
	private Canvas imageCanvas;
	private boolean imageLoaded = false;
	private ScrolledComposite scrolledComposite;
	private ImageData imageData;
	private Label suggestions;
	private Job job;
	private Button check;
	private Text word;
	private List<Button> suggestionButtons = new ArrayList<Button>();
	private Composite bottom;
	private Page page;
	private Composite zoomBottom;
	private Scale scale;
	private float scaleFactor = 0.3F;
	private Image cachedImage;

	@Override
	public void setFocus() {
	}

	/**
	 * @param parent
	 *            The parent composite for this part
	 */
	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		scale();
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.BORDER);
		imageCanvas = new Canvas(scrolledComposite, SWT.BORDER | SWT.CENTER);
		scrolledComposite.setContent(imageCanvas);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		addSuggestions();
		attachSelectionListener();
		imageCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (word != null && !word.isDisposed()) {
					markPosition(word, e.gc, wordFromWidget(word));
				}
			}
		});
		GridLayoutFactory.fillDefaults().generateLayout(parent);
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
					List<Page> pages = Arrays.asList((Page) structuredSelection
							.getFirstElement());
					setSelection(pages);
				}
			}
		});

	}

	/**
	 * @param pages
	 *            The selected pages
	 */
	public void setSelection(final List<Page> pages) {
		if (pages != null && pages.size() > 0
				&& (page == null || !page.equals(pages.get(0)))) {
			page = pages.get(0);
			try {
				updateImage(page);
			} catch (MalformedURLException e) {
				handle(e);
			} catch (IOException e) {
				handle(e);
			}
		} else {
			return;
		}
	}

	/**
	 * @param text
	 *            The active text widget
	 */
	public void setSelection(final Text text) {
		Word word = null;
		if (imageLoaded && text != null && !text.isDisposed()
				&& (word = wordFromWidget(text)) != null) {
			this.word = text;

			imageCanvas.redraw();
			scrollTo(word);
			if (job != null) {
				/*
				 * If a word is selected while we had a Job running for the
				 * previous word, cancel that:
				 */
				job.cancel();
			}
			if (!check.getSelection()) {
				suggestions.setText(Messages.get().EditSuggestionsDisabled);
				disposeButtons();
			} else if (word.isLocked()) {
				suggestions
						.setText(Messages.get().NoEditSuggestionsWordIsLocked);
			} else {
				findEditSuggestions(word, text);
				job.setPriority(Job.DECORATE);
				job.schedule();
			}
		} else if (text == null) {
			suggestions.setText(Messages.get().NoWordSelected);
		}
	}

	private void scrollTo(Word word) {
		Point p = newOrigin(word.position());
		scrolledComposite.setOrigin(p);
	}

	private void scale() {
		zoomBottom = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		zoomBottom.setLayout(layout);
		Label zoomLabel = new Label(zoomBottom, SWT.NONE);
		zoomLabel.setText(Messages.get().Zoom);
		Label downScaleLabel = new Label(zoomBottom, SWT.NONE);
		downScaleLabel.setText(Messages.get().Minus);
		scale = new Scale(zoomBottom, SWT.NONE);
		Label upScaleLabel = new Label(zoomBottom, SWT.NONE);
		upScaleLabel.setText(Messages.get().Plus);
		Rectangle clientArea = zoomBottom.getClientArea();
		scale.setBounds(clientArea.x, clientArea.y, 200, 64);
		scale.setOrientation(SWT.LEFT_TO_RIGHT);
		scale.setMinimum(30);
		scale.setMaximum(100);
		scale.setSelection(30);
		scale.setToolTipText(Messages.get().ZoomToolTip);
		addListenerToScaleWidget();
	}

	private void addListenerToScaleWidget() {
		scale.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scaleFactor = scale.getSelection() / 100F;
				scale.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							updateImage(page);
						} catch (IOException e) {
							e.printStackTrace();
						}
						imageCanvas.redraw();
						if (word != null && !word.isDisposed()) {
							scrollTo(wordFromWidget(word));
						}
					}
				});
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	private void addSuggestions() {
		bottom = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(7, false);
		bottom.setLayout(layout);
		check = new Button(bottom, SWT.CHECK);
		check.setToolTipText(Messages.get().SuggestCorrections);
		check.setSelection(false);
		check.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSelection(word);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		suggestions = new Label(bottom, SWT.WRAP);
		suggestions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void displaySuggestionButtons(final Word word, final Text text) {
		bottom.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				String words = word.suggestions().mkString(" "); //$NON-NLS-1$
				for (String string : words.split(" ")) { //$NON-NLS-1$
					final Button b = new Button(bottom, SWT.FLAT);
					b.setLayoutData(new GridData(SWT.NONE));
					b.setText(string);
					b.addSelectionListener(new SelectionListener() {

						@Override
						public void widgetSelected(SelectionEvent e) {
							if (text != null) {
								text.setText(b.getText());
								text.setSelection(text.getCaretPosition());
								text.setFocus();
							}
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
					suggestionButtons.add(b);
				}
				bottom.pack();
				bottom.redraw();
			}
		});
	}

	private void disposeButtons() {
		if (!suggestionButtons.isEmpty()) {
			for (Button b : suggestionButtons) {
				b.dispose();
			}
		}
		suggestionButtons = new ArrayList<Button>();
	}

	private void drawBoxArea(final Rectangle rect, final GC gc) {
		gc.setAlpha(50);
		gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		gc.fillRectangle(rect);
	}

	private void drawBoxBorder(final Rectangle rect, final GC gc) {
		gc.setAlpha(200);
		gc.setLineWidth(2);
		gc.setForeground(parent.getDisplay().getSystemColor(
				SWT.COLOR_DARK_GREEN));
		gc.drawRectangle(rect);
	}

	private void findEditSuggestions(final Word word, final Text text) {
		disposeButtons();
		suggestions.setText(Messages.get().FindingEditSuggestions);
		job = new Job(Messages.get().EditSuggestionsSearchJob) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				final boolean complete = word.prepSuggestions();
				suggestions.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!complete) {
							suggestions.setText(Messages.get().FindingEditSuggestions);
						} else {
							String info = (word.suggestions().size() == 0) ? Messages
									.get().NoReasonableEditSuggestionsFound
									: String.format(
											Messages.get().SuggestionsFor
													+ " %s (" + Messages.get().Originally //$NON-NLS-1$
													+ " '%s'):", word.history().top() //$NON-NLS-1$
													.form(), word.original());

							if (!bottom.isDisposed())
								displaySuggestionButtons(word, text);

							if (!suggestions.isDisposed())
								suggestions.setText(info);
						}
					}
				});
				return Status.OK_STATUS;
			}

			@Override
			protected void canceling() {
				word.cancelled_$eq(true);
				suggestions.setText(Messages.get().FindingEditSuggestions);
			};
		};
	}

	private void handle(Exception e) {
		MessageDialog.openError(parent.getShell(),
				Messages.get().CouldNotLoadScan,
				Messages.get().CouldNotLoadImageForCurrentPage);
		e.printStackTrace();
	}

	private Image loadImage(final Page page) throws IOException {
		final InputStream in = getInputStream(page);
		imageData = new ImageData(in);
		Image newImage = new Image(parent.getDisplay(), imageData);
		return newImage;
	}

	private void markPosition(final Text text, GC gc, Word word) {
		if (imageCanvas.getBackgroundImage() != null) {
			Box box = word.position();
			Rectangle rect = getScaledRect(box);
			drawBoxArea(rect, gc);
			drawBoxBorder(rect, gc);
			scrolledComposite.setMinSize(imageCanvas.getBackgroundImage()
					.getBounds().width, imageCanvas.getBackgroundImage()
					.getBounds().height);
			scrolledComposite.layout(true, true);
			Point p = newOrigin(box);
			scrolledComposite.setOrigin(p);
		}
	}

	private void cacheImage(final Image img) {
		if (cachedImage != null)
			cachedImage.dispose();
		cachedImage = new Image(parent.getDisplay(), img, SWT.IMAGE_COPY);
		if (scaleFactor < 1) {
			cachedImage = scaleImage(cachedImage);
		}
		imageCanvas.setBackgroundImage(cachedImage);
	}

	private Point newOrigin(Box box) {
		int x = (int) (box.x() * scaleFactor);
		int y = (int) (box.y() * scaleFactor);
		int minX = Math.min(x, (x / 2));
		return new Point(x - minX, y - 65);
	}

	private Image scaleImage(final Image image) {
		Rectangle rect = image.getBounds();
		ImageData data = image.getImageData().scaledTo(
				(int) (rect.width * scaleFactor),
				(int) (rect.height * scaleFactor));
		image.dispose();
		return new Image(parent.getDisplay(), data);
	}

	private Rectangle getScaledRect(Box box) {
		int startX = (int) ((scaleFactor * box.x()) - (15 * scaleFactor));
		int startY = (int) ((scaleFactor * box.y()) - (6 * scaleFactor));
		int boxWidth = (int) ((scaleFactor * box.width()) + (25 * scaleFactor));
		int boxHeight = (int) ((scaleFactor * box.height()) + (18 * scaleFactor));
		return new Rectangle(startX, startY, boxWidth, boxHeight);
	}

	private void updateImage(final Page page) throws IOException {
		if (imageCanvas != null && imageCanvas.getBackgroundImage() != null
				&& !imageCanvas.getBackgroundImage().isDisposed())
			imageCanvas.getBackgroundImage().dispose();
		cacheImage(loadImage(page));
		Image displayedImage = new Image(parent.getDisplay(),
				cachedImage.getImageData());
		imageData = displayedImage.getImageData();
		if (imageCanvas != null)
			imageCanvas.setBackgroundImage(displayedImage);
		imageLoaded = true;
		scrolledComposite.setMinSize(displayedImage.getBounds().width,
				displayedImage.getBounds().height);
		scrolledComposite.layout(true, true);
	}

	private InputStream getInputStream(final Page page) {
		InputStream in = new BufferedInputStream(new ByteArrayInputStream(
				Index.loadImageFor(SessionContextSingleton.getInstance()
						.getCurrentUser().collection(), SessionContextSingleton
						.getInstance().db(), page)));
		return in;
	}

	private Word wordFromWidget(Text text) {
		return (Word) text.getData(Word.class.toString());
	}

}