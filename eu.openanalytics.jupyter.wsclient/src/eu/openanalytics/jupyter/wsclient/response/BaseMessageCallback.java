/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.wsclient.response;

import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.Reply;

public class BaseMessageCallback implements IMessageCallback {

	@Override
	public void onPubResult(Broadcast broadcast) {
		// Default: do nothing
	}

	@Override
	public void onShellReply(Reply reply) {
		// Default: do nothing
	}

	@Override
	public void onChannelError(Throwable cause) {
		// Default: do nothing
	}
}
