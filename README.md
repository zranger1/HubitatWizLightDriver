# HubitatWizLightDriver 
Hubitat Elevation device handler for Philips Wiz wi-fi color lights

## What can I do with this driver?
This driver lets the Hubitat control Wiz color bulbs.  It will probably
mostly work with Wiz on/off and variable CT white bulbs as well, but I only own
the color bulbs at this point and I haven't tested other types.

## What's New - v1.2.8
Support for the new 3 parameter setColorTemperature capability added
in Hubitat firmware v2.2.6

Fix to make sure polling restarts properly after Hub reboot.

## Previously...
For information on previous versions, see CHANGELOG.md in this repository.

## To Use
Install and provision your Wiz bulb using the phone app.  Note the bulb's IP address.

On your Hubitat Elevation's admin page, select "Drivers Code", then click the
"New Driver" button.  Paste the Groovy driver code from this repository into 
the editor window and click the "SAVE" button.

Create a new virtual device on your Hubitat, name and label it, and select 
"Wiz Color Light" as the device type.  Save your new device.

Click the new device on the Hubitat's "Devices" page, and enter your light's
IP address in the provided field.  That's it -- you can now send commands to
your Wiz light over the LAN.   

## Advanced Features
This driver includes hooks that can be used with an external program for dynamic assignment
of IP addresses in large installations.

*setIPAddress* allows an external system to set the bulb's IP address.  Once set, the
new address overrides the user-entered address, and will persist through reboots.  This
allows an external program to monitor the network for address reassignments and adjust
bulb IP addresses on the Hubitat accordingly.

To use, send the setIPAddress command via Maker API in the following format:

```devices/<device number>/setIPAddress/192.168.1.xxx```

The *macAddress* and *ipAddress* attributes are now available to as part of the bulb's device information,
and can be used by external programs to aid in network management. 

These features are enabled by default, but can be disabled from the device page UI.

## Donation
If this project saves you time and effort, please consider donating to help support further development.  Every donut or cup of coffee helps!  :-)

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate/?hosted_button_id=YM9DKUT5V34G8)

## Credit where credit is due
I got protocol and hardware information, and inspiration from the following:

http://blog.dammitly.net/2019/10/cheap-hackable-wifi-light-bulbs-or-iot.html

https://limitedresults.com/2019/02/pwn-the-wiz-connected/

The OpenHab team, particularly...
https://github.com/SRGDamia1/openhab2-addons/tree/master/bundles/org.openhab.binding.wizlighting/src/main/java/org/openhab/binding/wizlighting

https://www.wireshark.org/

And of course, the Hubitat dev community! I read through a ton of everyone's source looking for and
usually finding solutions to LAN device questions.  Especially useful were...
https://github.com/robheyes/lifxcode

https://github.com/muxa/hubitat/blob/master/drivers/wled-light.groovy

https://github.com/markus-li/Hubitat/tree/master/drivers


