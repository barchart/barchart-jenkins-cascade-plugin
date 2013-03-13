/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.queue.CauseOfBlockage;

/**
 * Description why a build is in a blocked state.
 * 
 * @author Andrei Pozolotin
 */
public class RunBlockCause extends CauseOfBlockage {

	private final String descrpition;

	public RunBlockCause(final String descrpition) {
		this.descrpition = PluginConstants.LOGGER_PREFIX + " " + descrpition;
	}

	@Override
	public String getShortDescription() {
		return descrpition;
	}

}
