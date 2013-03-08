/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;


import hudson.model.Result;
import hudson.model.Job;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Run;

/**
 * Layout build history link.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutPermalink extends Permalink {

	public static final Permalink INSTANCE = new LayoutPermalink();

	@Override
	public String getDisplayName() {
		return PluginConstants.LAYOUT_ACTION_NAME;
	}

	@Override
	public String getId() {
		return PluginConstants.LAYOUT_ACTION_URL + "-permalink";
	}

	@Override
	public Run<?, ?> resolve(final Job<?, ?> job) {

		for (final Run<?, ?> run : job.getBuilds()) {

			final LayoutBadge action = run
					.getAction(LayoutBadge.class);

			if (action != null && run.getResult() == Result.SUCCESS) {
				return run;
			}

		}

		return null;

	}

}
