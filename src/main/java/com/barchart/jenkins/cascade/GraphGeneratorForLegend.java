/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.plugins.depgraph_view.model.display.AbstractDotStringGenerator;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

import com.google.common.collect.ArrayListMultimap;

/**
 * Generates the legend in dot format.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphGeneratorForLegend extends AbstractDotStringGenerator {

	public GraphGeneratorForLegend() {
		super(new DependencyGraph(), ArrayListMultimap
				.<ProjectNode, ProjectNode> create());
	}

	@Override
	public String generate() {

		/**** Build the dot source file ****/
		final StringBuilder builder = new StringBuilder();

		builder.append("digraph {\n");
		builder.append("node [shape=box, style=rounded];\n");

		builder.append(cluster("Legend", legend()));

		builder.append("}");
		return builder.toString();
	}

	private String legend() {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder
				.append("label=\"Legend:\" labelloc=t centered=false color=black node [shape=plaintext]")
				.append("\"Dependency Graph\"\n")
				.append("\"Copy Artifact\"\n")
				.append("\"Sub-Project\"\n")
				.append("node [style=invis]\n")
				.append("a [label=\"\"] b [label=\"\"]")
				.append(" c [fillcolor=" + escapeString(subProjectColor)
						+ " style=filled fontcolor="
						+ escapeString(subProjectColor) + "]\n")
				.append("a -> b [style=invis]\n")
				.append("{rank=same a -> \"Dependency Graph\" [color=black style=bold minlen=2]}\n")
				.append("{rank=same b -> \"Copy Artifact\" [color=lightblue minlen=2]}\n")
				.append("{rank=same c -> \"Sub-Project\" [ style=invis]}\n");
		return stringBuilder.toString();
	}

}
