/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design_1;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.barchart.jenkins.cascade.CascadePermalink;
import com.barchart.jenkins.cascade.PluginConstants;

/**
 * The action appears as the link in the side bar that users will click on in
 * order to start cascade release process.
 * 
 * @author Andrei Pozolotin
 */
public class CascadeBuildAction implements PermalinkProjectAction {

	private static final List<Permalink> PERMALINKS = Collections
			.singletonList(CascadePermalink.INSTANCE);

	private final MavenModuleSet project;

	public CascadeBuildAction(final MavenModuleSet project) {
		this.project = project;
	}

	public void doSubmit(final StaplerRequest req, final StaplerResponse resp)
			throws Exception {

	}

	public String getDisplayName() {
		return PluginConstants.CASCADE_ACTION_NAME;
	}

	public String getIconFileName() {
		return PluginConstants.CASCADE_ACTION_ICON;
	}

	public Collection<MavenModule> getModules() {
		return project.getModules();
	}

	/**
	 * Gets the {@link ParameterDefinition} of the given name, if any.
	 */
	public ParameterDefinition getParameterDefinition(final String name) {
		for (final ParameterDefinition pd : getParameterDefinitions()) {
			if (pd.getName().equals(name)) {
				return pd;
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
		return PluginConstants.CASCADE_ACTION_URL;
	}

}
