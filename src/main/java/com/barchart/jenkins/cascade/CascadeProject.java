/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Project;
import hudson.model.Queue.FlyweightTask;

/**
 * Cascade orchestration project.
 * <p>
 * Peer project to the layout project. Provides cascade workflow.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeProject extends Project<CascadeProject, CascadeBuild>
		implements TopLevelItem, FlyweightTask, PluginConstants {

	public static class TheDescriptor extends AbstractProjectDescriptor {

		@Override
		public String getDisplayName() {
			return CASCADE_PROJECT_NAME;
		}

		@Jelly
		public String getIconFileName() {
			return CASCADE_PROJECT_ICON;
		}

		@Override
		public TopLevelItem newInstance(final ItemGroup parent,
				final String name) {
			return new CascadeProject(parent, name);
		}

	}

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	public CascadeProject(final ItemGroup parent, final String name) {
		super(parent, name);
	}

	@Override
	protected Class<CascadeBuild> getBuildClass() {
		return CascadeBuild.class;
	}

	public TheDescriptor getDescriptor() {
		return META;
	}

	@Override
	public String getPronoun() {
		return CASCADE_PROJECT_PRONOUN;
	}

}
