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
public class WorkflowInstanceDAO { //FIXME avoid code dup
	@Value(value="${instanceStoragePath:instanceStorage}")
	private String storagePath;
	private File storage;
	
	@PostConstruct
	public void init() {
		storage = new File(storagePath);
		storage.mkdirs();
	}

	public String load(String id) throws IOException {
		File workflowFile = new File(storage, UUID.fromString(id).toString() + ".json");
		
		BufferedReader r = new BufferedReader(new FileReader(workflowFile));
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
	
	public String insert(String id, String workflow) throws IOException {
		id = id == null ? UUID.randomUUID().toString() : UUID.fromString(id).toString();
		
		File workflowFile = new File(storage, id + ".json");
		
		BufferedWriter w = new BufferedWriter(new FileWriter(workflowFile));
		try {
			w.write(workflow);
		}
		finally {
			w.close();
		}
		
		return id;
	}
}
