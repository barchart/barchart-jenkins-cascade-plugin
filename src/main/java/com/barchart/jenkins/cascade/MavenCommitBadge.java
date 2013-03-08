/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Show icon for a SCM commit build.
 * 
 * @author Andrei Pozolotin
 */
public class MavenCommitBadge extends AbstractBadge {

	public MavenCommitBadge() {
		super("Commit Changes", "game-diamond.png");
	}

}
