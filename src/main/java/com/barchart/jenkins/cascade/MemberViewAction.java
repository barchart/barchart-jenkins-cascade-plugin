/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.model.ListView;
import hudson.views.ListViewColumn;

import java.util.List;
import java.util.logging.Logger;

import org.jvnet.hudson.plugins.m2release.LastReleaseListViewColumn;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBadgeAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Show cascade family view link for a cascade member project.
 * 
 * @author Andrei Pozolotin
 */
public class MemberViewAction extends AbstractAction {

	private static final Logger log = Logger.getLogger(MemberViewAction.class
			.getName());

	/**
	 * Force cross-plugin deterministic class loading.
	 */
	public static void init() {
		M2ReleaseBadgeAction.class.toString();
		LastReleaseListViewColumn.class.toString();
	}

	private final ProjectIdentity identity;

	public MemberViewAction( //
			final ProjectIdentity identity //
	) {
		super( //
				MEMBER_VIEW_NAME, //
				MEMBER_VIEW_ICON, //
				MEMBER_VIEW_URL //
		);
		this.identity = identity;
	}

	/**
	 * Show view page.
	 */
	@Jelly
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final ListView view = identity.familyView();

		if (view == null) {
			return;
		}

		response.sendRedirect(request.getContextPath() + '/' + view.getUrl());

	}

	/**
	 * Standard and custom columns for the view.
	 */
	@Jelly
	public List<ListViewColumn> getColumnList() {
		final List<ListViewColumn> columnList = ListViewColumn
				.createDefaultInitialColumnList();
		columnList.add(new GraphViewColumn());
		columnList.add(new LastReleaseListViewColumn());
		return columnList;
	}

	@Jelly
	public ProjectIdentity getIdentity() {
		return identity;
	}

	@Jelly
	public List<MavenModuleSet> getProjectList() {
		return identity.memberProjectList();
	}

}
