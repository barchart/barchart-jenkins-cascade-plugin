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

/**
 * Cascade build. Provides cascade release logic.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeBuild extends Build<CascadeProject, CascadeBuild> {

	/** New build form UI. */
	public CascadeBuild(final CascadeProject project) throws IOException {
		super(project);
		setup(project);
	}

	/** Old Build from file. */
	public CascadeBuild(final CascadeProject project, final File buildDir)
			throws IOException {
		super(project, buildDir);
		setup(project);
	}

	private void setup(final CascadeProject project) {
	}

	@Override
	public void run() {
		execute(new CascadeExecution());
	}

	protected class CascadeExecution extends RunExecution {

		@Override
		public Result run(final BuildListener listener) throws Exception,
				hudson.model.Run.RunnerAbortedException {

			final PluginLogger log = new PluginLogger(listener);

			log.text("RUN");

			return Result.SUCCESS;
		}

		@Override
		public void post(final BuildListener listener) throws Exception {

			final PluginLogger log = new PluginLogger(listener);

			log.text("POST");

		}

		@Override
		public void cleanUp(final BuildListener listener) throws Exception {

			final PluginLogger log = new PluginLogger(listener);

			log.text("CLEAN");

		}

	}

}
