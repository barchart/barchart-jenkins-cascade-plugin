/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jenkins.model.Jenkins;

import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Project identity property for cascade member projects.
 * <p>
 * Persists role, family id, member id.
 * 
 * @author Andrei Pozolotin
 */
public class ProjectIdentity extends JobProperty<AbstractProject<?, ?>>
		implements PluginConstants {

	/**
	 * Identity equality mode.
	 */
	public enum Mode {
		/**
		 * Compare by role only.
		 */
		ROLE() {
			@Override
			public boolean equals(final ProjectIdentity one,
					final ProjectIdentity two) {
				return one.equalsRole(two);
			}
		}, //
		/**
		 * Compare by role and family id.
		 */
		ROLE_FAMILY() {
			@Override
			public boolean equals(final ProjectIdentity one,
					final ProjectIdentity two) {
				return one.equalsRoleFamily(two);
			}
		}, //
		/**
		 * Compare by role, family id, project id.
		 */
		ROLE_FAMILY_PROJECT() {
			@Override
			public boolean equals(final ProjectIdentity one,
					final ProjectIdentity two) {
				return one.equalsRoleFamilyProject(two);
			}
		}, //
		;
		public abstract boolean equals(ProjectIdentity one, ProjectIdentity two);
	}

	public static class TheDescriptor extends JobPropertyDescriptor {

		@Override
		public String getDisplayName() {
			return MEMBER_ACTION_NAME;
		}

		/**
		 * Display this property for maven and cascade projects.
		 */
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

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	/**
	 * Find project by role and family.
	 */
	@SuppressWarnings("rawtypes")
	public static AbstractProject abstractProject(final ProjectIdentity one,
			final Mode mode) {
		for (final TopLevelItem item : PluginUtilities.projectList()) {
			if (item instanceof AbstractProject) {
				final AbstractProject project = (AbstractProject) item;
				final ProjectIdentity two = identity(project);
				if (two == null) {
					continue;
				}
				if (mode.equals(one, two)) {
					return project;
				}
			}
		}
		return null;
	}

	/**
	 * Find project.
	 */
	@SuppressWarnings("rawtypes")
	public static AbstractProject abstractProject(final ProjectRole role,
			final String familyID, final String projectID, final Mode mode) {
		final ProjectIdentity identity = new ProjectIdentity(role, familyID,
				projectID);
		return abstractProject(identity, mode);
	}

	/**
	 * Find cascade project by role, family.
	 */
	public static CascadeProject cascadeProject(final ProjectIdentity identity) {
		return (CascadeProject) abstractProject(ProjectRole.CASCADE,
				identity.getFamilyID(), identity.getProjectID(),
				Mode.ROLE_FAMILY);
	}

	/**
	 * Generate unique identity for cascade project.
	 */
	public static ProjectIdentity ensureCascadeIdentity(
			final AbstractProject<?, ?> layoutProject,
			final AbstractProject<?, ?> cascadeProject) throws IOException {

		final ProjectRole role = ProjectRole.CASCADE;
		final String familyID = familyID(layoutProject);
		final String projectID = UUID.randomUUID().toString();

		final ProjectIdentity cascadeIdentity = new ProjectIdentity(role,
				familyID, projectID);

		PluginUtilities.ensureProperty(cascadeProject, cascadeIdentity);

		return cascadeIdentity;

	}

	/**
	 * Generate unique identity for layout project.
	 */
	public static ProjectIdentity ensureLayoutIdentity(
			final AbstractProject<?, ?> layoutProject) throws IOException {

		if (hasIdentity(layoutProject)) {
			return identity(layoutProject);
		}

		final ProjectRole role = ProjectRole.LAYOUT;
		final String cascadeID = UUID.randomUUID().toString();
		final String projectID = UUID.randomUUID().toString();

		final ProjectIdentity layoutIdentity = new ProjectIdentity(role,
				cascadeID, projectID);

		PluginUtilities.ensureProperty(layoutProject, layoutIdentity);

		return layoutIdentity;

	}

	/**
	 * Generate unique identity for member project.
	 */
	public static ProjectIdentity ensureMemberIdentity(
			final AbstractProject<?, ?> layoutProject,
			final AbstractProject<?, ?> memberProject) throws IOException {

		final ProjectRole role = ProjectRole.MEMBER;
		final String familyID = familyID(layoutProject);
		final String projectID = UUID.randomUUID().toString();

		final ProjectIdentity memberIdentity = new ProjectIdentity(role,
				familyID, projectID);

		PluginUtilities.ensureProperty(memberProject, memberIdentity);

		return memberIdentity;

	}

	/**
	 * Extract family id from the project.
	 */
	public static String familyID(final AbstractProject<?, ?> project) {
		final ProjectIdentity identity = identity(project);
		if (identity == null) {
			return null;
		}
		return identity.getFamilyID();
	}

	/**
	 * Find all cascade family projects.
	 */
	@SuppressWarnings("rawtypes")
	public static List<AbstractProject> familyProjectList(final String familyID) {
		final List<AbstractProject> memberList = new ArrayList<AbstractProject>();
		for (final TopLevelItem item : PluginUtilities.projectList()) {
			if (item instanceof AbstractProject) {
				final AbstractProject project = (AbstractProject) item;
				if (familyID.equals(familyID(project))) {
					memberList.add(project);
				}
			}
		}
		return memberList;
	}

	/**
	 * Find cascade family view.
	 */
	public static ListView familyView(final ProjectIdentity identity) {
		final String viewName = familyViewName(identity);
		final View view = Jenkins.getInstance().getView(viewName);
		if (view instanceof ListView) {
			return (ListView) view;
		}
		return null;
	}

	/**
	 * Find cascade family view.
	 */
	public static String familyViewName(final ProjectIdentity identity) {

		final MavenModuleSet layoutProject = identity.layoutProject();

		if (layoutProject == null) {
			return "invalid-project";
		}

		final LayoutBuildWrapper wrpapper = LayoutBuildWrapper
				.wrapper(layoutProject);

		if (wrpapper == null) {
			return "invalid-wrapper";
		}

		final String viewName = wrpapper.getLayoutOptions().getLayoutViewName();

		if (viewName == null) {
			return "invalid-view";
		}

		return viewName;
	}

	/**
	 * Verify a project has this property with all component fields.
	 */
	public static boolean hasIdentity(final AbstractProject project) {
		if (project == null) {
			return false;
		}
		final ProjectIdentity identity = identity(project);
		if (identity == null) {
			return false;
		}
		return identity.isValid();
	}

	/**
	 * Extract this property from project.
	 */
	public static ProjectIdentity identity(final AbstractProject<?, ?> project) {
		return project.getProperty(ProjectIdentity.class);
	}

	/**
	 * Find layout project by role, family.
	 */
	public static MavenModuleSet layoutProject(final ProjectIdentity identity) {
		return (MavenModuleSet) abstractProject(ProjectRole.LAYOUT,
				identity.getFamilyID(), identity.getProjectID(),
				Mode.ROLE_FAMILY);
	}

	/**
	 * Find member project by role, family, member.
	 */
	public static MavenModuleSet memberProject(final ProjectIdentity identity) {
		return (MavenModuleSet) abstractProject(ProjectRole.MEMBER,
				identity.getFamilyID(), identity.getProjectID(),
				Mode.ROLE_FAMILY_PROJECT);
	}

	/**
	 * Find member projects by role, family.
	 */
	public static List<MavenModuleSet> memberProjectList(final String familyID) {

		final String projectID = "unused";

		final ProjectIdentity sourceID = new ProjectIdentity(
				ProjectRole.MEMBER, familyID, projectID);

		final List<MavenModuleSet> memberList = new ArrayList<MavenModuleSet>();

		for (final MavenModuleSet mavenProject : PluginUtilities
				.mavenProjectList()) {

			final ProjectIdentity targetID = identity(mavenProject);

			if (sourceID.equalsRoleFamily(targetID)) {
				memberList.add(mavenProject);
			}

		}

		return memberList;
	}

	private final String familyID;

	private String log = new DateTime() + " " + "Log started.";

	private final String projectID;

	private final String projectRole;

	public ProjectIdentity( //
			final ProjectRole role,//
			final String familyID, //
			final String projectID //
	) {
		this(role.code(), familyID, projectID);
	}

	@Jelly
	@DataBoundConstructor
	public ProjectIdentity( //
			final String projectRole, //
			final String familyID, //
			final String projectID //
	) {
		this.projectRole = projectRole;
		this.familyID = familyID;
		this.projectID = projectID;
	}

	/**
	 * Find cascade project by role, family.
	 */
	public CascadeProject cascadeProject() {
		return cascadeProject(this);
	}

	/**
	 * Equality by role, family id, project id.
	 */
	@Override
	public boolean equals(final Object other) {
		return equalsRoleFamilyProject(other);
	}

	/**
	 * Equality by family id.
	 */
	public boolean equalsFamily(final Object other) {
		if (other instanceof ProjectIdentity) {
			final ProjectIdentity that = (ProjectIdentity) other;
			final String thisId = this.getFamilyID();
			final String thatId = that.getFamilyID();
			return thisId.equals(thatId);
		}
		return false;
	}

	/**
	 * Equality by role.
	 */
	public boolean equalsRole(final Object other) {
		if (other instanceof ProjectIdentity) {
			final ProjectIdentity that = (ProjectIdentity) other;
			final String thisId = this.identityRole();
			final String thatId = that.identityRole();
			return thisId.equals(thatId);
		}
		return false;
	}

	/**
	 * Equality by role and family.
	 */
	public boolean equalsRoleFamily(final Object other) {
		if (other instanceof ProjectIdentity) {
			final ProjectIdentity that = (ProjectIdentity) other;
			final String thisId = this.identityRoleFamily();
			final String thatId = that.identityRoleFamily();
			return thisId.equals(thatId);
		}
		return false;
	}

	/**
	 * Equality by role, family, project.
	 */
	public boolean equalsRoleFamilyProject(final Object other) {
		if (other instanceof ProjectIdentity) {
			final ProjectIdentity that = (ProjectIdentity) other;
			final String thisId = this.identityRoleFamilyProject();
			final String thatId = that.identityRoleFamilyProject();
			return thisId.equals(thatId);
		}
		return false;
	}

	/**
	 * Find all cascade family projects.
	 */
	@SuppressWarnings("rawtypes")
	public List<AbstractProject> familyProjectList() {
		return familyProjectList(this.getFamilyID());
	}

	/**
	 * Find cascade family view.
	 */
	public ListView familyView() {
		return familyView(this);
	}

	/**
	 * Find cascade family view.
	 */
	public String familyViewName() {
		return familyViewName(this);
	}

	@Override
	public TheDescriptor getDescriptor() {
		return META;
	}

	/**
	 * Cascade project family id.
	 */
	@Jelly
	public String getFamilyID() {
		return familyID;
	}

	@Override
	public Collection<? extends Action> getJobActions(
			final AbstractProject<?, ?> project) {
		final List<Action> actionList = new ArrayList<Action>();
		actionList.add(new ProjectPageIdentity(this));
		actionList.add(new ProjectPageEventLog(this));
		return actionList;
	}

	/**
	 * ID of a member project.
	 */
	@Jelly
	public String getProjectID() {
		return projectID;
	}

	/**
	 * Project role in the family.
	 */
	@Jelly
	public String getProjectRole() {
		return projectRole;
	}

	/**
	 * Identity by role.
	 */
	public String identityRole() {
		return "/role=" + getProjectRole();
	}

	/**
	 * Identity by role and family.
	 */
	public String identityRoleFamily() {
		return identityRole() + "/family=" + getFamilyID();
	}

	/**
	 * Identity by role, family, project.
	 */
	public String identityRoleFamilyProject() {
		return identityRoleFamily() + "/project=" + getProjectID();
	}

	/**
	 * Verify all component fields present.
	 */
	public boolean isValid() {
		if (getProjectRole() == null || getProjectRole().isEmpty()) {
			return false;
		}
		if (getFamilyID() == null || getFamilyID().isEmpty()) {
			return false;
		}
		if (getProjectID() == null || getProjectID().isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Find layout project by family.
	 */
	public MavenModuleSet layoutProject() {
		return layoutProject(this);
	}

	/**
	 * Cascade event log.
	 */
	public String log() {
		return log;
	}

	/**
	 * Append to cascade event log.
	 */
	public void log(final String text) {
		log = log + "\n" + new DateTime() + " " + text;
	}

	/**
	 * Find member project by family.
	 */
	public MavenModuleSet memberProject() {
		return memberProject(this);
	}

	/**
	 * Find member project with module name.
	 */
	public MavenModuleSet memberProject(final ModuleName moduleName) {

		final List<MavenModuleSet> memberProjectList = memberProjectList();

		for (final MavenModuleSet memberProject : memberProjectList) {

			final MavenModule memberModule = memberProject.getRootModule();

			final ModuleName memberName = memberModule.getModuleName();

			if (moduleName.equals(memberName)) {
				return memberProject;
			}

		}

		return null;

	}

	/**
	 * Find member projects in a family.
	 */
	public List<MavenModuleSet> memberProjectList() {
		return memberProjectList(getFamilyID());
	}

	/**
	 * Find project role.
	 */
	public ProjectRole role() {
		return ProjectRole.from(projectRole);
	}

	/**
	 * Render project identity as unique string.
	 */
	@Override
	public String toString() {
		return identityRoleFamilyProject();
	}

}
