package org.arpnetwork.arpdevice.contracts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple6;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.5.0.
 */
public class ARPRegistry extends Contract {
    private static final String BINARY = null;

    public static final String FUNC_SERVERS = "servers";

    public static final String FUNC_ARPTOKEN = "arpToken";

    public static final String FUNC_DEVICE_UNBOUND_DELAY = "DEVICE_UNBOUND_DELAY";

    public static final String FUNC_EXPIRED_DELAY = "EXPIRED_DELAY";

    public static final String FUNC_SERVER_HOLDING = "SERVER_HOLDING";

    public static final String FUNC_DEVICE_HOLDING = "DEVICE_HOLDING";

    public static final String FUNC_CAPACITY_MIN = "CAPACITY_MIN";

    public static final String FUNC_DEVICES = "devices";

    public static final String FUNC_HOLDING_PER_DEVICE = "HOLDING_PER_DEVICE";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_UNREGISTER = "unregister";

    public static final String FUNC_UPDATE = "update";

    public static final String FUNC_BINDDEVICE = "bindDevice";

    public static final String FUNC_UNBINDDEVICE = "unbindDevice";

    public static final String FUNC_UNBINDDEVICEBYSERVER = "unbindDeviceByServer";

    public static final String FUNC_SERVERBYINDEX = "serverByIndex";

    public static final String FUNC_SERVERCOUNT = "serverCount";

    public static final Event REGISTERED_EVENT = new Event("Registered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event UNREGISTERED_EVENT = new Event("Unregistered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event UPDATED_EVENT = new Event("Updated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event DEVICEBOUND_EVENT = new Event("DeviceBound",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event DEVICEUNBOUND_EVENT = new Event("DeviceUnbound",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event DEVICEEXPIRED_EVENT = new Event("DeviceExpired",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    protected ARPRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ARPRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<Tuple6<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> servers(String param0) {
        final Function function = new Function(FUNC_SERVERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint16>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple6<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>(
                new Callable<Tuple6<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple6<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue());
                    }
                });
    }

    public RemoteCall<String> arpToken() {
        final Function function = new Function(FUNC_ARPTOKEN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<BigInteger> DEVICE_UNBOUND_DELAY() {
        final Function function = new Function(FUNC_DEVICE_UNBOUND_DELAY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> EXPIRED_DELAY() {
        final Function function = new Function(FUNC_EXPIRED_DELAY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> SERVER_HOLDING() {
        final Function function = new Function(FUNC_SERVER_HOLDING, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> DEVICE_HOLDING() {
        final Function function = new Function(FUNC_DEVICE_HOLDING, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> CAPACITY_MIN() {
        final Function function = new Function(FUNC_CAPACITY_MIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<Tuple3<String, BigInteger, BigInteger>> devices(String param0) {
        final Function function = new Function(FUNC_DEVICES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple3<String, BigInteger, BigInteger>>(
                new Callable<Tuple3<String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<String, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteCall<BigInteger> HOLDING_PER_DEVICE() {
        final Function function = new Function(FUNC_HOLDING_PER_DEVICE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public static RemoteCall<ARPRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _arpToken) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_arpToken)));
        return deployRemoteCall(ARPRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<ARPRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _arpToken) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_arpToken)));
        return deployRemoteCall(ARPRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<RegisteredEventResponse> getRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REGISTERED_EVENT, transactionReceipt);
        ArrayList<RegisteredEventResponse> responses = new ArrayList<RegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RegisteredEventResponse typedResponse = new RegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RegisteredEventResponse> registeredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, RegisteredEventResponse>() {
            @Override
            public RegisteredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REGISTERED_EVENT, log);
                RegisteredEventResponse typedResponse = new RegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<RegisteredEventResponse> registeredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REGISTERED_EVENT));
        return registeredEventObservable(filter);
    }

    public List<UnregisteredEventResponse> getUnregisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UNREGISTERED_EVENT, transactionReceipt);
        ArrayList<UnregisteredEventResponse> responses = new ArrayList<UnregisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UnregisteredEventResponse typedResponse = new UnregisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<UnregisteredEventResponse> unregisteredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, UnregisteredEventResponse>() {
            @Override
            public UnregisteredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UNREGISTERED_EVENT, log);
                UnregisteredEventResponse typedResponse = new UnregisteredEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<UnregisteredEventResponse> unregisteredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UNREGISTERED_EVENT));
        return unregisteredEventObservable(filter);
    }

    public List<UpdatedEventResponse> getUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPDATED_EVENT, transactionReceipt);
        ArrayList<UpdatedEventResponse> responses = new ArrayList<UpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpdatedEventResponse typedResponse = new UpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<UpdatedEventResponse> updatedEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, UpdatedEventResponse>() {
            @Override
            public UpdatedEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPDATED_EVENT, log);
                UpdatedEventResponse typedResponse = new UpdatedEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<UpdatedEventResponse> updatedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPDATED_EVENT));
        return updatedEventObservable(filter);
    }

    public List<DeviceBoundEventResponse> getDeviceBoundEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEVICEBOUND_EVENT, transactionReceipt);
        ArrayList<DeviceBoundEventResponse> responses = new ArrayList<DeviceBoundEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DeviceBoundEventResponse typedResponse = new DeviceBoundEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DeviceBoundEventResponse> deviceBoundEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, DeviceBoundEventResponse>() {
            @Override
            public DeviceBoundEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEVICEBOUND_EVENT, log);
                DeviceBoundEventResponse typedResponse = new DeviceBoundEventResponse();
                typedResponse.log = log;
                typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<DeviceBoundEventResponse> deviceBoundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEVICEBOUND_EVENT));
        return deviceBoundEventObservable(filter);
    }

    public List<DeviceUnboundEventResponse> getDeviceUnboundEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEVICEUNBOUND_EVENT, transactionReceipt);
        ArrayList<DeviceUnboundEventResponse> responses = new ArrayList<DeviceUnboundEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DeviceUnboundEventResponse typedResponse = new DeviceUnboundEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DeviceUnboundEventResponse> deviceUnboundEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, DeviceUnboundEventResponse>() {
            @Override
            public DeviceUnboundEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEVICEUNBOUND_EVENT, log);
                DeviceUnboundEventResponse typedResponse = new DeviceUnboundEventResponse();
                typedResponse.log = log;
                typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<DeviceUnboundEventResponse> deviceUnboundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEVICEUNBOUND_EVENT));
        return deviceUnboundEventObservable(filter);
    }

    public List<DeviceExpiredEventResponse> getDeviceExpiredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEVICEEXPIRED_EVENT, transactionReceipt);
        ArrayList<DeviceExpiredEventResponse> responses = new ArrayList<DeviceExpiredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DeviceExpiredEventResponse typedResponse = new DeviceExpiredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DeviceExpiredEventResponse> deviceExpiredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, DeviceExpiredEventResponse>() {
            @Override
            public DeviceExpiredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEVICEEXPIRED_EVENT, log);
                DeviceExpiredEventResponse typedResponse = new DeviceExpiredEventResponse();
                typedResponse.log = log;
                typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<DeviceExpiredEventResponse> deviceExpiredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEVICEEXPIRED_EVENT));
        return deviceExpiredEventObservable(filter);
    }

    public RemoteCall<TransactionReceipt> register(BigInteger _ip, BigInteger _port, BigInteger _capacity, BigInteger _amount) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_ip), 
                new org.web3j.abi.datatypes.generated.Uint16(_port), 
                new org.web3j.abi.datatypes.generated.Uint256(_capacity), 
                new org.web3j.abi.datatypes.generated.Uint256(_amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unregister() {
        final Function function = new Function(
                FUNC_UNREGISTER, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> update(BigInteger _ip, BigInteger _port, BigInteger _capacity, BigInteger _amount) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_ip), 
                new org.web3j.abi.datatypes.generated.Uint16(_port), 
                new org.web3j.abi.datatypes.generated.Uint256(_capacity), 
                new org.web3j.abi.datatypes.generated.Uint256(_amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> bindDevice(String _server) {
        final Function function = new Function(
                FUNC_BINDDEVICE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_server)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unbindDevice() {
        final Function function = new Function(
                FUNC_UNBINDDEVICE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unbindDeviceByServer(String _device) {
        final Function function = new Function(
                FUNC_UNBINDDEVICEBYSERVER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_device)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> serverByIndex(BigInteger _index) {
        final Function function = new Function(FUNC_SERVERBYINDEX, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint16>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>(
                new Callable<Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue(), 
                                (BigInteger) results.get(6).getValue());
                    }
                });
    }

    public RemoteCall<BigInteger> serverCount() {
        final Function function = new Function(FUNC_SERVERCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public static ARPRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static ARPRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class RegisteredEventResponse {
        public Log log;

        public String server;
    }

    public static class UnregisteredEventResponse {
        public Log log;

        public String server;
    }

    public static class UpdatedEventResponse {
        public Log log;

        public String server;
    }

    public static class DeviceBoundEventResponse {
        public Log log;

        public String device;

        public String server;
    }

    public static class DeviceUnboundEventResponse {
        public Log log;

        public String device;

        public String server;
    }

    public static class DeviceExpiredEventResponse {
        public Log log;

        public String device;

        public String server;
    }
}
