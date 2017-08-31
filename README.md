# log4j-patch

* Patched version of Apache log4j 1.2.17 which fixes the issus [#4913](https://bz.apache.org/bugzilla/show_bug.cgi?id=4913) and [#41214](https://bz.apache.org/bugzilla/show_bug.cgi?id=41214).
* [DailyExRollingFileAppender](https://github.com/DarkPhoenixs/log4j-patch/blob/master/src/main/java/org/apache/log4j/DailyExRollingFileAppender.java) extends `DailyRollingFileAppender`, support `maxBackupIndex`.

## Maven

```xml
<dependency>
  <groupId>org.darkphoenixs</groupId>
  <artifactId>log4j</artifactId>
  <version>1.3.1</version>
</dependency>
```
