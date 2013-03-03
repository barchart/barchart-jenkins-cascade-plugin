package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Maven build wrapper for layout management.
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

		if (PluginUtil.isLayoutBuild(build)) {

			listener.getLogger()
					.println("[CASCADE] Layout: initiate validate.");

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

					listener.getLogger().println(
							"[CASCADE] Layout: complete validate.");

					final Result result = build.getResult();
					if (result.isWorseThan(Result.SUCCESS)) {
						listener.getLogger().println(
								"[CASCADE] Layout aborted.");
						return false;
					} else {
						listener.getLogger().println(
								"[CASCADE] Layout proceed.");

						return process(build, listener);
					}

				}
			};

		} else {

			listener.getLogger().println("[CASCADE] Maven: initiate build.");

			return new Environment() {
				@Override
				public boolean tearDown(//
						final AbstractBuild build, //
						final BuildListener listener //
				) throws IOException {

					listener.getLogger().println(
							"[CASCADE] Maven: complete build.");

					return true;
				}
			};

		}

	}

	public static boolean process(//
			final AbstractBuild<?, ?> build, //
			final BuildListener listener //
	) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final MavenModuleSet root = PluginUtil.mavenModuleSet(build);

		final LayoutArgumentsAction action = build
				.getAction(LayoutArgumentsAction.class);

		final List<TopLevelItem> itemList = jenkins
				.getAllItems(TopLevelItem.class);

		/** Existing projects. */
		final List<MavenModuleSet> projectList = Util.createSubList(itemList,
				MavenModuleSet.class);

		/** Managed modules. */
		final Collection<MavenModule> moduleList = root.getModules();

		final Set<String> nameSet = nameSet(projectList);

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();
			final String moduleProject = moduleName.artifactId;

			listener.getLogger().println("[CASCADE] module: " + moduleName);

			switch (action.getConfigAction()) {

			default: {

				listener.getLogger().println("[CASCADE] Unexpected action.");

				break;
			}

			case CREATE: {

				if (nameSet.contains(moduleProject)) {
					listener.getLogger().println(
							"[CASCADE] exists: " + moduleName);
				} else {
					listener.getLogger().println(
							"[CASCADE] create: " + moduleName);

					final TopLevelItem project = jenkins.copy(
							(TopLevelItem) root, moduleProject);
				}

				break;

			}

			case DELETE: {

				if (!nameSet.contains(moduleProject)) {
					listener.getLogger().println(
							"[CASCADE] missing: " + moduleName);
				} else {
					listener.getLogger().println(
							"[CASCADE] delete : " + moduleName);

					final TopLevelItem item = jenkins.getItem(moduleProject);

					try {
						item.delete();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}

				}

				break;

			}

			case UPDATE:
				listener.getLogger().println("[CASCADE] TODO.");
				break;
			}

		}

		return true;

	}

	public static Set<String> nameSet(final List<MavenModuleSet> moduleList) {

		final Set<String> set = new HashSet<String>();

		for (final MavenModuleSet module : moduleList) {
			set.add(module.getName());
		}

		return set;

	}

}
