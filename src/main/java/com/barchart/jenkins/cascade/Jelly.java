/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker annotation for jelly servlet interaction, such as property fields,
 * form submit methods, static content rendering requests, etc.
 * <p>
 * Please check with the corresponding jelly file when making a change to this
 * class member.
 * 
 * @author Andrei Pozolotin
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Jelly {

}
