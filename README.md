# PoorManScriptingLanguageWebStateful

This is non-prod pre-alpha stuff - don't use it ;-)

# Sample

## Workflow (concat.pmsl)

```
workflow concat
	node start
		transition to prep
	end
	
	state prep
		transition to task1
	end
	
	state task1
		transition to task2
		
		enter
			task = "prefix,suffix"
		end
	end
	
	state task2
		transition to done
		
		enter
			message = "Here is your result"
			concat = prefix + " " + suffix
			task = "message,concat"
		end
	end
	
	node done
	end
end
```

## Script (concat.sh)

```
#!/bin/bash
export DEF_ID=`curl -s --data-binary @concat.pmsl -H'Content-Type:text/plain' https://pmw.furthermore.ch/definitions`
echo Workflow definition created: $DEF_ID

export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' https://pmw.furthermore.ch/definitions/$DEF_ID`
echo Workflow instance started: $WF_ID

export WF_ID=`curl -s --data-binary "{\"email\":\"$1\",\"prefix\":\"My Prefix\"}" -H'Content-Type:application/json' https://pmw.furthermore.ch/instances/$WF_ID`
echo Workflow instance signaled: $WF_ID with email: $1
```

## Test execution

```
./concat.sh your@email.com
```
