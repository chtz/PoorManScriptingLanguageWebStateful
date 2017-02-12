package ch.furthermore.pmslwebst;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Samples
 * <pre>
 * export DEF_ID=`curl -s --data-binary @simple.pmsl -H'Content-Type:text/plain' http://localhost:8080/definitions`
 * export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' http://localhost:8080/definitions/$DEF_ID`
 * curl -s --data-binary '{"foo":"bar"}' -H'Content-Type:application/json' http://localhost:8080/instances/$WF_ID
 * </pre>
 */
@SuppressWarnings("deprecation")
@Controller
public class WorkflowController {
	private final static ObjectMapper om = new ObjectMapper();
	
	@Autowired
	private WorkflowDefinitionDAO workflowDefinitionDAO;
	
	@Autowired
	private WorkflowInstanceDAO workflowInstanceDAO;
	
	@RequestMapping(path="/definitions", method=RequestMethod.POST, consumes="text/plain", produces="text/plain")
	@ResponseBody
	String createWorkflowDefinition(@RequestBody String workflowDefinition) {
		try {
			return workflowDefinitionDAO.save(workflowDefinition);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(path="/definitions/{definitionId}", method=RequestMethod.POST, consumes="application/json", produces="text/plain")
	@ResponseBody
	String createWorkflowInstance(@RequestBody Map<String,String> ignoredData, @PathVariable("definitionId") String definitionId) {
		try {
			String workflowDefinition = workflowDefinitionDAO.load(definitionId);
			
			String workflowDefAndState = post("https://pmsl.furthermore.ch/workflow", "application/json", "text/plain", workflowDefinition);

			return workflowInstanceDAO.insert(null, workflowDefAndState);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(path="/instances/{workflowId}", method=RequestMethod.POST, consumes="application/json", produces="text/plain")
	@ResponseBody
	String signalWorkflow(@RequestBody Map<String,String> data, @PathVariable("workflowId") String workflowId) {
		try {
			String workflowDefAndState = workflowInstanceDAO.load(workflowId);

			workflowDefAndState = addKeyValuePairsToRootToken(data, workflowDefAndState);
			
			workflowDefAndState = post("https://pmsl.furthermore.ch/instance", "application/json", "application/json", workflowDefAndState);
			
			workflowInstanceDAO.insert(workflowId, workflowDefAndState);
			
			return workflowId;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String addKeyValuePairsToRootToken(final Map<String, String> keyValuePairs, String workflowDefAndState)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException //FIXME somewhat hacky ;-)
	{
		return modifyToken(workflowDefAndState, new TokenModifier() {
			@Override
			public void modify(SerializedToken token) {
				token.getVars().putAll(keyValuePairs);
			}
		});
	}
	
	private static String post(String url, String accept, String contentType, String data) 
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException //FIXME avoid deprecated API 
	{
		CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(new AllowAllHostnameVerifier())
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                        return true;
                    }
                }).build())
                .build();
	    try {
	        HttpPost httpPost = new HttpPost(url);
	        httpPost.setEntity(new StringEntity(data));
	        httpPost.setHeader("Content-type", contentType);
	        httpPost.setHeader("Accept", accept);
	        
	        ResponseHandler<String> responseHandler = new BasicResponseHandler();
	        
	        return httpClient.execute(httpPost, responseHandler);
	    } finally {
	    	httpClient.close();
	    }
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String modifyToken(String workflowDefAndState, TokenModifier modifier)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException //FIXME somewhat hacky ;-)
	{
		Map parsedworkflowDefAndState = om.readValue(workflowDefAndState, Map.class);
		
		Map tokenMap = (Map) parsedworkflowDefAndState.get("token");
		
		StringWriter sw = new StringWriter();
		om.writeValue(sw, tokenMap);
		SerializedToken token = om.readValue(sw.toString(), SerializedToken.class);
		
		modifier.modify(token);
		
		sw = new StringWriter();
		om.writeValue(sw, token);
		tokenMap = om.readValue(sw.toString(), Map.class);
		
		parsedworkflowDefAndState.put("token", tokenMap);
		
		sw = new StringWriter();
		om.writeValue(sw, parsedworkflowDefAndState);
		return sw.toString();
	}
	
	public static interface TokenModifier {
		public void modify(SerializedToken token);
	}
}
