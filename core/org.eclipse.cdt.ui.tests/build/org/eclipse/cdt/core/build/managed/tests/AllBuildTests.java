/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.cdt.core.build.managed.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.cdt.core.build.managed.BuildException;
import org.eclipse.cdt.core.build.managed.IConfiguration;
import org.eclipse.cdt.core.build.managed.IOption;
import org.eclipse.cdt.core.build.managed.IOptionCategory;
import org.eclipse.cdt.core.build.managed.ITarget;
import org.eclipse.cdt.core.build.managed.ITool;
import org.eclipse.cdt.core.build.managed.ManagedBuildManager;
import org.eclipse.cdt.internal.core.build.managed.ToolReference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 */
public class AllBuildTests extends TestCase {

	public AllBuildTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite();
		
		suite.addTest(new AllBuildTests("testExtensions"));
		suite.addTest(new AllBuildTests("testProject"));
		
		return suite;
	}

	public void testThatAlwaysFails() {
		assertTrue(false);
	}
	
	/**
	 * Navigates through the build info as defined in the extensions
	 * defined in this plugin
	 */
	public void testExtensions() {
		ITarget testRoot = null;
		ITarget testSub = null;
		
		// Note secret null parameter which means just extensions
		ITarget[] targets = ManagedBuildManager.getDefinedTargets(null);

		for (int i = 0; i < targets.length; ++i) {
			ITarget target = targets[i];
			
			if (target.getName().equals("Test Root")) {
				testRoot = target;
				
				checkRootTarget(testRoot);
				
			} else if (target.getName().equals("Test Sub")) {
				testSub = target;
				
				// Tools
				ITool[] tools = testSub.getTools();
				// Root Tool
				ITool rootTool = tools[0];
				assertEquals("Root Tool", rootTool.getName());
				// Sub Tool
				ITool subTool = tools[1];
				assertEquals("Sub Tool", subTool.getName());

				// Configs
				IConfiguration[] configs = testSub.getConfigurations();
				// Root Config
				IConfiguration rootConfig = configs[0];
				assertEquals("Root Config", rootConfig.getName());
				assertEquals("Root Override Config", configs[1].getName());
				// Sub Config
				IConfiguration subConfig = configs[2];
				assertEquals("Sub Config", subConfig.getName());
			}
		}
		
		assertNotNull(testRoot);
		assertNotNull(testSub);
	}
	
	public void testProject() throws CoreException, BuildException {
		// Create new project
		IProject project = createProject("BuildTest");
		
		assertEquals(0, ManagedBuildManager.getTargets(project).length);
		
		// Find the base target definition
		ITarget targetDef = ManagedBuildManager.getTarget(project, "test.root");
		assertNotNull(targetDef);
		
		// Create the target for our project
		ITarget newTarget = ManagedBuildManager.createTarget(project, targetDef);
		assertEquals(newTarget.getName(), targetDef.getName());
		assertFalse(newTarget.equals(targetDef));
		
		ITarget[] targets = ManagedBuildManager.getTargets(project);
		assertEquals(1, targets.length);
		ITarget target = targets[0];
		assertEquals(target, newTarget);
		assertFalse(target.equals(targetDef));
		
		checkRootTarget(target);
		
		// Save, close, reopen and test again
		ManagedBuildManager.saveBuildInfo(project);
		project.close(null);
		ManagedBuildManager.removeBuildInfo(project);
		project.open(null);
		
		targets = ManagedBuildManager.getTargets(project);
		assertEquals(1, targets.length);
		checkRootTarget(targets[0]);
	}
	
	IProject createProject(String name) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
        
		if (!project.isOpen()) {
			project.open(null);
		}
		
		//CCorePlugin.getDefault().convertProjectToC(project, null, CCorePlugin.PLUGIN_ID + ".make", true);

		return project;	
	}
	
	private void checkRootTarget(ITarget target) {
		// Tools
		ITool[] tools = target.getTools();
		// Root Tool
		ITool rootTool = tools[0];
		assertEquals("Root Tool", rootTool.getName());
		// Options
		IOption[] options = rootTool.getOptions();
		assertEquals(2, options.length);
		assertEquals("Option in Top", options[0].getName());
		assertEquals("Option in Category", options[1].getName());
		// Option Categories
		IOptionCategory topCategory = rootTool.getTopOptionCategory();
		assertEquals("Root Tool", topCategory.getName());
		options = topCategory.getOptions(null);
		assertEquals(1, options.length);
		assertEquals("Option in Top", options[0].getName());
		IOptionCategory[] categories = topCategory.getChildCategories();
		assertEquals(1, categories.length);
		assertEquals("Category", categories[0].getName());
		options = categories[0].getOptions(null);
		assertEquals(1, options.length);
		assertEquals("Option in Category", options[0].getName());

		// Configs
		IConfiguration[] configs = target.getConfigurations();
		// Root Config
		IConfiguration rootConfig = configs[0];
		assertEquals("Root Config", rootConfig.getName());
		// Tools
		tools = rootConfig.getTools();
		assertEquals(1, tools.length);
		assertEquals("Root Tool", tools[0].getName());
		// Root Override Config
		assertEquals("Root Override Config", configs[1].getName());
		tools = configs[1].getTools();
		assertTrue(tools[0] instanceof ToolReference);
		options = tools[0].getOptions();
	}
	
}
