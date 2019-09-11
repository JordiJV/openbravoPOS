package com.openbravo.pos.sync;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppViewConnection;
import com.openbravo.pos.forms.JRootApp;
import com.openbravo.pos.util.CustomFormatter;
import com.openbravo.pos.util.SendEmail;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author jjuanmarti
 */
public class DiscountPrestashopStock {

    private static Logger logger = Logger.getLogger("com.openbravo.pos.sync.DiscountPrestashopStock");
    private static FileHandler fh;
    private static String WS_KEY="QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN";

    private static DataLogicSync m_dlSync = null;

    /** Creates a new instance of DiscountPrestashopStock */
    private DiscountPrestashopStock() {
    }

    public static boolean preguntaSN (String texto)
    {
        Scanner m=new Scanner(System.in);
        System.out.print(texto);
        if (m.next().toUpperCase().equals("S")) return true;
        return false;
    }

    public static  void printLine(String x)
    {
        logger.info(x);
        System.out.println(x);
    }

    public static  void printConsole(String x)
    {
        System.out.println(x);
    }

    public static void main (final String args[]) {

        logger.setLevel(Level.ALL);
        AppConfig config = new AppConfig(args);
        config.load();
        int numPendientes=0;
        int numCorrectos=0;
        int numErrores = 0;
        String referenciasErrores = "";
        boolean inicarImportacion = true;
        StringBuilder sb = new StringBuilder();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            String fechaSync = sdf.format(new Date());
            String logFile = "c:\\temp\\logs\\actualizacion_stock_"+fechaSync+".txt";
            fh = new FileHandler(logFile, true);
            fh.setFormatter(new CustomFormatter());
            logger.addHandler(fh);

            Session session = AppViewConnection.createSession(config);
            JRootApp rootApp = new JRootApp();
            m_dlSync = (DataLogicSync) rootApp.getBean("com.openbravo.pos.sync.DataLogicSync");
            m_dlSync.init(session);

            List stockPendiente = m_dlSync.getStockLinesPending();
            logger.log(Level.INFO,"Hay " + stockPendiente.size() + " productos para descontar.");

            if (stockPendiente.isEmpty() || stockPendiente.size() == 0) {
                inicarImportacion = false;
                preguntaSN("Hola guapetona!! No hay productos para descontar. Escribir para cerrar ventana: ");
            } else {
                inicarImportacion = preguntaSN("Hola guapetona!! Hay " + stockPendiente.size() + " productos para descontar. ¿Deseas descontarlos ahora? (s / n): ");
            }

            sb.append( "Total de caja este mes: " + m_dlSync.getCurrentMonthClosedCash() );
            sb.append(System.getProperty("line.separator"));

            if (!inicarImportacion) {
                SendEmail email = new SendEmail();
                email.send(null, "jordijuanmarti@gmail.com", "Caja", null, sb);
                logger.log(Level.INFO,"Fin del proceso.");
                return;
            }

            numPendientes = stockPendiente.size();
            Client client = Client.create();
            client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(WS_KEY, ""));

            for (Object stockDiary : stockPendiente) {
                String referenciaProducto = ((StockDiarySync)stockDiary).productRef.trim();
                String nombreProducto = ((StockDiarySync)stockDiary).productName.trim();
                printConsole ("Descontando ["+referenciaProducto+"]... ");
                //logger.info("---------- INI ref: ["+referenciaProducto+"] ------------");
                int cantidadADescontar = ((StockDiarySync)stockDiary).getUnits();
                WebResource webResource;
                ClientResponse response;
                String xmlResponse;
                DocumentBuilderFactory dbf;
                DocumentBuilder db;
                InputSource is;
                Document dom;
                Element docEle;
                try {
                    //1. Contactamos con el WS
                    // 1.1 Buscamos el producto por referencia - http://localhost/dulcinenca_prestashop/api/products?filter[reference]=rd3005
                    webResource = client.resource("http://www.dulcinenca.com/comprar/api/products?ws_key="+WS_KEY+"&filter[reference]="+referenciaProducto);
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
                    NodeList productNodeList = docEle.getElementsByTagName("product");

                    if (productNodeList.getLength() == 0) {
                        throw new Exception("No se ha encontrado productos en la tienda online con esta referencia ["+ referenciaProducto +"]");
                    }
                    if (productNodeList.getLength() > 1) {
                        throw new Exception("Se ha encontrado más de un producto en la tienda online con esta referencia ["+ referenciaProducto +"]");
                    }

                    String id = ((Element)productNodeList.item(0)).getAttribute("id");
                    String linkProduct = ((Element)productNodeList.item(0)).getAttribute("xlink:href");

                    //logger.info("Un resultado, id: " + id + ", link: " + linkProduct);
                    // 1.2 Obtenemos los datos del producto, entre ellos el id de stock_available - http://localhost/dulcinenca_prestashop/api/products/1520
                    webResource = client.resource(linkProduct+"?ws_key="+WS_KEY);
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
                    String nombreProductoOnline = docEle.getElementsByTagName("name").item(0).getChildNodes().item(0).getTextContent();
                    //logger.info("Un stock_available, link: " + linkStockAvailable);

                    // 1.3 Obtenemos el quantity actual de ese stock_available - http://localhost/dulcinenca_prestashop/api/stock_availables/1182
                    webResource = client.resource(linkStockAvailable+"?ws_key="+WS_KEY);
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

                    //logger.info("Cantidad actual: " + cantidadActual);
                    // 1.4 Actualizamos el quantity de ese stock_available
                    webResource = client.resource(linkStockAvailable+"?ws_key="+WS_KEY);
                    int cantidadNueva = (Integer.parseInt(cantidadActual)+cantidadADescontar);
                    docEle.getElementsByTagName("quantity").item(0).setTextContent(""+cantidadNueva);
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

                    //Todo ok - marcamos en POS stock sincronizado
                    SentenceExec sentUpdateStockDiary = m_dlSync.getStockSyncToUpdate();
                    sentUpdateStockDiary.exec(new Object[] {
                            ((StockDiarySync)stockDiary).getId() //id
                    });
                    logger.info("Stock actualizado - referencia:["+referenciaProducto+"], nombre en física:["+nombreProducto+"], nombre en online:["+nombreProductoOnline+"], cantidad actual:["+cantidadActual+"], a descontar:["+cantidadADescontar+"], cantidad nueva:["+cantidadNueva+"]");
                    sb.append( "Quedan:[" + cantidadNueva + "] - " + referenciaProducto + " - " + nombreProducto);
                    sb.append(System.getProperty("line.separator"));
                    numCorrectos++;
                    printConsole("OK");
                } catch (Exception e) {
                    //Si error
                    logger.log(Level.WARNING, "ERROR: Referencia de producto [" + referenciaProducto + "], cantidad a descontar [" + cantidadADescontar + "] -> " + e.getMessage());
                    numErrores++;
                    printConsole("Error");
                    referenciasErrores = referenciasErrores + referenciaProducto + ",";
                }
            }
            client.destroy();
            printLine ("Num. productos a descontar: " + numPendientes);
            printLine ("Num. productos descontados correctamente: " + numCorrectos);
            printLine ("Num. errores: "+numErrores);
            if(numErrores > 0)
                printLine ("Referencias con error:["+referenciasErrores+"]");
            printLine ("Puedes ver los logs en c:/temp/logs/actualizacion_stock_"+fechaSync+".txt");

            //Emails
            SendEmail email = new SendEmail();
            email.send(logFile, "jordijuanmarti@gmail.com", "Resultado de sincronización", "logs.txt", null);
            email.send(null, "dulcinenca@gmail.com, jordijuanmarti@gmail.com", "Resultado de descuento de stocks", null, sb);

            logger.log(Level.INFO,"Fin del proceso.");
            preguntaSN("Escribir para cerrar ventana: ");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error", e);
        } finally {
            fh.close();
        }

    }

}
