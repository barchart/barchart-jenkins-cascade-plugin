/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.google.common.base.Functions.*;
import static com.google.common.collect.Lists.*;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.plugins.depgraph_view.model.display.AbstractDotStringGenerator;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.Edge;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;

/**
 * Generates the main graph in dot format.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphGeneratorForGraph extends AbstractDotStringGenerator {

	private static final Function<String, String> ESCAPE = new Function<String, String>() {
		public String apply(final String from) {
			return escapeString(from);
		}
	};

	public GraphGeneratorForGraph(final DependencyGraph graph,
			final ListMultimap<ProjectNode, ProjectNode> projects2Subprojects) {
		super(graph, projects2Subprojects);
	}

	/**
	 * Generates the graphviz code for the given projects and dependencies
	 * 
	 * @return graphviz code
	 */
	@Override
	public String generate() {
		/**** Build the dot source file ****/
		final StringBuilder builder = new StringBuilder();

		builder.append("digraph {\n");
		builder.append("node [shape=box, style=rounded];\n");
		builder.append("rankdir=TB;\n");
		builder.append("aspect=1;\n");

		/**** First define all the objects and clusters ****/

		// up/downstream linked jobs
		builder.append(cluster("Main", projectsInDependenciesNodes(),
				"color=invis;"));

		// Stuff not linked to other stuff
		final List<String> standaloneNames = transform(standaloneProjects,
				compose(ESCAPE, PROJECT_NAME_FUNCTION));
		builder.append(cluster("Standalone",
				standaloneProjectNodes(standaloneNames), "color=invis;"));

		/**** Now define links between objects ****/

		// edges
		for (final Edge edge : edges) {
			builder.append(dependencyToEdgeString(edge));
			builder.append(";\n");
		}

		if (!standaloneNames.isEmpty()) {
			builder.append("edge[style=\"invisible\",dir=\"none\"];\n"
					+ Joiner.on(" -> ").join(standaloneNames) + ";\n");
			builder.append("edge[style=\"invisible\",dir=\"none\"];\n"
					+ standaloneNames.get(standaloneNames.size() - 1)
					+ " -> \"Dependency Graph\"");
		}

		builder.append("}");

		return builder.toString();
	}

	private String standaloneProjectNodes(final List<String> standaloneNames) {
		final StringBuilder builder = new StringBuilder();
		for (final ProjectNode proj : standaloneProjects) {
			builder.append(projectToNodeString(proj, subJobs.get(proj)));
			builder.append(";\n");
		}
		if (!standaloneNames.isEmpty()) {
			builder.append("edge[style=\"invisible\",dir=\"none\"];\n"
					+ Joiner.on(" -> ").join(standaloneNames) + ";\n");
		}
		return builder.toString();
	}

	private String projectsInDependenciesNodes() {
		final StringBuilder stringBuilder = new StringBuilder();
		for (final ProjectNode proj : projectsInDeps) {
			if (subJobs.containsKey(proj)) {
				stringBuilder.append(projectToNodeString(proj,
						subJobs.get(proj)));
			} else {
				stringBuilder.append(projectToNodeString(proj));
			}
			stringBuilder.append(";\n");
		}
		return stringBuilder.toString();
	}

	private String projectToNodeString(final ProjectNode node) {

		final String name = escapeString(node.getName());
		final String url = getEscapedProjectUrl(node);
		String label = name;

		final AbstractProject<?, ?> project = node.getProject();
		if (project instanceof MavenModuleSet) {
			final MavenModuleSet mavenProject = (MavenModuleSet) project;
			try {
				final Model model = PluginUtilities.mavenModel(mavenProject);
				final String version = model.getVersion();
				label = escapeString(node.getName() + "\\n" + version);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return name + " [ " + " label=" + label + " href=" + url
				+ " fontsize=10 ] ";
	}

	private String projectToNodeString(final ProjectNode proj,
			final List<ProjectNode> subprojects) {
		final StringBuilder builder = new StringBuilder();
		builder.append(escapeString(proj.getName()))
				.append(" [shape=\"Mrecord\" href=")
				.append(getEscapedProjectUrl(proj))
				.append(" label=<<table border=\"0\" cellborder=\"0\" cellpadding=\"3\" bgcolor=\"white\">\n");
		builder.append(getProjectRow(proj));
		for (final ProjectNode subproject : subprojects) {
			builder.append(
					getProjectRow(subproject, "bgcolor="
							+ escapeString(subProjectColor))).append("\n");
		}
		builder.append("</table>>]");
		return builder.toString();
	}

	private String getProjectRow(final ProjectNode project,
			final String... extraColumnProperties) {
		return String.format(
				"<tr><td align=\"center\" href=%s %s>%s</td></tr>",
				getEscapedProjectUrl(project),
				Joiner.on(" ").join(extraColumnProperties), project.getName());
	}

	private String getEscapedProjectUrl(final ProjectNode proj) {
		return escapeString(proj.getProject().getAbsoluteUrl());
	}

	private String dependencyToEdgeString(final Edge edge,
			final String... options) {
		return escapeString(edge.source.getName()) + " -> "
				+ escapeString(edge.target.getName()) + " [ color="
				+ edge.getColor() + " " + Joiner.on(" ").join(options) + " ] ";
	}

}
