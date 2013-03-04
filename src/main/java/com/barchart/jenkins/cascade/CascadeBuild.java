/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Build;

import java.io.File;
import java.io.IOException;

/**
 * Cascade build. Provides cascade release logic.
 */
public class CascadeBuild extends Build<CascadeProject, CascadeBuild> {

	public CascadeBuild(final CascadeProject project) throws IOException {
		super(project);
		setup(project);
	}

	public CascadeBuild(final CascadeProject project, final File buildDir)
			throws IOException {
		super(project, buildDir);
		setup(project);
	}

	@Override
	public void run() {
	}

	private void setup(final CascadeProject job) {
	}

}
