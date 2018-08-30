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

import org.arpnetwork.arpdevice.upnp.ClingRouterException;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.ActionArgument;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class AbstractClingAction<T> implements ClingAction<T> {

    private final Service<RemoteDevice, RemoteService> service;
    private final String actionName;

    public AbstractClingAction(final Service<RemoteDevice, RemoteService> service, final String actionName) {
        this.service = service;
        this.actionName = actionName;
    }

    public Map<String, Object> getArgumentValues() {
        return Collections.emptyMap();
    }

    @Override
    public ActionInvocation<RemoteService> getActionInvocation() {
        final Action<RemoteService> action = service.getAction(actionName);
        if (action == null) {
            throw new ClingRouterException("No action found for name '" + actionName + "'. Available actions: "
                    + Arrays.toString(service.getActions()));
        }
        final ActionArgumentValue<RemoteService>[] argumentArray = getArguments(action);
        return new ActionInvocation<RemoteService>(action, argumentArray);
    }

    private ActionArgumentValue<RemoteService>[] getArguments(final Action<RemoteService> action) {
        @SuppressWarnings("unchecked")
        final ActionArgument<RemoteService>[] actionArguments = action.getArguments();
        final Map<String, Object> argumentValues = getArgumentValues();
        final List<ActionArgumentValue<RemoteService>> actionArgumentValues = new ArrayList<>(actionArguments.length);

        for (final ActionArgument<RemoteService> actionArgument : actionArguments) {
            if (actionArgument.getDirection() == ActionArgument.Direction.IN) {
                final Object value = argumentValues.get(actionArgument.getName());
                actionArgumentValues.add(new ActionArgumentValue<>(actionArgument, value));
            }
        }
        @SuppressWarnings("unchecked")
        final ActionArgumentValue<RemoteService>[] array = actionArgumentValues.toArray(new ActionArgumentValue[0]);
        return array;
    }
}