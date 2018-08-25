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
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;
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
    public static final String CONTRACT_ADDRESS = "0x9f6b469dd5ec3e86f19cac817a2bc802ae54520d";
    private static final String BINARY = null;

    public static final String FUNC_SERVERS = "servers";

    public static final String FUNC_BINDINGS = "bindings";

    public static final String FUNC_PERMANENT = "PERMANENT";

    public static final String FUNC_EXPIRED_DELAY = "EXPIRED_DELAY";

    public static final String FUNC_SERVER_HOLDING = "SERVER_HOLDING";

    public static final String FUNC_ARPBANK = "arpBank";

    public static final String FUNC_DEVICE_HOLDING = "DEVICE_HOLDING";

    public static final String FUNC_REGISTERSERVER = "registerServer";

    public static final String FUNC_UPDATESERVER = "updateServer";

    public static final String FUNC_UNREGISTERSERVER = "unregisterServer";

    public static final String FUNC_BINDDEVICE = "bindDevice";

    public static final String FUNC_UNBINDDEVICE = "unbindDevice";

    public static final String FUNC_UNBINDDEVICEBYSERVER = "unbindDeviceByServer";

    public static final String FUNC_BINDAPP = "bindApp";

    public static final String FUNC_UNBINDAPP = "unbindApp";

    public static final String FUNC_UNBINDAPPBYSERVER = "unbindAppByServer";

    public static final String FUNC_SERVERBYINDEX = "serverByIndex";

    public static final String FUNC_SERVERCOUNT = "serverCount";

    public static final Event SERVERREGISTERED_EVENT = new Event("ServerRegistered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event SERVERUNREGISTERED_EVENT = new Event("ServerUnregistered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event SERVEREXPIRED_EVENT = new Event("ServerExpired",
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

    public static final Event DEVICEBOUNDEXPIRED_EVENT = new Event("DeviceBoundExpired",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event APPBOUND_EVENT = new Event("AppBound",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event APPUNBOUND_EVENT = new Event("AppUnbound",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event APPBOUNDEXPIRED_EVENT = new Event("AppBoundExpired",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    protected ARPRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ARPRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> servers(String param0) {
        final Function function = new Function(FUNC_SERVERS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Uint16>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteCall<Tuple2<String, BigInteger>> bindings(byte[] param0) {
        final Function function = new Function(FUNC_BINDINGS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple2<String, BigInteger>>(
                new Callable<Tuple2<String, BigInteger>>() {
                    @Override
                    public Tuple2<String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, BigInteger>(
                                (String) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteCall<BigInteger> PERMANENT() {
        final Function function = new Function(FUNC_PERMANENT,
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

    public RemoteCall<String> arpBank() {
        final Function function = new Function(FUNC_ARPBANK,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<BigInteger> DEVICE_HOLDING() {
        final Function function = new Function(FUNC_DEVICE_HOLDING,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public static RemoteCall<ARPRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _arpBank) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_arpBank)));
        return deployRemoteCall(ARPRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<ARPRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _arpBank) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_arpBank)));
        return deployRemoteCall(ARPRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<ServerRegisteredEventResponse> getServerRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVERREGISTERED_EVENT, transactionReceipt);
        ArrayList<ServerRegisteredEventResponse> responses = new ArrayList<ServerRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServerRegisteredEventResponse typedResponse = new ServerRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ServerRegisteredEventResponse> serverRegisteredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ServerRegisteredEventResponse>() {
            @Override
            public ServerRegisteredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVERREGISTERED_EVENT, log);
                ServerRegisteredEventResponse typedResponse = new ServerRegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ServerRegisteredEventResponse> serverRegisteredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVERREGISTERED_EVENT));
        return serverRegisteredEventObservable(filter);
    }

    public List<ServerUnregisteredEventResponse> getServerUnregisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVERUNREGISTERED_EVENT, transactionReceipt);
        ArrayList<ServerUnregisteredEventResponse> responses = new ArrayList<ServerUnregisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServerUnregisteredEventResponse typedResponse = new ServerUnregisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ServerUnregisteredEventResponse> serverUnregisteredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ServerUnregisteredEventResponse>() {
            @Override
            public ServerUnregisteredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVERUNREGISTERED_EVENT, log);
                ServerUnregisteredEventResponse typedResponse = new ServerUnregisteredEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ServerUnregisteredEventResponse> serverUnregisteredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVERUNREGISTERED_EVENT));
        return serverUnregisteredEventObservable(filter);
    }

    public List<ServerExpiredEventResponse> getServerExpiredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVEREXPIRED_EVENT, transactionReceipt);
        ArrayList<ServerExpiredEventResponse> responses = new ArrayList<ServerExpiredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServerExpiredEventResponse typedResponse = new ServerExpiredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ServerExpiredEventResponse> serverExpiredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ServerExpiredEventResponse>() {
            @Override
            public ServerExpiredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVEREXPIRED_EVENT, log);
                ServerExpiredEventResponse typedResponse = new ServerExpiredEventResponse();
                typedResponse.log = log;
                typedResponse.server = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ServerExpiredEventResponse> serverExpiredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVEREXPIRED_EVENT));
        return serverExpiredEventObservable(filter);
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

    public List<DeviceBoundExpiredEventResponse> getDeviceBoundExpiredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEVICEBOUNDEXPIRED_EVENT, transactionReceipt);
        ArrayList<DeviceBoundExpiredEventResponse> responses = new ArrayList<DeviceBoundExpiredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DeviceBoundExpiredEventResponse typedResponse = new DeviceBoundExpiredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DeviceBoundExpiredEventResponse> deviceBoundExpiredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, DeviceBoundExpiredEventResponse>() {
            @Override
            public DeviceBoundExpiredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEVICEBOUNDEXPIRED_EVENT, log);
                DeviceBoundExpiredEventResponse typedResponse = new DeviceBoundExpiredEventResponse();
                typedResponse.log = log;
                typedResponse.device = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<DeviceBoundExpiredEventResponse> deviceBoundExpiredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEVICEBOUNDEXPIRED_EVENT));
        return deviceBoundExpiredEventObservable(filter);
    }

    public List<AppBoundEventResponse> getAppBoundEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPBOUND_EVENT, transactionReceipt);
        ArrayList<AppBoundEventResponse> responses = new ArrayList<AppBoundEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppBoundEventResponse typedResponse = new AppBoundEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AppBoundEventResponse> appBoundEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AppBoundEventResponse>() {
            @Override
            public AppBoundEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPBOUND_EVENT, log);
                AppBoundEventResponse typedResponse = new AppBoundEventResponse();
                typedResponse.log = log;
                typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AppBoundEventResponse> appBoundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPBOUND_EVENT));
        return appBoundEventObservable(filter);
    }

    public List<AppUnboundEventResponse> getAppUnboundEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPUNBOUND_EVENT, transactionReceipt);
        ArrayList<AppUnboundEventResponse> responses = new ArrayList<AppUnboundEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppUnboundEventResponse typedResponse = new AppUnboundEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AppUnboundEventResponse> appUnboundEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AppUnboundEventResponse>() {
            @Override
            public AppUnboundEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPUNBOUND_EVENT, log);
                AppUnboundEventResponse typedResponse = new AppUnboundEventResponse();
                typedResponse.log = log;
                typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AppUnboundEventResponse> appUnboundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPUNBOUND_EVENT));
        return appUnboundEventObservable(filter);
    }

    public List<AppBoundExpiredEventResponse> getAppBoundExpiredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPBOUNDEXPIRED_EVENT, transactionReceipt);
        ArrayList<AppBoundExpiredEventResponse> responses = new ArrayList<AppBoundExpiredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppBoundExpiredEventResponse typedResponse = new AppBoundExpiredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AppBoundExpiredEventResponse> appBoundExpiredEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AppBoundExpiredEventResponse>() {
            @Override
            public AppBoundExpiredEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPBOUNDEXPIRED_EVENT, log);
                AppBoundExpiredEventResponse typedResponse = new AppBoundExpiredEventResponse();
                typedResponse.log = log;
                typedResponse.app = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.server = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AppBoundExpiredEventResponse> appBoundExpiredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPBOUNDEXPIRED_EVENT));
        return appBoundExpiredEventObservable(filter);
    }

    public RemoteCall<TransactionReceipt> registerServer(BigInteger _ip, BigInteger _port) {
        final Function function = new Function(
                FUNC_REGISTERSERVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_ip),
                        new org.web3j.abi.datatypes.generated.Uint16(_port)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> updateServer(BigInteger _ip, BigInteger _port) {
        final Function function = new Function(
                FUNC_UPDATESERVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_ip),
                        new org.web3j.abi.datatypes.generated.Uint16(_port)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unregisterServer() {
        final Function function = new Function(
                FUNC_UNREGISTERSERVER,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> bindDevice(String _server, BigInteger _amount, BigInteger _expired, BigInteger _signExpired, BigInteger _v, byte[] _r, byte[] _s) {
        final Function function = new Function(
                FUNC_BINDDEVICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_server),
                        new org.web3j.abi.datatypes.generated.Uint256(_amount),
                        new org.web3j.abi.datatypes.generated.Uint256(_expired),
                        new org.web3j.abi.datatypes.generated.Uint256(_signExpired),
                        new org.web3j.abi.datatypes.generated.Uint8(_v),
                        new org.web3j.abi.datatypes.generated.Bytes32(_r),
                        new org.web3j.abi.datatypes.generated.Bytes32(_s)),
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

    public RemoteCall<TransactionReceipt> bindApp(String _server) {
        final Function function = new Function(
                FUNC_BINDAPP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_server)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unbindApp(String _server) {
        final Function function = new Function(
                FUNC_UNBINDAPP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_server)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unbindAppByServer(String _app) {
        final Function function = new Function(
                FUNC_UNBINDAPPBYSERVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_app)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger>> serverByIndex(BigInteger _index) {
        final Function function = new Function(FUNC_SERVERBYINDEX,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint32>() {}, new TypeReference<Uint16>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger>>(
                new Callable<Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue(),
                                (BigInteger) results.get(4).getValue());
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

    public static class ServerRegisteredEventResponse {
        public Log log;

        public String server;
    }

    public static class ServerUnregisteredEventResponse {
        public Log log;

        public String server;
    }

    public static class ServerExpiredEventResponse {
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

    public static class DeviceBoundExpiredEventResponse {
        public Log log;

        public String device;

        public String server;
    }

    public static class AppBoundEventResponse {
        public Log log;

        public String app;

        public String server;
    }

    public static class AppUnboundEventResponse {
        public Log log;

        public String app;

        public String server;
    }

    public static class AppBoundExpiredEventResponse {
        public Log log;

        public String app;

        public String server;
    }
}
