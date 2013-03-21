/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.log;

import hudson.model.TaskListener;

import com.barchart.jenkins.cascade.PluginConstants;

/**
 * Convenience build logger.
 * 
 * @author Andrei Pozolotin
 */
public class PluginLogger implements PluginConstants {

	private final TaskListener listener;

	public PluginLogger(final TaskListener listener) {
		this.listener = listener;
	}

	/** Log text with plug-in prefix. */
	public void text(final String text) {
		listener.getLogger().println(LOGGER_PREFIX + " " + text);
	}

}
