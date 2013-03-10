/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.util.logging.Logger;

/**
 * Manage global project life cycle events.
 * 
 * TODO Change identity after copy.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class PluginProjectListener extends ItemListener {

	private final static Logger log = Logger
			.getLogger(PluginProjectListener.class.getName());

	@Override
	public void onCopied(final Item source, final Item target) {
	}

}
