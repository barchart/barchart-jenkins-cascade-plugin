/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.ListViewColumn;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provide graph link column for cascade member projects.
 * 
 * @author Andrei Pozolotin
 */
public class GraphViewColumn extends ListViewColumn implements PluginConstants {

	public static class TheDescriptor extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Cascade Member Graph View";
		}

		@Override
		public boolean shownByDefault() {
			return false;
		}
	}

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	@DataBoundConstructor
	public GraphViewColumn() {
	}

	/**
	 * Find graph view link for a project.
	 */
	@Jelly
	public String graphViewUrl(final AbstractProject<?, ?> project) {

		final GraphProjectAction action = project
				.getAction(GraphProjectAction.class);

		if (action == null) {
			return null;
		}

		return project.getAbsoluteUrl() + action.getUrlName();

	}

}
