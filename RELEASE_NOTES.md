### Version 2.3.1 (March 21, 2015)

* Invalid generation of POM dependencies - [Issue 49](https://github.com/bmuschko/gradle-nexus-plugin/issues/49).

### Version 2.3 (February 4, 2015)

* Do not fail upload for non-Java project - [Pull request 45](https://github.com/bmuschko/gradle-nexus-plugin/pull/45).

### Version 2.2 (December 20, 2014)

* Use toString() representation of project.version - [Pull request 39](https://github.com/bmuschko/gradle-nexus-plugin/pull/39).

### Version 2.1.1 (November 2, 2014)

* Allow for applying Java plugin after Nexus plugin - [Issue 8](https://github.com/bmuschko/gradle-nexus-plugin/issues/8).

### Version 2.1 (November 2, 2014)

* The functionality of adding archive task is now part of the `ExtraArchivePlugin`.
* Extra archives need to be configured via the extension `extraArchive`.

### Version 2.0.1 (November 2, 2014)

* Generate Javadoc JAR and added URL to POM metadata to make Maven Central happy.
* Test case cleanups. Only run some of the tests if signing is configured properly.

### Version 2.0 (October 11, 2014)

* Upgrade to Gradle Wrapper 2.1.
* Changed package name to `com.bmuschko.gradle.vagrant`.
* Changed group ID to `com.bmuschko`.
* Adapted plugin IDs to be compatible with Gradle's plugin portal.

### Version 0.7.1 (June 8, 2014)

* Request signing properties on the command line if not provided through `gradle.properties` - [Pull request 25](https://github.com/bmuschko/gradle-nexus-plugin/pull/25).

### Version 0.7 (January 19, 2014)

* Fix POM signing for multi-project builds - [Issue 19](https://github.com/bmuschko/gradle-nexus-plugin/issues/19).

### Version 0.6.1 (December 28, 2013)

* Could not find method modifyPom - [Issue 18](https://github.com/bmuschko/gradle-nexus-plugin/issues/18).

### Version 0.6 (December 15, 2013)

* Allow publishing using a custom configuration - [Issue 1](https://github.com/bmuschko/gradle-nexus-plugin/issues/1).

### Version 0.5.1 (December 11, 2013)

* Fix signing the POMs - [Issue 17](https://github.com/bmuschko/gradle-nexus-plugin/issues/17).

### Version 0.5 (December 7, 2013)

* Make console input messages more generic - [Issue 16](https://github.com/bmuschko/gradle-nexus-plugin/issues/16).
* Fix configuration for `install` task - [Issue 7](https://github.com/bmuschko/gradle-nexus-plugin/issues/7).
* Update to Gradle Wrapper 1.9.

### Version 0.4 (October 27, 2013)

* If username and password are not specified, then prompt for them - [Pull request 14](https://github.com/bmuschko/gradle-nexus-plugin/pull/14).
* Add a group and description to the JAR tasks - [Pull request 6](https://github.com/bmuschko/gradle-nexus-plugin/pull/6).
* Only sign POM file once - [Pull request 5](https://github.com/bmuschko/gradle-nexus-plugin/pull/5).
* Make JAR tasks configurable - [Pull request 4](https://github.com/bmuschko/gradle-nexus-plugin/pull/4).
* Added integration tests.
* Publish the plugin to Bintray.

### Version 0.3 (August 17, 2013)

* Fixed deprecation messages.
* Update to Gradle Wrapper 1.7.

### Version 0.2 (September 23, 2012)

* Only sign artifacts if task graph has upload task - [Issue 2](https://github.com/bmuschko/gradle-nexus-plugin/issues/2).
* Update to Gradle Wrapper 1.2.

### Version 0.1 (June 10, 2012)

* Initial release.