/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.AbstractBuildExecution;
import hudson.model.AbstractProject;

/**
 * Build context bean.
 * 
 * @author Andrei Pozolotin
 */
public class BuildContext<B extends AbstractBuild> {

	private final AbstractBuild build;
	private final Launcher launcher;
	private final BuildListener listener;

	public BuildContext(final AbstractBuildExecution execution) {
		this.build = (AbstractBuild) execution.getBuild();
		this.launcher = execution.getLauncher();
		this.listener = execution.getListener();
	}

	public BuildContext(//
			final AbstractBuild build, //
			final Launcher launcher, //
			final BuildListener listener //
	) {
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
	}

	/**
	 * Context build.
	 */
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
	 * Project identity of the context build.
	 */
	public ProjectIdentity identity() {
		return ProjectIdentity.identity(build().getProject());
	}

	/**
	 * Context launcher.
	 */
	public Launcher launcher() {
		return launcher;
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

	/**
	 * Context listener.
	 */
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

}
