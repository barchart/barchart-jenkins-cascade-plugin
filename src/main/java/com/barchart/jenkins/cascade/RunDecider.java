/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;

import java.util.List;

/**
 * @author Andrei Pozolotin
 */
@Extension
public class RunDecider extends QueueDecisionHandler {

	@Override
	public boolean shouldSchedule(final Task task, final List<Action> actions) {

		if (!(task instanceof AbstractProject)) {
			return true;
		}

		final AbstractProject<?, ?> project = (AbstractProject<?, ?>) task;

		final ProjectIdentity identity = ProjectIdentity.identity(project);

		if (identity == null) {
			return true;
		}

		// for (final AbstractBuild<?, ?> build : project.getBuilds()) {
		//
		// if (!build.isBuilding()) {
		// continue;
		// }
		//
		// for (final QueueAction action : build.getActions(QueueAction.class))
		// {
		// if (action.shouldSchedule(actions) == false) {
		// return false;
		// }
		// }
		//
		// }

		return true;

	}

}
