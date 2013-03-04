/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

/**
 */
@Extension
public class Rebuilder extends RunListener<Run> {

	/**
	 */
	public Rebuilder() {
		super(Run.class);
	}

	@Override
	public void onCompleted(final Run r, final TaskListener listener) {
	}

}
