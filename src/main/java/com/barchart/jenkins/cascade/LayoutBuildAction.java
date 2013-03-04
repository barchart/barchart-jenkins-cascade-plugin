/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jvnet.hudson.plugins.m2release.LastReleasePermalink;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action to display on project page to start layout build.
 */
public class LayoutBuildAction implements PermalinkProjectAction {

	public static final List<Permalink> PERMALINKS = Collections
			.singletonList(LastReleasePermalink.INSTANCE);

	private final MavenModuleSet project;

	public LayoutBuildAction(final MavenModuleSet project) {
		this.project = project;
	}

	public String getDisplayName() {
		return PluginConstants.LAYOUT_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.LAYOUT_ACTION_ICON;
	}

	public Collection<MavenModule> getModules() {
		return project.getModules();
	}

	/**
	 * Gets the {@link ParameterDefinition} of the given name, if any.
	 */
	public ParameterDefinition getParameterDefinition(final String name) {
		for (final ParameterDefinition definition : getParameterDefinitions()) {
			if (definition.getName().equals(name)) {
				return definition;
			}
		}
		return null;
	}

	public List<ParameterDefinition> getParameterDefinitions() {
		final ParametersDefinitionProperty pdp = project
				.getProperty(ParametersDefinitionProperty.class);
		List<ParameterDefinition> pds = Collections.emptyList();
		if (pdp != null) {
			pds = pdp.getParameterDefinitions();
		}
		return pds;
	}

	public List<Permalink> getPermalinks() {
		return PERMALINKS;
	}

	public MavenModule getRootModule() {
		return project.getRootModule();
	}

	public String getUrlName() {
		return PluginConstants.LAYOUT_ACTION_URL;
	}

	/**
	 * Jelly form submit.
	 */
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final LayoutBuildWrapper wrapper = project.getBuildWrappersList().get(
				LayoutBuildWrapper.class);

		final Map<?, ?> params = request.getParameterMap();

		final List<ParameterValue> values = new ArrayList<ParameterValue>();
		final Action parameters = new ParametersAction(values);

		final String configAction = getString("configAction", params);
		final Action arguments = new LayoutArgumentsAction(configAction);

		project.scheduleBuild(0, new LayoutUserCause(), parameters, arguments);

		response.sendRedirect(request.getContextPath() + '/' + project.getUrl());

	}

	public static String getString(final String key, final Map<?, ?> params) {
		return (String) (((Object[]) params.get(key))[0]);
	}

}
