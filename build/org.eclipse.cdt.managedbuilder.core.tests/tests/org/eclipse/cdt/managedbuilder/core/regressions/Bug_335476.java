/*******************************************************************************
 * Copyright (c) 2011 Broadcom Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Broadcom Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.core.regressions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.managedbuilder.testplugin.AbstractBuilderTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;

/**
 * This tests that an environment variable, which is part of the build
 * (in this case referenced by a -I), makes it through to makefile
 * correctly when it changes.
 */
public class Bug_335476 extends AbstractBuilderTest {

	IProject app;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setWorkspace("regressions");
		app = loadProject("bug_335476");
		// Ensure Debug is the active configuration
		setActiveConfigurationByName(app, "Debug");
	}

	/**
	 * Build the project a few times, changing the value of the environment variable each time
	 * @param build_kind
	 * @throws Exception
	 */
	public void runTest(int build_kind) throws Exception {
		// Environment containingg the "Lala" environment variable
		final IFile lala = app.getFile(new Path(".settings/org.eclipse.cdt.core.prefs.lala"));
		// Environment containing the "Foo" environment variable
		final IFile foo = app.getFile(new Path(".settings/org.eclipse.cdt.core.prefs.foo"));

		final IFile env = app.getFile(new Path(".settings/org.eclipse.cdt.core.prefs"));

		IFile current = foo;
		for (int i = 0; i < 5; i++) {
			// Update the environment to reflect the new value.
			env.setContents(current.getContents(), IResource.NONE, null);

			// Ask for a full build
			app.build(build_kind, null);

			// Check the makefile for the correct environment
			IFile makefile = app.getFile("Debug/src/subdir.mk");
			BufferedReader reader = new BufferedReader(new InputStreamReader(makefile.getContents()));
			try {
				Pattern p = Pattern.compile(".*?-I.*?\"(.*?)\".*");
				while (reader.ready()) {
					String line = reader.readLine();
					if (!line.contains("gcc"))
						continue;
					Matcher m = p.matcher(line);
					assertTrue(m.matches());
					String variable = m.group(1);
					if (current == foo)
						assertTrue("foo exepected, but was: " + variable ,variable.equals("foo"));
					else
						assertTrue("foo exepected, but was: " + variable, variable.equals("lala"));
				}
			} finally {
				reader.close();
			}

			// Change environment
			if (current == lala)
				current = foo;
			else
				current = lala;
		}
	}

	public void testChangingEnvironmentBuildSystem_FULL_BUILD() throws Exception {
		runTest(IncrementalProjectBuilder.FULL_BUILD);
	}

	public void testChangingEnvironmentBuildSystem_INC_BUILD() throws Exception {
		runTest(IncrementalProjectBuilder.INCREMENTAL_BUILD);
	}

}
