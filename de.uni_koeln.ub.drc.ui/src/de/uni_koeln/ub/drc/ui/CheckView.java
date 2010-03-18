/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.services.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.uni_koeln.ub.drc.data.Box;
import de.uni_koeln.ub.drc.data.Page;
import de.uni_koeln.ub.drc.data.Word;

/**
 * View containing the scanned page used to check the original word while editing.
 * @author Fabian Steeg (fsteeg)
 */
public final class CheckView {
  private URL jpgFile;
  private Composite parent;
  private Label imageLabel;
  private boolean imageLoaded = false;
  private ScrolledComposite scrolledComposite;

  @Inject public CheckView(final Composite parent) {
    this.parent = parent;
    scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
    imageLabel = new Label(scrolledComposite, SWT.BORDER);
    scrolledComposite.setContent(imageLabel);
    scrolledComposite.setContent(imageLabel);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setMinSize(imageLabel.computeSize(SWT.MAX, SWT.MAX));
  }

  @Inject public void setSelection(@Optional @Named( IServiceConstants.SELECTION ) final Page page) {
    if (page != null) {
      try {
        updateImage(page);
      } catch (MalformedURLException e) {
        MessageDialog.openError(parent.getShell(), "Could not load scan",
            "Could not load the image file for the current page");
        e.printStackTrace();
      }
    } else {
      return;
    }
  }

  @Inject public void setSelection(@Optional @Named( IServiceConstants.SELECTION ) final Text word) {
    if (imageLoaded) {
      if (word != null) {
        markPosition(word);
      } else {
        clearMarker();
        return;
      }
    }
  }

  private void updateImage(final Page page) throws MalformedURLException {
    String folderName = "pages";
    String pageName = page.id().substring(0, page.id().lastIndexOf('.'));
    jpgFile = EditComposite.fileFromBundle(folderName + "/" + pageName + ".jpg").toURI().toURL();
    final Image image = loadImage();
    imageLabel.setImage(image);
    imageLoaded = true;
  }

  private Image loadImage() {
    Display display = parent.getDisplay();
    Image newImage = null;
    try {
      newImage = new Image(display, jpgFile.openStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return newImage;
  }

  private void markPosition(final Text text) {
    Word word = (Word) text.getData();
    Box box = word.position();
    Rectangle rect = new Rectangle(box.x() - 15, box.y() - 5, box.width() + 65, box.height() + 20);
    System.out.println("Current word: " + word);
    Image image = loadImage();
    GC gc = new GC(image);
    drawBoxArea(rect, gc);
    drawBoxBorder(rect, gc);
    imageLabel.setImage(image);
    scrolledComposite.setOrigin(new Point(box.x(), box.y()));
  }

  private void drawBoxBorder(final Rectangle rect, final GC gc) {
    gc.setAlpha(200);
    gc.setLineWidth(5);
    gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
    gc.drawRectangle(rect);
  }

  private void drawBoxArea(final Rectangle rect, final GC gc) {
    gc.setAlpha(50);
    gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GREEN));
    gc.fillRectangle(rect);
  }

  private void clearMarker() {
    Image image = loadImage();
    imageLabel.setImage(image);
  }

}
