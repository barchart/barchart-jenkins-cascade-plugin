/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.BuildBadgeAction;
import hudson.model.InvisibleAction;

/**
 * Member project badge.
 * <p>
 * Attach action to build to display a release build icon in build history.
 * 
 * @author Andrei Pozolotin
 */
public class MemberBadgeAction extends InvisibleAction implements
		BuildBadgeAction {

}
