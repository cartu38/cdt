/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.parser.scanner;

public class TokenWithImage extends Token {
	private char[] fImage;

	public TokenWithImage(int kind, Object source, int offset, int endOffset, char[] image) {
		super(kind, source, offset, endOffset);
		fImage = image;
	}

	@Override
	public char[] getCharImage() {
		return fImage;
	}
}
