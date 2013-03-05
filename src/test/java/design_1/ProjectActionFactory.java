/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design_1;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Collections;


/**
 * Factory to add a view action to each project.
 */
@Extension
public class ProjectActionFactory extends TransientProjectActionFactory {

	@Override
	public Collection<? extends Action> createFor(final AbstractProject project) {

		return Collections.singleton(new ProjectAction(project));

		// return Collections.emptyList();

	}

}
