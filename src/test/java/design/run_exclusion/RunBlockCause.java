/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.run_exclusion;

import hudson.model.queue.CauseOfBlockage;

import com.barchart.jenkins.cascade.PluginConstants;

/**
 * Description why a build is in a blocked state.
 * 
 * @author Andrei Pozolotin
 */
public class RunBlockCause extends CauseOfBlockage implements PluginConstants {

	private final String descrpition;

	public RunBlockCause(final String descrpition) {
		this.descrpition = LOGGER_PREFIX + " " + descrpition;
	}

	@Override
	public String getShortDescription() {
		return descrpition;
	}

	@Override
	public String toString() {
		return descrpition;
	}

}
