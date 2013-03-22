/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.plugins.depgraph_view.DependencyGraphProjectActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Show cascade view link for member projects.
 * 
 * @author Stefan Wolf
 * @author Andrei Pozolotin
 */
@Extension
public class GraphProjectActionFatory extends
		DependencyGraphProjectActionFactory {

	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		final ProjectIdentity identity = ProjectIdentity.identity(project);

		if (identity == null) {
			return Collections.emptyList();
		}

		if (identity.role() != ProjectRole.MEMBER) {
			return Collections.emptyList();
		}

		return Collections.singleton(new GraphProjectAction(project));
	}

}
