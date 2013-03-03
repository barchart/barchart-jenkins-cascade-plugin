package com.barchart.jenkins.cascade;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jvnet.hudson.plugins.m2release.LastReleasePermalink;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * The action appears as the link in the side bar that users will click on in
 * order to start the release process.
 */
public class CascadeAction implements PermalinkProjectAction {

	private final MavenModuleSet project;

	public CascadeAction(final MavenModuleSet project) {
		this.project = project;
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

	public String getDisplayName() {
		return "XXX";
	}

	public String getIconFileName() {
		if (M2ReleaseBuildWrapper.hasReleasePermission(project)) {
			return "installer.gif"; //$NON-NLS-1$
		}
		// by returning null the link will not be shown.
		return null;
	}

	public String getUrlName() {
		return "m2release"; //$NON-NLS-1$
	}

	public Collection<MavenModule> getModules() {
		return project.getModules();
	}

	public MavenModule getRootModule() {
		return project.getRootModule();
	}

	public void doSubmit(final StaplerRequest req, final StaplerResponse resp)
			throws Exception {

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

	private static final List<Permalink> PERMALINKS = Collections
			.singletonList(LastReleasePermalink.INSTANCE);
}
