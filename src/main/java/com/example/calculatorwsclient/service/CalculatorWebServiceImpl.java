package com.example.calculatorwsclient.service;

import com.example.AddResponse;
import com.example.calculatorwsclient.CalculatorStub;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class CalculatorWebServiceImpl implements CalculatorWebService {
    @Autowired
    private CalculatorStub calculatorStub;
    private final ExecutorService virtualThreadExecutorService;

    @Autowired
    public CalculatorWebServiceImpl(ExecutorService virtualThreadExecutorService) {
        this.virtualThreadExecutorService = virtualThreadExecutorService;
    }

    @Async
    public CompletableFuture<Integer> performAsyncAddOperation(int a, int b) {
        try {
            CalculatorStub.Add add = new CalculatorStub.Add();
            add.setIntA(a);
            add.setIntB(b);
            CalculatorStub.AddResponse response = calculatorStub.add(add);
            int result = response.getAddResult();
            return CompletableFuture.completedFuture(result);
        } catch (RemoteException e) {
            throw new RuntimeException("Error invoking remote service", e);
        }
    }

    @Override
    public CompletableFuture<Integer> performAsyncVirtualThreadAddOperation(int a, int b) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Calculate the result asynchronously
                CalculatorStub.Add add = new CalculatorStub.Add();
                add.setIntA(a);
                add.setIntB(b);
                CalculatorStub.AddResponse response = calculatorStub.add(add);
                int result = response.getAddResult();
                return result;
            } catch (RemoteException e) {
                throw new RuntimeException("Error invoking remote service", e);
            }
        }, virtualThreadExecutorService);
    }

    @Override
    public int performSyncAddOperation(int a, int b) throws RemoteException, InterruptedException {
        CalculatorStub.Add add = new CalculatorStub.Add();
        add.setIntA(a);
        add.setIntB(b);
        return calculatorStub.add(add).getAddResult();
    }

    @Override
    public void performAxisAsync(int A, int B,AxisCallback axisCallback) throws Exception {
        QName operationName = new QName("http://tempuri.org/", "Add");
        String wsdlUrl = "http://www.dneonline.com/calculator.asmx?wsdl";
//        RPCServiceClient serviceClient = new RPCServiceClient(ConfigurationContextFactory.createDefaultConfigurationContext(),new URL(wsdlUrl),operationName,"CalculatorSoap");
        RPCServiceClient serviceClient = new RPCServiceClient();
        Options options = serviceClient.getOptions();
        EndpointReference targetEPR = new EndpointReference("http://www.dneonline.com/calculator.asmx?wsdl");
        serviceClient.setTargetEPR(targetEPR);
        options.setAction("http://tempuri.org/Add");
        options.setTo(targetEPR);
        options.setSoapVersionURI(org.apache.axiom.soap.SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.CHUNKED, Boolean.FALSE);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.CONTENT_TYPE, org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_APPLICATION_XML);
        serviceClient.setOptions(options);
        Object[] parameters = new Object[]{10, 5};
        OMElement requestPayload = buildRequestPayload(operationName, parameters);
        Class[] returnTypes = new Class[]{AddResponse.class};
        serviceClient.sendReceiveNonBlocking(requestPayload, axisCallback);
//        Object[] response4 = BeanUtil.deserialize(omElement, returnTypes, new DefaultObjectSupplier());
//        System.out.println(response4);
    }

    private OMElement buildRequestPayload(QName operationName, Object[] parameters) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace namespace = factory.createOMNamespace(operationName.getNamespaceURI(), "");
        OMElement operationElement = factory.createOMElement(operationName.getLocalPart(), namespace);
        OMElement paramElement = factory.createOMElement("intA", namespace);
        paramElement.setText(String.valueOf(parameters[0]));
        operationElement.addChild(paramElement);
        OMElement paramElement2 = factory.createOMElement("intB", namespace);
        paramElement2.setText(String.valueOf(parameters[1]));
        operationElement.addChild(paramElement2);

        return operationElement;
    }

    public static class TestCallback implements AxisCallback {

        public String name = null;
        public boolean complete = false;
        public int result;

        public TestCallback(String name) {
            this.name = name;
            this.complete = false;
            this.result = 0;
        }


        public void onError(Exception e) {
            e.printStackTrace();
        }

        public void onComplete() {
            System.out.println("Message transmission complete");
            complete = true;
        }

        public boolean isComplete() {
            return complete;
        }

        public void onMessage(org.apache.axis2.context.MessageContext messageContext) {
            System.out.println("Call Back " + name + " got Result: " + messageContext.getEnvelope());
            try {
                OMElement responseElement = messageContext.getEnvelope().getBody().getFirstElement();
                if (responseElement != null) {
                    // Örnek olarak int tipinde bir sonuç alıyorsak
                    System.out.println("RESPONSE:"+responseElement.getText());
                    result = Integer.parseInt("1");
                    System.out.println("Call Back " + name + " got Result: " + result);
                } else {
                    System.out.println("Response element is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        public void onFault(org.apache.axis2.context.MessageContext arg0) {
            System.out.println("Call Back " + name + " got Fault: " + arg0.getEnvelope());
        }
    }


}
