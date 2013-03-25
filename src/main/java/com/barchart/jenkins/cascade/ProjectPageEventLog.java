/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Show cascade event log on a member project page.
 * 
 * @author Andrei Pozolotin
 */
public class ProjectPageEventLog extends AbstractAction {

	private final ProjectIdentity identity;

	public ProjectPageEventLog(final ProjectIdentity identity) {
		this.identity = identity;
	}

	@Jelly
	public ProjectIdentity identity() {
		return identity;
	}

}
