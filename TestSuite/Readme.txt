Android application testsuite for taint analysis

Each of the included .apk-files either contains a flow from a source to a sink
or does not disclose information and can be used to check your analysis for false positives.
The apps are very lightwight and concentrate on one small scenario, in most cases there is no UI.



ButtonXml:
The IMEI is read out and after a click on a button it is sent via SMS (do not run this app on an actual decive).
The method which is called is referenced in the layout-xml.

EasyLifecycle:
A taint value is written in one lifecycle method and propagated to a sink in another lifecycle method.

InheritedActivities:
The activity has a superclass which defines a lifecycle method containing a sink.
The superclass is not defined in the manifest.

IntentSink:
Taint is propagated via Intent. As other apps are not trustworthy by default, this is a sink, too.

LocaionLeak:
This example contains a location information leakage in the onResume() callback method.
 
The callback method onLocationChanged
 must be identified and should be classified as source.
