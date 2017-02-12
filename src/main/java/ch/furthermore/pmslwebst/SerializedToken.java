package ch.furthermore.pmslwebst;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SerializedToken {
	private String node;
	private Map<String,Object> vars = new HashMap<>();
	private List<SerializedToken> children = new LinkedList<>();
	
	public Map<String, Object> getVars() {
		return vars;
	}

	public void setVars(Map<String, Object> vars) {
		this.vars = vars;
	}

	public List<SerializedToken> getChildren() {
		return children;
	}

	public void setChildren(List<SerializedToken> children) {
		this.children = children;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}
}
