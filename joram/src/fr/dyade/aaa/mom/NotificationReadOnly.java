/*
 * Copyright (C) 1996 - 2000 BULL
 * Copyright (C) 1996 - 2000 INRIA
 *
 * The contents of this file are subject to the Joram Public License,
 * as defined by the file JORAM_LICENSE.TXT 
 * 
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License on the Objectweb web site
 * (www.objectweb.org). 
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific terms governing rights and limitations under the License. 
 * 
 * The Original Code is Joram, including the java packages fr.dyade.aaa.agent,
 * fr.dyade.aaa.util, fr.dyade.aaa.ip, fr.dyade.aaa.mom, and fr.dyade.aaa.joram,
 * released May 24, 2000. 
 * 
 * The Initial Developer of the Original Code is Dyade. The Original Code and
 * portions created by Dyade are Copyright Bull and Copyright INRIA.
 * All Rights Reserved.
 */


package fr.dyade.aaa.mom; 
 
import java.lang.*; 
import fr.dyade.aaa.agent.*; 
 
/** 
 *	NotificationReadOnly allows a agentClient to ask to a Queue 
 *	the list of messages which it contains   
 * 
 *	@see	fr.dyade.aaa.mom.Topic 
 *	@see	fr.dyade.aaa.mom.Queue 
 *	@see	fr.dyade.aaa.mom.AgentClient 
 */ 
 
public class NotificationReadOnly extends fr.dyade.aaa.mom.NotificationMOMRequest { 
	 
	/** the selector of the request */ 
	String selector;  
	 
	public NotificationReadOnly(long messageID, String selectorNew) {
		super(messageID);
		selector = selectorNew;
	} 
 
}
