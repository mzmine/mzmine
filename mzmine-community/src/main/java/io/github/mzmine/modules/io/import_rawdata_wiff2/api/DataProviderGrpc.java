/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_wiff2.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Serves as an access point to extract information from SCIEX data files
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class DataProviderGrpc {

  private DataProviderGrpc() {}

  public static final String SERVICE_NAME = "Clearcore2.SampleData.DataAccessApi.V002.DataProvider";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ListSamplesRequest,
      Sample> getGetSamplesDescriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSamplesDescriptions",
      requestType = ListSamplesRequest.class,
      responseType = Sample.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<ListSamplesRequest,
      Sample> getGetSamplesDescriptionsMethod() {
    io.grpc.MethodDescriptor<ListSamplesRequest, Sample> getGetSamplesDescriptionsMethod;
    if ((getGetSamplesDescriptionsMethod = DataProviderGrpc.getGetSamplesDescriptionsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetSamplesDescriptionsMethod = DataProviderGrpc.getGetSamplesDescriptionsMethod) == null) {
          DataProviderGrpc.getGetSamplesDescriptionsMethod = getGetSamplesDescriptionsMethod =
              io.grpc.MethodDescriptor.<ListSamplesRequest, Sample>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSamplesDescriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ListSamplesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetSamplesDescriptions"))
              .build();
        }
      }
    }
    return getGetSamplesDescriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetExperimentsRequest,
      Experiment> getGetExperimentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetExperiments",
      requestType = GetExperimentsRequest.class,
      responseType = Experiment.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetExperimentsRequest,
      Experiment> getGetExperimentsMethod() {
    io.grpc.MethodDescriptor<GetExperimentsRequest, Experiment> getGetExperimentsMethod;
    if ((getGetExperimentsMethod = DataProviderGrpc.getGetExperimentsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetExperimentsMethod = DataProviderGrpc.getGetExperimentsMethod) == null) {
          DataProviderGrpc.getGetExperimentsMethod = getGetExperimentsMethod =
              io.grpc.MethodDescriptor.<GetExperimentsRequest, Experiment>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetExperiments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetExperimentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Experiment.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetExperiments"))
              .build();
        }
      }
    }
    return getGetExperimentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetSpectraRequest,
      Spectrum> getGetSpectraMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSpectra",
      requestType = GetSpectraRequest.class,
      responseType = Spectrum.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetSpectraRequest,
      Spectrum> getGetSpectraMethod() {
    io.grpc.MethodDescriptor<GetSpectraRequest, Spectrum> getGetSpectraMethod;
    if ((getGetSpectraMethod = DataProviderGrpc.getGetSpectraMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetSpectraMethod = DataProviderGrpc.getGetSpectraMethod) == null) {
          DataProviderGrpc.getGetSpectraMethod = getGetSpectraMethod =
              io.grpc.MethodDescriptor.<GetSpectraRequest, Spectrum>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSpectra"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetSpectraRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Spectrum.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetSpectra"))
              .build();
        }
      }
    }
    return getGetSpectraMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetWavelengthSpectraRequest,
      WavelengthSpectrum> getGetWavelengthSpectraMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetWavelengthSpectra",
      requestType = GetWavelengthSpectraRequest.class,
      responseType = WavelengthSpectrum.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetWavelengthSpectraRequest,
      WavelengthSpectrum> getGetWavelengthSpectraMethod() {
    io.grpc.MethodDescriptor<GetWavelengthSpectraRequest, WavelengthSpectrum> getGetWavelengthSpectraMethod;
    if ((getGetWavelengthSpectraMethod = DataProviderGrpc.getGetWavelengthSpectraMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetWavelengthSpectraMethod = DataProviderGrpc.getGetWavelengthSpectraMethod) == null) {
          DataProviderGrpc.getGetWavelengthSpectraMethod = getGetWavelengthSpectraMethod =
              io.grpc.MethodDescriptor.<GetWavelengthSpectraRequest, WavelengthSpectrum>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetWavelengthSpectra"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetWavelengthSpectraRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  WavelengthSpectrum.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetWavelengthSpectra"))
              .build();
        }
      }
    }
    return getGetWavelengthSpectraMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetAdcChannelDescriptionsRequest,
      AdcChannelsDescriptions> getGetAdcChannelDescriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAdcChannelDescriptions",
      requestType = GetAdcChannelDescriptionsRequest.class,
      responseType = AdcChannelsDescriptions.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<GetAdcChannelDescriptionsRequest,
      AdcChannelsDescriptions> getGetAdcChannelDescriptionsMethod() {
    io.grpc.MethodDescriptor<GetAdcChannelDescriptionsRequest, AdcChannelsDescriptions> getGetAdcChannelDescriptionsMethod;
    if ((getGetAdcChannelDescriptionsMethod = DataProviderGrpc.getGetAdcChannelDescriptionsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetAdcChannelDescriptionsMethod = DataProviderGrpc.getGetAdcChannelDescriptionsMethod) == null) {
          DataProviderGrpc.getGetAdcChannelDescriptionsMethod = getGetAdcChannelDescriptionsMethod =
              io.grpc.MethodDescriptor.<GetAdcChannelDescriptionsRequest, AdcChannelsDescriptions>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAdcChannelDescriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetAdcChannelDescriptionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  AdcChannelsDescriptions.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetAdcChannelDescriptions"))
              .build();
        }
      }
    }
    return getGetAdcChannelDescriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<SourceFile,
      Empty> getCloseFileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CloseFile",
      requestType = SourceFile.class,
      responseType = Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<SourceFile,
      Empty> getCloseFileMethod() {
    io.grpc.MethodDescriptor<SourceFile, Empty> getCloseFileMethod;
    if ((getCloseFileMethod = DataProviderGrpc.getCloseFileMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getCloseFileMethod = DataProviderGrpc.getCloseFileMethod) == null) {
          DataProviderGrpc.getCloseFileMethod = getCloseFileMethod =
              io.grpc.MethodDescriptor.<SourceFile, Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CloseFile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  SourceFile.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("CloseFile"))
              .build();
        }
      }
    }
    return getCloseFileMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetExperimentTicRequest,
      ExperimentTic> getGetExperimentTicMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetExperimentTic",
      requestType = GetExperimentTicRequest.class,
      responseType = ExperimentTic.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<GetExperimentTicRequest,
      ExperimentTic> getGetExperimentTicMethod() {
    io.grpc.MethodDescriptor<GetExperimentTicRequest, ExperimentTic> getGetExperimentTicMethod;
    if ((getGetExperimentTicMethod = DataProviderGrpc.getGetExperimentTicMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetExperimentTicMethod = DataProviderGrpc.getGetExperimentTicMethod) == null) {
          DataProviderGrpc.getGetExperimentTicMethod = getGetExperimentTicMethod =
              io.grpc.MethodDescriptor.<GetExperimentTicRequest, ExperimentTic>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetExperimentTic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetExperimentTicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ExperimentTic.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetExperimentTic"))
              .build();
        }
      }
    }
    return getGetExperimentTicMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetExperimentCyclesRequest,
      ExperimentCycles> getGetExperimentCyclesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetExperimentCycles",
      requestType = GetExperimentCyclesRequest.class,
      responseType = ExperimentCycles.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<GetExperimentCyclesRequest,
      ExperimentCycles> getGetExperimentCyclesMethod() {
    io.grpc.MethodDescriptor<GetExperimentCyclesRequest, ExperimentCycles> getGetExperimentCyclesMethod;
    if ((getGetExperimentCyclesMethod = DataProviderGrpc.getGetExperimentCyclesMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetExperimentCyclesMethod = DataProviderGrpc.getGetExperimentCyclesMethod) == null) {
          DataProviderGrpc.getGetExperimentCyclesMethod = getGetExperimentCyclesMethod =
              io.grpc.MethodDescriptor.<GetExperimentCyclesRequest, ExperimentCycles>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetExperimentCycles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetExperimentCyclesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ExperimentCycles.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetExperimentCycles"))
              .build();
        }
      }
    }
    return getGetExperimentCyclesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetExperimentScanRecordsRequest,
      ExperimentScanRecordsResponse> getGetExperimentScanRecordsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetExperimentScanRecords",
      requestType = GetExperimentScanRecordsRequest.class,
      responseType = ExperimentScanRecordsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetExperimentScanRecordsRequest,
      ExperimentScanRecordsResponse> getGetExperimentScanRecordsMethod() {
    io.grpc.MethodDescriptor<GetExperimentScanRecordsRequest, ExperimentScanRecordsResponse> getGetExperimentScanRecordsMethod;
    if ((getGetExperimentScanRecordsMethod = DataProviderGrpc.getGetExperimentScanRecordsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetExperimentScanRecordsMethod = DataProviderGrpc.getGetExperimentScanRecordsMethod) == null) {
          DataProviderGrpc.getGetExperimentScanRecordsMethod = getGetExperimentScanRecordsMethod =
              io.grpc.MethodDescriptor.<GetExperimentScanRecordsRequest, ExperimentScanRecordsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetExperimentScanRecords"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetExperimentScanRecordsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ExperimentScanRecordsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetExperimentScanRecords"))
              .build();
        }
      }
    }
    return getGetExperimentScanRecordsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetScanXicRequest,
      ScanXic> getGetScanXicsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetScanXics",
      requestType = GetScanXicRequest.class,
      responseType = ScanXic.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetScanXicRequest,
      ScanXic> getGetScanXicsMethod() {
    io.grpc.MethodDescriptor<GetScanXicRequest, ScanXic> getGetScanXicsMethod;
    if ((getGetScanXicsMethod = DataProviderGrpc.getGetScanXicsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetScanXicsMethod = DataProviderGrpc.getGetScanXicsMethod) == null) {
          DataProviderGrpc.getGetScanXicsMethod = getGetScanXicsMethod =
              io.grpc.MethodDescriptor.<GetScanXicRequest, ScanXic>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetScanXics"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetScanXicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ScanXic.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetScanXics"))
              .build();
        }
      }
    }
    return getGetScanXicsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetMrmXicRequest,
      MrmXic> getGetMrmXicsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMrmXics",
      requestType = GetMrmXicRequest.class,
      responseType = MrmXic.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetMrmXicRequest,
      MrmXic> getGetMrmXicsMethod() {
    io.grpc.MethodDescriptor<GetMrmXicRequest, MrmXic> getGetMrmXicsMethod;
    if ((getGetMrmXicsMethod = DataProviderGrpc.getGetMrmXicsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetMrmXicsMethod = DataProviderGrpc.getGetMrmXicsMethod) == null) {
          DataProviderGrpc.getGetMrmXicsMethod = getGetMrmXicsMethod =
              io.grpc.MethodDescriptor.<GetMrmXicRequest, MrmXic>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMrmXics"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetMrmXicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  MrmXic.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetMrmXics"))
              .build();
        }
      }
    }
    return getGetMrmXicsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetChannelTracesRequest,
      ChannelTrace> getGetChannelTracesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetChannelTraces",
      requestType = GetChannelTracesRequest.class,
      responseType = ChannelTrace.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetChannelTracesRequest,
      ChannelTrace> getGetChannelTracesMethod() {
    io.grpc.MethodDescriptor<GetChannelTracesRequest, ChannelTrace> getGetChannelTracesMethod;
    if ((getGetChannelTracesMethod = DataProviderGrpc.getGetChannelTracesMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetChannelTracesMethod = DataProviderGrpc.getGetChannelTracesMethod) == null) {
          DataProviderGrpc.getGetChannelTracesMethod = getGetChannelTracesMethod =
              io.grpc.MethodDescriptor.<GetChannelTracesRequest, ChannelTrace>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetChannelTraces"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetChannelTracesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ChannelTrace.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetChannelTraces"))
              .build();
        }
      }
    }
    return getGetChannelTracesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetMsMethodParametersRequest,
      MsMethod> getGetMsMethodParametersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMsMethodParameters",
      requestType = GetMsMethodParametersRequest.class,
      responseType = MsMethod.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetMsMethodParametersRequest,
      MsMethod> getGetMsMethodParametersMethod() {
    io.grpc.MethodDescriptor<GetMsMethodParametersRequest, MsMethod> getGetMsMethodParametersMethod;
    if ((getGetMsMethodParametersMethod = DataProviderGrpc.getGetMsMethodParametersMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetMsMethodParametersMethod = DataProviderGrpc.getGetMsMethodParametersMethod) == null) {
          DataProviderGrpc.getGetMsMethodParametersMethod = getGetMsMethodParametersMethod =
              io.grpc.MethodDescriptor.<GetMsMethodParametersRequest, MsMethod>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMsMethodParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetMsMethodParametersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  MsMethod.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetMsMethodParameters"))
              .build();
        }
      }
    }
    return getGetMsMethodParametersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetSampleInfoRequest,
      SampleInfoSection> getGetSampleInfoParametersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSampleInfoParameters",
      requestType = GetSampleInfoRequest.class,
      responseType = SampleInfoSection.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetSampleInfoRequest,
      SampleInfoSection> getGetSampleInfoParametersMethod() {
    io.grpc.MethodDescriptor<GetSampleInfoRequest, SampleInfoSection> getGetSampleInfoParametersMethod;
    if ((getGetSampleInfoParametersMethod = DataProviderGrpc.getGetSampleInfoParametersMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetSampleInfoParametersMethod = DataProviderGrpc.getGetSampleInfoParametersMethod) == null) {
          DataProviderGrpc.getGetSampleInfoParametersMethod = getGetSampleInfoParametersMethod =
              io.grpc.MethodDescriptor.<GetSampleInfoRequest, SampleInfoSection>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSampleInfoParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetSampleInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  SampleInfoSection.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetSampleInfoParameters"))
              .build();
        }
      }
    }
    return getGetSampleInfoParametersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetBpcRequest,
      Bpc> getGetBpcsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBpcs",
      requestType = GetBpcRequest.class,
      responseType = Bpc.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetBpcRequest,
      Bpc> getGetBpcsMethod() {
    io.grpc.MethodDescriptor<GetBpcRequest, Bpc> getGetBpcsMethod;
    if ((getGetBpcsMethod = DataProviderGrpc.getGetBpcsMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetBpcsMethod = DataProviderGrpc.getGetBpcsMethod) == null) {
          DataProviderGrpc.getGetBpcsMethod = getGetBpcsMethod =
              io.grpc.MethodDescriptor.<GetBpcRequest, Bpc>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBpcs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetBpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Bpc.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetBpcs"))
              .build();
        }
      }
    }
    return getGetBpcsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetLcMethodParametersRequest,
      SampleInfoSection> getGetLcMethodParametersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLcMethodParameters",
      requestType = GetLcMethodParametersRequest.class,
      responseType = SampleInfoSection.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<GetLcMethodParametersRequest,
      SampleInfoSection> getGetLcMethodParametersMethod() {
    io.grpc.MethodDescriptor<GetLcMethodParametersRequest, SampleInfoSection> getGetLcMethodParametersMethod;
    if ((getGetLcMethodParametersMethod = DataProviderGrpc.getGetLcMethodParametersMethod) == null) {
      synchronized (DataProviderGrpc.class) {
        if ((getGetLcMethodParametersMethod = DataProviderGrpc.getGetLcMethodParametersMethod) == null) {
          DataProviderGrpc.getGetLcMethodParametersMethod = getGetLcMethodParametersMethod =
              io.grpc.MethodDescriptor.<GetLcMethodParametersRequest, SampleInfoSection>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLcMethodParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetLcMethodParametersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  SampleInfoSection.getDefaultInstance()))
              .setSchemaDescriptor(new DataProviderMethodDescriptorSupplier("GetLcMethodParameters"))
              .build();
        }
      }
    }
    return getGetLcMethodParametersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DataProviderStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataProviderStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataProviderStub>() {
        @Override
        public DataProviderStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataProviderStub(channel, callOptions);
        }
      };
    return DataProviderStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static DataProviderBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataProviderBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataProviderBlockingV2Stub>() {
        @Override
        public DataProviderBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataProviderBlockingV2Stub(channel, callOptions);
        }
      };
    return DataProviderBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DataProviderBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataProviderBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataProviderBlockingStub>() {
        @Override
        public DataProviderBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataProviderBlockingStub(channel, callOptions);
        }
      };
    return DataProviderBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DataProviderFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataProviderFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataProviderFutureStub>() {
        @Override
        public DataProviderFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataProviderFutureStub(channel, callOptions);
        }
      };
    return DataProviderFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * retrieves the stream of samples signals of the specific sample
     * </pre>
     */
    default void getSamplesDescriptions(ListSamplesRequest request,
        io.grpc.stub.StreamObserver<Sample> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSamplesDescriptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of experiments descriptors of the specific sample
     * </pre>
     */
    default void getExperiments(GetExperimentsRequest request,
        io.grpc.stub.StreamObserver<Experiment> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetExperimentsMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of spectra of the specific sample
     * </pre>
     */
    default void getSpectra(GetSpectraRequest request,
        io.grpc.stub.StreamObserver<Spectrum> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSpectraMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of wavelength signals of the specific sample
     * </pre>
     */
    default void getWavelengthSpectra(GetWavelengthSpectraRequest request,
        io.grpc.stub.StreamObserver<WavelengthSpectrum> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetWavelengthSpectraMethod(), responseObserver);
    }

    /**
     * <pre>
     * Retrieves the Meta Data associated with the ADC channels of the sample.
     * Returns: if no adc data is available then an empty dictionary will be returned.
     * Notice:  please don't assume sequential ids of the channels
     * </pre>
     */
    default void getAdcChannelDescriptions(GetAdcChannelDescriptionsRequest request,
        io.grpc.stub.StreamObserver<AdcChannelsDescriptions> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAdcChannelDescriptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Closing the file handles and releases the memory allocated for the caches and metadata for all samples in that file 
     * </pre>
     */
    default void closeFile(SourceFile request,
        io.grpc.stub.StreamObserver<Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCloseFileMethod(), responseObserver);
    }

    /**
     * <pre>
     * request for experiment TIC
     * </pre>
     */
    default void getExperimentTic(GetExperimentTicRequest request,
        io.grpc.stub.StreamObserver<ExperimentTic> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetExperimentTicMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrives the list of all cycles in an experiment for the given sample
     * </pre>
     */
    default void getExperimentCycles(GetExperimentCyclesRequest request,
        io.grpc.stub.StreamObserver<ExperimentCycles> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetExperimentCyclesMethod(), responseObserver);
    }

    /**
     * <pre>
     * request to get absolute pointers from the beginning of the scan file
     * </pre>
     */
    default void getExperimentScanRecords(GetExperimentScanRecordsRequest request,
        io.grpc.stub.StreamObserver<ExperimentScanRecordsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetExperimentScanRecordsMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of scan XIC descriptors of the specific sample and experiment
     * </pre>
     */
    default void getScanXics(GetScanXicRequest request,
        io.grpc.stub.StreamObserver<ScanXic> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetScanXicsMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of MRM XIC descriptors of the specific sample and experiment
     * </pre>
     */
    default void getMrmXics(GetMrmXicRequest request,
        io.grpc.stub.StreamObserver<MrmXic> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMrmXicsMethod(), responseObserver);
    }

    /**
     * <pre>
     *retrieves the channel traces of the specific Sample
     * </pre>
     */
    default void getChannelTraces(GetChannelTracesRequest request,
        io.grpc.stub.StreamObserver<ChannelTrace> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetChannelTracesMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves ms method parameters of the specific sample and experiment
     * </pre>
     */
    default void getMsMethodParameters(GetMsMethodParametersRequest request,
        io.grpc.stub.StreamObserver<MsMethod> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMsMethodParametersMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of sample information parameters of the specific sample.
     * </pre>
     */
    default void getSampleInfoParameters(GetSampleInfoRequest request,
        io.grpc.stub.StreamObserver<SampleInfoSection> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSampleInfoParametersMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of BPC descriptors of the specific sample and experiment
     * </pre>
     */
    default void getBpcs(GetBpcRequest request,
        io.grpc.stub.StreamObserver<Bpc> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetBpcsMethod(), responseObserver);
    }

    /**
     * <pre>
     * retrieves lc method parameters of the specific sample and experiment
     * </pre>
     */
    default void getLcMethodParameters(GetLcMethodParametersRequest request,
        io.grpc.stub.StreamObserver<SampleInfoSection> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLcMethodParametersMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service DataProvider.
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public static abstract class DataProviderImplBase
      implements io.grpc.BindableService, AsyncService {

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return DataProviderGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service DataProvider.
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public static final class DataProviderStub
      extends io.grpc.stub.AbstractAsyncStub<DataProviderStub> {
    private DataProviderStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected DataProviderStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataProviderStub(channel, callOptions);
    }

    /**
     * <pre>
     * retrieves the stream of samples signals of the specific sample
     * </pre>
     */
    public void getSamplesDescriptions(ListSamplesRequest request,
        io.grpc.stub.StreamObserver<Sample> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetSamplesDescriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of experiments descriptors of the specific sample
     * </pre>
     */
    public void getExperiments(GetExperimentsRequest request,
        io.grpc.stub.StreamObserver<Experiment> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetExperimentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of spectra of the specific sample
     * </pre>
     */
    public void getSpectra(GetSpectraRequest request,
        io.grpc.stub.StreamObserver<Spectrum> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetSpectraMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of wavelength signals of the specific sample
     * </pre>
     */
    public void getWavelengthSpectra(GetWavelengthSpectraRequest request,
        io.grpc.stub.StreamObserver<WavelengthSpectrum> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetWavelengthSpectraMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Retrieves the Meta Data associated with the ADC channels of the sample.
     * Returns: if no adc data is available then an empty dictionary will be returned.
     * Notice:  please don't assume sequential ids of the channels
     * </pre>
     */
    public void getAdcChannelDescriptions(GetAdcChannelDescriptionsRequest request,
        io.grpc.stub.StreamObserver<AdcChannelsDescriptions> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAdcChannelDescriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Closing the file handles and releases the memory allocated for the caches and metadata for all samples in that file 
     * </pre>
     */
    public void closeFile(SourceFile request,
        io.grpc.stub.StreamObserver<Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCloseFileMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * request for experiment TIC
     * </pre>
     */
    public void getExperimentTic(GetExperimentTicRequest request,
        io.grpc.stub.StreamObserver<ExperimentTic> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetExperimentTicMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrives the list of all cycles in an experiment for the given sample
     * </pre>
     */
    public void getExperimentCycles(GetExperimentCyclesRequest request,
        io.grpc.stub.StreamObserver<ExperimentCycles> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetExperimentCyclesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * request to get absolute pointers from the beginning of the scan file
     * </pre>
     */
    public void getExperimentScanRecords(GetExperimentScanRecordsRequest request,
        io.grpc.stub.StreamObserver<ExperimentScanRecordsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetExperimentScanRecordsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of scan XIC descriptors of the specific sample and experiment
     * </pre>
     */
    public void getScanXics(GetScanXicRequest request,
        io.grpc.stub.StreamObserver<ScanXic> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetScanXicsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of MRM XIC descriptors of the specific sample and experiment
     * </pre>
     */
    public void getMrmXics(GetMrmXicRequest request,
        io.grpc.stub.StreamObserver<MrmXic> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetMrmXicsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *retrieves the channel traces of the specific Sample
     * </pre>
     */
    public void getChannelTraces(GetChannelTracesRequest request,
        io.grpc.stub.StreamObserver<ChannelTrace> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetChannelTracesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves ms method parameters of the specific sample and experiment
     * </pre>
     */
    public void getMsMethodParameters(GetMsMethodParametersRequest request,
        io.grpc.stub.StreamObserver<MsMethod> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetMsMethodParametersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of sample information parameters of the specific sample.
     * </pre>
     */
    public void getSampleInfoParameters(GetSampleInfoRequest request,
        io.grpc.stub.StreamObserver<SampleInfoSection> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetSampleInfoParametersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves the stream of BPC descriptors of the specific sample and experiment
     * </pre>
     */
    public void getBpcs(GetBpcRequest request,
        io.grpc.stub.StreamObserver<Bpc> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetBpcsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * retrieves lc method parameters of the specific sample and experiment
     * </pre>
     */
    public void getLcMethodParameters(GetLcMethodParametersRequest request,
        io.grpc.stub.StreamObserver<SampleInfoSection> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetLcMethodParametersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service DataProvider.
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public static final class DataProviderBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<DataProviderBlockingV2Stub> {
    private DataProviderBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected DataProviderBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataProviderBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * retrieves the stream of samples signals of the specific sample
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, Sample>
        getSamplesDescriptions(ListSamplesRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetSamplesDescriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of experiments descriptors of the specific sample
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, Experiment>
        getExperiments(GetExperimentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetExperimentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of spectra of the specific sample
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, Spectrum>
        getSpectra(GetSpectraRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetSpectraMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of wavelength signals of the specific sample
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, WavelengthSpectrum>
        getWavelengthSpectra(GetWavelengthSpectraRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetWavelengthSpectraMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieves the Meta Data associated with the ADC channels of the sample.
     * Returns: if no adc data is available then an empty dictionary will be returned.
     * Notice:  please don't assume sequential ids of the channels
     * </pre>
     */
    public AdcChannelsDescriptions getAdcChannelDescriptions(GetAdcChannelDescriptionsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetAdcChannelDescriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Closing the file handles and releases the memory allocated for the caches and metadata for all samples in that file 
     * </pre>
     */
    public Empty closeFile(SourceFile request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCloseFileMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * request for experiment TIC
     * </pre>
     */
    public ExperimentTic getExperimentTic(GetExperimentTicRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetExperimentTicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrives the list of all cycles in an experiment for the given sample
     * </pre>
     */
    public ExperimentCycles getExperimentCycles(GetExperimentCyclesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetExperimentCyclesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * request to get absolute pointers from the beginning of the scan file
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, ExperimentScanRecordsResponse>
        getExperimentScanRecords(GetExperimentScanRecordsRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetExperimentScanRecordsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of scan XIC descriptors of the specific sample and experiment
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, ScanXic>
        getScanXics(GetScanXicRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetScanXicsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of MRM XIC descriptors of the specific sample and experiment
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, MrmXic>
        getMrmXics(GetMrmXicRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetMrmXicsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *retrieves the channel traces of the specific Sample
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, ChannelTrace>
        getChannelTraces(GetChannelTracesRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetChannelTracesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves ms method parameters of the specific sample and experiment
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, MsMethod>
        getMsMethodParameters(GetMsMethodParametersRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetMsMethodParametersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of sample information parameters of the specific sample.
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, SampleInfoSection>
        getSampleInfoParameters(GetSampleInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetSampleInfoParametersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of BPC descriptors of the specific sample and experiment
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, Bpc>
        getBpcs(GetBpcRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetBpcsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves lc method parameters of the specific sample and experiment
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, SampleInfoSection>
        getLcMethodParameters(GetLcMethodParametersRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getGetLcMethodParametersMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service DataProvider.
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public static final class DataProviderBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<DataProviderBlockingStub> {
    private DataProviderBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected DataProviderBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataProviderBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * retrieves the stream of samples signals of the specific sample
     * </pre>
     */
    public java.util.Iterator<Sample> getSamplesDescriptions(
        ListSamplesRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetSamplesDescriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of experiments descriptors of the specific sample
     * </pre>
     */
    public java.util.Iterator<Experiment> getExperiments(
        GetExperimentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetExperimentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of spectra of the specific sample
     * </pre>
     */
    public java.util.Iterator<Spectrum> getSpectra(
        GetSpectraRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetSpectraMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of wavelength signals of the specific sample
     * </pre>
     */
    public java.util.Iterator<WavelengthSpectrum> getWavelengthSpectra(
        GetWavelengthSpectraRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetWavelengthSpectraMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Retrieves the Meta Data associated with the ADC channels of the sample.
     * Returns: if no adc data is available then an empty dictionary will be returned.
     * Notice:  please don't assume sequential ids of the channels
     * </pre>
     */
    public AdcChannelsDescriptions getAdcChannelDescriptions(GetAdcChannelDescriptionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAdcChannelDescriptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Closing the file handles and releases the memory allocated for the caches and metadata for all samples in that file 
     * </pre>
     */
    public Empty closeFile(SourceFile request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCloseFileMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * request for experiment TIC
     * </pre>
     */
    public ExperimentTic getExperimentTic(GetExperimentTicRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetExperimentTicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrives the list of all cycles in an experiment for the given sample
     * </pre>
     */
    public ExperimentCycles getExperimentCycles(GetExperimentCyclesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetExperimentCyclesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * request to get absolute pointers from the beginning of the scan file
     * </pre>
     */
    public java.util.Iterator<ExperimentScanRecordsResponse> getExperimentScanRecords(
        GetExperimentScanRecordsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetExperimentScanRecordsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of scan XIC descriptors of the specific sample and experiment
     * </pre>
     */
    public java.util.Iterator<ScanXic> getScanXics(
        GetScanXicRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetScanXicsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of MRM XIC descriptors of the specific sample and experiment
     * </pre>
     */
    public java.util.Iterator<MrmXic> getMrmXics(
        GetMrmXicRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetMrmXicsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *retrieves the channel traces of the specific Sample
     * </pre>
     */
    public java.util.Iterator<ChannelTrace> getChannelTraces(
        GetChannelTracesRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetChannelTracesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves ms method parameters of the specific sample and experiment
     * </pre>
     */
    public java.util.Iterator<MsMethod> getMsMethodParameters(
        GetMsMethodParametersRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetMsMethodParametersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of sample information parameters of the specific sample.
     * </pre>
     */
    public java.util.Iterator<SampleInfoSection> getSampleInfoParameters(
        GetSampleInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetSampleInfoParametersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves the stream of BPC descriptors of the specific sample and experiment
     * </pre>
     */
    public java.util.Iterator<Bpc> getBpcs(
        GetBpcRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetBpcsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * retrieves lc method parameters of the specific sample and experiment
     * </pre>
     */
    public java.util.Iterator<SampleInfoSection> getLcMethodParameters(
        GetLcMethodParametersRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetLcMethodParametersMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service DataProvider.
   * <pre>
   * Serves as an access point to extract information from SCIEX data files
   * </pre>
   */
  public static final class DataProviderFutureStub
      extends io.grpc.stub.AbstractFutureStub<DataProviderFutureStub> {
    private DataProviderFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected DataProviderFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataProviderFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Retrieves the Meta Data associated with the ADC channels of the sample.
     * Returns: if no adc data is available then an empty dictionary will be returned.
     * Notice:  please don't assume sequential ids of the channels
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<AdcChannelsDescriptions> getAdcChannelDescriptions(
        GetAdcChannelDescriptionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAdcChannelDescriptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Closing the file handles and releases the memory allocated for the caches and metadata for all samples in that file 
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Empty> closeFile(
        SourceFile request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCloseFileMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * request for experiment TIC
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ExperimentTic> getExperimentTic(
        GetExperimentTicRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetExperimentTicMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * retrives the list of all cycles in an experiment for the given sample
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ExperimentCycles> getExperimentCycles(
        GetExperimentCyclesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetExperimentCyclesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_SAMPLES_DESCRIPTIONS = 0;
  private static final int METHODID_GET_EXPERIMENTS = 1;
  private static final int METHODID_GET_SPECTRA = 2;
  private static final int METHODID_GET_WAVELENGTH_SPECTRA = 3;
  private static final int METHODID_GET_ADC_CHANNEL_DESCRIPTIONS = 4;
  private static final int METHODID_CLOSE_FILE = 5;
  private static final int METHODID_GET_EXPERIMENT_TIC = 6;
  private static final int METHODID_GET_EXPERIMENT_CYCLES = 7;
  private static final int METHODID_GET_EXPERIMENT_SCAN_RECORDS = 8;
  private static final int METHODID_GET_SCAN_XICS = 9;
  private static final int METHODID_GET_MRM_XICS = 10;
  private static final int METHODID_GET_CHANNEL_TRACES = 11;
  private static final int METHODID_GET_MS_METHOD_PARAMETERS = 12;
  private static final int METHODID_GET_SAMPLE_INFO_PARAMETERS = 13;
  private static final int METHODID_GET_BPCS = 14;
  private static final int METHODID_GET_LC_METHOD_PARAMETERS = 15;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_SAMPLES_DESCRIPTIONS:
          serviceImpl.getSamplesDescriptions((ListSamplesRequest) request,
              (io.grpc.stub.StreamObserver<Sample>) responseObserver);
          break;
        case METHODID_GET_EXPERIMENTS:
          serviceImpl.getExperiments((GetExperimentsRequest) request,
              (io.grpc.stub.StreamObserver<Experiment>) responseObserver);
          break;
        case METHODID_GET_SPECTRA:
          serviceImpl.getSpectra((GetSpectraRequest) request,
              (io.grpc.stub.StreamObserver<Spectrum>) responseObserver);
          break;
        case METHODID_GET_WAVELENGTH_SPECTRA:
          serviceImpl.getWavelengthSpectra((GetWavelengthSpectraRequest) request,
              (io.grpc.stub.StreamObserver<WavelengthSpectrum>) responseObserver);
          break;
        case METHODID_GET_ADC_CHANNEL_DESCRIPTIONS:
          serviceImpl.getAdcChannelDescriptions((GetAdcChannelDescriptionsRequest) request,
              (io.grpc.stub.StreamObserver<AdcChannelsDescriptions>) responseObserver);
          break;
        case METHODID_CLOSE_FILE:
          serviceImpl.closeFile((SourceFile) request,
              (io.grpc.stub.StreamObserver<Empty>) responseObserver);
          break;
        case METHODID_GET_EXPERIMENT_TIC:
          serviceImpl.getExperimentTic((GetExperimentTicRequest) request,
              (io.grpc.stub.StreamObserver<ExperimentTic>) responseObserver);
          break;
        case METHODID_GET_EXPERIMENT_CYCLES:
          serviceImpl.getExperimentCycles((GetExperimentCyclesRequest) request,
              (io.grpc.stub.StreamObserver<ExperimentCycles>) responseObserver);
          break;
        case METHODID_GET_EXPERIMENT_SCAN_RECORDS:
          serviceImpl.getExperimentScanRecords((GetExperimentScanRecordsRequest) request,
              (io.grpc.stub.StreamObserver<ExperimentScanRecordsResponse>) responseObserver);
          break;
        case METHODID_GET_SCAN_XICS:
          serviceImpl.getScanXics((GetScanXicRequest) request,
              (io.grpc.stub.StreamObserver<ScanXic>) responseObserver);
          break;
        case METHODID_GET_MRM_XICS:
          serviceImpl.getMrmXics((GetMrmXicRequest) request,
              (io.grpc.stub.StreamObserver<MrmXic>) responseObserver);
          break;
        case METHODID_GET_CHANNEL_TRACES:
          serviceImpl.getChannelTraces((GetChannelTracesRequest) request,
              (io.grpc.stub.StreamObserver<ChannelTrace>) responseObserver);
          break;
        case METHODID_GET_MS_METHOD_PARAMETERS:
          serviceImpl.getMsMethodParameters((GetMsMethodParametersRequest) request,
              (io.grpc.stub.StreamObserver<MsMethod>) responseObserver);
          break;
        case METHODID_GET_SAMPLE_INFO_PARAMETERS:
          serviceImpl.getSampleInfoParameters((GetSampleInfoRequest) request,
              (io.grpc.stub.StreamObserver<SampleInfoSection>) responseObserver);
          break;
        case METHODID_GET_BPCS:
          serviceImpl.getBpcs((GetBpcRequest) request,
              (io.grpc.stub.StreamObserver<Bpc>) responseObserver);
          break;
        case METHODID_GET_LC_METHOD_PARAMETERS:
          serviceImpl.getLcMethodParameters((GetLcMethodParametersRequest) request,
              (io.grpc.stub.StreamObserver<SampleInfoSection>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetSamplesDescriptionsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              ListSamplesRequest,
              Sample>(
                service, METHODID_GET_SAMPLES_DESCRIPTIONS)))
        .addMethod(
          getGetExperimentsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetExperimentsRequest,
              Experiment>(
                service, METHODID_GET_EXPERIMENTS)))
        .addMethod(
          getGetSpectraMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetSpectraRequest,
              Spectrum>(
                service, METHODID_GET_SPECTRA)))
        .addMethod(
          getGetWavelengthSpectraMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetWavelengthSpectraRequest,
              WavelengthSpectrum>(
                service, METHODID_GET_WAVELENGTH_SPECTRA)))
        .addMethod(
          getGetAdcChannelDescriptionsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              GetAdcChannelDescriptionsRequest,
              AdcChannelsDescriptions>(
                service, METHODID_GET_ADC_CHANNEL_DESCRIPTIONS)))
        .addMethod(
          getCloseFileMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              SourceFile,
              Empty>(
                service, METHODID_CLOSE_FILE)))
        .addMethod(
          getGetExperimentTicMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              GetExperimentTicRequest,
              ExperimentTic>(
                service, METHODID_GET_EXPERIMENT_TIC)))
        .addMethod(
          getGetExperimentCyclesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              GetExperimentCyclesRequest,
              ExperimentCycles>(
                service, METHODID_GET_EXPERIMENT_CYCLES)))
        .addMethod(
          getGetExperimentScanRecordsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetExperimentScanRecordsRequest,
              ExperimentScanRecordsResponse>(
                service, METHODID_GET_EXPERIMENT_SCAN_RECORDS)))
        .addMethod(
          getGetScanXicsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetScanXicRequest,
              ScanXic>(
                service, METHODID_GET_SCAN_XICS)))
        .addMethod(
          getGetMrmXicsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetMrmXicRequest,
              MrmXic>(
                service, METHODID_GET_MRM_XICS)))
        .addMethod(
          getGetChannelTracesMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetChannelTracesRequest,
              ChannelTrace>(
                service, METHODID_GET_CHANNEL_TRACES)))
        .addMethod(
          getGetMsMethodParametersMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetMsMethodParametersRequest,
              MsMethod>(
                service, METHODID_GET_MS_METHOD_PARAMETERS)))
        .addMethod(
          getGetSampleInfoParametersMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetSampleInfoRequest,
              SampleInfoSection>(
                service, METHODID_GET_SAMPLE_INFO_PARAMETERS)))
        .addMethod(
          getGetBpcsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetBpcRequest,
              Bpc>(
                service, METHODID_GET_BPCS)))
        .addMethod(
          getGetLcMethodParametersMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              GetLcMethodParametersRequest,
              SampleInfoSection>(
                service, METHODID_GET_LC_METHOD_PARAMETERS)))
        .build();
  }

  private static abstract class DataProviderBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DataProviderBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Clearcore2SampleDataGrpcContracts.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DataProvider");
    }
  }

  private static final class DataProviderFileDescriptorSupplier
      extends DataProviderBaseDescriptorSupplier {
    DataProviderFileDescriptorSupplier() {}
  }

  private static final class DataProviderMethodDescriptorSupplier
      extends DataProviderBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DataProviderMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DataProviderGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DataProviderFileDescriptorSupplier())
              .addMethod(getGetSamplesDescriptionsMethod())
              .addMethod(getGetExperimentsMethod())
              .addMethod(getGetSpectraMethod())
              .addMethod(getGetWavelengthSpectraMethod())
              .addMethod(getGetAdcChannelDescriptionsMethod())
              .addMethod(getCloseFileMethod())
              .addMethod(getGetExperimentTicMethod())
              .addMethod(getGetExperimentCyclesMethod())
              .addMethod(getGetExperimentScanRecordsMethod())
              .addMethod(getGetScanXicsMethod())
              .addMethod(getGetMrmXicsMethod())
              .addMethod(getGetChannelTracesMethod())
              .addMethod(getGetMsMethodParametersMethod())
              .addMethod(getGetSampleInfoParametersMethod())
              .addMethod(getGetBpcsMethod())
              .addMethod(getGetLcMethodParametersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
