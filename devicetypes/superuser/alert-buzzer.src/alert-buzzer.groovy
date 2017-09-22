metadata {
	definition (name: "Alert Buzzer", namespace: "", author: "JoePUNK") {
		capability "Switch"

		attribute "buzzStatus", "string"
        
        command "mute"
        command "shortbuzz"
        command "longbuzz"
     }

        preferences {
	    	input("ip", "string", title:"IP of BuzzHandler", description: "IP of BuzzHandler", required: true, displayDuringSetup: true)
		    input("port", "string", title:"HTTP Port", description: "HTTP Port", defaultValue: 80 , required: true, displayDuringSetup: true)
            input("keyvalue", "string", title:"Trigger Key", description:"Trigger Key", required: true, displayDuringSetup: true)
	    }

	tiles {
		standardTile("switch", "device.buzzStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "mute", label: "mute", action: "shortbuzz",  icon: "st.alarm.alarm.alarm", backgroundColor: "#ffffff"
            state "short", label: "short", action: "longbuzz",  icon: "st.alarm.alarm.alarm", backgroundColor: "#95c8db"
            state "long", label: "long", action: "mute",  icon: "st.alarm.alarm.alarm", backgroundColor: "#00a0dc"

		}
 		standardTile("mutebuzz", "device.buzzStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "mute", label: "Mute", backgroundColor: "#00a0dc", action: "mute"
            state "short", label: "Mute", backgroundColor: "#ffffff", action: "mute"
            state "long", label: "Mute", backgroundColor: "#ffffff", action: "mute"

  }
 		standardTile("shortbuzz", "device.buzzStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "mute", label: "Short", backgroundColor: "#ffffff", action: "shortbuzz"
            state "short", label: "Short", backgroundColor: "#00a0dc", action: "shortbuzz"
            state "long", label: "Short", backgroundColor: "#ffffff", action: "shortbuzz"
		}
 		standardTile("longbuzz", "device.buzzStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "mute", label: "Long", backgroundColor: "#ffffff", action: "longbuzz"
            state "short", label: "Long", backgroundColor: "#ffffff", action: "longbuzz"
            state "long", label: "Long", backgroundColor: "#00a0dc", action: "longbuzz"
		}
 	
		main "switch"
		details(["mutebuzz","shortbuzz","longbuzz"])
	}
}

def mute() {
	sendEvent(name: "buzzStatus", value: "mute");
	hubGet("/?key=${keyvalue}&trigger=mute")
    }
    
def shortbuzz() {
	sendEvent(name: "buzzStatus", value: "short");
	hubGet("/?key=${keyvalue}&trigger=buzz-short")
    }

def longbuzz() {
  	sendEvent(name: "buzzStatus", value: "long");
   	hubGet("/?key=${keyvalue}&trigger=buzz-long")
    }

private hubGet(def apiCommand) {
	//Setting Network Device Id
	def iphex = convertIPtoHex(ip)
	def porthex = convertPortToHex(port)
	device.deviceNetworkId = "$iphex:$porthex"
	//log.debug "Device Network Id set to ${iphex}:${porthex}"

	// Create headers
	def headers = [:]
	def hostAddress = "${ip}:${port}"
	headers.put("HOST", hostAddress)
	def authorizationClear = "${username}:${password}"
	def authorizationEncoded = "Basic " + authorizationClear.encodeAsBase64().toString()
	headers.put("Authorization", authorizationEncoded)

	log.debug("Getting ${apiCommand}")
	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: apiCommand,
		headers: headers)
	hubAction
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	return hexport
}