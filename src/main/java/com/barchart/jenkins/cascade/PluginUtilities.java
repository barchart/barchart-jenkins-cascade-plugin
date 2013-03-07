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
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

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

	static final String SNAPSHOT = "-SNAPSHOT";

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
	 * Replace project job property.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void ensureProperty(final AbstractProject project,
			final JobProperty property) throws IOException {
		project.removeProperty(property.getClass());
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
	 * Build originated by layout action.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isLayoutBuild(final AbstractBuild build) {
		return build.getCause(LayoutUserCause.class) != null;
	}

	/**
	 * Build originated by cascade action.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isMemberBuild(final AbstractBuild build) {
		return build.getCause(MemberUserCause.class) != null;
	}

	/**
	 * Check if project exists.
	 */
	public static boolean isProjectExists(final String projectName) {
		return null != Jenkins.getInstance().getItem(projectName);
	}

	public static boolean isRelease(final Model model) {
		return !isSnapshot(model);
	}

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
	 * Maven nested module immediate dependencies.
	 */
	// public static List<Dependency> mavenDependencies(final MavenModule
	// module,
	// final DependencyMatcher matcher) throws Exception {
	//
	// return mavenDependencies(mavenPomFile(module), matcher);
	//
	// }

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

	// public static Model mavenModel(final MavenModule module) throws Exception
	// {
	//
	// return mavenModel(mavenPomFile(module));
	//
	// }

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
	 * Module pom.xml file.
	 */
	// public static File mavenPomFile(final MavenModule module) {
	//
	// final File rootDir = module.getParent().getRootDir();
	//
	// final File projectDir = new File(rootDir, module.getRelativePath());
	//
	// final File pomFile = new File(projectDir, "pom.xml");
	//
	// return pomFile;
	//
	// }

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

	public static ModuleName moduleName(final Dependency dependency) {
		return new ModuleName(dependency.getGroupId(),
				dependency.getArtifactId());
	}

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

	/** TODO */
	public static String moduleRelativePath(final MavenModule module) {

		final StringBuilder text = new StringBuilder();

		final MavenModuleSet parent = module.getParent();

		return text.toString();
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
