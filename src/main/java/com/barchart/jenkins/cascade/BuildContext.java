/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 * Build context bean.
 * 
 * @author Andrei Pozolotin
 */
public class BuildContext<B extends AbstractBuild> {

	private final AbstractBuild build;

	private final BuildListener listener;

	public BuildContext(//
			final AbstractBuild build, //
			final BuildListener listener //
	) {
		this.build = build;
		this.listener = listener;
	}

	public B build() {
		return (B) build;
	}

	/**
	 * Extract cascade options from layout build wrapper.
	 */
	public CascadeOptions cascadeOptions() {
		final MavenModuleSet layoutProject = layoutProject();
		final LayoutBuildWrapper wrapper = LayoutBuildWrapper
				.wrapper(layoutProject);
		return wrapper.getCascadeOptions();
	}

	/**
	 * Extract layout options from layout build wrapper.
	 */
	public LayoutOptions layoutOptions() {
		final MavenModuleSet layoutProject = layoutProject();
		final LayoutBuildWrapper wrapper = LayoutBuildWrapper
				.wrapper(layoutProject);
		return wrapper.getLayoutOptions();
	}

	public BuildListener listener() {
		return listener;
	}

	/** Log text with plug-in prefix. */
	public void log(final String text) {
		listener.getLogger()
				.println(PluginConstants.LOGGER_PREFIX + " " + text);
	}

	/** Log error with plug-in prefix. */
	public void logErr(final String text) {
		listener.error(PluginConstants.LOGGER_PREFIX + " " + text);
	}

	public void logExc(final Throwable e) {
		e.printStackTrace(listener().getLogger());
	}

	/** Log text with plug-in prefix and a tab. */
	public void logTab(final String text) {
		log("\t" + text);
	}

	/**
	 * Find layout project form any cascade family project.
	 */
	public MavenModuleSet layoutProject() {
		final AbstractProject<?, ?> currentProject = build().getProject();
		final ProjectIdentity property = ProjectIdentity
				.identity(currentProject);
		final MavenModuleSet layoutProject = property.layoutProject();
		return layoutProject;
	}

}
