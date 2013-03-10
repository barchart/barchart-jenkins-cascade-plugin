/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Action;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Cascade build action link on member project page.
 * 
 * @author Andrei Pozolotin
 */
public class MemberBuildAction implements Action {

	final private MemberProjectProperty property;

	private String releaseVersion;
	private String developmentVersion;

	public MemberBuildAction( //
			final MemberProjectProperty property) {
		this.property = property;
	}

	/**
	 * Jelly form submit.
	 * <p>
	 * Start cascade build.
	 */
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final CascadeProject project = MemberProjectProperty
				.cascadeProject(property);

		final MemberBuildCause cause = new MemberBuildCause();
		final MemberBadge badge = new MemberBadge();

		project.scheduleBuild(0, cause, this, badge);

		response.sendRedirect(request.getContextPath() + '/' + project.getUrl());

	}

	public String getCascadeName() {
		return "TODO";
	}

	public String getDevelopmentVersion() {
		return developmentVersion;
	}

	public String getDisplayName() {
		return PluginConstants.MEMBER_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.MEMBER_ACTION_ICON;
	}

	public String getLayoutName() {
		return "TODO";
	}

	public String getMemberName() {
		return "TODO";
	}

	public String getReleaseVersion() {
		return releaseVersion;
	}

	public String getUrlName() {
		return PluginConstants.MEMBER_ACTION_URL;
	}

}
