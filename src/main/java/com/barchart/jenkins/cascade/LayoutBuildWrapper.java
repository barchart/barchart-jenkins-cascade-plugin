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
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import com.barchart.jenkins.cascade.PluginUtilities.JenkinsTask;

/**
 * Maven build wrapper for cascade layout management.
 * <p>
 * Validates maven build and updates cascade projects layout.
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
	private String mavenGoals = LayoutBuildWrapperDescriptor.DEFAULT_MAVEN_GOALS;

	/** Jelly field. */
	private String layoutView;

	/** Jelly field. */
	private String namePattern;

	public LayoutBuildWrapper() {
		/** Required for injection. */
	}

	/**
	 * Injected from jelly.
	 */
	@DataBoundConstructor
	public LayoutBuildWrapper( //
			final String mavenGoals, //
			final String layoutView, //
			final String namePattern //
	) {
		this.mavenGoals = mavenGoals;
		this.layoutView = layoutView;
		this.namePattern = namePattern;
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
	 * Maven goals to use for layout validation.
	 */
	public String getMavenGoals() {
		return mavenGoals;
	}

	/**
	 * Jenkins view name for the cascade layout. This view will contain
	 * generated projects.
	 */
	public String getLayoutView() {
		return layoutView;
	}

	/**
	 * Jenkins generated project naming convention. New project names will use
	 * this regex rule.
	 */
	public String getNamePattern() {
		return namePattern;
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

	/**
	 * Process layout build action.
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

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();

			/**
			 * Module-to-Project naming convention.
			 * <p>
			 * TODO expose in UI.
			 */
			final String projectName = moduleName.artifactId;

			log.text("---");
			log.text("Module name: " + moduleName);
			log.text("Project name: " + projectName);

			if (isSameModuleName(rootProject.getRootModule(), module)) {
				log.text("This is a root module project, managed by user, skip.");
				continue;
			}

			final JenkinsTask projectCreate = new JenkinsTask() {
				public void run() throws IOException {
					if (isProjectExists(projectName)) {
						log.text("Project exists, create skipped: "
								+ projectName);
					} else {
						log.text("Creating project: " + projectName);

						/** Clone project via XML. */
						final TopLevelItem item = jenkins.copy(
								(TopLevelItem) rootProject, projectName);

						final MavenModuleSet project = (MavenModuleSet) item;

						process(module, project);

						log.text("Project created: " + projectName);
					}
				}
			};

			final JenkinsTask projectDelete = new JenkinsTask() {
				public void run() throws IOException {
					if (!isProjectExists(projectName)) {
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

	/**
	 * Process details of created project.
	 */
	public static void process(final MavenModule module,
			final MavenModuleSet project) throws IOException {

		/** Update SCM paths. */
		{
			final SCM scm = project.getScm();

			if (scm instanceof GitSCM) {

				final GitSCM gitScm = (GitSCM) scm;

				final String includedRegions = module.getRelativePath() + "/.*";

				changeField(gitScm, "includedRegions", includedRegions);

			}
		}

		/** Update Maven paths. */
		{
			final String rootPOM = module.getRelativePath() + "/pom.xml";

			project.setRootPOM(rootPOM);

		}

		/** Disable cascade layout. */
		{
			final DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrapperList = project
					.getBuildWrappersList();

			buildWrapperList.remove(LayoutBuildWrapper.class);
		}

		/** Persist changes. */
		{
			project.save();
		}

	}

}
