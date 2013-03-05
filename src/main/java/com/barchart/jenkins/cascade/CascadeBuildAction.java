/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.model.Action;

/**
 * Cascade build action link on project page.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeBuildAction implements Action {

	final private MavenModuleSet project;

	public CascadeBuildAction(final MavenModuleSet project) {
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
