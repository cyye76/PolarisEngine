//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.29 at 07:01:18 PM EDT 
//


package scripts.MutationScript;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MutationDefinition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MutationDefinition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="serviceName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="type" type="{http://www.example.org/MutationSchema}MutationType"/>
 *         &lt;element name="mutationName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="contents" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MutationDefinition", propOrder = {
    "serviceName",
    "type",
    "mutationName",
    "contents"
})
public class MutationDefinition {

    @XmlElement(required = true)
    protected String serviceName;
    @XmlElement(required = true)
    protected MutationType type;
    @XmlElement(required = true)
    protected String mutationName;
    @XmlElement(required = true)
    protected String contents;

    /**
     * Gets the value of the serviceName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the value of the serviceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServiceName(String value) {
        this.serviceName = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link MutationType }
     *     
     */
    public MutationType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link MutationType }
     *     
     */
    public void setType(MutationType value) {
        this.type = value;
    }

    /**
     * Gets the value of the mutationName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMutationName() {
        return mutationName;
    }

    /**
     * Sets the value of the mutationName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMutationName(String value) {
        this.mutationName = value;
    }

    /**
     * Gets the value of the contents property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContents() {
        return contents;
    }

    /**
     * Sets the value of the contents property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContents(String value) {
        this.contents = value;
    }

}
