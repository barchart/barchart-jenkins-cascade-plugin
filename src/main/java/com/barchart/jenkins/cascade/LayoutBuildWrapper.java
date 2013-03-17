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
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Layout project.
 * <p>
 * Maven build wrapper for cascade layout management.
 * <p>
 * Validates maven build and updates cascade member projects.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class LayoutBuildWrapper extends BuildWrapper {

	@Extension
	public static class TheDescriptor extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return "Cascade layout and release builds";
		}

		/**
		 * Interested in top level maven multi-module projects only.
		 */
		@Override
		public boolean isApplicable(final AbstractProject<?, ?> project) {
			if (project instanceof MavenModuleSet) {
				return true;
			}
			return false;
		}

	}

	public static boolean hasWrapper(final MavenModuleSet project) {
		return wrapper(project) != null;
	}

	public static LayoutBuildWrapper wrapper(final MavenModuleSet project) {

		final LayoutBuildWrapper wrapper = project.getBuildWrappersList().get(
				LayoutBuildWrapper.class);

		return wrapper;

	}

	private CascadeOptions cascadeOptions = CascadeOptions.META.global();

	private LayoutOptions layoutOptions = LayoutOptions.META.global();

	/**
	 * Required for injection.
	 */
	public LayoutBuildWrapper() {
	}

	/**
	 * Jelly form submit.
	 */
	@DataBoundConstructor
	public LayoutBuildWrapper( //
			final CascadeOptions cascadeOptions, //
			final LayoutOptions layoutOptions //
	) {
		this.cascadeOptions = cascadeOptions;
		this.layoutOptions = layoutOptions;
	}

	public CascadeOptions getCascadeOptions() {
		if (cascadeOptions == null) {
			cascadeOptions = CascadeOptions.META.global();
		}
		return cascadeOptions;
	}

	public LayoutOptions getLayoutOptions() {
		if (layoutOptions == null) {
			layoutOptions = LayoutOptions.META.global();
		}
		return layoutOptions;
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

		final BuildContext<MavenModuleSetBuild> context = new BuildContext<MavenModuleSetBuild>(
				build, listener);

		if (LayoutBuildCause.hasCause(build)) {

			context.log("Start maven validation.");

			/** Override project build goals to do validation only. */
			build.addAction(new MavenGoalsIntercept(getLayoutOptions()
					.getMavenValidateGoals()));

			return new Environment() {
				@Override
				public boolean tearDown(//
						final AbstractBuild build, //
						final BuildListener listener //
				) throws IOException {

					context.log("Maven validation finished.");

					final Result result = build.getResult();
					if (isFailure(result)) {
						context.log("Maven result is failure, abort.");
						return false;
					} else {
						context.log("Maven result is success, proceed.");
						return LayoutLogic.process(context);
					}

				}
			};

		} else {

			context.log("Maven build start.");

			return new Environment() {
				@Override
				public boolean tearDown(//
						final AbstractBuild build, //
						final BuildListener listener //
				) throws IOException {
					context.log("Maven build finish.");
					return true;
				}
			};

		}

	}

}
