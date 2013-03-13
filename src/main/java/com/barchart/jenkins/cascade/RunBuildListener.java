/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;

/**
 * Build life cycle events listener.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunBuildListener extends RunListener<AbstractBuild<?, ?>> {

	@Override
	public void onFinalized(final AbstractBuild<?, ?> build) {

		final ProjectIdentity identity = ProjectIdentity.identity(build
				.getProject());

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

		final ProjectIdentity identity = ProjectIdentity.identity(build
				.getProject());

		if (identity == null) {
			return;
		}

		final RunLock lock = RunLock.ensure(identity.getFamilyID());

		synchronized (lock) {
			lock.setActive(identity.role(), true);
		}

	}

}
