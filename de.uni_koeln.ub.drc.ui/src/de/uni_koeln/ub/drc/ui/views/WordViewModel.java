/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.views;

import java.util.Date;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import scala.collection.JavaConversions;
import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.Modification;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.data.Word;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * Model, content and label providers for the {@link WordView}.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
final class WordViewModel {

	public static final WordViewModel CONTENT = new WordViewModel();

	public Modification[] getDetails(final Word word) {
		return JavaConversions.asJavaCollection(word.history()).toArray(
				new Modification[] {});
	}

	static final class WordViewContentProvider implements
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
				Modification[] newMods = (Modification[]) newInput;
				for (int i = 0; i < newMods.length; i++) {
					viewer.setData(i + "", newMods[i]); //$NON-NLS-1$
				}
			}
		}
	}

	static final class WordViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			Modification modification = (Modification) element;
			switch (columnIndex) {
			case 0:
				return modification.form();
			case 1:
				return userDetails(modification.author());
			case 2:
				return new Date(modification.date()).toString();
			case 3:
				return modification.score() + ""; //$NON-NLS-1$
			default:
				return null;
			}
		}

		static String userDetails(String id) {
			if (id.equals("OCR"))return "--"; //$NON-NLS-1$ //$NON-NLS-2$
			User user = User.withId(Index.DefaultCollection(),
					SessionContextSingleton.getInstance().getUserDb(), id);
			return String
					.format("%s " + Messages.get().From + " %s (%s, %s)", user.name(), user.region(), user.id(), //$NON-NLS-1$ //$NON-NLS-2$
							user.reputation());
		}

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}
	}
}