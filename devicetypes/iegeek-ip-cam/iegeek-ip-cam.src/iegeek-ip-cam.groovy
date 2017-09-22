metadata {
	definition (name: "ieGeek IP Cam", namespace: "ieGeek IP Cam", author: "JoePUNK") {
            capability "Switch"  
            capability "Image Capture"
            
            attribute "emailStatus", "string"
            
            command "take"
            command "emailoff"
            command "emailon"
	}

	preferences {
		input("ip", "string", title:"Camera IP Address", description: "Camera IP Address", required: true, displayDuringSetup: true)
		input("port", "string", title:"Camera Port", description: "Camera Port", defaultValue: 80 , required: true, displayDuringSetup: true)
		input("username", "string", title:"Camera Username", description: "Camera Username", required: true, displayDuringSetup: true)
		input("password", "password", title:"Camera Password", description: "Camera Password", required: true, displayDuringSetup: true)    
	}
  
	tiles(scale: 2) {

        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "off", label: "Motion", action: "switch.on", icon: "st.Health & Wellness.health12", backgroundColor: "#FFFFFF"
            state "on", label: "Motion", action: "switch.off", icon: "st.Health & Wellness.health12", backgroundColor: "#00a0dc"
    }

		carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }

		standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
			state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
			state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
		}

        standardTile("emailon", "device.emailStatus", width: 1, height: 1, decoration: "flat") {
            state "off", label: "Email On", action: "emailon", backgroundColor: "#ffffff"
			state "on", label: "Email On", action: "emailon", backgroundColor: "#00a0dc"
    }
        standardTile("emailoff", "device.emailStatus", width: 1, height: 1, decoration: "flat") {
            state "off", label: "Email Off", action: "emailoff", backgroundColor: "#00a0dc"
            state "on", label: "Email Off", action: "emailoff", backgroundColor: "#ffffff"

        }
 	main "switch"
    details (["cameraDetails","take","emailoff","emailon"])
    }
}

def take() {
	hubGet("/tmpfs/auto.jpg", true)
}


def emailon() {
    sendEvent(name: "emailStatus", value: "on");
   	hubGet("/cgi-bin/hi3510/param.cgi?cmd=setmdalarm&-aname=email&-switch=on", false)
    }
    
def emailoff() {
    sendEvent(name: "emailStatus", value: "off");
   	hubGet("/cgi-bin/hi3510/param.cgi?cmd=setmdalarm&-aname=email&-switch=off", false)
   }
   
def on() {
    sendEvent(name: "switch", value: "on");
   	hubGet("/cgi-bin/hi3510/param.cgi?cmd=setmdattr&-enable=1&-name=1", false)
    }
    
def off() {
    sendEvent(name: "switch", value: "off");
   	hubGet("/cgi-bin/hi3510/param.cgi?cmd=setmdattr&-enable=0&-name=1", false)
    }

private hubGet(def apiCommand, def useS3) {
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
	if(useS3) {
		//log.debug "Outputting to S3"
		hubAction.options = [outputMsgToS3:true]
	} else {
		//log.debug "Outputting to local"
		hubAction.options = [outputMsgToS3:false]
	}
	hubAction
}

def parse(String description) {
	//log.debug "Parsing '${description}'"
	def map = stringToMap(description)
	//log.debug map
	def result = []

	if (map.bucket && map.key) {
		putImageInS3(map)
	} else if (map.headers && map.body) {
		if (map.body) {
			def body = new String(map.body.decodeBase64())
			if(body.find("infraredstat=\"auto\"")) {
				log.info("Polled: LED Status Auto")
				sendEvent(name: "ledStatus", value: "auto")
			} else if(body.find("infraredstat=\"open\"")) {
				log.info("Polled: LED Status Open")
				sendEvent(name: "ledStatus", value: "on")
			} else if(body.find("infraredstat=\"close\"")) {
				log.info("Polled: LED Status Close")
				sendEvent(name: "ledStatus", value: "off")
			}
		}
	}
	result
}

def putImageInS3(map) {
	def s3ObjectContent
	try {
		def imageBytes = getS3Object(map.bucket, map.key + ".jpg")
		if(imageBytes) {
			s3ObjectContent = imageBytes.getObjectContent()
			def bytes = new ByteArrayInputStream(s3ObjectContent.bytes)
			//log.debug("PutImageInS3: Storing Image")
			storeImage(getPictureName(), bytes)
		}
	} catch(Exception e) {
		log.error e
	} finally {
		if (s3ObjectContent) {
			s3ObjectContent.close()
		}
	}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	"image" + "_$pictureUuid" + ".jpg"
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	return hexport
}