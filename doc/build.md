#Build 
How to build the application yourself.

## Edit sources

The project relies on the Lombok code generation. Install the corresponding plugin in your IDE to edit sources with valid syntax.

Note that for intellij, the lambok plugin requires to [enable to code preprocessor](http://stackoverflow.com/questions/9424364/cant-compile-project-when-im-using-lombok-under-intellij-idea)

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

## Launch integration tests

For running mysql tests locally on ubuntu, check .travis.yml ``before_script`` and ``common/src/test/resources/application.properties`` for up-to-date source of truth prereqs on the DB.   

``` 
$ sudo apt-get install mysql-server
$ mysql -u root -p root
    mysql> CREATE DATABASE autosleep;
    mysql> CREATE USER 'travis'@'localhost';
```

For running postgresql tests locally on [ubuntu](https://help.ubuntu.com/community/PostgreSQL)

``` 
$ sudo apt-get install postgresql
$ sudo -u postgres createdb autosleep

# You may need to change the default pg authentication policy to allow black password, by editing 
# /etc/postgresql/9.4/main/pg_hba.conf 
# host    all             all             127.0.0.1/32            trust
#see http://dba.stackexchange.com/a/83233/111196
```

then launch tests

```
$ ./gradlew build -Dmysql -Dpostgresql -Dintegration-test=true
```
