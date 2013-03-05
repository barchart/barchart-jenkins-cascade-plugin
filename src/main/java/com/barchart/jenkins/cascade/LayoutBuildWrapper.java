/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Layout project.
 * <p>
 * Maven build wrapper for cascade layout management.
 * <p>
 * Validates maven build and updates cascade member projects layout.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class LayoutBuildWrapper extends BuildWrapper {

	@Extension
	public static class TheDescriptor extends BuildWrapperDescriptor {

		public static final String DEFAULT_LAYOUT_VIEW = "cascade";
		public static final String DEFAULT_MAVEN_GOALS = "clean validate";
		public static final String DEFAULT_MEMBER_PATTERN = "${artifactId}";
		public static final String DEFAULT_CASCADE_PATTERN = "${artifactId}_CASCADE";

		@Override
		public String getDisplayName() {
			return PluginConstants.LAYOUT_ACTION_NAME;
		}

		/**
		 * Interested in top level maven multi-module projects only.
		 */
		@Override
		public boolean isApplicable(final AbstractProject<?, ?> project) {

			if (project instanceof MavenModuleSet) {

				final MavenModuleSet mavenProject = (MavenModuleSet) project;

				final MavenModule rootModule = mavenProject.getRootModule();

				if (rootModule == null) {
					return false;
				}

				final List<MavenModule> mavenModuleList = rootModule
						.getChildren();

				if (mavenModuleList != null && !mavenModuleList.isEmpty()) {
					return true;
				}
			}

			return false;

		}

	}

	/** Jelly field. */
	private String layoutView;

	/** Jelly field. */
	private String mavenGoals;

	/** Jelly field. */
	private String memberPattern;

	/** Jelly field. */
	private String cascadePattern;

	public LayoutBuildWrapper() {
		/** Required for injection. */
	}

	/**
	 * Jelly injected.
	 */
	@DataBoundConstructor
	public LayoutBuildWrapper( //
			final String mavenGoals, //
			final String layoutView, //
			final String namePattern //
	) {
		this.mavenGoals = mavenGoals;
		this.layoutView = layoutView;
		this.memberPattern = namePattern;
	}

	@Override
	public TheDescriptor getDescriptor() {
		return (TheDescriptor) super.getDescriptor();
	}

	/**
	 * Jenkins view name for the cascade layout. This view will contain
	 * generated projects.
	 */
	public String getLayoutView() {
		return layoutView;
	}

	/**
	 * Maven goals to use for layout validation.
	 */
	public String getMavenGoals() {
		return mavenGoals;
	}

	/**
	 * Jenkins generated member project naming convention.
	 */
	public String getMemberPattern() {
		return memberPattern;
	}

	/**
	 * Jenkins generated cascade project naming convention.
	 */
	public String getCascadePattern() {
		return cascadePattern;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Action getProjectAction(final AbstractProject project) {
		return new LayoutBuildAction((MavenModuleSet) project);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Environment setUp(//
			final AbstractBuild build, //
			final Launcher launcher, //
			final BuildListener listener //
	) throws IOException {

		final PluginLogger log = new PluginLogger(listener);

		if (isLayoutBuild(build)) {

			log.text("Initiate maven validation.");

			/** Attach icon in build history. */
			build.addAction(new LayoutBadgeAction());

			/** Override maven build goals for validation. */
			build.addAction(new LayoutInterceptorAction(getMavenGoals()));

			return new Environment() {
				@Override
				public boolean tearDown(//
						final AbstractBuild build, //
						final BuildListener listener //
				) throws IOException {

					log.text("Maven validation finished.");

					final Result result = build.getResult();
					if (result.isWorseThan(Result.SUCCESS)) {
						log.text("Maven result is not success, abort.");
						return false;
					} else {
						log.text("Maven result is success, proceed.");
						return LayoutBuildLogic.process(log, build, listener);
					}

				}
			};

		} else {

			log.text("Maven build start.");

			return new Environment() {
				@Override
				public boolean tearDown(//
						final AbstractBuild build, //
						final BuildListener listener //
				) throws IOException {
					log.text("Maven build finish.");
					return true;
				}
			};

		}

	}

}
