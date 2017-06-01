
import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.saxon.s9api.SaxonApiException;

public class Servlet extends HttpServlet {
  
	private static final String PATH_DATA = "path-data";
	private ServletConfig servletConfig;
	private ServletContext servletContext;
	private Engine engine;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);			
    this.servletConfig = config;
		this.servletContext = config.getServletContext();		
		try {
      String dataPath = getContextParam(PATH_DATA);		  
		  this.engine = new Engine(dataPath);  
		} catch (SaxonApiException e) {
			throw new ServletException(e);
		}				
	}

	@Override
	public void destroy() { 
	  super.destroy(); 
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
	    throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
	    throws ServletException, IOException {
		try {		  		  
		  String charset = getCharset(request);
		  String mimeType = getMimeType(request, charset);		  
		  boolean noCache = hasNoCache(request);
		  boolean testMode = isTestMode(request);
			request.setCharacterEncoding(charset);
  	  response.setCharacterEncoding(charset);
  	  response.setContentType(mimeType);
  		if (noCache) {
  			response.setHeader("Cache-Control", "no-cache,no-store,max-age=0,s-maxage=0");
  			response.setHeader("Pragma", "no-cache");
  			response.setHeader("Expires", "-1");
  		} else {
  			response.setHeader("Cache-Control", "max-age=600,s-maxage=600");
  		}	  		
  		Writer out = response.getWriter();
  		String dataPath = getDataPath(request);
  		if (testMode) {			    		  
			  (new Tester()).exec(request, out);
			} else {			  
			  this.engine.exec(request, out);
			}
			out.close();
		}				
		catch (ServletException e) { throw e; }
		catch (IOException e) { throw e; }		
		catch (Throwable t) {
			throw new ServletException("Fatal error occured", t);
		}
	}
	
	private String getCharset(HttpServletRequest request) {
		String charset = "utf-8";							
		String param = request.getParameter("charset");
		if (param == null) { 
		  param = getConfigParam("charset"); 
		}
		if (param == null) { 
		  param = getContextParam("charset");  
		}
		if (param != null) { 
		  charset = param; 
		}
		return charset;
	}	
	
	private String getMimeType(HttpServletRequest request, String charset) {
		String mimeType = "text/xml";				
		String param = request.getParameter("mime-type");
		if (param == null) { 
		  param = getConfigParam("mime-type"); 
		}
		if (param == null) { 
		  param = getContextParam("mime-type");  
		}
		if (param != null) { 
		  mimeType = param; 
		}
		if (mimeType.indexOf(";") < 0) { 
		  mimeType += ";charset=" + charset; 
		}
		return mimeType;
	}
	
	private boolean hasNoCache(HttpServletRequest request) {
		boolean noCache = true;		
		String param = request.getParameter("no-cache");
		if (param == null) { 
		  param = getConfigParam("no-cache"); 
		}
		if (param == null) { 
		  param = getContextParam("no-cache"); 
		}
		if (param != null) { 
		  noCache = param.toLowerCase().equals("true"); 
		}
		return noCache;
	}		

	private boolean isTestMode(HttpServletRequest request) {
		boolean testMode = false;		
		String param = request.getParameter("test-mode");
		if (param == null) { 
		  param = getConfigParam("test-mode"); 
		}
		if (param == null) { 
		  param = getContextParam("test-mode"); 
		}
		if (param != null) { 
		  testMode = param.toLowerCase().equals("true"); 
		}
		return testMode;
	}		
	
	private String getDataPath(HttpServletRequest request) {			  
		return getContextParam(PATH_DATA);  		
	}	
		
	private String getConfigParam(String name) {
		return this.servletConfig.getInitParameter(name);		
	}

	public String getContextParam(String name) {
		return this.servletContext.getInitParameter(name);		
	}	

}
