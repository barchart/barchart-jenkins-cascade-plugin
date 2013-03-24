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
public class BuildContext<B extends AbstractBuild<?, ?>> extends BuildLogger {

	private static final long serialVersionUID = 1L;

	private final AbstractBuild<?, ?> build;
	private final Launcher launcher;

	public BuildContext(//
			final AbstractBuild<?, ?> build, //
			final Launcher launcher, //
			final BuildListener listener //
	) {
		super(listener);
		this.build = build;
		this.launcher = launcher;
	}

	public BuildContext(final AbstractBuildExecution execution) {
		super(execution.getListener());
		this.build = (AbstractBuild<?, ?>) execution.getBuild();
		this.launcher = execution.getLauncher();
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
	 * Remote-friendly logger for the same context.
	 */
	public BuildLogger logger() {
		return new BuildLogger(listener());
	}

}
