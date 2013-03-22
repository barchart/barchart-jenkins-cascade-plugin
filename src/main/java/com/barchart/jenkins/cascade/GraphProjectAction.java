/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.AbstractModelObject;
import hudson.model.AbstractProject;
import hudson.plugins.depgraph_view.SupportedImageType;
import hudson.plugins.depgraph_view.AbstractDependencyGraphAction;
import hudson.plugins.depgraph_view.model.display.AbstractGraphStringGenerator;
import hudson.plugins.depgraph_view.model.display.GeneratorFactory;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.EdgeProvider;
import hudson.plugins.depgraph_view.model.graph.GraphCalculator;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;
import hudson.plugins.depgraph_view.model.graph.SubProjectProvider;
import hudson.plugins.depgraph_view.model.graph.SubprojectCalculator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.collect.ListMultimap;

/**
 * Show cascade view link for member projects.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
public class GraphProjectAction extends AbstractDependencyGraphAction implements
		PluginConstants {

	protected final static Logger log = Logger
			.getLogger(GraphProjectAction.class.getName());

	private final AbstractProject<?, ?> project;

	public GraphProjectAction(final AbstractProject<?, ?> project) {
		this.project = project;
	}

	@Override
	protected Collection<? extends AbstractProject<?, ?>> getProjectsForDepgraph() {
		return Collections.singletonList(project);
	}

	@Override
	public String getTitle() {
		return MEMBER_GRAPH_NAME;
	}

	@Override
	public AbstractModelObject getParentObject() {
		return project;
	}

	@Override
	public String getDisplayName() {
		return getTitle();
	}

	@Override
	public String getIconFileName() {
		/** Must use absolute path. */
		return PLUGIN_ICON;
	}

	@Override
	public String getUrlName() {
		return PLUGIN_ID + "-" + super.getUrlName();
	}

	@Override
	public void doDynamic(final StaplerRequest req, final StaplerResponse rsp)
			throws IOException, ServletException, InterruptedException {

		final String path = req.getRestOfPath();

		SupportedImageType imageType = null;
		try {
			imageType = SupportedImageType.valueOf(path.substring(
					path.lastIndexOf('.') + 1).toUpperCase());
		} catch (final Exception e) {
			imageType = SupportedImageType.PNG;
		}

		final GeneratorFactory generatorFactory = new GraphGeneratorFactory();

		AbstractGraphStringGenerator stringGenerator = null;

		if (path.startsWith("/graph.")) {

			final Set<EdgeProvider> edgeProviderSet = new HashSet<EdgeProvider>();
			edgeProviderSet.add(new GraphEdgeProvider());

			final GraphCalculator graphCalculator = new GraphCalculator(
					edgeProviderSet);

			final DependencyGraph graph = graphCalculator
					.generateGraph(GraphCalculator
							.abstractProjectSetToProjectNodeSet(getProjectsForDepgraph()));

			final Set<SubProjectProvider> subprojectProiveerSet = new HashSet<SubProjectProvider>();
			subprojectProiveerSet.add(new GraphSubProjectProvider());

			final SubprojectCalculator subprojCalculator = new SubprojectCalculator(
					subprojectProiveerSet);

			final ListMultimap<ProjectNode, ProjectNode> projects2Subprojects = subprojCalculator
					.generate(graph);

			stringGenerator = generatorFactory.newGenerator(graph,
					projects2Subprojects);

		} else if (path.startsWith("/legend.")) {

			stringGenerator = generatorFactory.newLegendGenerator();

		} else {

			rsp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);

			return;
		}

		final String graphString = stringGenerator.generate();

		rsp.setContentType(imageType.contentType);
		if (imageType.requiresProcessing) {
			runDot(rsp.getOutputStream(),
					new ByteArrayInputStream(graphString.getBytes(Charset
							.forName("UTF-8"))), imageType.dotType);
		} else {
			rsp.getWriter().append(graphString).close();
		}

	}

}
