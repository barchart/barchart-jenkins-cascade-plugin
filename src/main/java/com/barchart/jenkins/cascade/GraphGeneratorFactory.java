/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.plugins.depgraph_view.model.display.AbstractGraphStringGenerator;
import hudson.plugins.depgraph_view.model.display.GeneratorFactory;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

import com.google.common.collect.ListMultimap;

/**
 * Provide graph generators.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphGeneratorFactory extends GeneratorFactory {

	@Override
	public AbstractGraphStringGenerator newGenerator(
			final DependencyGraph graph,
			final ListMultimap<ProjectNode, ProjectNode> subprojects) {

		return new GraphGeneratorForGraph(graph, subprojects);

	}

	@Override
	public AbstractGraphStringGenerator newLegendGenerator() {

		return new GraphGeneratorForLegend();

	}

}
