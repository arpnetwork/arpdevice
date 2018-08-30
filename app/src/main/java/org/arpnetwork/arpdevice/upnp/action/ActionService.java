/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpdevice.upnp.action;

import org.arpnetwork.arpdevice.upnp.ClingOperationFailedException;
import org.arpnetwork.arpdevice.upnp.ClingRouterException;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.protocol.sync.SendingAction;

import java.net.URL;

public class ActionService {
    private final RemoteService remoteService;
    private final ControlPoint controlPoint;

    public ActionService(final RemoteService remoteService, final ControlPoint controlPoint) {
        this.remoteService = remoteService;
        this.controlPoint = controlPoint;
    }

    public <T> T run(final ClingAction<T> action) {
        // Figure out the remote URL where we'd like to send the action request to
        final URL controLURL = remoteService.getDevice().normalizeURI(remoteService.getControlURI());

        final ActionInvocation<RemoteService> actionInvocation = action.getActionInvocation();
        final SendingAction prot = controlPoint.getProtocolFactory().createSendingAction(actionInvocation, controLURL);
        prot.run();

        final IncomingActionResponseMessage response = prot.getOutputMessage();
        if (response == null) {
            throw new ClingRouterException("Got null response for action " + actionInvocation);
        } else if (response.getOperation().isFailed()) {
            throw new ClingOperationFailedException("Invocation " + actionInvocation + " failed with operation '"
                    + response.getOperation() + "', body '" + response.getBodyString() + "'", response);
        }
        return action.convert(actionInvocation);
    }

    public RemoteService getService() {
        return remoteService;
    }
}
