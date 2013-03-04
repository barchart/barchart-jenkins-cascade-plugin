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
 * Peer project to root maven project. Provides cascade state persistence.
 */
public class CascadeProject extends Project<CascadeProject, CascadeBuild>
		implements TopLevelItem, FlyweightTask {

	public static class CascadeProjectDescriptor extends
			AbstractProjectDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.CASCADE_ACTION_NAME;
		}

		@Override
		public TopLevelItem newInstance(final ItemGroup parent,
				final String name) {
			return new CascadeProject(parent, name);
		}

	}

	@Extension
	public static final CascadeProjectDescriptor DESCRIPTOR = new CascadeProjectDescriptor();

	public CascadeProject(final ItemGroup parent, final String name) {
		super(parent, name);
	}

	@Override
	protected Class<CascadeBuild> getBuildClass() {
		return CascadeBuild.class;
	}

	public CascadeProjectDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public String getPronoun() {
		return PluginConstants.PLUGIN_PRONOUN;
	}

	@Override
	protected void submit(final StaplerRequest request,
			final StaplerResponse response) throws IOException,
			ServletException, FormException {

		super.submit(request, response);

		final JSONObject json = request.getSubmittedForm();

	}

}
