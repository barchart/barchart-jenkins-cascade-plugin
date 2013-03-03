package com.barchart.jenkins.cascade;

import hudson.maven.MavenArgumentInterceptorAction;
import hudson.maven.MavenModuleSetBuild;
import hudson.util.ArgumentListBuilder;

/**
 * Action provides custom arguments for layout build.
 */
public class LayoutInterceptorAction extends AdapterAction implements
		MavenArgumentInterceptorAction {

	private final String goalsAndOptions;

	public LayoutInterceptorAction(final String goalsAndOptions) {
		this.goalsAndOptions = goalsAndOptions;
	}

	public String getGoalsAndOptions(final MavenModuleSetBuild build) {
		return goalsAndOptions;
	}

	public ArgumentListBuilder intercept(final ArgumentListBuilder mavenargs,
			final MavenModuleSetBuild build) {
		return null;
	}

}
