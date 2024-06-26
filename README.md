# OpenAPI Developer Example Application

This is a minimal Android Application to show the usage of the OpenAPI to
connect to the AX Visio. The application uses only a very small feature of the
OpenAPI. This is intentional. The app should only showcase the necessary steps,
UI screens and code to guide the user

* to grant the necessary Android permissions to the app and enable Bluetooth,
* to connect to the AX Visio using the *SOCommOutsideAPI* library and
* to start the OpenAPI Inside Application with the selection wheel

Only after these setup steps are completed, the OpenAPI contexts and other
contexts can be used.

To see all features of the OpenAPI, please see the
[Swarovski Optik OpenAPI Documentation](https://swarovskioptik.github.io/openapi-docu/).

The different activities, screens and user steps are

![Screenshots of applications](screens.png "Screenshots")


# How to use the example code

The code should demonstrate the necessary steps, UI interfaces and user flow to
connect to the AX Visio and to use the OpenAPI. It's intended to be a reference
implementation to look at the code and to copy the relevant parts into your own
application. All the error cases and possibilities are handled. Examples: the
users selects a different app on the selection wheel or the user power down
the AX Visio. In these cases the app switches back to the relevant screen.

The code is already structure to be easily reusable. The common steps to
connect to the AX Visio are factory out into a single
[ConnectActivity](./app/src/main/java/com/example/openapideveloperexampleapp/ConnectActivity.kt).

The code is _not_ a showcase of general Android Application development best
practices. The different screens in the ConnectActivity are shown by simply
calling 'setContentView()'. A real world application is expected to use
Android's
[Navigation Components](https://developer.android.com/guide/navigation/) or
similar libraries.

Furthermore the UI of the example is very basic. For example there are no
transitions between the different screens. This is sometimes confusing because
the transitions happen not based on user input, but on external events, like
losing the connection to the AX Visio. A real world application is expected to
provide a better user experience.

For a good user experience some in-process animations are needed. For example
when the app search for available Fakle devices in reach or when the device
connects to a AX Visio device. Otherwise the user does not know that a operation
is in progress.


# How to build it

Before building the app, you need the OpenAPI API key. It's a JSON web token
that grants your application to specific contexts on the AX Visio device.

After receiving the key from Swarovski Optik, add it to the `local.properties`
file as the constant `OPENAPI_API_KEY`. Example:

    $ cat local.properties
    [...]
    sdk.dir=/home/slengfeld/Android/Sdk
    OPENAPI_API_KEY = ey[...]IA

You can also add it to the user wide `~/.gradle/gradle.properties` file.

Then you can compile the app either with Android Studio or on the command line

    $ ./gradlew app:assembleDebug

If you see the build error

    FAILURE: Build failed with an exception.
    [...]
    * What went wrong:
    Please add 'OPENAPI_API_KEY' property!

the API key is missing. Please add it in the user wide
`~/.gradle/gradle.properties` or the project  local `local.properties` file.


# Open Issues

UI/UX can be improved with better UI elements and transitions only on user
feedback.

Factor out string constants from the layout XML to resource files.
