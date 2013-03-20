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
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Layout project extender.
 * <p>
 * Maven build wrapper for cascade layout management.
 * <p>
 * Validates maven build and updates cascade member projects.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class LayoutBuildWrapper extends BuildWrapper {

	public static class TheDescriptor extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return PluginConstants.PLUGIN_NAME;
		}

		/**
		 * Interested in non-cascade maven projects (layout candidate) or
		 * existing layout projects.
		 */
		@Override
		public boolean isApplicable(final AbstractProject<?, ?> project) {

			final ProjectIdentity identity = ProjectIdentity.identity(project);

			/** Non-cascade maven project. */
			if (identity == null) {
				if (project instanceof MavenModuleSet) {
					return true;
				} else {
					return false;
				}
			}

			/** Cascade family layout project. */
			switch (identity.role()) {
			case LAYOUT:
				return true;
			case CASCADE:
			case MEMBER:
			default:
				return false;
			}

		}

	}

	protected final static Logger log = Logger
			.getLogger(LayoutBuildWrapper.class.getName());

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	public static boolean hasWrapper(final MavenModuleSet project) {
		return wrapper(project) != null;
	}

	/**
	 * Validate project configuration against cascade assumptions.
	 * 
	 * @jelly
	 */
	public static String validateConfig(final String projectName) {

		final TopLevelItem item = Jenkins.getInstance().getItem(projectName);

		if (item == null) {
			log.severe("Project is missing: " + item);
			return "Project is missing";
		}

		if (!(item instanceof MavenModuleSet)) {
			log.severe("Project is of wrong type: " + item);
			return "Project is of wrong type";
		}

		final MavenModuleSet project = (MavenModuleSet) item;

		final String messageScm = PluginScm.checkScm(project);

		if (messageScm != null) {
			log.severe(messageScm);
			return messageScm;
		}

		return null;
	}

	public static LayoutBuildWrapper wrapper(final MavenModuleSet project) {

		final LayoutBuildWrapper wrapper = project.getBuildWrappersList().get(
				LayoutBuildWrapper.class);

		return wrapper;

	}

	private CascadeOptions cascadeOptions;
	private LayoutOptions layoutOptions;

	private String projectName;

	/**
	 * Required for injection.
	 */
	public LayoutBuildWrapper() {
	}

	/**
	 * @jelly
	 */
	@DataBoundConstructor
	public LayoutBuildWrapper( //
			final CascadeOptions cascadeOptions, //
			final LayoutOptions layoutOptions, //
			final String projectName//
	) {
		this.cascadeOptions = cascadeOptions;
		this.layoutOptions = layoutOptions;
		this.projectName = projectName;
	}

	public CascadeOptions getCascadeOptions() {
		if (cascadeOptions == null) {
			cascadeOptions = CascadeOptions.META.global();
		}
		return cascadeOptions;
	}

	@Override
	public TheDescriptor getDescriptor() {
		return META;
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
				build, launcher, listener);

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
