package Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import org.xml.sax.ContentHandler;

public class XMLProcessing {

	public static <T> T unmarshal( Class<T> docClass, InputStream inputStream )
		throws JAXBException {
		String packageName = docClass.getPackage().getName();
		JAXBContext jc = JAXBContext.newInstance( packageName );
		Unmarshaller u = jc.createUnmarshaller();
		JAXBElement<T> doc = (JAXBElement<T>)u.unmarshal(inputStream);
		return doc.getValue();		
	}
	
	public static <T> T unmarshal( Class<T> docClass, String fn )
			throws JAXBException {
			String packageName = docClass.getPackage().getName();
			JAXBContext jc = JAXBContext.newInstance( packageName );
			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<T> doc = (JAXBElement<T>)u.unmarshal(new File(fn));
			return doc.getValue();		
		}
	 
	public static void writeDocument(JAXBElement<?> document, OutputStream output)
		throws JAXBException, IOException {
		
		Class<? extends Object> clazz = document.getValue().getClass();
		JAXBContext context = JAXBContext.newInstance(clazz.getPackage().getName());
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
		m.marshal(document, output);
	}
	
	public static void writeDocument(JAXBElement<?> document, ContentHandler output)
		throws JAXBException, IOException {
	
		Class<? extends Object> clazz = document.getValue().getClass();
		JAXBContext context = JAXBContext.newInstance(clazz.getPackage().getName());
		Marshaller m = context.createMarshaller();
		//m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
		m.marshal(document, output);
	}
	
	public static Document parseXMLString(String txt) {
		 StringReader read = new StringReader(txt);
		 InputSource source = new InputSource(read);
		 SAXBuilder sb = new SAXBuilder();
		 try {
	            //通过输入源构造一个Document
	            Document doc = sb.build(source);
	            return doc;
		 } catch(Exception e) {
			   return null;
		 }
	}
	
	public static float calculateSimilarity(String xml1, String xml2) {
		Document doc1 = parseXMLString(xml1);
		Document doc2 = parseXMLString(xml2);
		if(doc1==null&&doc2==null) return 1;
		if(doc1==null||doc2==null) return 0;
		
		Element root1 = doc1.getRootElement();
		Element root2 = doc2.getRootElement();
		return calculateSimilarity(root1, root2);
	}

	private static float calculateSimilarity(Element e1, Element e2) {

		float result = 0;
		String en1 = e1.getName();
		String en2 = e2.getName();
		
		float rs =0;
		if(en1.equals(en2)  ) rs= 1;
		List clist1 =  e1.getContent();
		List clist2 =  e2.getContent();
		int num1 = clist1.size();
		int num2 = clist2.size();
		int maxnum = (num1>num2?num1: num2) + 1;
		int minnum =  num1>num2?num2: num1;
		
		result+= rs/maxnum;
		
		for(int i=0;i<minnum;i++) {
			Object child1 = clist1.get(i);
			Object child2 = clist2.get(i);
			
			if(child1 instanceof Element && child2 instanceof Element) {
			      float er = calculateSimilarity((Element)child1, (Element)child2);
			      result+=er/maxnum;
			}
			
			if(child1 instanceof Text && child2 instanceof Text) {
				 float er = calculateSimilarity((Text)child1, (Text)child2);
				 result+=er/maxnum;
			}
		}					    	
		
		return result;
	}

	private static float calculateSimilarity(Text child1, Text child2) {

		String txt1 = child1.getText();
		String txt2 = child2.getText();
		
		if(txt1.equals(txt2)) return 1;
		return 0;
	}
}
