/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.run_exclusion;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Queue;
import hudson.model.Queue.Item;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.util.List;
import java.util.logging.Logger;

import com.barchart.jenkins.cascade.MemberBuildCause;
import com.barchart.jenkins.cascade.ProjectIdentity;

import jenkins.model.Jenkins;

/**
 * Controls when cascade family project builds are permitted to run.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunDispatcher extends QueueTaskDispatcher {

	protected final static Logger log = Logger.getLogger(RunDispatcher.class
			.getName());

	/**
	 * Cause of no block.
	 */
	public static final CauseOfBlockage YES_CAN_RUN = null;

	public static ProjectIdentity identity(final Item item) {
		final AbstractProject<?, ?> project = project(item);
		if (project == null) {
			return null;
		}
		final ProjectIdentity identity = ProjectIdentity.identity(project);
		return identity;
	}

	public static AbstractProject<?, ?> project(final Item item) {
		if (!(item.task instanceof AbstractProject)) {
			return null;
		}
		final AbstractProject<?, ?> project = (AbstractProject<?, ?>) item.task;
		return project;
	}

	@Override
	public CauseOfBlockage canRun(final Queue.Item item) {

		return null;

		// final ProjectIdentity identity = identity(item);
		//
		// /** Cascade family projects must have identity. */
		// if (identity == null) {
		// return YES_CAN_RUN;
		// }
		//
		// final RunLock lock = RunLock.ensure(identity.getFamilyID());
		//
		// synchronized (lock) {
		// final CauseOfBlockage buildCause = canRunDueBuild(identity, lock,
		// item);
		// if (buildCause != null) {
		// return buildCause;
		// }
		// final CauseOfBlockage queueCause = canRunDueQueue(identity);
		// if (queueCause != null) {
		// return queueCause;
		// }
		// }
		//
		// return YES_CAN_RUN;

	}

	/**
	 */
	public CauseOfBlockage canRunDueBuild(final ProjectIdentity identity,
			final RunLock lock, final Queue.Item item) {

		switch (identity.role()) {

		case LAYOUT:
			if (lock.hasLayout()) {
				return new RunBlockCause(
						"Layout build is waiting on another layout build.");
			}
			if (lock.hasCascade()) {
				return new RunBlockCause(
						"Layout build is waiting on a cascade build.");
			}
			if (lock.hasMember()) {
				return new RunBlockCause(
						"Layout build is waiting on a member build.");
			}
			break;

		case CASCADE:
			if (lock.hasLayout()) {
				return new RunBlockCause(
						"Cascade build is waiting on a layout build.");
			}
			if (lock.hasCascade()) {
				return new RunBlockCause(
						"Cascade build is waiting on other cascade build.");
			}
			if (lock.hasMember()) {
				return new RunBlockCause(
						"Cascade build is waiting on a member build.");
			}
			break;

		case MEMBER:
			if (lock.hasLayout()) {
				return new RunBlockCause(
						"Member build is waiting on a layout build.");
			}
			if (lock.hasCascade()) {
				final List<Cause> causeList = item.getCauses();
				if (MemberBuildCause.hasCause(causeList)) {
					/** Cascade member build, proceed. */
					return YES_CAN_RUN;
				} else {
					/** Non-cascade member build, must wait. */
					return new RunBlockCause(
							"Non-cascade member build is waiting on a cascade build.");
				}
			}
			if (lock.hasMember()) {
				return new RunBlockCause(
						"Member build is waiting on another member build.");
			}
			break;

		default:
			break;
		}

		return YES_CAN_RUN;

	}

	/**
	 */
	public CauseOfBlockage canRunDueQueue(final ProjectIdentity source) {

		int sourceItemId = 0;
		int minimumItemId = Integer.MAX_VALUE;

		final String sourceId = source.getFamilyID();

		final Queue queue = Jenkins.getInstance().getQueue();

		final Item[] itemList = queue.getItems();

		for (final Item item : itemList) {

			final ProjectIdentity target = identity(item);

			/** Cascade projects must have identity. */
			if (target == null) {
				continue;
			}

			final String targetId = target.getFamilyID();

			/** Cascade project from another family. */
			if (!sourceId.equals(targetId)) {
				continue;
			}

			/** Found self. */
			if (source.equals(target)) {
				sourceItemId = item.id;
			}

			/** Found minimal id. */
			if (item.id < minimumItemId) {
				minimumItemId = item.id;
			}

		}

		/** No cascade projects - permit to run. */
		if (sourceItemId == 0) {
			return null;
		}

		/** Permit to run project with minimum id. */
		if (sourceItemId == minimumItemId) {
			return null;
		}

		return new RunBlockCause("Project is yielding to another project.");
	}

}
