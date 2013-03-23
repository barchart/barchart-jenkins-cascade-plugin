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
		Describable<PluginActivator>, PluginConstants {

	protected final static Logger log = Logger.getLogger(PluginActivator.class
			.getName());

	public static final class TheDescriptor extends Descriptor<PluginActivator> {
		@Override
		public String getDisplayName() {
			return PLUGIN_NAME;
		}
	}

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	public static PluginActivator get() {
		return Hudson.getInstance().getPlugin(PluginActivator.class);
	}

	public TheDescriptor getDescriptor() {
		return META;
	}

	@Override
	public void start() throws Exception {
		log.info("### Start");
		super.start();
		load();
		MemberViewAction.init();
		GraphProjectAction.init();
	}

	@Override
	public void stop() throws Exception {
		save();
		super.stop();
		log.info("### Stop");
	}

}
