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
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory provides cascade actions for member projects.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class MemberActionFactory extends TransientProjectActionFactory {

	/**
	 * Interested in cascade member projects.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		final List<Action> actionList = new ArrayList<Action>();

		if (!(project instanceof AbstractProject)) {
			return actionList;
		}

		final AbstractProject memberProject = project;

		final ProjectIdentity identity = ProjectIdentity
				.identity(memberProject);

		if (identity == null) {
			return actionList;
		}

		final ProjectRole role = ProjectRole.from(identity.getProjectRole());

		switch (role) {
		case CASCADE:
			break;
		case MEMBER:
			actionList.add(new MemberBuildAction(identity));
			actionList.add(new MemberViewAction(identity));
			break;
		case LAYOUT:
			break;
		}

		return actionList;

	}

}
