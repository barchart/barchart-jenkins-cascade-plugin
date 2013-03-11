/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
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

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action to display on project page to start layout build.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutBuildAction implements PermalinkProjectAction {

	public static final List<Permalink> PERMALINKS = Collections
			.singletonList(LayoutPermalink.INSTANCE);

	private final MavenModuleSet layoutProject;

	public LayoutBuildAction(final MavenModuleSet layoutProject) {
		this.layoutProject = layoutProject;
	}

	public String getDisplayName() {
		return PluginConstants.LAYOUT_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.LAYOUT_ACTION_ICON;
	}

	public Collection<MavenModule> getModules() {
		return layoutProject.getModules();
	}

	public ParameterDefinition getParameterDefinition(final String name) {
		for (final ParameterDefinition definition : getParameterDefinitions()) {
			if (definition.getName().equals(name)) {
				return definition;
			}
		}
		return null;
	}

	public List<ParameterDefinition> getParameterDefinitions() {
		final ParametersDefinitionProperty pdp = layoutProject
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
		return layoutProject.getRootModule();
	}

	public String getUrlName() {
		return PluginConstants.LAYOUT_ACTION_URL;
	}

	/**
	 * Jelly form submit.
	 * <p>
	 * Start layout build.
	 */
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final Map<?, ?> params = request.getParameterMap();

		final List<ParameterValue> values = new ArrayList<ParameterValue>();
		final Action parameters = new ParametersAction(values);

		final String configAction = httpStringParam("configAction", params);
		final Action arguments = new LayoutArgumentsAction(configAction);

		final Action badge = ProjectAction.form(configAction).badge();

		layoutProject.scheduleBuild(0, new LayoutBuildCause(), parameters,
				arguments, badge);

		response.sendRedirect(request.getContextPath() + '/'
				+ layoutProject.getUrl());

	}

	public LayoutOptions getLayoutOptions() {
		return LayoutBuildWrapper.wrapper(layoutProject).getLayoutOptions();
	}

}
