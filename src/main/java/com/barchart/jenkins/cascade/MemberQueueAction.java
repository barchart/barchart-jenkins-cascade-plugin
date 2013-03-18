/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Action;
import hudson.model.Queue.QueueAction;

import java.util.List;

/**
 * @author Andrei Pozolotin
 */
public class MemberQueueAction extends AbstractAction implements QueueAction {

	public boolean shouldSchedule(final List<Action> actions) {
		return true;
	}

}
