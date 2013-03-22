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
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.plugins.depgraph_view.model.graph.Edge;
import hudson.plugins.depgraph_view.model.graph.EdgeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;

/**
 * Provide cascade project graph dependency edge discovery.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphEdgeProvider implements EdgeProvider {

	protected final static Logger log = Logger
			.getLogger(GraphEdgeProvider.class.getName());

	@Inject
	public GraphEdgeProvider() {
		// log.info("### GraphEdgeProvider");
	}

	public Iterable<Edge> getEdgesIncidentWith(
			final AbstractProject<?, ?> project) {

		final List<Edge> edgeList = new ArrayList<Edge>();

		try {

			final ProjectIdentity identity = ProjectIdentity.identity(project);

			/** Cascade projects have identity. */
			if (identity == null) {
				return edgeList;
			}

			/** Interested in member projects only. */
			if (!(project instanceof MavenModuleSet)) {
				return edgeList;
			}

			final MavenModuleSet currentProject = (MavenModuleSet) project;

			/** Process parent. */
			final Parent parent = mavenParent(currentProject);
			if (parent != null) {

				final ModuleName parentName = moduleName(parent);

				final MavenModuleSet parentProject = identity
						.memberProject(parentName);

				/** Parent is not part of cascade. */
				if (parentProject != null) {

					final GraphEdge edge = new GraphEdge(parentProject,
							currentProject);

					if (isSnapshot(parent)) {
						edge.setColor("red");
					}

					edgeList.add(edge);
				}

			}

			/** Process dependencies. */
			final List<Dependency> dependencyList = mavenDependencies(
					currentProject, MATCH_ANY);

			for (final Dependency dependency : dependencyList) {

				final ModuleName dependencyName = moduleName(dependency);

				final MavenModuleSet dependencyProject = identity
						.memberProject(dependencyName);

				/** Dependency is not part of cascade. */
				if (dependencyProject == null) {
					continue;
				}

				final GraphEdge edge = new GraphEdge(dependencyProject,
						currentProject);

				if (isSnapshot(dependency)) {
					edge.setColor("blue");
				}

				edgeList.add(edge);

			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		return edgeList;

	}

}
