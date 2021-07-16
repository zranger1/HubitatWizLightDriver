# Change Log for HubitatWizLightDriver 
Hubitat Elevation device handler for Philips Wiz wi-fi color lights

### v1.2.6
Minor code cleanup

### v1.2.5
Performance and stability improvements.

Maker API can now optionally call setEffect with an effect name instead of a number.  For example:
```devices/<device number>/setEffect/Club```

### v1.2.4
Support for dynamic assignment of IP addresses in large installations. Added *setIPAddress*,
*macAddress*. and *ipAddress* for use with the Maker API.  

*setIPAddress* allows an external system to set the bulb's IP address.  Once set, the
new address overrides the user-entered address, and will persist through reboots.  This
allows an external program to monitor the network for address reassignments and adjust
bulb IP addresses on the Hubitat accordingly.

To use, send the setIPAddress command via Maker API in the following format:

```devices/<device number>/setIPAddress/192.168.1.xxx```

The *macAddress* and *ipAddress* attributes are now available to as part of the bulb's device information,
and can be used by external programs to aid in network management. 

These features are enabled by default, but can be disabled from the device page UI.

### v1.2.3
Change to on/off/level message order to improve dimmer behavior
### v1.2.2
Fixed issues with Hubitat color bulb controller tile.  
### v1.2.1
Change to dimmer behavior -- the dimmer now turns the light off when fully dim,
and back on if the bulb is off and the dimmer is set above the minimum.
### v1.1.2 
Enables the use of Wiz Lighting Effects in Hubitat Scenes.  This is way nicer
than it sounds -- the Scenes app does not currently support lighting effects, and
some of the best looking colors on the Wiz bulb are only available as effects.

Now you can set them via the bulb's devices page or with the Wiz app, and capture them with 
the Scenes app on your hub in the normal way.  

Technical note:  I save effect information in a range of color temperatures that
the bulb won't use but that the hub can read and store.  So when you use effects in your scenes,
the bulb's color temperature may display an unusual looking value when you view or edit scenes
settings.

This is normal -- the CT value is 6000+the effect number -- and you can actually use this
to manually change effects when you're editing scenes.  To set effect #1, for example, you'd
set the bulb to CT mode and enter the color temperature 6001. 
