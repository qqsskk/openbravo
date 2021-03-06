// @formatter:off

/**
 * WebService3ImplServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.openbravo.services.webservice;

@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class WebService3ImplServiceLocator extends org.apache.axis.client.Service implements org.openbravo.services.webservice.WebService3ImplService {

    public WebService3ImplServiceLocator() {
    }


    public WebService3ImplServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public WebService3ImplServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for WebService3
    private java.lang.String WebService3_address = "http://centralrepository.openbravo.com/openbravo/services/WebService3";

    @Override
    public java.lang.String getWebService3Address() {
        return WebService3_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String WebService3WSDDServiceName = "WebService3";

    public java.lang.String getWebService3WSDDServiceName() {
        return WebService3WSDDServiceName;
    }

    public void setWebService3WSDDServiceName(java.lang.String name) {
        WebService3WSDDServiceName = name;
    }

    @Override
    public org.openbravo.services.webservice.WebService3Impl getWebService3() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(WebService3_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getWebService3(endpoint);
    }

    @Override
    public org.openbravo.services.webservice.WebService3Impl getWebService3(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.openbravo.services.webservice.WebService3SoapBindingStub _stub = new org.openbravo.services.webservice.WebService3SoapBindingStub(portAddress, this);
            _stub.setPortName(getWebService3WSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setWebService3EndpointAddress(java.lang.String address) {
        WebService3_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    @Override
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.openbravo.services.webservice.WebService3Impl.class.isAssignableFrom(serviceEndpointInterface)) {
                org.openbravo.services.webservice.WebService3SoapBindingStub _stub = new org.openbravo.services.webservice.WebService3SoapBindingStub(new java.net.URL(WebService3_address), this);
                _stub.setPortName(getWebService3WSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    @Override
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("WebService3".equals(inputPortName)) {
            return getWebService3();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    @Override
    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://centralrepository.openbravo.com/openbravo/services/WebService3", "WebService3ImplService");
    }

    private java.util.HashSet ports = null;

    @Override
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://centralrepository.openbravo.com/openbravo/services/WebService3", "WebService3"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("WebService3".equals(portName)) {
            setWebService3EndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
