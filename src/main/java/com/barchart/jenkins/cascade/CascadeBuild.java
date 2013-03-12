/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cascade build.
 * <p>
 * Manages cascade release logic and stores the result.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeBuild extends Build<CascadeProject, CascadeBuild> {

	protected class CascadeExecution extends RunExecution {

		@Override
		public void cleanUp(final BuildListener listener) throws Exception {
		}

		@Override
		public void post(final BuildListener listener) throws Exception {

		}

		@Override
		public Result run(final BuildListener listener) throws Exception {

			final BuildContext<CascadeBuild> context = new BuildContext<CascadeBuild>(
					CascadeBuild.this, listener);

			return CascadeLogic.process(context);
		}

	}

	private final Set<CascadeResult> resultSet = new TreeSet<CascadeResult>();

	/** New build form UI. */
	public CascadeBuild(final CascadeProject project) throws IOException {
		super(project);
		setup(project);
	}

	/** Old Build from history file. */
	public CascadeBuild(final CascadeProject project, final File buildDir)
			throws IOException {
		super(project, buildDir);
		setup(project);
	}

	/**
	 * Artifacts release in this cascade build.
	 */
	public Set<CascadeResult> resultSet() {
		return resultSet;
	}

	@Override
	public void run() {
		execute(new CascadeExecution());
	}

	private void setup(final CascadeProject project) {
	}

}
