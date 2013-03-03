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
import hudson.maven.AbstractMavenProject;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Maven build wrapper for cascade layout management.
 * <p>
 * Validates maven build and updates project layout.
 */
@Extension
public class LayoutBuildWrapper extends BuildWrapper {

	@Extension
	public static class LayoutBuildWrapperDescriptor extends
			BuildWrapperDescriptor {

		/**
		 * Default maven invocation command for layout management.
		 */
		public static final String DEFAULT_MAVEN_GOALS = "clean validate";

		@Override
		public String getDisplayName() {
			return "Configure Cascade";
		}

		@Override
		public boolean isApplicable(final AbstractProject<?, ?> project) {
			return (project instanceof AbstractMavenProject);
		}

	}

	private String mavenGoals = LayoutBuildWrapperDescriptor.DEFAULT_MAVEN_GOALS;

	public LayoutBuildWrapper() {
		/** Required for injection. */
	}

	/**
	 * Injected from jelly.
	 */
	@DataBoundConstructor
	public LayoutBuildWrapper(final String mavenGoals) {
		this.mavenGoals = mavenGoals;
	}

	@Override
	public LayoutBuildWrapperDescriptor getDescriptor() {
		return (LayoutBuildWrapperDescriptor) super.getDescriptor();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Action getProjectAction(final AbstractProject project) {
		return new LayoutBuildAction((MavenModuleSet) project);
	}

	/**
	 * Jelly field.
	 */
	public String getMavenGoals() {
		return mavenGoals;
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
						return process(build, listener);
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

	public static interface JenkinsTask {
		void run() throws IOException;
	}

	/**
	 * Process layout action.
	 */
	public static boolean process(//
			final AbstractBuild<?, ?> build, //
			final BuildListener listener //
	) throws IOException {

		final PluginLogger log = new PluginLogger(listener);

		final Jenkins jenkins = Jenkins.getInstance();

		final MavenModuleSet rootProject = mavenProject(build);

		final LayoutArgumentsAction action = build
				.getAction(LayoutArgumentsAction.class);

		/** Existing projects. */
		final List<MavenModuleSet> projectList = mavenProjectList();

		/** Managed modules. */
		final Collection<MavenModule> moduleList = rootProject.getModules();

		final Set<String> projectNameSet = moduleNameSet(projectList);

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();

			/** Module-to-Project naming convention. */
			final String projectName = moduleName.artifactId;

			log.text("---");
			log.text("Module name: " + moduleName);
			log.text("Project name: " + projectName);

			final JenkinsTask projectCreate = new JenkinsTask() {
				public void run() throws IOException {
					if (projectNameSet.contains(projectName)) {
						log.text("Project exists, create skipped: "
								+ projectName);
					} else {
						log.text("Creating project: " + projectName);

						final TopLevelItem project = jenkins.copy(
								(TopLevelItem) rootProject, projectName);

						log.text("Project created: " + projectName);
					}
				}
			};

			final JenkinsTask projectDelete = new JenkinsTask() {
				public void run() throws IOException {
					if (!projectNameSet.contains(projectName)) {
						log.text("Project not present, delete skipped: "
								+ projectName);
					} else {
						final TopLevelItem item = jenkins.getItem(projectName);
						log.text("Deleting project : " + projectName);
						try {
							item.delete();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
						log.text("Project deleted: " + projectName);
					}
				}
			};

			switch (action.getConfigAction()) {
			default:
				log.text("Unexpected config action, ignore: "
						+ action.getConfigAction());
				break;
			case CREATE:
				projectCreate.run();
				break;
			case DELETE:
				projectDelete.run();
				break;
			case UPDATE:
				projectDelete.run();
				projectCreate.run();
				break;
			}

		}

		return true;
	}

}
