/*******************************************************************************
 * Copyright (c) 2016 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.wizards.project;

import org.eclipse.tools.templates.ui.NewWizard;

import org.eclipse.cdt.internal.ui.CUIMessages;

public class NewCDTProjectWizard extends NewWizard {

	private static final String cdtTag = "org.eclipse.cdt.ui.cdtTag"; //$NON-NLS-1$

	public NewCDTProjectWizard() {
		super(cdtTag);
		setWindowTitle(CUIMessages.NewCDTProjectWizard_Title);
		setTemplateSelectionPageTitle(CUIMessages.NewCDTProjectWizard_PageTitle);
	}

}