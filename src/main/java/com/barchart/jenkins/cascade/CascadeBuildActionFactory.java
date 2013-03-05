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
 * Factory to add a view action to each project.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class CascadeBuildActionFactory extends TransientProjectActionFactory {

	/** Interested on in cascade layout projects. */
	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		if (!(project instanceof MavenModuleSet)) {
			return Collections.emptyList();
		}

		final MavenModuleSet mavenProject = (MavenModuleSet) project;

		final CascadeProjectProperty property = mavenProject
				.getProperty(CascadeProjectProperty.class);

		if (property == null) {
			return Collections.emptyList();
		}

		final String projectName = property.getProjectName();

		if (projectName == null || projectName.isEmpty()) {
			return Collections.emptyList();
		}

		final CascadeBuildAction action = new CascadeBuildAction(mavenProject);

		return Collections.singleton(action);

	}

}
