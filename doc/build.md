#Build 
How to build the application yourself.

## Edit sources

The project relies on the Lombok code generation. Install the corresponding plugin in your IDE to edit sources with valid syntax

## Launch build and unit tests

Retrieve the sources, and execute the following command in the root directory of the project:

``` 

./gradlew build
# behind a proxy
# ./gradlew -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128 build
```

or 

`
./gradlew.bat build
`



according to your environment.

Generated war will be available in the *spring-apps/autosleep-core/build/libs/* and *spring-apps/autowakeup-proxy/build/libs* directories.

