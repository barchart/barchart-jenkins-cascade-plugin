/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;

import java.util.Map;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.maven.model.Model;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Cascade build link on member project page.
 * 
 * @author Andrei Pozolotin
 */
public class MemberBuildAction extends AbstractAction {

	private static final Logger log = Logger.getLogger(MemberBuildAction.class
			.getName());

	private final ProjectIdentity identity;

	private String releaseVersion;

	private String snapshotVersion;

	public MemberBuildAction( //
			final ProjectIdentity identity //
	) {
		super(MEMBER_ACTION_NAME, MEMBER_ACTION_ICON, MEMBER_ACTION_URL);
		this.identity = identity;
	}

	/**
	 * Calculate current release.
	 */
	@Jelly
	public String defaultReleaseVersion() {
		try {
			final MavenModuleSet project = identity.memberProject();
			final Model model = mavenModel(project);
			final String version = model.getVersion();
			final VersionInfo past = new DefaultVersionInfo(version);
			return past.getReleaseVersionString();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Calculate future snapshot.
	 */
	@Jelly
	public String defaultSnapshotVersion() {
		try {
			final MavenModuleSet project = identity.memberProject();
			final Model model = mavenModel(project);
			final String version = model.getVersion();
			final VersionInfo past = new DefaultVersionInfo(version);
			final VersionInfo next = past.getNextVersion();
			return next.getSnapshotVersionString();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Start cascade build.
	 */
	@Jelly
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final JSONObject json = request.getSubmittedForm();
		final JSONObject props = json.getJSONObject("releaseProperties");
		releaseVersion = props.getString("releaseVersion");
		snapshotVersion = props.getString("snapshotVersion");

		final CascadeProject cascadeProject = identity.cascadeProject();
		final MavenModuleSet memberProject = identity.memberProject();

		// final Map<String, AbstractProject> map =
		// reportActiveFamilyProjects();
		// if (map.isEmpty()) {
		// response.sendRedirect(request.getContextPath() + '/'
		// + memberProject.getUrl() + '/' + getUrlName()
		// + "/failedActive");
		// return;
		// }

		final MemberBuildCause cause = new MemberBuildCause();
		final DoCascadeBadge badge = new DoCascadeBadge();

		final boolean isScheduled = cascadeProject.scheduleBuild(0, cause,
				this, badge);

		if (isScheduled) {
			response.sendRedirect(request.getContextPath() + '/'
					+ cascadeProject.getUrl());
		} else {
			response.sendRedirect(request.getContextPath() + '/'
					+ memberProject.getUrl() + '/' + getUrlName()
					+ "/failedSchedule");
		}

	}

	@Jelly
	public CascadeOptions getCascadeOptions() {
		return LayoutBuildWrapper.wrapper(identity.layoutProject())
				.getCascadeOptions();
	}

	@Jelly
	public ProjectIdentity getIdentity() {
		return identity;
	}

	@Jelly
	public LayoutOptions getLayoutOptions() {
		return LayoutBuildWrapper.wrapper(identity.layoutProject())
				.getLayoutOptions();
	}

	@Jelly
	public String getReleaseVersion() {
		return releaseVersion;
	}

	@Jelly
	public String getSnapshotVersion() {
		return snapshotVersion;
	}

	/**
	 * Report any cascade family projects that are pending or building.
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, AbstractProject> reportActiveFamilyProjects() {
		return RunDecider.reportActiveFamilyProjects(identity);
	}

}
