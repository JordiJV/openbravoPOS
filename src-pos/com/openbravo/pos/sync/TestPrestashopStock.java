package com.openbravo.pos.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppViewConnection;
import com.openbravo.pos.forms.JRootApp;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.util.SendEmail;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 *
 * @author jjuanmarti
 */
public class TestPrestashopStock {

    private static Logger logger = Logger.getLogger("com.openbravo.pos.sync.TestPrestashopStock");
    private static FileHandler fh;
    private static String WS_KEY="QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN";
    
    private static DataLogicSync m_dlSync = null;
    
    /** Creates a new instance of DiscountPrestashopStock */
    private TestPrestashopStock() {
    }
    
    public static boolean preguntaSN (String texto)
    {
        Scanner m=new Scanner(System.in);
        System.out.print(texto);
        if (m.next().toUpperCase().equals("S")) return true;
        return false;
    }
    
    public static  void imp(String x)
    {
        System.out.println(x);
    }
    
    public static void main (final String args[]) {

		AppConfig config = new AppConfig(args);
        config.load();
        int numPendientes=0;
        int numCorrectos=0;
        int numErrores = 0;
        String referenciasErrores = "";
        
        try {
        	String logFile = "c:\\temp\\logs\\test_stock_fecha.txt";
        	fh = new FileHandler(logFile, true);
        	fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            
        	String referenciaProducto="ML_0235";
        	
        	Client client = Client.create();
			//client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("5JGC49RGPX5EHRXCMB5531B7PHYVMCTU", ""));
        	client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(WS_KEY, ""));
        	WebResource webResource;
    		ClientResponse response;
    		DocumentBuilderFactory dbf;
    		DocumentBuilder db;
    		InputSource is;
    		Document dom;
    		Element docEle;
    		
    		//1.1
        	webResource = client.resource("http://www.dulcinenca.com/comprar/api/products?ws_key="+WS_KEY+"&filter[reference]="+referenciaProducto);//+"&ws_key=QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN");
			response = webResource.accept("application/xml").get(ClientResponse.class);
			if (response.getStatus() != 200) {
				   throw new RuntimeException("Failed : HTTP error code : "+ response.getStatus());
				}
			String xmlResponse;
			xmlResponse = response.getEntity(String.class);
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			is = new InputSource(new StringReader(xmlResponse));
			dom = db.parse(is);
			docEle = dom.getDocumentElement();
			NodeList productNodeList = docEle.getElementsByTagName("product");
			
			if (productNodeList.getLength() == 0) {
				throw new Exception("No se ha encontrado productos en la tienda online con esta referencia ["+ referenciaProducto +"]");
			}
			if (productNodeList.getLength() > 1) {
				throw new Exception("Se ha encontrado más de un producto en la tienda online con esta referencia ["+ referenciaProducto +"]");
			}
			
			String id = ((Element)productNodeList.item(0)).getAttribute("id");
			String linkProduct = ((Element)productNodeList.item(0)).getAttribute("xlink:href");
			
			System.out.println("Un resultado, id: " + id + ", link: " + linkProduct);
			
			// 1.2 Obtenemos los datos del producto, entre ellos el id de stock_available - http://localhost/dulcinenca_prestashop/api/products/1520
			webResource = client.resource(linkProduct+"?ws_key="+WS_KEY);//+"&ws_key=QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN");
			response = webResource.accept("application/xml").get(ClientResponse.class);
			if (response.getStatus() != 200) {
			   throw new RuntimeException("Failed : HTTP error code : "+ response.getStatus());
			}
			xmlResponse = response.getEntity(String.class);
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			is = new InputSource(new StringReader(xmlResponse));
			dom = db.parse(is);
			docEle = dom.getDocumentElement();
			NodeList stockAvaNodeList = docEle.getElementsByTagName("stock_available");
			
			if (stockAvaNodeList.getLength() == 0) {
				throw new Exception("No se ha encontrado linea de stock para el producto en la tienda online con esta referencia ["+ referenciaProducto +"]");
			}
			if (stockAvaNodeList.getLength() > 1) {
				throw new Exception("Se ha encontrado más de una linea de stock para el producto en la tienda online con esta referencia ["+ referenciaProducto +"]");
			}
			
			String linkStockAvailable = ((Element)stockAvaNodeList.item(0)).getAttribute("xlink:href");
			System.out.println("Un stock_available, link: " + linkStockAvailable);
        	
			String nombreProductoOnline = docEle.getElementsByTagName("name").item(0).getChildNodes().item(0).getTextContent();
			
			// 1.3 Obtenemos el quantity actual de ese stock_available - http://localhost/dulcinenca_prestashop/api/stock_availables/1182
			webResource = client.resource(linkStockAvailable+"?ws_key="+WS_KEY);//+"&ws_key=QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN");
			response = webResource.accept("application/xml").get(ClientResponse.class);
			if (response.getStatus() != 200) {
			   throw new RuntimeException("Failed : HTTP error code : "+ response.getStatus());
			}
			xmlResponse = response.getEntity(String.class);
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			is = new InputSource(new StringReader(xmlResponse));
			dom = db.parse(is);
			docEle = dom.getDocumentElement();
			String cantidadActual = docEle.getElementsByTagName("quantity").item(0).getTextContent();

			System.out.println("Cantidad actual: " + cantidadActual);
			
			// 1.4 Actualizamos el quantity de ese stock_available 
			webResource = client.resource(linkStockAvailable+"?ws_key="+WS_KEY);
			//response = webResource.path("prestashop").path("stock_available").path("quantity").accept(MediaType.TEXT_PLAIN).put(ClientResponse.class, "5");
			
			docEle.getElementsByTagName("quantity").item(0).setTextContent("2");
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(docEle), new StreamResult(writer));
			String input = writer.getBuffer().toString().replaceAll("\n|\r", "");

			response = webResource.type(MediaType.APPLICATION_XML).put(ClientResponse.class, input);
    		if (response.getStatus() != 200) {
			   throw new RuntimeException("Failed : HTTP error code : "+ response.getStatus());
			}
			xmlResponse = response.getEntity(String.class);
    		
    		
			
        	client.destroy();
        	imp ("Num. productos a descontar: " + numPendientes);
            imp ("Num. productos descontados correctamente: " + numCorrectos);
        	imp ("Num. errores: "+numErrores);
        	if(numErrores > 0)
        		imp ("Referencias con error:["+referenciasErrores+"]");
        	
        	SendEmail email = new SendEmail(); 
        	email.send(logFile, "jordijuanmarti@gmail.com", "Resultado de sincrionización", "logs.txt", null);
        	
        	preguntaSN("Escribir para cerrar ventana: ");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error", e);
        } finally {
        	fh.close();
        }
        
    }
   
}
