/**
 *
 *  File: HubitatWizLightDriver
 *  Platform: Hubitat
 *
 *  Allows hubitat to control a wiz color bulb
 *
 *  Requirements:
 *    A provisioned (with the Wiz app) Wiz color bulb on the local LAN. 
 *
 *    Date        Ver           Who       What
 *    ----        ---           ---       ----
 *    2020-1-12   0.1           JEM       Created
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Constants and configuration data
 */
import groovy.transform.Field

@Field static List lightEffects = [
    "none",
    "Ocean",
    "Romance",
    "Sunset",
    "Party",
    "Fireplace",
    "Cozy",
    "Forest",
    "Pastel Colors",
    "Wake-up",
    "Bedtime",
    "Warm White",
    "Daylight",
    "Cool White",
    "Night Light",
    "Focus",
    "Relax",
    "True Colors",
    "TV Time",
    "Plant Growth",
    "Spring",
    "Summer",
    "Fall",
    "Deep Dive",
    "Jungle",
    "Mojito",
    "Club",
    "Christmas",
    "Halloween",
    "Candlelight",
    "Golden White",
    "Pulse",
    "Steampunk"
]
 
def version() {"v0.01"}
def commandPort() { "38899" }
def unknownString() { "none" }
def statusPort()  { "38900" }  
def statusPollInterval() { 15 }  // seconds

metadata {
    definition (name: "Wiz Color Light", namespace: "jem", author: "JEM",importUrl: "") {
        capability "Actuator"
        capability "SignalStrength"  
        capability "LightEffects"
        capability "Switch"
        capability "SwitchLevel"
        capability "ColorControl"
        capability "ColorTemperature"
        capability "ColorMode"     
        
        command    "pulse",[[name:"Delta*", type: "NUMBER",,description: "Change in intensity, positive or negative",constraints:[]],
                            [name:"Duration*", type: "NUMBER", description: "Duration in milliseconds", constraints:[]]]                           
        command    "setEffectSpeed", [[name: "Effect speed*", type: "NUMBER", description: "(0 to 200)", constraints:[]]]        
        
        attribute  "effectNumber","number"
        attribute  "effectSpeed", "number" 
    }
}

preferences {
    input("ip", "text", title: "IP Address", description: "IP address of Wiz light", required: true)
//    input name: "remoteStateEnable", type: "bool", title: "Use remote status server", defaultValue: false
//    input("ipRemote", "text", title: "Status Server IP Address", description: "IP address of status server")
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false        
}

/**
 * helper methods for logging
 */
def logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def logDebug(String str) {
  if (logEnable) { log.debug(str) }
}

/**
 * initialization & configuration
 */ 
def installed(){
    log.info "Wiz color light handler ${version()} installed."       
    initialize()
}

def updated() {
    log.info "Wiz color light updated. ${version()}."
    initialize()
}
 
def initialize() {

    logDebug("initialize")
 
    unschedule()
    state.version = version()
    
    eff = new groovy.json.JsonBuilder(lightEffects)    
    sendEvent(name:"lightEffects",value: eff)   

    sendEvent([name: "hue", value: 0])  
    sendEvent([name: "level", value: 100])  
    sendEvent([name: "saturation", value: 100])      
  
// TBD - enable remote listener support when the server is done.  
//    if (remoteStateEnable) runIn(statusPollInterval, getCurrentState)     
}

def refresh() {
  logDebug("refresh")
  initialize()
}

/**
 * Network infrastructure
 */
 
def sendCommand(String cmd) {
  def addr = ip+":"+commandPort()
    
  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     destinationAddress: addr])  
  try {    
    logDebug("sendCommand: ${cmd} to ip ${addr}")
    sendHubCommand(pkt)                   
  }
  catch (Exception e) {      
      logDebug e
  }      
}

// Wiz bulbs require applications to register for status updates, which
// are then sent at about 5 second intervals for approximately the next
// 20 seconds.  At that point, the app must re-register.  Awesome for
// phones, but not so nice for Hubitat, which doesn't have a general
// purpose UDP listener. For the time being, I have punted this
// functionality to a python program which returns all that is
// known about a light's status in response to a UDP request.  
// 
def getCurrentStatus() {

  if (!remoteStateEnable) return
  
  logDebug("getCurrentStatus")
  
  def addr = ipRemote+":"+statusPort()
  String cmd = JsonOutput.toJson(["lightIP":ip,"method":"status"])
    
  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     destinationAddress: addr],
                     ignoreWarning     : true,
                     callback          : "parse")                     
  try {    
    logDebug("getCurrentStatus: ${cmd} to ip ${addr}")
    sendHubCommand(pkt)                   
  }
  catch (Exception e) {      
      logDebug e
  }         
  
  runIn(statusPollInterval(),getCurrentState)  
}

def WizCommandBuilder(methodIn, idIn, paramsIn) {   
  String cmd = JsonOutput.toJson(["method":methodIn,"id":idIn,"params":paramsIn])
  return cmd
}	

def WizCommandSet(paramsIn) {
  sendCommand(WizCommandBuilder("setPilot",13,paramsIn))
}

// handle command responses & status updates from bulb 
def parse(String description) {
   
  i = description.indexOf("payload:")
    if (i == -1) {
        logDebug("Received packet with no payload")
        return
  }
    
  payload = new String(hubitat.helper.HexUtils.hexStringToByteArray(description.substring(i+8)))

  logDebug("payload: ${payload}")
  
 // TBD -- not yet implemented.  Will decode payload to json and update device
 // handler state with light status.
}

/**
 * Command handlers and associated helper functions
 */
 
// Switch commands 
def on() {         
  WizCommandSet(["state":true])
  sendEvent([name: "switch", value: "on"])   
}

def off() {
  WizCommandSet(["state":false])
  sendEvent([name: "switch", value: "off"]) 
}

// ColorControl commands

// sends all the events necessary to keep the light's state
// in the hub coherent.
def updateCurrentStatus(hsv,ct,effectNo) {

// directly setting a color
  if (hsv != null) {
    logDebug("updateCurrentStatus - set color ${hsv}")
    sendEvent([name: "hue", value: hsv.hue])  
    sendEvent([name: "level", value: hsv.level ])  
    sendEvent([name: "saturation", value: hsv.saturation])    
    
    sendEvent([name: "colorMode", value: "RGB"])
    sendEvent([name: "colorTemperature", value: unknownString()])
    sendEvent([name: "effectNumber", value: 0])
    sendEvent([name: "effectName", value: unknownString()])      
    
    setGenericColorName(hsv)     
  }
// setting color temperature  
  else if (ct != null) {
    logDebug("updateCurrentStatus - set temp ${ct}")  
    sendEvent([name: "hue", value: unknownString()])  
    sendEvent([name: "saturation", value: unknownString()])    
    
    sendEvent([name: "colorMode", value: "CT"])
    sendEvent([name: "colorTemperature", value: ct])
    sendEvent([name: "effectNumber", value: 0])
    sendEvent([name: "effectName", value: unknownString()])       
  
    setGenericTempName(ct)  
  }

// setting a lighting effect
  else if (effectNo != null) {
    name = lightEffects[effectNo.toInteger()]
    sendEvent([name:"effectNumber", value:effectNo])
    sendEvent([name:"effectName", value:name]) 

// if we're setting effect to <none>, restore the previously
// set mode and color.  Do nothing if no previous mode
    if (effectNo == 0) {
      mode = device.currentValue("colorMode")
      if (mode == null) return
      
      if (mode.startsWith("CT")) {
        setColorTemperature(getDeviceColorTemp())
      } 
      else if (mode.startsWith("RGB")){
        def color = getDeviceColor()
       
        setColor(color)
      }

// if no mode defined, go with a nice normal light bulb-ish color
      else {
        setColorTemperature(3000) // warm white
      }      
    }  
  }
}

def setColor(hsv) {
    logDebug("setColor(${hsv})")   
     
    def rgb = hubitat.helper.ColorUtils.hsvToRGB([hsv.hue, hsv.saturation, hsv.level])
    WizCommandSet(["r":rgb[0],"g":rgb[1],"b":rgb[2]]) 
    
    updateCurrentStatus(hsv,null,null)
}

def setHue(value) {
    hsv = getDeviceColor()
    setColor([hue: value, saturation: hsv.saturation, level: hsv.level])
}

def setSaturation(value) {
    hsv = getDeviceColor()
    setColor([hue: hsv.hue, saturation: value, level: hsv.level])
}

// ColorTemperature & ColorMode commands
def setColorTemperature(ct) {
  logDebug("setColorTemperature(${ct})")
  ct = validateCT(ct)
  
  WizCommandSet(["temp":ct])   
  updateCurrentStatus(null,ct,null)  
}

// SwitchLevel commands
def setLevel(lev) {
  WizCommandSet(["dimming":lev])
  sendEvent([name: "level", value: lev]) 
}

// LightEffects commands
def  setEffect(effectNo) {
  logDebug("setEffect to ${effectNo}")
  
  WizCommandSet(["sceneId":effectNo])
  updateCurrentStatus(null,null,effectNo)  
}

def setNextEffect() {
  logDebug("setNextEffect")
  
  i = device.currentValue("effectNumber")
  i = (i == null) ? 1 : i + 1
  if (i >= lightEffects.size()) i = 1
  
  setEffect(i) 
}
      
def setPreviousEffect() {
  logDebug("setPreviousEffect")
  
  maxIndex = lightEffects.size - 1
  
  i = device.currentValue("effectNumber")
  i = (i == null) ? maxIndex : i - 1
  if (i < 1) i = maxIndex
  
  setEffect(i)  
}

// other commands

// pulse the light once, over a period of <millis> milliseconds,
// varying intensity by <intensity>, which can be positive or negative 
// 
def pulse(BigDecimal intensity, BigDecimal millis) {
  pkt = WizCommandBuilder("pulse",7,["delta": intensity, "duration": millis])
  sendCommand(pkt)      
}

def setEffectSpeed(BigDecimal speed) {
  WizCommandSet(["speed":speed])
  sendEvent([name: "effectSpeed", value: speed])   
}

// Additional color helper functions 

def validateCT(ct) {
// restrict range and make sure it's a valid number

  try {
    if (ct < 1000) ct = 1000
    else if (ct > 6000) ct = 6000     
  }
  catch (Exception e) {      
      ct = 2500
  }

  return ct    
}

def getDeviceColor() {
  hsv = [hue: device.currentValue("hue"),
         saturation: device.currentValue("saturation"),
         level: device.currentValue("level")]
        
  return hsv
}

def getDeviceColorTemp() {
  ct = device.currentValue("colorTemperature")
  
  return ct
}

def setGenericTempName(temp){
    def name = "(not set)"

    if (temp) {   
      def value = temp.toInteger()
    
      if (value < 2001) name = "Sodium"
      else if (value < 2101)  name = "Starlight"
      else if (value < 2400)  name = "Sunrise"
      else if (value < 2800)  name = "Incandescent"
      else if (value < 3300)  name = "Soft White"
      else if (value < 3500)  name = "Warm White"
      else if (value < 4150)  name = "Moonlight"
      else if (value < 5001)  name = "Horizon"
      else if (value < 5500)  name = "Daylight"
      else if (value < 6000)  name = "Electronic"
      else if (value < 6501)  name = "Skylight"
      else if (value < 20000) name = "Polar"
    }  
    sendEvent(name: "colorName", value: name)  
}

// A very rough approximation, based on empirical observation
// of the Wiz A19 bulb on a white background. The bulb's color is
// a little erratic at the lowest brightness levels.
def setGenericColorName(hsv){
    def colorName = "(not set)"
    
    if (hsv.saturation < 17) {
      colorName = "White"
    }
    else {
      switch (hsv.hue.toInteger()){
          case 0..2: colorName = "Red"
              break
          case 3..6: colorName = "Orange"
              break
          case 7..10: colorName = "Yellow"
              break
          case 11..13: colorName = "Chartreuse"
              break
          case 14..34: colorName = "Green"
              break
          case 35..68: colorName = "Blue"
              break
          case 69..73: colorName = "Violet"
              break
          case 74..83: colorName = "Magenta"
              break
          case 84..98: colorName = "Pink"
              break
          case 99..100: colorName = "Red"
              break
        }
    }
    sendEvent(name: "colorName", value: colorName)
}



