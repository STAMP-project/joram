/*
 * Copyright (C) 2016 ScalAgent Distributed Technologies
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA.
 *
 * Initial developer(s): ScalAgent Distributed Technologies
 * Contributor(s): 
 */
package org.ow2.joram.monitoring;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import fr.dyade.aaa.common.Debug;

public class Activator implements BundleActivator{	
	public static final Logger logmon = Debug.getLogger(Activator.class.getName());
	public static BundleContext context;

	public void start(BundleContext context) throws Exception {
		Activator.context = context;

		try{
			Monitoring.init();
		}catch(Exception exc){
			System.out.println("Monitoring initialization failed ");
			System.out.println(exc.toString());
			logmon.log(BasicLevel.ERROR, "Monitoring initialization failed", exc);
			throw exc;
		}
	}
	public void stop(BundleContext context) throws Exception {
		Monitoring.stop();
		Activator.context = null;
	}
}
