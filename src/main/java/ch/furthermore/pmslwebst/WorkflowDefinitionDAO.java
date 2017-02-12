package ch.furthermore.pmslwebst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkflowDefinitionDAO { //FIXME avoid code dup
	@Value(value="${definitionStoragePath:definitionStorage}")
	private String storagePath;
	private File storage;
	
	@PostConstruct
	public void init() {
		storage = new File(storagePath);
		storage.mkdirs();
	}

	public String load(String id) throws IOException {
		File definitionFile = new File(storage, UUID.fromString(id).toString() + ".pmsl");
		
		BufferedReader r = new BufferedReader(new FileReader(definitionFile));
		try {
			StringBuilder sb = new StringBuilder();
			for (String line = r.readLine(); line != null; line = r.readLine()) {
				if (sb.length() > 0) {
					sb.append('\n');
				}
				sb.append(line);
			}
			return sb.toString();
		}
		finally {
			r.close();
		}
	}
	
	public String save(String workflowDefinition) throws IOException {
		String id = UUID.randomUUID().toString();
		
		File definitionFile = new File(storage, id + ".pmsl");
		
		BufferedWriter w = new BufferedWriter(new FileWriter(definitionFile));
		try {
			w.write(workflowDefinition);
		}
		finally {
			w.close();
		}
		
		return id;
	}
}
