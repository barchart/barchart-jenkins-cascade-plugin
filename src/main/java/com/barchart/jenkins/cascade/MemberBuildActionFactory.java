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
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Collections;

/**
 * Factory provides cascade build action for member projects.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class MemberBuildActionFactory extends TransientProjectActionFactory {

	/** Interested on in cascade member projects. */
	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		if (!(project instanceof MavenModuleSet)) {
			return Collections.emptyList();
		}

		final MavenModuleSet memberProject = (MavenModuleSet) project;

		final MemberProjectProperty property = MemberProjectProperty
				.property(memberProject);

		if (property == null) {
			return Collections.emptyList();
		}

		final ProjectRole role = ProjectRole.from(property.getProjectRole());

		switch (role) {
		case MEMBER:
			break;
		default:
			return Collections.emptyList();
		}

		final MemberBuildAction action = new MemberBuildAction(property);

		return Collections.singleton(action);

	}

}
