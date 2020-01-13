# HubitatWizLightDriver v0.01
Hubitat Elevation device handler for Philips Wiz color lights

## What can I do with this driver?
This driver lets the Hubitat control Wiz color bulbs.  It will probably
mostly work with Wiz on/off and variable CT white bulbs as well, but I only own
the color bulbs at this point and I haven't tested the others yet.

## Notes for Beta Version (0.01)

Important Limitations - You can only *control* the light with this driver. It can't yet read status from the bulb,
so if somebody changes settings with the Wiz app, that will not be reflected in the driver's state. Due
to the Hubitat's limited ability to listen for datagrams, this may not be possible without an external
program (currently in progress).

The Wiz protocol allows you to set current state via the LAN, with no cloud involved.  Setting persistent
bulb configuration parameters requires you use the Wiz app and cloud.  The Wiz architecture 
is highly cloud dependent. The bulbs phone home A LOT. If you disconnect from the Internet, the
app gets very cranky and many features just stop working. 

But with this driver, you can set up your bulbs, and ignore the cloud for day-to-day operation.

I'm still working on breaking the app<->cloud protocol, and from some exploration of packet captures during setup,
I think there may be more local controls to be discovered too. These bulbs are capable and inexpensive.  I'd
welcome help figuring out all they can do.  


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


