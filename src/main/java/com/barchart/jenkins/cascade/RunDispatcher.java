/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.util.List;

/**
 * Controls when cascade family project builds are permitted to run.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class RunDispatcher extends QueueTaskDispatcher {

	/**
	 * Cause of no block.
	 */
	public static final CauseOfBlockage YES_CAN_RUN = null;

	@Override
	public CauseOfBlockage canRun(final Queue.Item item) {

		/** Expect only maven and cascade projects. */
		if (!(item.task instanceof AbstractProject)) {
			return YES_CAN_RUN;
		}

		final AbstractProject<?, ?> project = (AbstractProject<?, ?>) item.task;

		final ProjectIdentity identity = ProjectIdentity.identity(project);

		/** Cascade family projects must have identity. */
		if (identity == null) {
			return YES_CAN_RUN;
		}

		final String familyID = identity.getFamilyID();

		final RunLock lock = RunLock.ensure(familyID);

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
				if (MemberBuildCause.hasBuildCause(causeList)) {
					/** Cascade member build, proceed. */
					return YES_CAN_RUN;
				} else {
					/** Non-cascade member build, must wait. */
					return new RunBlockCause(
							"Non-cascade member build is waiting on a cascade build.");
				}
			}
			if (lock.hasMember()) {
				/**
				 * Non-cascade member build exclusion is managed by jenkins
				 * core.
				 */
				return YES_CAN_RUN;
			}
			break;

		default:
			break;
		}

		return YES_CAN_RUN;

	}

}
