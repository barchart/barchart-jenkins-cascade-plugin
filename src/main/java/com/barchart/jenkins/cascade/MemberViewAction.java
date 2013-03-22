/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.maven.MavenModuleSet;
import hudson.model.ListView;
import hudson.plugins.depgraph_view.model.display.AbstractGraphStringGenerator;
import hudson.plugins.depgraph_view.model.display.DotGeneratorFactory;
import hudson.plugins.depgraph_view.model.display.GeneratorFactory;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.GraphCalculator;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;
import hudson.plugins.depgraph_view.model.graph.SubprojectCalculator;
import hudson.views.ListViewColumn;

import java.util.List;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jvnet.hudson.plugins.m2release.LastReleaseListViewColumn;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;

/**
 * Show cascade family view link for a cascade member project.
 * 
 * @author Andrei Pozolotin
 */
public class MemberViewAction extends AbstractAction {

	private static final Logger log = Logger.getLogger(MemberViewAction.class
			.getName());

	private final ProjectIdentity identity;

	public MemberViewAction( //
			final ProjectIdentity identity //
	) {
		super( //
				MEMBER_VIEW_NAME, //
				MEMBER_VIEW_ICON, //
				MEMBER_VIEW_URL //
		);
		this.identity = identity;
	}

	/**
	 * @jelly
	 */
	public void doSubmit(final StaplerRequest request,
			final StaplerResponse response) throws Exception {

		final ListView view = identity.familyView();

		if (view == null) {
			return;
		}

		response.sendRedirect(request.getContextPath() + '/' + view.getUrl());

	}

	@JellyField
	public List<ListViewColumn> getColumnList() {
		final List<ListViewColumn> columnList = ListViewColumn
				.createDefaultInitialColumnList();
		columnList.add(new LastReleaseListViewColumn());
		return columnList;
	}

	@JellyField
	public ProjectIdentity getIdentity() {
		return identity;
	}

	@JellyField
	public List<MavenModuleSet> getProjectList() {
		return identity.memberProjectList();
	}

	public void projectGraph() {

		final GeneratorFactory generatorFactory = new DotGeneratorFactory();

		final Injector injector = Jenkins.lookup(Injector.class);

		final GraphCalculator graphCalculator = injector
				.getInstance(GraphCalculator.class);

		final Iterable<ProjectNode> projectNodeSet = GraphCalculator
				.abstractProjectSetToProjectNodeSet(getProjectList());

		final DependencyGraph graph = graphCalculator
				.generateGraph(projectNodeSet);

		final ListMultimap<ProjectNode, ProjectNode> projects2Subprojects = injector
				.getInstance(SubprojectCalculator.class).generate(graph);

		final AbstractGraphStringGenerator generator = generatorFactory
				.newGenerator(graph, projects2Subprojects);

		final String graphText = generator.generate();

		// rsp.setContentType(imageType.contentType);
		// if (imageType.requiresProcessing) {
		// runDot(rsp.getOutputStream(), new
		// ByteArrayInputStream(graphString.getBytes(Charset.forName("UTF-8"))),
		// imageType.dotType);
		// } else {
		// rsp.getWriter().append(graphString).close();
		// }

	}

}
