/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.BuildListener;

import java.io.Serializable;

/**
 * Convenience build logger.
 * 
 * @author Andrei Pozolotin
 */
public class BuildLogger implements Serializable, PluginConstants {

	private static final long serialVersionUID = 1L;

	private final BuildListener listener;

	public BuildLogger(final BuildListener listener) {
		this.listener = listener;
	}

	/**
	 * Context listener.
	 */
	public BuildListener listener() {
		return listener;
	}

	/** Log text with plug-in prefix. */
	public void log(final String text) {
		listener().getLogger().println(LOGGER_PREFIX + " " + text);
	}

	/** Log error with plug-in prefix. */
	public void logErr(final String text) {
		listener().error(LOGGER_PREFIX + " " + text);
	}

	public void logExc(final Throwable e) {
		e.printStackTrace(listener().getLogger());
	}

	/** Log text with plug-in prefix and a tab. */
	public void logTab(final String text) {
		log("\t" + text);
	}

}
