package ch.furthermore.pmslwebst;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * export DEF_ID=`curl -s --data-binary @simple.pmsl -H'Content-Type:text/plain' https://pmw.furthermore.ch/definitions`
 * export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' https://pmw.furthermore.ch/definitions/$DEF_ID`
 * curl -s --data-binary '{"foo":"bar"}' -H'Content-Type:application/json' https://pmw.furthermore.ch/instances/$WF_ID
 * </pre>
 */
@SuppressWarnings("deprecation")
@Controller
public class WorkflowController {
	private final static Logger log = LoggerFactory.getLogger(WorkflowController.class);
	private final static ObjectMapper om = new ObjectMapper();
	
	@Autowired
	private WorkflowDefinitionDAO workflowDefinitionDAO;
	
	@Autowired
	private WorkflowInstanceDAO workflowInstanceDAO;

	@Autowired
	private HttpServletRequest request;
	
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
	
	@RequestMapping(path="/definitionsX", method=RequestMethod.POST, consumes="text/plain", produces="application/json")
	@ResponseBody
	Map<String,Object> createWorkflowDefinitionX(@RequestBody String workflowDefinition) {
		Map<String,Object> data2 = new HashMap<>();
		data2.put("definitionId", createWorkflowDefinition(workflowDefinition));
		return data2;
	}
	
	@RequestMapping(path="/definitions/{definitionId}", method=RequestMethod.POST, consumes="application/json", produces="text/plain")
	@ResponseBody
	String createWorkflowInstance(@RequestBody Map<String,String> data, @PathVariable("definitionId") String definitionId) {
		try {
			String workflowDefinition = workflowDefinitionDAO.load(definitionId);
			
			String workflowDefAndState = post("https://pmsl.furthermore.ch/workflow?autoStart=false", "application/json", "text/plain", workflowDefinition);

			return signal(data, UUID.randomUUID().toString(), null, workflowDefAndState);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	@RequestMapping(path="/definitionsX/{definitionId}", method=RequestMethod.POST, consumes="application/json", produces="application/json")
	@ResponseBody
	Map<String,Object> createWorkflowInstanceX(@RequestBody Map<String,String> data, @PathVariable("definitionId") String definitionId) {
		Map<String,Object> data2 = new HashMap<>();
		data2.put("instanceId", createWorkflowInstance(data, definitionId));
		return data2;
	}
	
	@RequestMapping(path="/instances/{workflowId}", method=RequestMethod.POST, consumes="application/json", produces="text/plain")
	@ResponseBody
	String signalWorkflow(@RequestBody Map<String,String> data, @PathVariable("workflowId") String workflowId) {
		try {
			String workflowDefAndState = workflowInstanceDAO.load(workflowId);

			return signal(data, workflowId, null, workflowDefAndState);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@RequestMapping(path="/instances/{workflowId}", method=RequestMethod.GET, produces="application/json")
	@ResponseBody
	Map lookupWorkflow(@PathVariable("workflowId") String workflowId) {
		try {
			String workflowDefAndState = workflowInstanceDAO.load(workflowId);

			Map parsedworkflowDefAndState = om.readValue(workflowDefAndState, Map.class);
			
			return (Map) parsedworkflowDefAndState.get("token");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(path="/tokens/{workflowId}/{instanceId}", method=RequestMethod.POST, consumes="application/json", produces="text/plain")
	@ResponseBody
	String signalToken(@RequestBody Map<String,String> data, @PathVariable("workflowId") String workflowId, @PathVariable("instanceId") String instanceId) {
		try {
			String workflowDefAndState = workflowInstanceDAO.load(workflowId);

			return signal(data, workflowId, instanceId, workflowDefAndState);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private String signal(Map<String, String> data, String workflowId, String optionalTokenId, String workflowDefAndState)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException,
			KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException 
	{
		workflowDefAndState = addKeyValuePairsToToken(data, workflowDefAndState, optionalTokenId);
		
		workflowDefAndState = post("https://pmsl.furthermore.ch/instance" + (optionalTokenId == null ? "" : "/" + optionalTokenId), 
				"application/json", "application/json", workflowDefAndState);
		
		workflowDefAndState = processTasks(workflowDefAndState, workflowId);
		
		LinkedList<InstanceToken> instanceIdSignals = new LinkedList<>();
		workflowDefAndState = processHttpPostRequests(workflowDefAndState, workflowId, instanceIdSignals);
		
		workflowInstanceDAO.insert(workflowId, workflowDefAndState);
		
		if (!instanceIdSignals.isEmpty()) {
			InstanceToken signal = instanceIdSignals.removeFirst();
			
			signal(new HashMap<String,String>(), signal.instanceId, signal.tokenId, workflowDefAndState);
		}
		
		return workflowId;
	}
	
	static class InstanceToken {
		public final String instanceId;
		public final String tokenId;
		
		public InstanceToken(String instanceId, String tokenId) {
			this.instanceId = instanceId;
			this.tokenId = tokenId;
		}
	}
	
	private String addKeyValuePairsToToken(final Map<String, String> keyValuePairs, String workflowDefAndState, final String optionalTokenId)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException //FIXME somewhat hacky ;-)
	{
		return modifyToken(workflowDefAndState, new TokenModifier() {
			@Override
			public void modify(SerializedToken token) {
				if (optionalTokenId == null || optionalTokenId.equals(token.getVars().get("id"))) {
					token.getVars().putAll(keyValuePairs);
				}
				else {
					for (SerializedToken child : token.getChildren()) {
						modify(child);
					}
				}
			}
		});
	}

	private String processHttpPostRequests(String workflowDefAndState, final String instanceId, final List<InstanceToken> instanceIdSignals)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException 
	{
		return modifyToken(workflowDefAndState, new TokenModifier() {
			@Override
			public void modify(SerializedToken token) {
				for (SerializedToken child : token.getChildren()) { 
					modify(child);
					
					if (!instanceIdSignals.isEmpty()) break;
				}

				if (token.getVars().containsKey("post")) {
					String tokenId = (String) token.getVars().get("id");
					
					StringTokenizer st = new StringTokenizer((String) token.getVars().get("post"), ",");
					String url = st.nextToken();
					Map<String,Object> data = new HashMap<>();
					while (st.hasMoreTokens()) {
						String key = st.nextToken();
						data.put(key, token.getVars().get(key));
					}
					
					try {
						@SuppressWarnings("unchecked")
						Map<String,Object> result = (Map<String,Object>) om.readValue(
								post(url, "application/json", "application/json", 
										om.writeValueAsString(data)), Map.class);
						token.getVars().remove("post");
						
						for (Entry<String, Object> e : result.entrySet()) {
							if ("id".equals(e.getKey())) continue; //don't mess with token ids
							token.getVars().put(e.getKey(), e.getValue());
						}
						
						instanceIdSignals.add(new InstanceToken(instanceId, tokenId));
					} catch(com.fasterxml.jackson.core.JsonParseException e) {
						token.getVars().remove("post"); //fixme
						instanceIdSignals.add(new InstanceToken(instanceId, tokenId));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}
	
	private String processTasks(String workflowDefAndState, final String instanceId)
			throws IOException, JsonParseException, JsonMappingException, JsonGenerationException 
	{
		return modifyToken(workflowDefAndState, new TokenModifier() {
			@Override
			public void modify(SerializedToken token) {
				for (SerializedToken child : token.getChildren()) { 
					modify(child);
				}
				
				if (token.getVars().containsKey("task")) {
					String tokenId = (String) token.getVars().get("id");
					
					List<Map<String,Object>> fields = new LinkedList<>();
					for (StringTokenizer st = new StringTokenizer((String) token.getVars().get("task"), ","); st.hasMoreTokens(); ) {
						String fieldName = st.nextToken();
						
						Map<String,Object> field = new HashMap<>();
						field.put("name", fieldName);
						field.put("label", fieldName);
						field.put("value", token.getVars().containsKey(fieldName) 
								? "" + token.getVars().get(fieldName) 
								: "");
						field.put("type", "INPUT");
						
						fields.add(field);
					}
					
					Map<String,Object> task = new HashMap<>();
					task.put("callbackUrl", serverUrlPrefix() + "tokens/" + instanceId + "/" + tokenId);
					task.put("fields", fields);
					
					if (token.getVars().containsKey("email")) {
						task.put("email", token.getVars().get("email"));
					}
					
					Map<String,Object> createTaskRequest = new HashMap<>();
					createTaskRequest.put("task", task);
					
					try {
						@SuppressWarnings("unchecked")
						Map<String,Object> result = (Map<String,Object>) om.readValue(
								post("https://pmt.furthermore.ch/tasks", "application/json", "application/json", 
										om.writeValueAsString(createTaskRequest)), Map.class);
						token.getVars().remove("task");
						token.getVars().put("taskURL", "https://pmt.furthermore.ch/pending/" + result.get("taskId"));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
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
	
	private String serverUrlPrefix() {
		/*
		 * Header x-forwarded-proto='https'
		 * Header x-forwarded-port='443'
		 * Header x-forwarded-host='pmt.furthermore.ch'
		 */
		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements(); ) {
			String name = e.nextElement();
			
			log.info("Header {}='{}'", name, request.getHeader(name)); //TODO DEBUG log
		}
		
		StringBuilder dashboardUrlPrefix = new StringBuilder();
		
		if (request.getHeader("x-forwarded-proto") != null) {
			dashboardUrlPrefix.append(request.getHeader("x-forwarded-proto"));
		}
		else {
			dashboardUrlPrefix.append(request.getScheme()); 
		}
		
		dashboardUrlPrefix.append("://");
		
		if (request.getHeader("x-forwarded-host") != null) {
			dashboardUrlPrefix.append(request.getHeader("x-forwarded-host"));
		}
		else {
			dashboardUrlPrefix.append(request.getServerName());
		}
		
		dashboardUrlPrefix.append(":");
		
		if (request.getHeader("x-forwarded-port") != null) {
			dashboardUrlPrefix.append(request.getHeader("x-forwarded-port"));
		}
		else {
			dashboardUrlPrefix.append(request.getServerPort());
		}
		
		dashboardUrlPrefix.append(request.getContextPath());
		
		dashboardUrlPrefix.append("/");
		
		return dashboardUrlPrefix.toString();
	}
	
	private static String post(String url, String accept, String contentType, String data) 
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException //FIXME avoid deprecated API 
	{
		CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(new AllowAllHostnameVerifier())
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                        return true; //FIXME temporary work-around for let's encrypt certs :(
                    }
                }).build())
                .build();
	    try {
	        HttpPost httpPost = new HttpPost(url);
	        httpPost.setEntity(new StringEntity(data));
	        httpPost.setHeader("Content-type", contentType);
	        httpPost.setHeader("Accept", accept);
	        
	        ResponseHandler<String> responseHandler = new BasicResponseHandler();
	        
	        String result = httpClient.execute(httpPost, responseHandler);
	        
	        log.info("HTTP POST '{}' to {} => '{}'", data, url, result); //FIXME debug log
	        
			return result;
	    } catch (java.lang.Exception e) {
		    log.warn("HTTP POST '{}' to {} => '{}'", data, url, e); //FIXME debug log
		    throw new java.lang.RuntimeException(e);
	    } finally {
	    	httpClient.close();
	    }
	}
}
