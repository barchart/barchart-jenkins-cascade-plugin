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

				.append("label=\"Legend:\" labelloc=t centered=false color=black node [shape=plaintext] fontsize=10")

				.append("\"Released Parent\" [fontsize=10] \n")
				.append("\"Snapshot Parent\" [fontsize=10] \n")
				.append("\"Released Dependency\" [fontsize=10] \n")
				.append("\"Snapshot Dependency\" [fontsize=10] \n")

				.append("node [style=invis]\n")
				.append("a [label=\"\"]")
				.append("b [label=\"\"]")
				.append("c [label=\"\"]")
				.append("d [label=\"\"]")

				.append("a -> \"Released Parent\" [color=red4 style=bold minlen=2 ]\n")
				.append("b -> \"Snapshot Parent\" [color=red style=bold  minlen=2] \n")
				.append("c -> \"Released Dependency\" [color=blue4 style=bold  minlen=2 ]\n")
				.append("d -> \"Snapshot Dependency\" [color=blue style=bold  minlen=2 ]\n")

		;

		return stringBuilder.toString();
	}

}
