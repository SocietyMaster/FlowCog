Android application testsuite for taint analysis

Each of the included .apk-files either contains a flow from a source to a sink
or does not disclose information and can be used to check your analysis for false positives.
The apps are very lightwight and concentrate on one small scenario, in most cases there is no UI.

AnonymousClass:
Contains a location listener that is defined as an anonymous class. It contains the onLocationChanged() method. That method 
sets the sources (longitude and latitude) and the onResume() callback contains the sink (Log.i)

ArrayAccess:
A tainted value is stored in an array. The value of a different array position is propagated to a sink.
Negative testcase, there should be no flow from source to sink.

ArrayAccess2:
This app contains an access to a field in an array that is not tainted. This field is send via sms (no data leakage). The important
aspect here is that the index of the array is not a constant, it is created via a method (some arithmetic operations).

BroadcastReceiverLifecycle:
A source and a sink are called in the lifecycle method of a broadcastreceiver.

ButtonXml:
The IMEI is read out and after a click on a button it is sent via SMS (do not run this app on an actual decive).
The method which is called is referenced in the layout-xml.

CompleteTest:
Contains various difficulties for static analysis, essentially a summary of some of the other testcases.

DirectLeak:
Reads out the subscriber id and sends it via SMS in the activity's onCreate() event. Basic test case.

EasyLifecycle:
A taint value is written in one lifecycle method and propagated to a sink in another lifecycle method.

FieldSensitivity1:
A dataobject has two fields - a tainted value is written to one field, the value of the other field is propagated
to a sink. Negative testcase, there should be no flow from source to sink.

FieldSensitivity2:
A dataobject has two fields - a tainted value is written to one field, the value of the other field is propagated
to a sink in a different lifecycle method. Negative testcase, there should be no flow from source to sink.

FieldSensitivity2Pos:
Similar to FieldSensitivity, but with information flow: A dataobject has two fields - a tainted value is written to 
one field, the value of the this field is propagated to a sink in a different lifecycle method.

IndirectFlow1:
App contains an indirect flow.

IndirectFlow2:
App contains an indirect flow.

IndirectFlow3:
App contains an indirect flow.

IndirectFlow4:
App contains an indirect flow.

InheritedActivities:
The activity has a superclass which defines a lifecycle method containing a sink.
The superclass is not defined in the manifest.

InheritedObjects:
This activity uses inheritance and a conditional statement to create an informaiton flow from a source to a sink.

InstanceStateCallback:
The activity contains two callback methods related to the instanceState which contain a dataflow from source to sink

IntentSink:
Taint is propagated via Intent. As other apps are not trustworthy by default, this is a sink, too.

IntentSink2:
Taint is sent via intent to another app. The taint is created and sent in the sendIntent callback (layout-XML) method.

Lifecycle2:
A taint value is written in the onResume() callback method and propagated to a sink in the onPause() callback method.
Loops must be considered to find this taint.

ListenerSpecific:
This app contains the following difficulties:
	- Ability to check Listeners
	- Ability to know that the callback of button3 is clickOnButton3 (XML-file)
	- There is only one data leak iff first button3 and then button1 is pressed!

ListAccess:
A tainted value is stored in a list. The value of a different list position is propagated to a sink.
Negative testcase, there should be no flow from source to sink.

LocaionLeak:
This example contains a location information leakage in the onResume() callback method.

LogNoLeak:
This app does not contain any leaking data.

LoopExample1:
This example contains a simple loop and a data leakage.

LoopExample2:
This example contains a nested loop-if-loop construct and a data leakage:
 
The callback method onLocationChanged
 must be identified and should be classified as source.

LocationLeakSimple:
A simplified version of LocationLeak in which the activity directly implements the
onLocationChanged interface instead of using an inner class for the job.

MethodOverrides:
This is a special-case where a system method gets overwriden (check for correct implementation of lifecycle)

ObjectSensitivity1:
A tainted value is written to one object, the value of a different object (of the same type) is propagated to a sink.
Negative testcase, there should be no flow from source to sink.

OverwriteValue:
A tainted value is stored in a local variable and a field, but overwritten before they reach a sink.
Negative testcase, there should be no flow from source to sink.

PasswordField:
A value from a password field is stored in the log.

ServiceLifecycle:
The app contains a Service with three callback methods. One of them contains a source, another one contains a sink
which receives value originated from the source as input parameter.

SourceCodeSpecific:
This app contains a Set<String> and the conditional operator "( ) ? :".

StaticFieldActivities:
A static field gets tainted from a different activity. Both lifecycles have to be analyzed and combined to find 
this information flow.

StaticInitialization:
The sink is called in a static initialization method of a static class.

UnreachableCode:
This app contains a method call that contains a data leakage but is never called.