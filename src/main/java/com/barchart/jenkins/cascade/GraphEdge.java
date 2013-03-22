/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.plugins.depgraph_view.model.graph.Edge;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

import java.util.logging.Logger;

/**
 * Provide cascade project graph dependency edge.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphEdge extends Edge {

	protected final static Logger log = Logger.getLogger(GraphEdge.class
			.getName());

	private String color;

	public GraphEdge(final MavenModuleSet source, final MavenModuleSet target) {
		this(ProjectNode.node(source), ProjectNode.node(target));
	}

	public GraphEdge(final ProjectNode source, final ProjectNode target) {
		super(source, target);
	}

	public GraphEdge setColor(final String color) {
		this.color = color;
		return this;
	}

	@Override
	public String getColor() {
		return color == null ? super.getColor() : color;
	}

	@Override
	public String getType() {
		return "cascade";
	}

}
