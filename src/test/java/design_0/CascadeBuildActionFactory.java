/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design_0;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Collections;

/**
 * Factory to add a view action to each project.
 */
@Extension
public class CascadeBuildActionFactory extends TransientProjectActionFactory {

	/** Interested on in cascade layout projects. */
	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		if (project instanceof MavenModuleSet) {

			final MavenModuleSet mavenProject = (MavenModuleSet) project;

			return Collections.singleton(new CascadeBuildAction(project));

		} else {

			return Collections.emptyList();

		}

	}

}
