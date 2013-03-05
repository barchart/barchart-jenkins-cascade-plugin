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
import hudson.model.Descriptor.FormException;
import hudson.model.Project;
import hudson.model.Queue.FlyweightTask;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Orchestration project.
 * <p>
 * Peer project to the layout project. Provides cascade state persistence.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeProject extends Project<CascadeProject, CascadeBuild>
		implements TopLevelItem, FlyweightTask {

	public static class TheDescriptor extends AbstractProjectDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.CASCADE_PROJECT_NAME;
		}

		@Override
		public TopLevelItem newInstance(final ItemGroup parent,
				final String name) {
			return new CascadeProject(parent, name);
		}

	}

	@Extension
	public static final TheDescriptor DESCRIPTOR = new TheDescriptor();

	public CascadeProject(final ItemGroup parent, final String name) {
		super(parent, name);
	}

	@Override
	protected Class<CascadeBuild> getBuildClass() {
		return CascadeBuild.class;
	}

	public TheDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public String getPronoun() {
		return PluginConstants.CASCADE_PROJECT_PRONOUN;
	}

	@Override
	protected void submit(final StaplerRequest request,
			final StaplerResponse response) throws IOException,
			ServletException, FormException {

		super.submit(request, response);

		final JSONObject json = request.getSubmittedForm();

	}

}
