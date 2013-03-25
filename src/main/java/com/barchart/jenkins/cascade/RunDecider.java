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
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Prevents external build submission during cascade operations.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunDecider extends QueueDecisionHandler implements PluginConstants {

	protected final static Logger log = Logger.getLogger(RunDecider.class
			.getName());

	public static void log(final String text) {
		log.info(LOGGER_PREFIX + " " + text);
	}

	/**
	 * Verify if project is present in the build queue.
	 */
	public static boolean queueHas(final AbstractProject<?, ?> project) {
		final Queue queue = Queue.getInstance();
		return queue.getItem(project) != null;
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
	 * Report family projects.
	 */
	@SuppressWarnings("rawtypes")
	public static void report(final ProjectIdentity identity) {
		identity.log("##############");
		identity.log("Family Report:");
		final List<AbstractProject> list = identity.familyProjectList();
		final Set<String> set = new TreeSet<String>();
		for (final AbstractProject project : list) {
			set.add(project.getName());
		}
		for (final String name : set) {
			identity.log("\t" + name);
		}
		identity.log("##############");
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

	/**
	 * Report any cascade family projects are pending or building.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, AbstractProject> reportActiveFamilyProjects(
			final ProjectIdentity source) {
		final Map<String, AbstractProject> map = new TreeMap<String, AbstractProject>();
		for (final TopLevelItem item : PluginUtilities.projectList()) {
			if (item instanceof AbstractProject) {
				final AbstractProject project = (AbstractProject) item;
				final ProjectIdentity target = ProjectIdentity
						.identity(project);
				if (target == null) {
					continue;
				}
				if (!source.equalsFamily(target)) {
					continue;
				}
				if (project.isBuilding()) {
					map.put(project.getName(), project);
					continue;
				}
				if (queueHas(project)) {
					map.put(project.getName(), project);
					continue;
				}
			}
		}
		return map;
	}

	/**
	 * Concurrent execution mutual exclusion logic.
	 */
	@Override
	public boolean shouldSchedule(final Task task, final List<Action> actionList) {

		/** Project type not managed here. */
		if (!(task instanceof AbstractProject)) {
			return true;
		}

		final AbstractProject<?, ?> project = (AbstractProject<?, ?>) task;

		final ProjectIdentity identity = ProjectIdentity.identity(project);

		/** Cascade family projects have identity. */
		if (identity == null) {
			return true;
		}

		final MavenModuleSet layoutProject = identity.layoutProject();
		final CascadeProject cascadeProject = identity.cascadeProject();

		/** Layout project constraint. */
		if (layoutProject == null) {
			report(identity, project, actionList,
					"Unexpected: layout project is missing, drop the task.");
			return false;
		}
		if (layoutProject.isBuilding()) {
			if (LayoutLogicAction.hasAction(actionList)) {
				report(identity, project, actionList,
						"Permit task started by layout project.");
				return true;
			} else {
				report(identity, project, actionList,
						"Layout  project is building, drop the task.");
				return false;
			}
		}
		if (queueHas(layoutProject)) {
			report(identity, project, actionList,
					"Layout  project is pending, drop the task.");
			return false;
		}

		/** Cascade project constraint. */
		if (cascadeProject == null) {
			report(identity, project, actionList,
					"Permit task while there is no cascade project.");
			// report(identity);
			return true;
		}
		if (cascadeProject.isBuilding()) {
			if (CascadeLogicAction.hasAction(actionList)) {
				report(identity, project, actionList,
						"Permit task started by cascade project.");
				return true;
			} else {
				report(identity, project, actionList,
						"Cascade project is building, drop the task.");
				return false;
			}
		}
		if (queueHas(cascadeProject)) {
			report(identity, project, actionList,
					"Cascade project is pending, drop the task.");
			return false;
		}

		report(identity, project, actionList,
				"Unconstrained condition, permit the task.");

		return true;

	}

}
