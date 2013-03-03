package bench;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.DependencyGraph;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.Maven.MavenInstallation;

import java.util.List;

import jenkins.model.Jenkins;

public class Logic {

	MavenModuleSet upstream(final MavenModuleSet job) {

		final MavenModule root = job.getRootModule();

		final String version = root.getVersion();

		return null;
	}

	/** project is snapshot itself */
	boolean isSnapshot(final MavenModuleSet job) {
		return false;
	}

	/** project has snapshot dependencies */
	boolean hasSnapshots(final MavenModuleSet job) {
		return false;
	}

	/** immediate snapshot dependencies are present as jobs */
	boolean hasUpstream(final MavenModuleSet job) {
		return false;
	}

	/** job {@link #hasSnapshots(Job)} and */
	boolean canRelease(final MavenModuleSet job) {
		return false;
	}

	void runner() {

		final Jenkins jenkins = Jenkins.getInstance();

		final DependencyGraph graph = jenkins.getDependencyGraph();

		final List<AbstractProject> up = graph.getUpstream(null);

		final MavenModuleSet current = null;

		final MavenInstallation maven = current.getMaven();

		final MavenModuleSet upstream = upstream(current);

		if (upstream == null) {
			return;
		}

	}

}
