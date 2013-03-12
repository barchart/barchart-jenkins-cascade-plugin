/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;

/**
 * @author Andrei Pozolotin
 */
public class CascadeReport extends AbstractAction implements
		HealthReportingAction {

	public HealthReport getBuildHealth() {
		return new HealthReport(50, "HELLO");
	}

}
