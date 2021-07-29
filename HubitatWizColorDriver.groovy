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
 *  File: HubitatWizLightDriver
 *  Platform: Hubitat
 *
 *  Allows hubitat to control a wiz color bulb
 *
 *  Requirements:
 *    A provisioned (with the Wiz app) Wiz color bulb on the local LAN. 
 *
 *    Date        Ver    What
 *    ----        ---    ----
 *    2020-1-12   0.1    Created
 *    2020-3-08   1.0    Added status requester, update to 1.0
 *    2020-3-13   1.01   Added duration to setLevel command to make RM happy
 *    2020-7-21   1.1.1  Use new Hub feature to fix unwanted logging of UDP timeouts.
 *    2020-10-26  1.1.2  Enable use of Wiz lighting effects in HE Scenes
 *    2020-12-05  1.1.3  Hubitat Package Manager support
 *    2020-12-07  1.2.1  Change dimmer behavior to allow on/off switching
 *    2020-12-09  1.2.2  Fix issue #2 - Hubitat UI requires valid RGB even in ct mode
 *    2020-12-22  1.2.3  Change on/off/level message order to improve dimmer behavior 
 *    2021-01-13  1.2.4  Add setIPAddress/macAddress to support large installations 
 *    2021-01-17  1.2.5  Performance & stability improvements
 *    2021-02-02  1.2.6  Remove unused code in definition 
 *    2021-07-16  1.2.7  Support new 3 parameter setColorTemperature capability
 *    2021-07-19  1.2.8  Make sure polling restarts after Hub reboot
 *    2021-07-23  1.2.9  Stop rssi from showing up as state change in event log
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

@Field static Map lightEffects = [
     0:"none",
     1:"Ocean",
     2:"Romance",
     3:"Sunset",
     4:"Party",
     5:"Fireplace",
     6:"Cozy",
     7:"Forest",
     8:"Pastel Colors",
     9:"Wake-up",
    10:"Bedtime",
    11:"Warm White",
    12:"Daylight",
    13:"Cool White",
    14:"Night Light",
    15:"Focus",
    16:"Relax",
    17:"True Colors",
    18:"TV Time",
    19:"Plant Growth",
    20:"Spring",
    21:"Summer",
    22:"Fall",
    23:"Deep Dive",
    24:"Jungle",
    25:"Mojito",
    26:"Club",
    27:"Christmas",
    28:"Halloween",
    29:"Candlelight",
    30:"Golden White",
    31:"Pulse",
    32:"Steampunk"
]
 
def version() {"1.2.9"}
def commandPort() { "38899" }
def unknownString() { "none" }
def statusPort()  { "38899" } 
def commandDelayMs() { 100 }

metadata {
    definition (
        name: "Wiz Color Light",
        namespace: "ZRanger1",
        author: "ZRanger1(JEM)",
        importUrl: "https://raw.githubusercontent.com/zranger1/HubitatWizLightDriver/master/HubitatWizColorDriver.groovy") {        
        capability "Actuator"
        capability "Initialize"        
        capability "SignalStrength"  
        capability "LightEffects"
        capability "Switch"
        capability "SwitchLevel"
        capability "Refresh"
        capability "ColorControl"
        capability "ColorTemperature"
        capability "ColorMode"     
        
        command    "pulse",[[name:"Delta*", type: "NUMBER",,description: "Change in intensity, positive or negative"],
                            [name:"Duration*", type: "NUMBER", description: "Duration in milliseconds"]]                           
        command    "setEffectSpeed", [[name: "Effect speed*", type: "NUMBER", description: "(0 to 200)"]]    
        command    "setIPAddress",["string"]
        
        attribute  "effectNumber","number"
        attribute  "effectSpeed", "number"         
        attribute  "macAddress","string"
        attribute  "ipAddress","string"
    }
}

preferences {    
    input("ip", "text", title: "IP Address", description: "IP address of Wiz light", required: true)
    input name: "pollingInterval", type: "number", title: "Time (seconds) between light status checks", defaultValue: 6    
    input name: "makerIPEnable", type: "bool", title: "Allow Maker API to set IP address", defaultValue: true
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
    log.info "Wiz Color Light installed. Version${version()}."       
    initialize()
}

def updated() {
    log.info "Wiz Color Light updated. Version ${version()}."
    initialize()
}
 
def initialize() {
    logDebug("initialize")
 
    unschedule()
    state.version = version()
    
// Build a reasonable default color temp setting to use if
// the bulb is unavailable at initialization time.
    val = device.currentValue("colorMode")
    if (val == null) val = "CT"    
    state.lastMode = val    
    
    val = device.currentValue("colorTemperature")
    val =  (val == null) ? val = 3000 : val.toInteger();   
    state.lastTemp = val; 
         
// initialize parameters for all bulb modes so the Hub 
// dashboard will work correctly    
    eff = new groovy.json.JsonBuilder(lightEffects)    
    sendEvent(name:"lightEffects",value: eff)   

    sendEvent([name: "hue", value: 0])  
    sendEvent([name: "level", value: 100])  
    sendEvent([name: "saturation", value: 100])  
    
// save IP address in a place that's accessible to Maker API        
    sendEvent([name: "ipAddress", value: ip]) 

    device.updateDataValue("lastCommand","0") 
    runIn(pollingInterval, getCurrentStatus)     
}

def refresh() {
  logDebug("refresh")
  getCurrentStatus(false)
}

/**
 * Network infrastructure
 */
def getIPString() {
   return ip+":"+commandPort()
} 
 
def setLastCommandTime() {
    device.updateDataValue("lastCommand",Long.toString(now()))       
}

def getLastCommandInterval() {
    s = device.getDataValue("lastCommand")
    t = (s == null) ? 0 : Long.valueOf(s)
    return now() - t
} 
 
def sendCommand(String cmd) {
  def addr = getIPString();

  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     callback: parse,
                     parseWarning: true,
                     destinationAddress: addr])  
  try {    
    logDebug("sendCommand: ${cmd} to ip ${addr}")
    sendHubCommand(pkt) 
    setLastCommandTime()    
  }
  catch (Exception e) {      
      logDebug e
  }      
}

def getCurrentStatus(resched=true) {                             
  if (ip != null) {
    String cmd = WizCommandBuilder("getPilot",15,[" "])
    sendCommand(cmd);
  } else {
    logDebug("getCurrentStatus: ip address not set")   
  }
  
  if (resched) {
    runIn(pollingInterval, getCurrentStatus)       
  }  
}

def WizCommandBuilder(methodIn, idIn, paramsIn) {   
  String cmd = JsonOutput.toJson(["method":methodIn,"id":idIn,"params":paramsIn])
  return cmd
}	

def WizCommandSet(paramsIn,delay = 0) {
// rate throttling -- see if we need to introduce a short
// delay before sending the next command.
//     
  def t = getLastCommandInterval()
  if (t < delay) {
     t = 1+delay - t
     pauseExecution(t)
  } 
    
// send UDP command string to bulb    
  sendCommand(WizCommandBuilder("setPilot",13,paramsIn))
  setLastCommandTime()
}

// NOTE: These items are in priority order.  Be careful about changing it
def parseLightParams(params) {
    lev = device.currentValue("level")
    useCT = true

    if (params.containsKey("state")) {    
      sendEvent([name: "switch", value: params.state ? "on" : "off"])       
    }   
    if (params.containsKey("dimming")) {
      if ((params.dimming <= 10) && (!params.state)) {
        lev = 0;
      } else {
        lev = params.dimming  
      }
      sendEvent([name: "level", value: lev])
    }
    if (params.containsKey("r")) {
      hsv = RGBtoHSVMap([params.r,params.g,params.b])
      hsv.level = lev
      updateCurrentStatus(hsv,null,null, true)           
    }    
    if (params.containsKey("sceneId")) {
      if (params.sceneId > 0) useCT = false
      updateCurrentStatus(null,null,params.sceneId, true)      
    }    
    if (useCT && params.containsKey("temp")) {
      updateCurrentStatus(null,params.temp,null, true)
    }
    if (params.containsKey("speed")) {
      sendEvent([name: "effectSpeed", value: params.speed])
    }  

    if (params.containsKey("rssi") && logEnable) { 
      sendEvent([name:"rssi", value: params.rssi, isStateChange: false])
    } 
    if (params.containsKey("mac")) {
      sendEvent([name:"macAddress", value: params.mac])
    }   
}

// handle command responses & status updates from bulb 
def parse(String description) {

// ignore UDP timeout errors from the hub
  def i = description.indexOf("UDPCLIENT_ERROR")
  if (i != -1) { return }                                                                                             
  
// is it a valid packet from the light?
  i = description.indexOf("payload")
  if (i == -1) {
    logDebug("parse: unknown datagram. Ignored.")
    return
  }  
  
  payload = new String(hubitat.helper.HexUtils.hexStringToByteArray(description.substring(i+8))) 
  logDebug("parse: ${payload}") 
   
  json = null
   
  try {
    json = new groovy.json.JsonSlurper().parseText(payload)
      if (json == null){
        logDebug "parse: JsonSlurper returned null"
        return
      }   
      
    i = payload.indexOf("getPilot")
    if (i != -1) {  
      parseLightParams(json.result)  // status request packet
    }
    else if (json.containsKey("params")) {
      parseLightParams(json.params)  // command result packet
    }
    else if (json.containsKey("setPilot")) {
       // received ack from command.  Uncomment below if needed for debugging
       // logDebug("parse: setPilot response logged");
    }
    else {
      ; // logDebug("parse: Unhandled packet. Ignored.")
    }             
  }
  catch(e) {
    log.error("parse Exception: ",e)
    log.error payload
    return
  }    
}

/**
 * Command handlers and associated helper functions
 */
 
// Switch commands 
def on() {         
  WizCommandSet(["state":true])
  sendEvent([name: "switch", value: "on"]) 

  lev = device.currentValue("level") 
  if (lev < 10) {
    // introduce short delay after "on" command so the
    // bulb doesn't drop packets
    WizCommandSet(["dimming":10],commandDelayMs())
    sendEvent([name: "level", value: 10])      
  }
}

def off() {
  WizCommandSet(["state":false])
  sendEvent([name: "switch", value: "off"]) 
}

// light switch management helper. Changes the switch state if 
// necessary.  Returns the previous switch state true = on, false = off
def setSwitchState(state) {
  sw = (device.currentValue("switch") == "on")
    
// if not in desired state, set switch to new state     
  if (sw != state) {
    if (state) {
      on() 
    } else {
      off()
    }
  }   
// return previous state
  return sw
}

// ColorControl commands

// sends all the events necessary to keep the light's state
// in the hub coherent.
def updateCurrentStatus(hsv,ct,effectNo,inParse = false) {

// directly setting a color
  if (hsv != null) {
//    logDebug("updateCurrentStatus - set color ${hsv}")
    sendEvent([name: "hue", value: hsv.hue])  
    sendEvent([name: "level", value: hsv.level ])  
    sendEvent([name: "saturation", value: hsv.saturation])    
    
    sendEvent([name: "colorMode", value: "RGB"])
    sendEvent([name: "effectNumber", value: 0])
    sendEvent([name: "effectName", value: unknownString()])      
    
    state.lastMode = "RGB"
    setGenericColorName(hsv)     
  }
// setting color temperature  
  else if (ct != null) {
//    logDebug("updateCurrentStatus - set temp ${ct}")     
    sendEvent([name: "colorMode", value: "CT"])
    sendEvent([name: "colorTemperature", value: ct])
    sendEvent([name: "effectNumber", value: 0])
    sendEvent([name: "effectName", value: unknownString()])       
  
    state.lastMode = "CT"
    state.lastTemp = ct
    setGenericTempName(ct)  
  }

// setting a lighting effect
  else if (effectNo != null) {
    name = lightEffects[effectNo.toInteger()]
    sendEvent([name:"effectNumber", value:effectNo])
    sendEvent([name:"effectName", value:name]) 
    
// if we're setting effect to 0 - disabling it, we want
// to restore the previous mode and color.    
    if (effectNo == 0) {
      if (inParse) return  // if responding to msg, do nothing
      
      mode = device.currentValue("colorMode")
      if (mode == null) return
      
      if (mode.startsWith("CT")) {
        setColorTemperature(getDeviceColorTemp())
      } 
      else if (mode.startsWith("RGB")){
        def color = getDeviceColor()      
        setColor(color)
      }  
      else {
        setColorTemperature(3000) // warm white if no mode set
      }  
    }

// experimental -- encode effect in color temp for Hubitat scenes
    else { 
      ct = effectNo+6000;    
      sendEvent([name: "colorMode", value: "CT"])        
      sendEvent([name: "colorTemperature", value: ct])  
      setGenericTempName(ct)        
    }
  }
}

def setColor(hsv) {
    def rgb
    
    logDebug("setColor(${hsv})")   
    try {
      rgb = hubitat.helper.ColorUtils.hsvToRGB([hsv.hue, hsv.saturation, hsv.level])
    }
    catch (e) {
      logDebug("Attempt to set RGB color w/NaN value - ignoring.")
      return
    }
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
def setColorTemperatureWorker(ct) {
  logDebug("setColorTemperature(${ct})")
  
// Experimental -- valid color temp range is 2500-6000k
// We use CT 6001 - 6032 to allow Hubitat's scene's app to read
// and set effects. 

// if it's an effect code, set the current effect, then set the
// (now fake) color temp to the coded value and return.
  if ((ct > 6000) && (ct <= 6032)) { 
    setEffect(ct-6000);
    return;
  }

// otherwise restrict color temp to the bulb's range of
// 2500-6000K
  if (ct < 2500) ct = 2500;
  else if (ct > 6000) ct = 6000;
 
 // otherwise, it's a valid color temp, so we do the normal thing.
  WizCommandSet(["temp":ct])   
  updateCurrentStatus(null,ct,null)  
}

// SwitchLevel command
// Set brightness, turn bulb off if fully dimmed, turn it on
// if it's off and the dimmer is changed to > 0
// NOTE - Wiz color bulb does not support levels less than 10, and
// the duration argument is not currently supported
def setLevel(BigDecimal lev,BigDecimal duration=0)  {
  
// if trying to dim below 10%, set the bulb to full dim and
// turn it off  
  if (lev < 10) {
    WizCommandSet(["dimming":10])
    sendEvent([name: "level", value: 0]) 
    setSwitchState(false)    
  }
 
// otherwise, set the specified level, checking first to see
// that the bulb is on (otherwise it won't listen to dimming commands)
  else {
    // turn on light if necessary, adding 
    // a short delay after "on" command so the
    // bulb doesn't drop packets  
    delay = (setSwitchState(true)) ? 0 : commandDelayMs()   
    WizCommandSet(["dimming":lev],delay)
    sendEvent([name: "level", value: lev])      
  }
}

// new 3 parameter form for firmware v2.26
def setColorTemperature(ct,level = null,transitionTime = null) {
  if (level != null) setLevel(level,transitionTime);
  setColorTemperatureWorker(ct);
}

// LightEffects commands
def setEffect(String effectName){
   def id = lightEffects.find{ it.value == effectName }
   if (id) setEffect(id.key)
}

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

// maker API can call this to set IP address, enabling mDNS
// resolution of bulb mac vs. address on remote system.
def setIPAddress(String addr) {
   if (makerIPEnable) {
       logDebug("setIPAdress(${addr})")   
       device.updateSetting("ip",addr);
       sendEvent([name: "ipAddress", value: addr])  
   }   
   else {
     logDebug("setIPAddress: request from Maker API denied.");
   }
}

// Additional color helper functions 

def RGBtoHSVMap(rgb) {
    def hsvList = hubitat.helper.ColorUtils.rgbToHSV(rgb)   
    return [hue:hsvList[0], saturation:hsvList[1], level: hsvList[2]]
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
      else if (value <= 6000)  name = "Electronic"
      else name = unknownString();
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
