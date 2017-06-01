import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

class Engine {
	protected static final String PARAM_DOC = "doc";

	private static final String TEMPLATE_INIT = "init";
	// Directory structure
	// /data/
	// (:reference implementation:)
	// /iso-schematron-xslt2/
	// (:stylesheet to do post-process of result:)
	// /post-process/
	// (:stylesheet to do pre-process of the input data :)
	// /pre-process/
	// (:stylesheet to prepare and assemble the rules:)
	// /assemble/
	// (:rules in schematron:)
	// /rules/

	private final static String DATA_ISO_SCHEMATRON_XSLT2 = "/iso-schematron-xslt2/";
	private final static String DATA_POST_PROCESS = "/post-process/";
	private final static String DATA_PRE_PROCESS = "/pre-process/";
	private final static String DATA_RULES = "/rules/";
	private final static String DATA_DATA = "/data/";
	private final static String DATA_URI = "http://data.xml";
	private final static String MAIN_DATA_FILE = "data.xml";
	private final static String DATA_ASSEMBLE = "/assemble/";
	private final static String MAIN_FILE = "transform.xsl";
	private final static String SCHEMATRON_XSLT_ENTRY_POINT = "iso_svrl_for_xslt2.xsl";

	private final String dataPath;
	private final Processor processor;
	private final XsltExecutable exec_apply_rules, exec_pre_process,
			exec_post_process;
	private final URIResolver uri_resolver;

	public Engine(String dataPath) throws SaxonApiException {
		this.dataPath = dataPath;
		Processor l_processor = new Processor(false);
		XsltCompiler compiler = l_processor.newXsltCompiler();
		this.processor = l_processor;
		this.exec_apply_rules = getSchematronTransformer(dataPath, compiler);
		this.exec_pre_process = getPreProcessTransformer(dataPath, compiler);
		this.exec_post_process = getPostProcessTransformer(dataPath, compiler);
		this.uri_resolver = getURIResolver(dataPath);
	}

	private static URIResolver getURIResolver(final String dataPath) {
		return new URIResolver() {

			@Override
			public Source resolve(String href, String base)
					throws TransformerException {
				if (DATA_URI.equals(href)) {
					// System.out.println("href = "+href+"; base = "+base);
					return getStreamSource(dataPath + DATA_DATA
							+ MAIN_DATA_FILE);
				}
				return null;
			}

		};
	}

	protected void exec(HttpServletRequest request, Writer out)
			throws  IOException {
		StreamSource streamSource = getSourceFromParameter(request, PARAM_DOC);
		try {
			out.write(process(streamSource));
		} catch (SaxonApiException e) {
			throw new IOException(e);
		}
	}

	private String process(StreamSource input) throws SaxonApiException {
		XsltTransformer pre_process_transformer = this.exec_pre_process.load();
		XsltTransformer post_process_transformer = this.exec_post_process
				.load();
		XsltTransformer main_transformer = this.exec_apply_rules.load();
		XdmNode source = this.processor.newDocumentBuilder().build(input);
		// System.out.println("BaseURI : "+source.getBaseURI());
		XdmDestination result = new XdmDestination();

		// link the processes

		pre_process_transformer.setInitialContextNode(source);
		pre_process_transformer.setDestination(main_transformer);

		main_transformer.setDestination(post_process_transformer);
		main_transformer.setURIResolver(this.uri_resolver);

		post_process_transformer.setDestination(result);

		// transform
		pre_process_transformer.transform();

		return result.getXdmNode().toString();
	}

	private final static StreamSource getSourceFromParameter(
			HttpServletRequest request, String paramName) {
		// the input stream as a parameter
		String doc = request.getParameter(paramName);
		// Create a reader
		StringReader reader = new StringReader(doc);
		// Create a source
		StreamSource streamSource = new StreamSource(reader);
		//
		return streamSource;
	}

	private final static XsltExecutable getPreProcessTransformer(
			String dataPath, XsltCompiler compiler) throws SaxonApiException {
		return compiler.compile(getPreProcessXSLT(dataPath));
	}

	private final static XsltExecutable getPostProcessTransformer(
			String dataPath, XsltCompiler compiler) throws SaxonApiException {
		return compiler.compile(getPostProcessXSLT(dataPath));
	}

	// This is the operation to do once at loading of the servlet
	private final static XsltExecutable getSchematronTransformer(
			String dataPath, XsltCompiler compiler) throws SaxonApiException {
		// the compiler
		// log start time
		// log URI of the stylesheet
		// log URI of the document
		//
		XsltExecutable exec_SCH_TO_XSL = compiler.compile(getSchematronXSLT(
				dataPath, SCHEMATRON_XSLT_ENTRY_POINT));
		XsltTransformer trans_SCH_TO_XSL = exec_SCH_TO_XSL.load();
		// the assemble stylesheet
		XsltExecutable exec_ASSEMBLE = compiler
				.compile(getAssembleXSLT(dataPath));
		XsltTransformer trans_ASSEMBLE = exec_ASSEMBLE.load();
		XdmDestination result_SCH_AS_XSL = new XdmDestination();
		// pipeline
		trans_ASSEMBLE.setInitialTemplate(new QName(TEMPLATE_INIT));
		trans_ASSEMBLE.setDestination(trans_SCH_TO_XSL);
		trans_SCH_TO_XSL.setDestination(result_SCH_AS_XSL);
		trans_ASSEMBLE.transform();
		// log
		// System.out.println(result_SCH_AS_XSL.getXdmNode().toString());
		XsltExecutable exec_apply_rules = compiler.compile(result_SCH_AS_XSL
				.getXdmNode().asSource());

		// log end time
		// serialize the stylesheet result
		return exec_apply_rules;
	}

	private final static StreamSource getSchematronXSLT(String dataPath,
			String which) {
		return getStreamSource(dataPath + DATA_ISO_SCHEMATRON_XSLT2 + which);
	}

	private final static StreamSource getPreProcessXSLT(String dataPath) {
		return getStreamSource(dataPath + DATA_PRE_PROCESS + MAIN_FILE);
	}

	private final static StreamSource getPostProcessXSLT(String dataPath) {
		return getStreamSource(dataPath + DATA_POST_PROCESS + MAIN_FILE);
	}

	private final static StreamSource getAssembleXSLT(String dataPath) {
		return getStreamSource(dataPath + DATA_ASSEMBLE + MAIN_FILE);
	}

	private final static StreamSource getStreamSource(String path) {
		File file = new File(path);
		StreamSource streamSource = new StreamSource(file);
		return streamSource;
	}

	public static void main(String[] args) throws SaxonApiException {
		Engine engine = new Engine(
				"data/schfmk/");
		String inputfile = "test.xml";
		StreamSource input = getStreamSource(inputfile);
		System.out.println(engine.process(input));
	}

}