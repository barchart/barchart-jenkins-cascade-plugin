/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;
import hudson.plugins.depgraph_view.model.graph.SubProjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;

/**
 * Provide cascade project graph dependency selector.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphSubProjectProvider implements SubProjectProvider {

	protected final static Logger log = Logger
			.getLogger(GraphSubProjectProvider.class.getName());

	@Inject
	public GraphSubProjectProvider() {
	}

	public Iterable<ProjectNode> getSubProjectsOf(
			final AbstractProject<?, ?> project) {

		// log.info("### getSubProjectsOf: " + project.getName());

		final List<ProjectNode> subProjects = new ArrayList<ProjectNode>();

		try {

			final ProjectIdentity identity = ProjectIdentity.identity(project);

			if (identity == null) {
				return subProjects;
			}

			if (!(project instanceof MavenModuleSet)) {
				return subProjects;
			}

			final MavenModuleSet mavenProject = (MavenModuleSet) project;

			final List<MavenModuleSet> memberProjectList = identity
					.memberProjectList();

			final List<Dependency> dependencyList = mavenDependencies(
					mavenProject, MATCH_ANY);

			for (final Dependency dependency : dependencyList) {

				final ModuleName dependencyModule = moduleName(dependency);

				for (final MavenModuleSet memberProject : memberProjectList) {

					final MavenModule memberModule = memberProject
							.getRootModule();

					// XXX
					if (dependencyModule.equals(memberModule)) {
						// subProjects.add(ProjectNode.node(memberProject)); //
					}

				}

			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		return subProjects;

	}
}
