package com.openbravo.pos.importer;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.Session;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppViewConnection;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JRootApp;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.util.CustomFormatter;
import com.openbravo.pos.util.SendEmail;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author jjuanmarti
 */
public class ImporterPOS {

    private static Logger logger = Logger.getLogger("com.openbravo.pos.forms.ImporterPOS");
    private static FileHandler fh;
    
    private static DataLogicSales m_dlSales = null;
    private static DataLogicWarehouse m_dlWarehouse = null;
    
    /** Creates a new instance of ImporterPOS */
    private ImporterPOS() {
    }
    
    public static double p()
    {
        Scanner m=new Scanner(System.in);
        double x;
        System.out.print("DAME UN VALOR: ");
        x=m.nextDouble();
        return x;
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
        logger.info(x);
    	System.out.println(x);
    }
    
    private static ProductInfoExt getProduct (String ref, String code) throws BasicException{
    	ProductInfoExt prd = null;     	
    	//Por referencia
    	if (!ref.isEmpty()) {
        	prd = m_dlSales.getProductInfoByReference(ref);
    	} 
    	if (prd==null && !code.isEmpty()) {
    		//Por codigo de barras
    		prd = m_dlSales.getProductInfoByCode(code);
    	}  
    	/*Anterior a la actualizaci�n de c�digo de barras
    	//Por codigo de barras
    	if (!code.isEmpty())
    		prd = m_dlSales.getProductInfoByCode(code);     	
    	if (prd==null && !ref.isEmpty()) {
    		//Por referencia
        	prd = m_dlSales.getProductInfoByReference(ref);
    	}    	
    	*/
    	return prd;    	
    }
    
    private static Double calculaPrecioVentaDeVentaMasIVA(String precioConIva, String impuesto) throws BasicException  {
        
    	// Load the taxes logic
        TaxesLogic taxeslogic = new TaxesLogic(m_dlSales.getTaxList().list());
    	
    	Double dPriceSellTax = (Double)Formats.CURRENCY.parseValue(precioConIva);  
        double dTaxRate = taxeslogic.getTaxRate(impuesto, new Date());
        return (new Double(dPriceSellTax.doubleValue() / (1.0 + dTaxRate)));
    }
    
    public static Object preparaProductoNuevo(String[] fields)  throws BasicException {
    
	    Object[] myprod = new Object[17];
    	myprod[0] = UUID.randomUUID().toString();
	    myprod[1] = fields[0]; //ref
	    myprod[2] = fields[1]; //codigo barras
	    myprod[3] = fields[2]; //nombre
	    myprod[4] = false; //comment
	    myprod[5] = false; //scale
	    myprod[6] = Formats.CURRENCY.parseValue(fields[3]); //Precio compra
	    //myprod[7] = Formats.CURRENCY.parseValue(fields[4]); //Precio venta sin IVA
	    //myprod[7] = calculaPrecioVentaDeVentaMasIVA(fields[4], fields[6]); //Precio venta sin IVA calculado de precio venta con IVA
	    myprod[7] = (Double)Formats.CURRENCY.parseValue(fields[4].replace(".",",")); //Precio venta sin IVA calculado de precio venta con IVA
	    myprod[8] = fields[5]; //Id categoria
	    myprod[9] = fields[6]; //Id categoria impuesto
	    myprod[10] = null; //attmodel
	    myprod[11] = null; //imagen
	    myprod[12] = null; //stock cost
	    myprod[13] = null; //stock volume
	    myprod[14] = true; //en catalogo
	    myprod[15] = null; //orden catalogo
	    myprod[16] = null; //atributos
	    
	    return myprod;    
    }
    
    public static Object preparaProductoExistente(ProductInfoExt prod, String[] fields)  throws BasicException {
        
	    Object[] myprod = new Object[17];
    	myprod[0] = prod.getID();
	    myprod[1] = fields[0]; //ref
	    myprod[2] = fields[1]; //codigo barras
	    myprod[3] = fields[2]; //nombre
	    myprod[4] = false; //comment
	    myprod[5] = false; //scale
	    myprod[6] = Formats.CURRENCY.parseValue(fields[3]); //Precio compra
	    //myprod[7] = Formats.CURRENCY.parseValue(fields[4]); //Precio venta sin IVA
	    myprod[7] = calculaPrecioVentaDeVentaMasIVA(fields[4], fields[6]); //Precio venta sin IVA calculado de precio venta con IVA
	    myprod[8] = fields[5]; //Id categoria
	    myprod[9] = fields[6]; //Id categoria impuesto
	    myprod[10] = null; //attmodel
	    myprod[11] = null; //imagen
	    myprod[12] = null; //stock cost
	    myprod[13] = null; //stock volume
	    myprod[14] = true; //en catalogo
	    myprod[15] = null; //orden catalogo
	    myprod[16] = null; //atributos
	    
	    return myprod;    
    }
    
public static Object preparaProductoExistenteEan13(ProductInfoExt prod, String code)  throws BasicException {
        
	    Object[] myprod = new Object[2];
    	myprod[0] = prod.getID();
	    myprod[1] = code; //codigo barras
	    
	    return myprod;    
    }
    
    private static void insertStockLimits (String idProduct, String[] fields) throws BasicException {
        //Insertamos stock limits
    	SentenceExec sentLimits = m_dlWarehouse.getStockLimitsInsert();
    	sentLimits.exec(new Object[] {
    		null, //id
            idProduct,
            "0", //location
            Double.parseDouble(fields[7]), //min
            Double.parseDouble(fields[8]) //max                           
        });
    }
    
    public static void main (final String args[]) {

		AppConfig config = new AppConfig(args);
        config.load();
        int numTotal=0;
        int numCreados=0;
        int numModificados = 0;
        int numEan13Modificados = 0;
        int numErrores = 0;
        String lineaErrores = "";
        boolean actualizaProductos=true, importaStock=false;
        
        try {
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        	String fechaSync = sdf.format(new Date());
        	String logFile = "c:\\temp\\logs\\importacion_"+fechaSync+".log";
        	fh = new FileHandler(logFile, true);
        	fh.setFormatter(new CustomFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            
        	Session session = AppViewConnection.createSession(config);
        	JRootApp rootApp = new JRootApp();
        	m_dlSales = (DataLogicSales) rootApp.getBean("com.openbravo.pos.forms.DataLogicSales");
        	m_dlSales.init(session);
        	
        	m_dlWarehouse = (DataLogicWarehouse) rootApp.getBean("com.openbravo.pos.importer.DataLogicWarehouse");
        	m_dlWarehouse.init(session);
        	
        	//actualizaProductos = preguntaSN("Deseas actualizar productos ya existentes? (s / n): ");
        	//importaStock = preguntaSN("Se trata de una importacion de nuevo stock? (s / n): ");
        	imp("Importando, el proceso puede tardar más de 10 minutos, paciencia ...");
        	/*File f = new File("C:\\temp\\carga_productos_POS.csv"); 
            BufferedReader r = null; 
            try { 
                r = new BufferedReader(new FileReader(f)); 
            } catch (FileNotFoundException e) { 
                e.printStackTrace(); 
                return; 
            } 
            String line = null; 
            try { 
                line = r.readLine(); // Skip header 
                line = r.readLine(); 
            } catch (IOException e) { 
                e.printStackTrace(); 
            }*/
        	WebResource webResource;
    		ClientResponse response;
    		String xmlResponse;
    		DocumentBuilderFactory dbf;
    		DocumentBuilder db;
    		InputSource is;
    		Document dom;
    		Element docEle;
    		
    		Client client = Client.create();
        	client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("QZAK1ZLLQ3378U186WCW49LSQ8UJ54TN", ""));
        	webResource = client.resource("http://www.dulcinenca.com/comprar/api/products");
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
			String linkProduct;
			String ref = "?";
        	
            //while (line != null) { 
			for (int i=0; i<productNodeList.getLength(); i++) {
                numTotal++;
            	try {
	                //String[] fields = line.split(";"); 
            		linkProduct = ((Element)productNodeList.item(i)).getAttribute("xlink:href");
            		webResource = client.resource(linkProduct);
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
					String[] fields = new String[9];
					fields[0] = docEle.getElementsByTagName("reference").item(0).getTextContent();
					fields[1] = docEle.getElementsByTagName("ean13").item(0).getTextContent();
					fields[2] = docEle.getElementsByTagName("name").item(0).getChildNodes().item(0).getTextContent();
					fields[3] = "0";
					fields[4] = docEle.getElementsByTagName("price").item(0).getTextContent();
					fields[5] = "000";
					fields[6] = "00"+docEle.getElementsByTagName("id_tax_rules_group").item(0).getTextContent();
					fields[7] = "0";
					fields[8] = "0";
	            	if (fields.length > 2) {
		            	ref = fields[0]; 
		                if (ref.startsWith("\"") && ref.endsWith("\"")) { 
		                    ref = ref.substring(1, ref.length() - 1); 
		                    ref = ref.replace("\\\"", "\""); 
		                }
		                String code = fields[1];
	                	//Si ean13 es 0 se autogenera un ean13 random
	            		boolean ean13Cero = false;
		                if (code.equals("0") || code.equals("")) {
	            			code = newRandom();
	            			fields[1] = code;
	            		}
		                
		                ProductInfoExt prd = null; 
		                try { 
		                    prd = getProduct(ref, ""); 
		                } catch (BasicException e) { 
		                    e.printStackTrace(); 
		                } 
		                String idProduct; 
		                Double productPrice = new Double(0.0);
		                if (prd != null) { 
		                	//Existe ese producto con esa referencia (ref) -> intentamos actualizar
		                	//logger.log(Level.INFO, "PRODUCTO EXISTENTE: ref["+ref+"], code["+code+"]");
		                    if (actualizaProductos && actualizaCodigoBarras(prd, code)) {
		                		//Actualizamos producto
			                	/*Object updatedProduct = preparaProductoExistente(prd, fields);
			                	m_dlSales.getProductCatUpdate().exec(updatedProduct);
			                	insertStockLimits(prd.getID(), fields);
			                	numEan13Modificados++;
			                	numModificados++;
			                	logger.log(Level.INFO, "PRODUCTO ACTUALIZADO: ref["+ref+"], code["+code+"]");*/
		        	        	Object ean13updatedProduct = preparaProductoExistenteEan13(prd, code);
		        	        	m_dlSales.getProductEan13Update().exec(ean13updatedProduct);
		        	        	numEan13Modificados++;
		        	        	logger.log(Level.INFO, "OK");
		        	        	//logger.log(Level.INFO, "PRODUCTO ACTUALIZADO: ref["+ref+"], code["+code+"]");
		                    }
		                } else { 
		                	try { 
			                    prd = getProduct("", code); 
			                } catch (BasicException e) { 
			                    e.printStackTrace(); 
			                } 
		                	if (prd==null) {
				                // Tampoco existe producto con ese ean13 -> creamos producto
		                		String prodName = fields[2];
			                	if (m_dlSales.getProductInfoByName(prodName)!=null) {
			                		Random generator = new Random(); 
			                		fields[2] = prodName + "-" + generator.nextInt(10) + 1;
			                	}
			                	logger.log(Level.INFO, "CÓDIGO DE BARRAS A CREAR : name ["+fields[2]+"], ref["+ref+"], EAN13["+code+"]");
			                	Object newProduct = preparaProductoNuevo(fields);
			                	idProduct = ((Object[]) newProduct)[0].toString();
			                	productPrice = new Double(((Object[]) newProduct)[7].toString());
			                	m_dlSales.getProductCatInsert().exec(newProduct);
			                	insertStockLimits(idProduct, fields);
			                    numCreados++;
			                	logger.log(Level.INFO, "OK");
			                    //logger.log(Level.INFO, "PRODUCTO CREADO: ref["+ref+"], code["+code+"]");
		                	} else {
		                		logger.log(Level.INFO, "PRODUCTO NO CREADO. NO EXISTE REFERENCIA PERO EAN13 YA UTILIZADO: nombre nuevo["+fields[2]+"] / existente["+prd.getName()+"], ref["+ref+" / "+prd.getReference()+"], code["+code+" / "+prd.getCode()+"]");
		                	}
		                }
		                //Insertamos stock diary
		                if (importaStock) {
		                	SentenceExec sent = m_dlSales.getStockDiaryInsert();
	                        sent.exec(new Object[] {
	                            UUID.randomUUID().toString(),
	                            new Date(),
	                            1,
	                            "0", //location
	                            prd.getID(),
	                            null,
	                            Double.parseDouble(fields[9]), //unidades
	                            productPrice//precio                            
	                        });
		                }
	                } else {
	                    logger.log(Level.WARNING, "ERROR: Linea " + numTotal + " incompleta");
	                }
            	} catch (Exception e) {
            		logger.log(Level.WARNING, "ERROR: Producto de la linea [" + numTotal + "] - " + e.getMessage());
            		e.printStackTrace();
            		numErrores++;
            		lineaErrores = lineaErrores + ref + ",";
            	}
                /*try { 
                    line = r.readLine(); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                    line = null; 
                } */
            } 
            /*try { 
                r.close(); 
            } catch (IOException e) { 
                e.printStackTrace(); 
            } */
            
            client.destroy();
        	//SentenceList listaProductos = m_dlSales.getProductList();
            imp ("Num. productos comprobados: "+numTotal);
            imp ("Num. productos creados: "+numCreados);
        	//imp ("Num. productos actualizados: "+numModificados);
            imp ("Num. productos con ean13 nuevo: "+numEan13Modificados);
        	imp ("Num. errores: "+numErrores+" ["+lineaErrores+"]");
        	
        	//Emails
        	SendEmail email = new SendEmail(); 
        	email.send(logFile, "jordijuanmarti@gmail.com", "Resultado de descarga de productos", "logs.txt", null);
        	StringBuilder sb = new StringBuilder();
        	sb.append( "Num. productos comprobados: "+numTotal);
    		sb.append(System.getProperty("line.separator"));
    		sb.append( "Num. productos creados: "+numCreados);
    		sb.append(System.getProperty("line.separator"));
    		sb.append( "Num. productos con ean13 nuevo: "+numEan13Modificados);
    		sb.append(System.getProperty("line.separator"));
    		sb.append( "Num. errores: "+numErrores+" ["+lineaErrores+"]");
    		sb.append(System.getProperty("line.separator"));
        	email.send(null, "jordi@juanmarti.com", "Resultado de descuento de stocks", null, sb);
        	
        	preguntaSN("Escribir para cerrar ventana: ");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error", e);
        }
        
    }

	private static String newRandom() {
		Random r = new Random();
		int low = 1;
		int high = 999999999;
		int result = r.nextInt(high-low);
		return String.valueOf(10000000000000L+result);
	}

	private static boolean actualizaCodigoBarras(ProductInfoExt prd, String code) throws BasicException {
		boolean update = false;	
		//Validamos si es 0 (codigo de long. > 13
		if (code.length() > 13 && prd.getCode().length() > 13) {
			return false;
		}
		//Validamos que no exista producto con ese ean13
		ProductInfoExt prdMismoEan13 = getProduct("", code);
        if (prdMismoEan13 != null && !prdMismoEan13.getID().equals(prd.getID())) {
        	//logger.log(Level.INFO, "OJO! C�digo barras usado en otro producto: ref["+prd.getReference()+"], code["+code+"] - CÓDIGO DE BARRAS NO ACTUALIZADO");
        } else if (!prd.getCode().equals(code)) {
            if (prd.getCode().equals("0"+code)) {
            	logger.log(Level.INFO, "UPDATE dps_product SET ean13='0"+code+"' WHERE reference='"+prd.getReference()+"' - CÓDIGO DE BARRAS NO ACTUALIZADO - a ejecutar en online");
            } else {
            	update = true;
            }
        } else {
        	//logger.log(Level.INFO, "PRODUCTO SIN CAMBIOS");
        }
        if (update) {
        	if (code.length() <= 13) {
        		logger.log(Level.INFO, "CÓDIGO DE BARRAS A ACTUALIZAR: ref["+prd.getReference()+"], anterior EAN13["+prd.getCode()+"], nuevo EAN13["+code+"]");
        	} else {
    			logger.log(Level.INFO, "CÓDIGO DE BARRAS 0: ref["+prd.getReference()+"], nuevo EAN13["+code+"]");
        	}
        }
        return update;
	}
   
}
