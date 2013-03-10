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
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.Collection;
import java.util.Collections;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Property for cascade member projects.
 * 
 * @author Andrei Pozolotin
 */
public class MemberProjectProperty extends JobProperty<AbstractProject<?, ?>> {

	@Extension
	public static class TheDescriptor extends JobPropertyDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.MEMBER_ACTION_NAME;
		}

		@Override
		public boolean isApplicable(final Class<? extends Job> projectClass) {
			if (MavenModuleSet.class.isAssignableFrom(projectClass)) {
				return true;
			}
			if (CascadeProject.class.isAssignableFrom(projectClass)) {
				return true;
			}
			return false;
		}

	}

	/**
	 * Find project by property.
	 */
	public static AbstractProject abstractProject(
			final MemberProjectProperty one) {
		for (final TopLevelItem item : Jenkins.getInstance().getItems()) {
			if (item instanceof AbstractProject) {
				final AbstractProject project = (AbstractProject) item;
				final MemberProjectProperty two = property(project);
				if (one.equals(two)) {
					return project;
				}
			}
		}
		return null;
	}

	public static AbstractProject abstractProject(final ProjectRole role,
			final String cascadeUUID) {
		final MemberProjectProperty cascadeProperty = new MemberProjectProperty(
				role.code(), cascadeUUID, "");
		return abstractProject(cascadeProperty);
	}

	public static CascadeProject cascadeProject(
			final MemberProjectProperty property) {
		return (CascadeProject) abstractProject(ProjectRole.CASCADE,
				property.getCascadeUUID());
	}

	public static MavenModuleSet layoutProject(
			final MemberProjectProperty property) {
		return (MavenModuleSet) abstractProject(ProjectRole.LAYOUT,
				property.getCascadeUUID());
	}

	public static MavenModuleSet memberProject(
			final MemberProjectProperty property) {
		return (MavenModuleSet) abstractProject(ProjectRole.MEMBER,
				property.getCascadeUUID());
	}

	public static boolean hasProperty(final AbstractProject project) {
		if (project == null) {
			return false;
		}
		final MemberProjectProperty property = property(project);
		if (property == null) {
			return false;
		}
		final ProjectRole role = ProjectRole.from(property.projectRole);
		if (ProjectRole.UNKNOWN == role) {
			return false;
		}
		final String uuid = property.cascadeUUID;
		if (uuid == null || uuid.isEmpty()) {
			return false;
		}
		return true;
	}

	public static MemberProjectProperty property(
			final AbstractProject<?, ?> project) {
		return project.getProperty(MemberProjectProperty.class);
	}

	public static String propertyCascadeUUID(final AbstractProject<?, ?> project) {
		final MemberProjectProperty property = property(project);
		if (property == null) {
			return null;
		}
		return property.getCascadeUUID();
	}

	private final String cascadeUUID;

	private final String projectUUID;

	private final String projectRole;

	/** Jelly injected. */
	@DataBoundConstructor
	public MemberProjectProperty( //
			final String projectRole, //
			final String cascadeUUID, //
			final String projectUUID //
	) {
		this.projectRole = projectRole;
		this.cascadeUUID = cascadeUUID;
		this.projectUUID = projectUUID;
	}

	public boolean isValid() {
		if (projectRole == null || projectRole.isEmpty()) {
			return false;
		}
		if (cascadeUUID == null || cascadeUUID.isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Equality by role and UUID.
	 */
	@Override
	public boolean equals(final Object other) {
		if (other instanceof MemberProjectProperty) {
			final MemberProjectProperty that = (MemberProjectProperty) other;
			return this.projectRole.equals(that.projectRole)
					&& this.cascadeUUID.equals(that.cascadeUUID);
		}
		return false;
	}

	/**
	 * Cascade project family UUID.
	 */
	public String getCascadeUUID() {
		return cascadeUUID;
	}

	@Override
	public Collection<? extends Action> getJobActions(
			final AbstractProject project) {
		// return Collections.singletonList(new CascadeBuildAction(project));
		return Collections.emptyList();
	}

	/**
	 * Project role in the cascade.
	 */
	public String getProjectRole() {
		return projectRole;
	}

	@Override
	public String toString() {
		return projectRole + ":" + cascadeUUID + ":" + projectUUID;
	}

}
