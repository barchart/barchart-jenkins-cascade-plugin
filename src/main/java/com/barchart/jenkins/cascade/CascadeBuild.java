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
import java.util.logging.Logger;

/**
 * Cascade build.
 * <p>
 * Manages cascade release logic and stores the result.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeBuild extends Build<CascadeProject, CascadeBuild> {

	protected final static Logger log = Logger.getLogger(CascadeBuild.class
			.getName());

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

	private Set<CascadeResult> resultSet;

	/** New build form UI. */
	public CascadeBuild(final CascadeProject project) throws IOException {
		super(project);
		setup(project);
	}

	/** Old Build from job/build.xml file. */
	public CascadeBuild(final CascadeProject project, final File buildDir)
			throws IOException {
		super(project, buildDir);
		setup(project);
	}

	/**
	 * Artifacts release in this cascade build.
	 */
	public Set<CascadeResult> getResultSet() {
		return resultSet;
	}

	@Override
	public void run() {
		execute(new CascadeExecution());
	}

	/**
	 * Required for legacy xstream serializer to work.
	 */
	private void setup(final CascadeProject project) {
		if (resultSet == null) {
			resultSet = new TreeSet<CascadeResult>();
		}
	}

}
