import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

class Tester {
  
  public void exec(HttpServletRequest request, Writer out)
      throws ServletException {    
    String doc = request.getParameter("doc");   
    Reader input = new StringReader(doc);       
    try {      
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();        
      XMLEventReader eventReader = inputFactory.createXMLEventReader(input); 
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(out);                  
      eventWriter.add(eventReader);      
    } catch (XMLStreamException e) {
      throw new ServletException(e);
    }    
  } 
  
}
