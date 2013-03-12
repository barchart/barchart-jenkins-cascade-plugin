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
import hudson.model.Action;

import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.maven.model.Model;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Cascade build action link on member project page.
 * 
 * @author Andrei Pozolotin
 */
public class MemberBuildAction implements Action {

	private static final Logger log = Logger.getLogger(MemberBuildAction.class
			.getName());

	final private ProjectIdentity identity;

	private String releaseVersion;

	private String snapshotVersion;

	public MemberBuildAction( //
			final ProjectIdentity identity //
	) {
		this.identity = identity;
	}

	/**
	 * Calculate current release.
	 */
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
	 * Jelly form submit.
	 * <p>
	 * Start cascade build.
	 */
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final JSONObject json = request.getSubmittedForm();
		final JSONObject props = json.getJSONObject("releaseProperties");
		releaseVersion = props.getString("releaseVersion");
		snapshotVersion = props.getString("snapshotVersion");

		final CascadeProject project = identity.cascadeProject();

		final MemberBuildCause cause = new MemberBuildCause();
		final DoCascadeBadge badge = new DoCascadeBadge();

		project.scheduleBuild(0, cause, this, badge);

		response.sendRedirect(request.getContextPath() + '/' + project.getUrl());

	}

	public String getCascadeName() {
		return identity.cascadeProject().getName();
	}

	public String getDisplayName() {
		return PluginConstants.MEMBER_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.MEMBER_ACTION_ICON;
	}

	public ProjectIdentity getIdentity() {
		return identity;
	}

	public String getLayoutName() {
		return identity.layoutProject().getName();
	}

	public String getMemberName() {
		return identity.memberProject().getName();
	}

	public String getReleaseVersion() {
		return releaseVersion;
	}

	public String getSnapshotVersion() {
		return snapshotVersion;
	}

	public String getUrlName() {
		return PluginConstants.MEMBER_ACTION_URL;
	}

}
