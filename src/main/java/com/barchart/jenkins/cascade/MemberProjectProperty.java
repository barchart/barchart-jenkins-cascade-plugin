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
 * Orchestration properties for cascade member projects.
 * 
 * @author Andrei Pozolotin
 */
public class MemberProjectProperty extends JobProperty<MavenModuleSet> {

	@Extension
	public static class TheDescriptor extends JobPropertyDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.MEMBER_ACTION_NAME;
		}

		@Override
		public boolean isApplicable(final Class<? extends Job> projectType) {
			return MavenModuleSet.class.isAssignableFrom(projectType);
		}

	}

	private final String projectRole;
	private final String cascadeProject;
	private final String layoutProject;

	/** Jelly injected. */
	@DataBoundConstructor
	public MemberProjectProperty( //
			final String projectRole, //
			final String cascadeName, //
			final String layoutName //
	) {
		this.projectRole = projectRole;
		this.cascadeProject = cascadeName;
		this.layoutProject = layoutName;
	}

	/** Cascade project name. */
	public String getCascadeProject() {
		return cascadeProject;
	}

	/** Layout project name. */
	public String getLayoutProject() {
		return layoutProject;
	}

	/** Project role in the cascade. */
	public String getProjectRole() {
		return projectRole;
	}

	@Override
	public Collection<? extends Action> getJobActions(
			final MavenModuleSet project) {

		// return Collections.singletonList(new CascadeBuildAction(project));

		return Collections.emptyList();

	}

}
