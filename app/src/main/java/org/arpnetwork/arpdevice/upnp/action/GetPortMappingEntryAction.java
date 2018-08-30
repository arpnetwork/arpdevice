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

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.PortMapping;

import java.util.Collections;
import java.util.Map;

public class GetPortMappingEntryAction extends AbstractClingAction<PortMapping> {

    private final int index;

    public GetPortMappingEntryAction(final Service<RemoteDevice, RemoteService> service, final int index) {
        super(service, "GetGenericPortMappingEntry");
        this.index = index;
    }

    @Override
    public Map<String, Object> getArgumentValues() {
        return Collections.<String, Object>singletonMap("NewPortMappingIndex", new UnsignedIntegerTwoBytes(index));
    }

    @Override
    public PortMapping convert(final ActionInvocation<RemoteService> response) {
        PortMapping.Protocol protocol = PortMapping.Protocol.TCP;
        if (getStringValue(response, "NewProtocol").equalsIgnoreCase("udp")) {
            protocol = PortMapping.Protocol.UDP;
        }
        final String remoteHost = getStringValue(response, "NewRemoteHost");
        final int externalPort = getIntValue(response, "NewExternalPort");
        final String internalClient = getStringValue(response, "NewInternalClient");
        final int internalPort = getIntValue(response, "NewInternalPort");
        final String description = getStringValue(response, "NewPortMappingDescription");
        final boolean enabled = getBooleanValue(response, "NewEnabled");
        final long leaseDuration = getLongValue(response, "NewLeaseDuration");

        return new PortMapping(externalPort, internalClient, protocol, description);
    }

    private boolean getBooleanValue(final ActionInvocation<RemoteService> response, final String argumentName) {
        return (boolean) response.getOutput(argumentName).getValue();
    }

    protected int getIntValue(final ActionInvocation<?> response, final String argumentName) {
        return ((UnsignedIntegerTwoBytes) response.getOutput(argumentName).getValue()).getValue().intValue();
    }

    protected long getLongValue(final ActionInvocation<?> response, final String argumentName) {
        return ((UnsignedIntegerFourBytes) response.getOutput(argumentName).getValue()).getValue().longValue();
    }

    protected String getStringValue(final ActionInvocation<?> response, final String argumentName) {
        return (String) response.getOutput(argumentName).getValue();
    }
}
