/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design_0;

import hudson.model.Action;
import hudson.model.AbstractProject;

import com.barchart.jenkins.cascade.PluginConstants;

/**
 * Cascade build action link on project page.
 */
public class CascadeBuildAction implements Action {

	final private AbstractProject<?, ?> project;

	public CascadeBuildAction(final AbstractProject<?, ?> project) {
		this.project = project;
	}

	public String getDisplayName() {
		return PluginConstants.CASCADE_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.CASCADE_ACTION_ICON;
	}

	public String getUrlName() {
		return PluginConstants.CASCADE_ACTION_URL;
	}

}
