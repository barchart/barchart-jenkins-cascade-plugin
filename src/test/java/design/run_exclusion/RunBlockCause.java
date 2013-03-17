/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design.run_exclusion;

import com.barchart.jenkins.cascade.PluginConstants;

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

	@Override
	public String toString() {
		return descrpition;
	}

}
