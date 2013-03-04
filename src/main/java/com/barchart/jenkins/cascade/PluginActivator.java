/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.util.logging.Logger;

/**
 * Plug-in life cycle manager.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class PluginActivator extends Plugin implements
		Describable<PluginActivator> {

	@Extension
	public static final class PluginDescriptor extends
			Descriptor<PluginActivator> {
		@Override
		public String getDisplayName() {
			return PluginConstants.PLUGIN_NAME;
		}
	}

	private final static Logger log = Logger.getLogger(PluginActivator.class
			.getName());

	public static PluginActivator get() {
		return Hudson.getInstance().getPlugin(PluginActivator.class);
	}

	public PluginDescriptor getDescriptor() {
		return (PluginDescriptor) Hudson.getInstance().getDescriptorOrDie(
				getClass());
	}

	@Override
	public void start() throws Exception {
		log.info("Starting: " + new PluginDescriptor().getDisplayName());
		super.start();
		load();
	}

	@Override
	public void stop() throws Exception {
		log.info("Stopping: " + new PluginDescriptor().getDisplayName());
		save();
		super.stop();
	}

}
