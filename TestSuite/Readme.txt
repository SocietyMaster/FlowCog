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

InstanceStateCallback:
The activity contains two callback methods related to the instanceState which contain a dataflow from source to sink

IntentSink:
Taint is propagated via Intent. As other apps are not trustworthy by default, this is a sink, too.

LocaionLeak:
This example contains a location information leakage in the onResume() callback method.
 
The callback method onLocationChanged
 must be identified and should be classified as source.

LocationLeakSimple:
A simplified version of LocationLeak in which the activity directly implements the
onLocationChanged interface instead of using an inner class for the job.

PasswordField:
A value from a password field is stored in the log.

Lifecycle2:
A taint value is written in the onResume() callback method and propagated to a sink in the onPause() callback method.
Loops must be considered to find this taint.

ServiceLifecycle:
The app contains a Service with three callback methods. One of them contains a source, another one contains a sink
which receives value originated from the source as input parameter.
