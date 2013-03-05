/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

/**
 * Build context bean.
 * 
 * @author Andrei Pozolotin
 */
public class BuildContext {

	private final AbstractBuild<?, ?> build;
	private final Launcher launcher;
	private final BuildListener listener;

	public BuildContext(//
			final AbstractBuild<?, ?> build, //
			final Launcher launcher, //
			final BuildListener listener //
	) {
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
	}

	public AbstractBuild<?, ?> build() {
		return build;
	}

	public Launcher launcher() {
		return launcher;
	}

	public BuildListener listener() {
		return listener;
	}

	/** Log text with plug-in prefix. */
	public void log(final String text) {
		listener.getLogger()
				.println(PluginConstants.LOGGER_PREFIX + " " + text);
	}

}
