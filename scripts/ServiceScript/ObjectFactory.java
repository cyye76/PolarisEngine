//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.29 at 07:00:00 PM EDT 
//


package scripts.ServiceScript;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the scripts.ServiceScript package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Service_QNAME = new QName("http://www.example.org/ServiceSchema", "Service");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: scripts.ServiceScript
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceDefinition }
     * 
     */
    public ServiceDefinition createServiceDefinition() {
        return new ServiceDefinition();
    }

    /**
     * Create an instance of {@link VariableType }
     * 
     */
    public VariableType createVariableType() {
        return new VariableType();
    }

    /**
     * Create an instance of {@link TransitionList }
     * 
     */
    public TransitionList createTransitionList() {
        return new TransitionList();
    }

    /**
     * Create an instance of {@link ActivityList }
     * 
     */
    public ActivityList createActivityList() {
        return new ActivityList();
    }

    /**
     * Create an instance of {@link TransitionType }
     * 
     */
    public TransitionType createTransitionType() {
        return new TransitionType();
    }

    /**
     * Create an instance of {@link ActivityDefinition }
     * 
     */
    public ActivityDefinition createActivityDefinition() {
        return new ActivityDefinition();
    }

    /**
     * Create an instance of {@link VariablesListType }
     * 
     */
    public VariablesListType createVariablesListType() {
        return new VariablesListType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceDefinition }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.example.org/ServiceSchema", name = "Service")
    public JAXBElement<ServiceDefinition> createService(ServiceDefinition value) {
        return new JAXBElement<ServiceDefinition>(_Service_QNAME, ServiceDefinition.class, null, value);
    }

}
