/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Action;

import java.util.List;

/**
 * Marker for builds initiated by {@link CascadeLogic}.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeLogicAction extends AbstractAction {

	/**
	 * Check if {@link CascadeLogicAction} is present in action list.
	 */
	public static boolean hasAction(final List<Action> actionList) {
		if (actionList == null || actionList.isEmpty()) {
			return false;
		}
		for (final Action cause : actionList) {
			if (cause instanceof CascadeLogicAction) {
				return true;
			}
		}
		return false;
	}

}
