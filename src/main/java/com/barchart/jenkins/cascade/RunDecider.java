/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;

import java.util.List;
import java.util.logging.Logger;

/**
 * Prevents external build submission during cascade operations.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunDecider extends QueueDecisionHandler {

	protected final static Logger log = Logger.getLogger(RunDecider.class
			.getName());

	public static void log(final String text) {
		log.info(PluginConstants.LOGGER_PREFIX + " " + text);
	}

	/**
	 * Render action list for logging.
	 */
	public static String render(final List<Action> actionList) {
		final StringBuilder text = new StringBuilder();
		text.append(" [ ");
		for (final Action action : actionList) {

			text.append(" // ");
			text.append("action=");
			text.append(action.getDisplayName());
			text.append(" ");

			if (action instanceof CauseAction) {
				final CauseAction causeAction = (CauseAction) action;
				for (final Cause cause : causeAction.getCauses()) {
					text.append("cause=");
					text.append(cause.getShortDescription());
					text.append(" ");
				}
				continue;
			}

			if (action instanceof ParametersAction) {
				final ParametersAction paramAction = (ParametersAction) action;
				for (final ParameterValue param : paramAction.getParameters()) {
					text.append("name=");
					text.append(param.getName());
					text.append(";");
					text.append("value=");
					text.append(param.getDescription());
					text.append("");
				}
				continue;
			}

			text.append(action.toString());

		}
		text.append(" ] ");
		return text.toString();
	}

	/**
	 * Report decision in jenkins log and instance log.
	 */
	public static void report(final ProjectIdentity identity,
			final AbstractProject<?, ?> project, final List<Action> actionList,
			final String message) {
		final String actionText = render(actionList);
		log(message + " " + project.getName() + " " + actionText);
		identity.log(message + " " + actionText);
	}

	@Override
	public boolean shouldSchedule(final Task task, final List<Action> actionList) {

		if (!(task instanceof AbstractProject)) {
			return true;
		}

		final AbstractProject<?, ?> project = (AbstractProject<?, ?>) task;

		final ProjectIdentity identity = ProjectIdentity.identity(project);

		/** Cascade family projects have identity. */
		if (identity == null) {
			return true;
		}

		final Queue queue = Queue.getInstance();

		final CascadeProject cascadeProject = identity.cascadeProject();
		/** Layout setup incomplete. */
		if (cascadeProject == null) {
			report(identity, project, actionList,
					"Permit task while there is no cascade project.");
			return true;
		}
		if (cascadeProject.isBuilding()) {
			if (CascadeLogicAction.hasAction(actionList)) {
				report(identity, project, actionList,
						"Permit task started by cascade.");
				return true;
			} else {
				report(identity, project, actionList,
						"Cascade project is building, drop task.");
				return false;
			}
		}
		if (queue.getItem(cascadeProject) != null) {
			report(identity, project, actionList,
					"Cascade project is pending, drop task.");
			return false;
		}

		final MavenModuleSet layoutProject = identity.layoutProject();
		/** Layout setup incomplete. */
		if (layoutProject == null) {
			report(identity, project, actionList,
					"Permit task while there is no layout project.");
			return true;
		}
		if (layoutProject.isBuilding()) {
			if (LayoutLogicAction.hasAction(actionList)) {
				report(identity, project, actionList,
						"Permit task started by layout.");
				return true;
			} else {
				report(identity, project, actionList,
						"Layout  project is building, drop task.");
				return false;
			}
		}
		if (queue.getItem(layoutProject) != null) {
			report(identity, project, actionList,
					"Layout  project is pending, drop task.");
			return false;
		}

		report(identity, project, actionList, "Unkown condition, permit task.");

		return true;

	}

}
