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
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;

import java.util.Collection;
import java.util.Collections;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Orchestration project properties for cascade layout member projects.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeProjectProperty extends JobProperty<MavenModuleSet> {

	@Extension
	public static class TheDescriptor extends JobPropertyDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.CASCADE_ACTION_NAME;
		}

		@Override
		public boolean isApplicable(final Class<? extends Job> projectType) {
			return MavenModuleSet.class.isAssignableFrom(projectType);
		}

	}

	private final String projectName;

	/** Jelly injected. */
	@DataBoundConstructor
	public CascadeProjectProperty(final String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	@Override
	public Collection<? extends Action> getJobActions(
			final MavenModuleSet project) {

		// return Collections.singletonList(new CascadeBuildAction(project));

		return Collections.emptyList();

	}

}
