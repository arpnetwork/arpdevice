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

package org.arpnetwork.arpdevice.upnp;

import android.util.Log;

import org.arpnetwork.arpdevice.upnp.action.ActionService;
import org.arpnetwork.arpdevice.upnp.action.GetPortMappingEntryAction;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.transport.RouterException;

import java.util.Collection;
import java.util.LinkedList;

public class ClingPortMappingExtractor {
    private static final String TAG = "PortMappingExtractor";

    private final Collection<PortMapping> mappings;
    private boolean moreEntries;
    private int currentMappingNumber;

    /**
     * The maximum number of port mappings that we will try to retrieve from the router.
     */
    private final int maxNumPortMappings;
    private final ActionService actionService;

    public ClingPortMappingExtractor(final ActionService actionService, final int maxNumPortMappings) {
        this.actionService = actionService;
        this.maxNumPortMappings = maxNumPortMappings;
        this.mappings = new LinkedList<>();
        this.moreEntries = true;
        this.currentMappingNumber = 0;
    }

    public Collection<PortMapping> getPortMappings() throws RouterException {

        /*
         * This is a little trick to get all port mappings. There is a method that gets the number of available port
         * mappings (getNatMappingsCount()), but it seems, that this method just tries to get all port mappings and
         * checks, if an error is returned.
         *
         * In order to speed this up, we will do the same here, but stop, when the first exception is thrown.
         */

        while (morePortMappingsAvailable()) {

            try {
                final PortMapping portMapping = actionService
                        .run(new GetPortMappingEntryAction(actionService.getService(), currentMappingNumber));
                mappings.add(portMapping);
            } catch (final ClingOperationFailedException e) {
                handleFailureResponse(e.getResponse());
            } catch (final ClingRouterException ignored){
                // ignored,we loop to try again.
            }
            currentMappingNumber++;
        }

        checkMaxNumPortMappingsReached();

        return mappings;
    }

    /**
     * Check, if the max number of entries is reached and print a warning message.
     */
    private void checkMaxNumPortMappingsReached() {
        if (currentMappingNumber == maxNumPortMappings) {
            Log.w(TAG,
                    "Reached max number of port mappings to get ({}). Perhaps not all port mappings where retrieved." + maxNumPortMappings
            );
        }
    }

    private boolean morePortMappingsAvailable() {
        return moreEntries && currentMappingNumber < maxNumPortMappings;
    }

    private void handleFailureResponse(final IncomingActionResponseMessage incomingActionResponseMessage) {
        if (isNoMoreMappingsException(incomingActionResponseMessage)) {
            moreEntries = false;
            Log.d(TAG, "Got no port mapping");
        } else {
            moreEntries = false;
            Log.d(TAG, "Got error response: currentMappingNumber = " + currentMappingNumber + " incomingActionResponseMessage = " + incomingActionResponseMessage);
        }
    }

    /**
     * This method checks, if the error code of the given exception means, that no more mappings are available.
     * <p>
     * The following error codes are recognized:
     * <ul>
     * <li>SpecifiedArrayIndexInvalid: 713</li>
     * <li>NoSuchEntryInArray: 714</li>
     * <li>Invalid Args: 402 (e.g. for DD-WRT, TP-LINK TL-R460 firmware 4.7.6 Build 100714 Rel.63134n)</li>
     * <li>Other errors, e.g. "The reference to entity "T" must end with the ';' delimiter" or
     * "Content is not allowed in prolog": 899 (e.g. ActionTec MI424-WR, Thomson TWG850-4U)</li>
     * </ul>
     * See bug reports
     * <ul>
     * <li><a href= "https://sourceforge.net/tracker/index.php?func=detail&aid=1939749&group_id=213879&atid=1027466" >
     * https://sourceforge.net/tracker/index.php?func=detail&aid= 1939749&group_id=213879&atid=1027466</a></li>
     * <li><a href="http://www.sbbi.net/forum/viewtopic.php?p=394">http://www.sbbi .net/forum/viewtopic.php?p=394</a>
     * </li>
     * <li><a href= "http://sourceforge.net/tracker/?func=detail&atid=1027466&aid=3325388&group_id=213879" >http://
     * sourceforge.net/tracker/?func=detail&atid=1027466&aid=3325388& group_id=213879</a></li>
     * <a href= "https://sourceforge.net/tracker2/?func=detail&aid=2540478&group_id=213879&atid=1027466" >https://
     * sourceforge.net/tracker2/?func=detail&aid=2540478&group_id= 213879&atid=1027466</a></li>
     * </ul>
     *
     * @param incomingActionResponseMessage the exception to check
     * @return <code>true</code>, if the given exception means, that no more port mappings are available, else
     * <code>false</code>.
     */
    private boolean isNoMoreMappingsException(final IncomingActionResponseMessage incomingActionResponseMessage) {
        final int errorCode = incomingActionResponseMessage.getOperation().getStatusCode();
        switch (errorCode) {
            case 713:
            case 714:
            case 402:
            case 899:
                return true;

            default:
                return false;
        }
    }
}
