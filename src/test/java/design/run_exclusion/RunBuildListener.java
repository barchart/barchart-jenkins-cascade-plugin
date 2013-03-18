/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.run_exclusion;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.listeners.RunListener;

import java.util.logging.Logger;

import com.barchart.jenkins.cascade.ProjectIdentity;

/**
 * Helps cascade family build mutual exclusion.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunBuildListener extends RunListener<AbstractBuild<?, ?>> {

	protected final static Logger log = Logger.getLogger(RunBuildListener.class
			.getName());

	@Override
	public void onFinalized(final AbstractBuild<?, ?> build) {

		final AbstractProject<?, ?> project = build.getProject();
		final ProjectIdentity identity = ProjectIdentity.identity(project);
		// log.info("@@@ identity: " + project.getName() + " : " + identity);

		if (identity == null) {
			return;
		}

		final RunLock lock = RunLock.ensure(identity.getFamilyID());

		synchronized (lock) {
			lock.setActive(identity.role(), false);
		}

	}

	@Override
	public void onStarted(final AbstractBuild<?, ?> build,
			final TaskListener listener) {

		final AbstractProject<?, ?> project = build.getProject();
		final ProjectIdentity identity = ProjectIdentity.identity(project);
		// log.info("@@@ identity: " + project.getName() + " : " + identity);

		if (identity == null) {
			return;
		}

		final RunLock lock = RunLock.ensure(identity.getFamilyID());

		synchronized (lock) {
			lock.setActive(identity.role(), true);
		}

	}

}
