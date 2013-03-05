/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Util;
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
import java.util.Set;

import jenkins.model.Jenkins;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
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

	/**
	 * Check if project exists.
	 */
	public static boolean isProjectExists(final String projectName) {
		return null != Jenkins.getInstance().getItem(projectName);
	}

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isLayoutBuild(final AbstractBuild build) {
		return build.getCause(LayoutUserCause.class) != null;
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
	public static boolean isSnapshot(final MavenModule job) throws Exception {
		return isSnapshot(job.getVersion());
	}

	/**
	 * Maven job version looks like snapshot.
	 */
	public static boolean isSnapshot(final MavenModuleSet job) throws Exception {
		return isSnapshot(job.getRootModule());
	}

	/**
	 * Version looks like snapshot.
	 */
	public static boolean isSnapshot(final String version) {
		return version.endsWith("SNAPSHOT");
	}

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
	 * Maven nested module immediate dependencies.
	 */
	public static List<Dependency> mavenDependencies(final MavenModule module,
			final DependencyMatcher matcher) throws Exception {

		final File rootDir = module.getParent().getRootDir();

		final File projectDir = new File(rootDir, module.getRelativePath());

		final File pomFile = new File(projectDir, "pom.xml");

		return mavenDependencies(pomFile, matcher);

	}

	/**
	 * Maven top level project immediate dependencies.
	 */
	public static List<Dependency> mavenDependencies(
			final MavenModuleSet project, final DependencyMatcher matcher)
			throws Exception {

		final String pomPath = project.getRootPOM(null);

		final File pomFile = new File(pomPath);

		return mavenDependencies(pomFile, matcher);

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
	 * Top level jenkins maven project.
	 */
	public static MavenModuleSet mavenProject(final AbstractBuild<?, ?> build) {

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
	 * Null-safe module name check.
	 */
	public static boolean isSameModuleName(final MavenModule one,
			final MavenModule two) {
		if (one == null || two == null) {
			return false;
		}
		return one.getModuleName().equals(two.getModuleName());
	}

	public static String moduleRelativePath(final MavenModule module) {

		final StringBuilder text = new StringBuilder();

		final MavenModuleSet parent = module.getParent();

		return text.toString();
	}

	private PluginUtilities() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void ensureProperty(final AbstractProject project,
			final JobProperty property) throws IOException {
		project.removeProperty(property.getClass());
		project.addProperty(property);
	}

}
