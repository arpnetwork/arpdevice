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

package org.arpnetwork.arpdevice.ui.upnp;

import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;

public class ClingOperationFailedException extends ClingRouterException {

    private static final long serialVersionUID = 1L;
    private final IncomingActionResponseMessage response;

    public ClingOperationFailedException(final String message, final IncomingActionResponseMessage response) {
        super(message);
        assert response.getOperation().isFailed();
        this.response = response;
    }

    public IncomingActionResponseMessage getResponse() {
        return response;
    }
}