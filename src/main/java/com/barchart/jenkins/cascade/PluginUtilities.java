/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Util;
import hudson.maven.ModuleName;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.JobProperty;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ListView;
import hudson.model.View;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import com.rits.cloning.Cloner;

/**
 * Plug-in utilities.
 * 
 * @author Andrei Pozolotin
 */
public class PluginUtilities {

	/**
	 * Dependency filter.
	 */
	public static interface DependencyMatcher {
		boolean isMatch(Dependency dependency);
	}

	/**
	 * Jenkins runner.
	 */
	public static interface JenkinsTask {
		void run() throws IOException;
	}

	/**
	 * Snapshot dependency matcher.
	 */
	public static final DependencyMatcher MATCH_SNAPSHOT = new DependencyMatcher() {

		public boolean isMatch(final Dependency dependency) {
			return isSnapshot(dependency);
		}

	};

	public static final String SNAPSHOT = "-SNAPSHOT";

	/**
	 * Change known instance field.
	 */
	public static void changeField(final Object instance,
			final String fieldName, final Object value) throws IOException {
		try {

			final Field field = instance.getClass().getDeclaredField(fieldName);

			field.setAccessible(true);

			field.set(instance, value);

		} catch (final Throwable e) {
			throw new IOException(e);
		}
	}

	/**
	 * Perform deep object clone.
	 */
	public static <T> T cloneDeep(final T source) {

		final Cloner cloner = new Cloner();

		// cloner.setDumpClonedClasses(true);

		final T target = cloner.deepClone(source);

		return target;

	}

	/**
	 * Validate model entries and create defaults.
	 */
	public static void ensureFields(final Model model) {
		if (model.getGroupId() == null) {
			final Parent parent = model.getParent();
			if (parent == null) {
				return;
			}
			model.setGroupId(parent.getGroupId());
		}
	}

	public static ListView ensureListView(final String viewName)
			throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		View view = jenkins.getView(viewName);

		if (view == null) {
			view = new ListView(viewName);
			jenkins.addView(view);
			return (ListView) view;
		} else {
			if (view instanceof ListView) {
				return (ListView) view;
			} else {
				throw new IllegalStateException(
						"View exists, but not as ListView: " + viewName);
			}
		}

	}

	/**
	 * Replace project job property.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void ensureProperty(final AbstractProject project,
			final JobProperty property) throws IOException {
		while (true) {
			final JobProperty past = project
					.removeProperty(property.getClass());
			if (past == null) {
				break;
			}
		}
		/** Will do save(). */
		project.addProperty(property);
	}

	/**
	 * Extract HTTP string parameter.
	 */
	public static String httpStringParam(final String key,
			final Map<?, ?> params) {
		return (String) (((Object[]) params.get(key))[0]);
	}

	/**
	 * Version contains some ${expression}.
	 */
	public static boolean isExpression(final String version) {
		return version.contains("${") && version.contains("}");
	}

	/**
	 * Strict failure result.
	 */
	public static boolean isFailure(final Result result) {
		return Result.SUCCESS != result;
	}

	/**
	 * Build originated by cascade action.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isMemberBuild(final AbstractBuild build) {
		return build.getCause(MemberBuildCause.class) != null;
	}

	/**
	 * Check if project exists.
	 */
	public static boolean isProjectExists(final String projectName) {
		return null != Jenkins.getInstance().getItem(projectName);
	}

	/**
	 * Verify model version is not snapshot.
	 */
	public static boolean isRelease(final Model model) {
		return !isSnapshot(model);
	}

	/**
	 * Verify model version is a snapshot.
	 */
	public static boolean isRelease(final Parent parent) {
		return !isSnapshot(parent);
	}

	/**
	 * Null-safe module name check.
	 */
	public static boolean isSameModuleName(final MavenModule one,
			final MavenModule two) {
		if (one == null || two == null) {
			return false;
		}
		return one.getModuleName().equals(two.getModuleName());
	}

	/**
	 * Maven dependency version looks like snapshot.
	 */
	public static boolean isSnapshot(final Dependency dependency) {
		return isSnapshot(dependency.getVersion());
	}

	/**
	 * Maven job version looks like snapshot.
	 */
	public static boolean isSnapshot(final MavenModule project)
			throws Exception {
		return isSnapshot(project.getVersion());
	}

	/**
	 * Maven job version looks like snapshot.
	 */
	public static boolean isSnapshot(final MavenModuleSet project)
			throws Exception {
		return isSnapshot(project.getRootModule());
	}

	/**
	 * Maven model version looks like snapshot.
	 */
	public static boolean isSnapshot(final Model model) {
		return isSnapshot(model.getVersion());
	}

	/**
	 * Parent version looks like snapshot.
	 */
	public static boolean isSnapshot(final Parent parent) {
		return isSnapshot(parent.getVersion());
	}

	/**
	 * Version looks like snapshot.
	 */
	public static boolean isSnapshot(final String version) {
		return version.endsWith(SNAPSHOT);
	}

	/**
	 * Strict success result.
	 */
	public static boolean isSuccess(final Result result) {
		return Result.SUCCESS == result;
	}

	/**
	 * Build maven artifact from maven dependency.
	 */
	public static Artifact mavenArtifact(final Dependency dependency) {
		return new DefaultArtifact( //
				dependency.getGroupId(), //
				dependency.getArtifactId(), //
				dependency.getVersion(), //
				null, // scope
				dependency.getType(), // type
				"", // classifier
				null // handler
		);
	}

	/**
	 * Build maven artifact from maven model.
	 */
	public static Artifact mavenArtifact(final Model model) {
		return new DefaultArtifact( //
				model.getGroupId(), //
				model.getArtifactId(), //
				model.getVersion(), //
				null, // scope
				model.getPackaging(), // type
				"", // classifier
				null // handler
		);
	}

	public static Artifact mavenArtifact(final Parent parent) {
		return new DefaultArtifact( //
				parent.getGroupId(), //
				parent.getArtifactId(), //
				parent.getVersion(), //
				null, // scope
				"pom", // type
				"", // classifier
				null // handler
		);
	}

	/**
	 * Collect matching dependencies form a pom.xml file.
	 */
	public static List<Dependency> mavenDependencies(final File pomFile,
			final DependencyMatcher matcher) throws Exception {

		final Model model = mavenModel(pomFile);

		final List<Dependency> dependencyList = model.getDependencies();

		final List<Dependency> snapshotList = new ArrayList<Dependency>();

		for (final Dependency dependency : dependencyList) {

			if (matcher.isMatch(dependency)) {
				snapshotList.add(dependency);
			}

		}

		return snapshotList;

	}

	/**
	 * Maven top level project immediate dependencies.
	 */
	public static List<Dependency> mavenDependencies(
			final MavenModuleSet project, final DependencyMatcher matcher)
			throws Exception {

		return mavenDependencies(mavenPomFile(project), matcher);

	}

	/**
	 * Parse pom.xml file into maven model.
	 */
	public static Model mavenModel(final File pomFile) throws Exception {

		final MavenXpp3Reader xmlReader = new MavenXpp3Reader();

		final Reader fileReader = new FileReader(pomFile);

		final Model model = xmlReader.read(fileReader);

		return model;

	}

	/**
	 * Extract maven model from jenkins project.
	 */
	public static Model mavenModel(final MavenModuleSet project)
			throws Exception {

		return mavenModel(mavenPomFile(project));

	}

	/**
	 * Top level jenkins maven module resolved from the build, or null.
	 */
	public static MavenModule mavenModule(final AbstractBuild<?, ?> build) {

		if (build instanceof MavenBuild) {
			final MavenBuild mavenBuild = (MavenBuild) build;
			return mavenBuild.getProject();
		}

		if (build instanceof MavenModuleSetBuild) {
			final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
			return mavenBuild.getProject().getRootModule();
		}

		return null;

	}

	/**
	 * Top level jenkins maven project resolved from the build, or null.
	 */
	public static MavenModuleSet mavenModuleSet(final AbstractBuild<?, ?> build) {

		if (build instanceof MavenBuild) {
			final MavenBuild mavenBuild = (MavenBuild) build;
			final MavenModule mavenModule = mavenBuild.getProject();
			return mavenModule.getParent();
		}

		if (build instanceof MavenModuleSetBuild) {
			final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
			return mavenBuild.getProject();
		}

		return null;
	}

	/**
	 * Extract maven parent form a pom.xml file.
	 */
	public static Parent mavenParent(final File pomFile) throws Exception {

		final Model model = mavenModel(pomFile);

		final Parent parent = model.getParent();

		return parent;

	}

	/**
	 * Find maven parent for a jenkins maven job.
	 * 
	 * @return null, when no parent.
	 */
	public static Parent mavenParent(final MavenModuleSet project)
			throws Exception {

		final Model model = mavenModel(project);

		final Parent parent = model.getParent();

		return parent;

	}

	/**
	 * Project pom.xml file.
	 */
	public static File mavenPomFile(final MavenModuleSet project) {

		final String relativePath = project.getRootPOM(null);

		final String absolutePath = project.getWorkspace().child(relativePath)
				.getRemote();

		final File pomFile = new File(absolutePath);

		return pomFile;
	}

	/**
	 * Find top level maven jenkins job with a module name.
	 */
	public static MavenModuleSet mavenProject(final ModuleName moduleName) {
		for (final MavenModuleSet project : mavenProjectList()) {
			if (project.getRootModule().getModuleName().equals(moduleName)) {
				return project;
			}
		}
		return null;
	}

	/**
	 * Find top level maven jenkins job with a project name.
	 */
	public static MavenModuleSet mavenProject(final String projectName) {
		final TopLevelItem item = Jenkins.getInstance().getItem(projectName);
		if (item instanceof MavenModuleSet) {
			return (MavenModuleSet) item;
		}
		return null;
	}

	/**
	 * Find all top level maven jenkins jobs.
	 */
	public static List<MavenModuleSet> mavenProjectList() {

		final Jenkins jenkins = Jenkins.getInstance();

		final List<TopLevelItem> itemList = jenkins
				.getAllItems(TopLevelItem.class);

		final List<MavenModuleSet> projectList = Util.createSubList(itemList,
				MavenModuleSet.class);

		return projectList;

	}

	/**
	 * Convert from any version to release version.
	 */
	public static String mavenReleaseVersion(final String version) {
		if (version.endsWith(SNAPSHOT)) {
			return version.replaceAll(SNAPSHOT, "");
		} else {
			return version;
		}
	}

	/**
	 * Convert from any version to snapshot version.
	 */
	public static String mavenSnapshotVersion(final String version) {
		if (version.endsWith(SNAPSHOT)) {
			return version;
		} else {
			return version + SNAPSHOT;
		}
	}

	/**
	 * Build jenkins module name from maven dependency.
	 */
	public static ModuleName moduleName(final Dependency dependency) {
		return new ModuleName(dependency.getGroupId(),
				dependency.getArtifactId());
	}

	/**
	 * Build jenkins module name from maven parent.
	 */
	public static ModuleName moduleName(final Parent parent) {
		return new ModuleName(parent.getGroupId(), parent.getArtifactId());
	}

	/**
	 * Convert form project list to project names.
	 */
	public static Set<String> moduleNameSet(
			final List<MavenModuleSet> moduleList) {

		final Set<String> set = new HashSet<String>();

		for (final MavenModuleSet module : moduleList) {
			set.add(module.getName());
		}

		return set;

	}

	/**
	 * Produce token variable entry from token name.
	 */
	public static String tokenVariable(final String tokenName) {
		return "${" + tokenName + "}";
	}

	private PluginUtilities() {
	}

}
