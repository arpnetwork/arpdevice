package org.arpnetwork.arpdevice.ui;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.ui.my.MyFragment;
import org.arpnetwork.arpdevice.ui.upnp.ClingPortMappingExtractor;
import org.arpnetwork.arpdevice.ui.upnp.action.ActionService;
import org.arpnetwork.arpdevice.util.DeviceUtil;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.transport.RouterException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClingRegistryListener extends DefaultRegistryListener {
    private static final String TAG = "ClingRegistryListener";

    private static final String DATA_DESCRIPTOR = "ARP streaming";
    private static final String HTTP_DESCRIPTOR = "ARP http";
    /**
     * The maximum number of port mappings that we will try to retrieve from the router.
     */
    private final static int MAX_NUM_PORTMAPPINGS = 500;

    public static final DeviceType IGD_DEVICE_TYPE = new UDADeviceType("InternetGatewayDevice", 1);
    public static final DeviceType CONNECTION_DEVICE_TYPE = new UDADeviceType("WANConnectionDevice", 1);

    public static final ServiceType IP_SERVICE_TYPE = new UDAServiceType("WANIPConnection", 1);
    public static final ServiceType PPP_SERVICE_TYPE = new UDAServiceType("WANPPPConnection", 1);

    protected PortMapping[] portMappings;
    private ControlPoint mControlPoint;
    private Handler mHandler;

    // The key of the map is Service and equality is object identity, this is by-design
    protected Map<Service, List<PortMapping>> activePortMappings = new HashMap<>();

    public ClingRegistryListener(android.os.Handler handler, ControlPoint controlPoint) {
        mHandler = handler;
        mControlPoint = controlPoint;
    }


    public void setMapping(PortMapping[] portMappings) {
        this.portMappings = portMappings;
    }

    @Override
    synchronized public void deviceAdded(Registry registry, Device device) {
        Service connectionService;
        if ((connectionService = discoverConnectionService(device)) == null) return;

        ActionService actionService = new ActionService((RemoteService) connectionService, mControlPoint);
        try {
            Collection<PortMapping> portMappingList = new ClingPortMappingExtractor(actionService, MAX_NUM_PORTMAPPINGS).getPortMappings();

            int defaultDataPort = Config.DATA_SERVER_PORT;
            int defaultHttpPort = Config.HTTP_SERVER_PORT;
            long existDataPort = -1;
            long existHttpPort = -1;
            while (true) {
                boolean findData = false;
                boolean findHttp = false;
                for (PortMapping mapping : portMappingList) {
                    if (mapping.getInternalClient().equals(DeviceUtil.getIPAddress(true))
                            && mapping.getDescription().equalsIgnoreCase(DATA_DESCRIPTOR)) {
                        existDataPort = mapping.getExternalPort().getValue();
                    }
                    if (mapping.getInternalClient().equals(DeviceUtil.getIPAddress(true))
                            && mapping.getDescription().equalsIgnoreCase(HTTP_DESCRIPTOR)) {
                        existHttpPort = mapping.getExternalPort().getValue();
                    }
                    if (mapping.getProtocol() == PortMapping.Protocol.TCP
                            && mapping.getExternalPort().getValue() == defaultDataPort) {
                        findData = true;
                    }
                    if (mapping.getProtocol() == PortMapping.Protocol.TCP
                            && mapping.getExternalPort().getValue() == defaultHttpPort) {

                        findHttp = true;
                    }
                }
                if (existDataPort != -1 && existHttpPort != -1) {
                    break;
                } else if (findData || findHttp) {
                    defaultDataPort += 2;
                    defaultHttpPort += 2;
                    if (defaultDataPort > 65535 || defaultHttpPort > 65535) {
                        // exception
                        break;
                    }
                } else {
                    break;
                }
            }

            if (existDataPort == -1 || existHttpPort == -1) {
                PortMapping[] desiredMapping = new PortMapping[2];
                desiredMapping[0] = new PortMapping(defaultDataPort, DeviceUtil.getIPAddress(true),
                        PortMapping.Protocol.TCP, DATA_DESCRIPTOR);

                desiredMapping[1] = new PortMapping(defaultHttpPort, DeviceUtil.getIPAddress(true),
                        PortMapping.Protocol.TCP, HTTP_DESCRIPTOR);
                portMappings = desiredMapping;

                final List<PortMapping> activeForService = new ArrayList<>();
                for (final PortMapping pm : portMappings) {
                    new PortMappingAdd(connectionService, registry.getUpnpService().getControlPoint(), pm) {
                        @Override
                        public void success(ActionInvocation invocation) {
                            Log.d(TAG, "Port mapping added: " + pm);
                            activeForService.add(pm);

                            Message message = new Message();
                            message.what = MyFragment.MSG_PORT_SUCCESS;
                            mHandler.sendMessage(message);
                        }

                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            handleFailureMessage("Failed to add port mapping: " + pm);
                            handleFailureMessage("Reason: " + defaultMsg);

                            Message message = new Message();
                            message.what = MyFragment.MSG_PORT_FAILED;
                            mHandler.sendMessage(message);
                        }
                    }.run(); // Synchronous!
                }
                activePortMappings.put(connectionService, activeForService);
            } else {
                Message message = new Message();
                message.what = 1;
                mHandler.sendMessage(message);
            }
        } catch (RouterException e) {
            e.printStackTrace();
        }
    }

    @Override
    synchronized public void deviceRemoved(Registry registry, Device device) {
        for (Service service : device.findServices()) {
            Iterator<Map.Entry<Service, List<PortMapping>>> it = activePortMappings.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Service, List<PortMapping>> activeEntry = it.next();
                if (!activeEntry.getKey().equals(service)) continue;

                if (activeEntry.getValue().size() > 0)
                    handleFailureMessage("Device disappeared, couldn't delete port mappings: " + activeEntry.getValue().size());

                it.remove();
            }
        }
    }

    @Override
    synchronized public void beforeShutdown(Registry registry) {
        for (Map.Entry<Service, List<PortMapping>> activeEntry : activePortMappings.entrySet()) {

            final Iterator<PortMapping> it = activeEntry.getValue().iterator();
            while (it.hasNext()) {
                final PortMapping pm = it.next();
                Log.d(TAG, "Trying to delete port mapping on IGD: " + pm);
                new PortMappingDelete(activeEntry.getKey(), registry.getUpnpService().getControlPoint(), pm) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        Log.d(TAG, "Port mapping deleted: " + pm);
                        it.remove();
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        handleFailureMessage("Failed to delete port mapping: " + pm);
                        handleFailureMessage("Reason: " + defaultMsg);
                    }

                }.run(); // Synchronous!
            }
        }
    }

    protected Service discoverConnectionService(Device device) {
        if (!device.getType().equals(IGD_DEVICE_TYPE)) {
            return null;
        }

        Device[] connectionDevices = device.findDevices(CONNECTION_DEVICE_TYPE);
        if (connectionDevices.length == 0) {
            Log.d(TAG, "IGD doesn't support '" + CONNECTION_DEVICE_TYPE + "': " + device);
            return null;
        }

        Device connectionDevice = connectionDevices[0];
        Log.d(TAG, "Using first discovered WAN connection device: " + connectionDevice);

        Service ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE);
        Service pppConnectionService = connectionDevice.findService(PPP_SERVICE_TYPE);

        if (ipConnectionService == null && pppConnectionService == null) {
            Log.d(TAG, "IGD doesn't support IP or PPP WAN connection service: " + device);
        }

        return ipConnectionService != null ? ipConnectionService : pppConnectionService;
    }

    protected void handleFailureMessage(String s) {
        Log.w(TAG, s);
    }

}