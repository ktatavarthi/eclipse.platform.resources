/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.properties;

import java.util.Iterator;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.internal.properties.*;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests for the conversion from the old to the new local history 
 * implementation.
 */
public class PropertyConversionTest extends ResourceTest {

	public static Test suite() {
		TestSuite suite = new TestSuite(PropertyConversionTest.class);
		return suite;
	}

	public PropertyConversionTest(String name) {
		super(name);
	}

	private void compare(final String tag, IWorkspace workspace, final IPropertyManager base, final IPropertyManager another) {
		try {
			workspace.getRoot().accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					Map baseProperties = base.getProperties(resource);
					base.closePropertyStore(resource);
					Map otherProperties = another.getProperties(resource);
					assertEquals(tag + ".1 - " + resource.getFullPath(), baseProperties.size(), otherProperties.size());
					for (Iterator i = baseProperties.keySet().iterator(); i.hasNext();) {
						String propertyKey = (String) i.next();
						assertEquals(tag + ".2 - " + resource.getFullPath(), baseProperties.get(propertyKey), otherProperties.get(propertyKey));
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
			fail(tag + ".99", e);
		}
	}

	public void testConversion() {
		PropertyManager original = null;
		try {
			IProject project1 = getWorkspace().getRoot().getProject("proj1");
			IFile file11 = project1.getFile("file11.txt");
			IFolder folder11 = project1.getFolder("folder11");
			IFile file111 = folder11.getFile("file111.txt");
			IProject project2 = getWorkspace().getRoot().getProject("proj2");
			IFile file21 = project2.getFile("file21.txt");
			IFolder folder21 = project2.getFolder("folder21");
			IFile file211 = folder21.getFile("file211.txt");

			IResource[] files = {file11, file111, file21, file211};
			ensureExistsInWorkspace(files, true);

			original = new PropertyManager((Workspace) getWorkspace());
			final PropertyManager tmpOriginal = original;
			try {
				// set properties on all resources
				getWorkspace().getRoot().accept(new IResourceVisitor() {
					private int counter;

					public boolean visit(IResource resource) throws CoreException {
						for (int i = 0; i < 5; i++) {
							tmpOriginal.setProperty(resource, new QualifiedName(ResourceTest.PI_RESOURCES_TESTS, "property." + counter), "value." + counter);
							counter++;
						}
						tmpOriginal.closePropertyStore(resource);
						return true;
					}
				});
				// close existing history store so all data is committed
				original.shutdown(getMonitor());
			} catch (CoreException e) {
				fail("0.1", e);
			}
			// do the conversion
			PropertyManager2 destination = new PropertyManager2((Workspace) getWorkspace());
			new PropertyStoreConverter().convertProperties((Workspace) getWorkspace(), destination);

			// reopen history store for comparison
			original = new PropertyManager((Workspace) getWorkspace());
			compare("1", getWorkspace(), original, destination);
		} finally {
			if (original != null)
				try {
					original.shutdown(getMonitor());
				} catch (CoreException e) {
					fail("2.0", e);
				}
		}
	}
}
