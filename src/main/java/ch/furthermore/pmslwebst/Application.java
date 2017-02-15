package ch.furthermore.pmslwebst;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Build Samples:
 * <pre>
 * mvn package docker:build
 * docker push dockerchtz/pmsl-web-stateful:latest
 * </pre>
 * 
 * Run Samples:
 * <pre>
 * docker run -p 8080:8080 -d dockerchtz/pmsl-web-stateful
 * </pre>
 */
@SpringBootApplication
@Controller
public class Application {
	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "nothing to see here";
	}
	
	@RequestMapping(path="/info", method=RequestMethod.POST, consumes="application/json", produces="application/json")
	@ResponseBody
	Map<String,Object> info(@RequestBody Map<String,Object> data) {
		data.put("message", "nothing to see here");
		
		return data;
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}
}
