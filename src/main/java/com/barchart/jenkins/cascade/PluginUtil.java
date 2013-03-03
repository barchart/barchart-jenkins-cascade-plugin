package com.barchart.jenkins.cascade;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class PluginUtil {

	/**
	 * Dependency filter.
	 */
	public static interface DependencyMatcher {

		boolean isMatch(Dependency dependency);

	}

	public static final DependencyMatcher MATCH_SNAPSHOT = new DependencyMatcher() {

		public boolean isMatch(final Dependency dependency) {
			return isSnapshot(dependency);
		}

	};

	public static List<Dependency> mavenDependencies(final MavenModule job,
			final DependencyMatcher matcher) throws Exception {

		final File rootDir = job.getParent().getRootDir();

		final File projectDir = new File(rootDir, job.getRelativePath());

		final File pomFile = new File(projectDir, "pom.xml");

		return mavenDependencies(pomFile, matcher);

	}

	public static List<Dependency> mavenDependencies(final File pomFile,
			final DependencyMatcher matcher) throws Exception {

		final Model model = mavenModel(pomFile);

		@SuppressWarnings("unchecked")
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
	 */
	public static List<Dependency> mavenDependencies(final MavenModuleSet job,
			final Dependency matcher) throws Exception {

		final Collection<MavenModule> m = job.getModules();

		final String pomPath = job.getRootPOM(null);

		final File pomFile = new File(pomPath);

		final Model model = mavenModel(pomFile);

		@SuppressWarnings("unchecked")
		final List<Dependency> dependencies = model.getDependencies();

		for (final Dependency dependency : dependencies) {

			if (isSnapshot(dependency)) {
				return null;
			}

		}

		return null;
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

	/**
	 * Parse pom.xml file into maven model.
	 */
	public static Model mavenModel(final File pomFile) throws Exception {

		final MavenXpp3Reader xmlReader = new MavenXpp3Reader();

		final Reader fileReader = new FileReader(pomFile);

		final Model model = xmlReader.read(fileReader);

		return model;

	}

	private PluginUtil() {

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isLayoutBuild(final AbstractBuild build) {
		return build.getCause(LayoutUserCause.class) != null;
	}

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

}
