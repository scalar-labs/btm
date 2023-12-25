# Bitronix Transaction Manager (BTM)

Bitronix Transaction Manager (BTM) is a simple but complete implementation of the JTA 1.1 API. It is an XA transaction manager that provides all services required by the JTA API while keeping the code as simple as possible for easier understanding of the XA semantics.

The master version of the current source was built and published on the [Maven central repository](https://mvnrepository.com/artifact/com.github.marcus-nl.btm).

## What's new

The BTM 3.0 release has two primary goals: performance improvements and code modernization. The codebase was moved from
a Java 1.4 codebase to Java 5, taking advantage of generics for type-safety and `java.util.concurrent` classes for improved
performance.

Notable improvements include:

* Greater use of low-contention lock collections throughout the code and the removal of most large-grained locks.
* A new connection pool with a focus on zero-wait connection acquisition and concurrent expansion/contraction. For one large
workload compared to BTM 2.1, total connection wait time went from 76 seconds to 372ms, and lock contentions went from 26269
to just 21.
* High-performance proxies around `javax.sql` entities via bytecode generation with support for Javassist and cglib, with 
fallback to `java.lang.reflect.proxy`. Javassist and cglib offer substantial performance increases and are strongly recommended.
* High-performance transaction log journaling using a new design that allows concurrent appenders through a write-reservation model. The new journal is 3 to 14 times faster than BTM 2.1.
* Support for all levels of JDBC up to and including JDBC 4.1.
* OSGi support.
* A change of license from LGPL v3 to Apache 2.

## General information

For general information, see the following:

* [Overview](https://github.com/scalar-labs/btm/wiki/Overview)
* [FAQ](https://github.com/scalar-labs/btm/wiki/FAQ)

## Configuration

For details about configuring BTM, see the following:

* [Transaction manager configuration](https://github.com/scalar-labs/btm/wiki/Transaction-manager-configuration)
* [Resource loader configuration](https://github.com/scalar-labs/btm/wiki/Resource-loader-configuration)

## Credits

Scalar, Inc., would like to thank [@bitronix](https://twitter.com/bitronix) and [@BrettWooldridge](https://twitter.com/BrettWooldridge) for their work on BTM. They have gracefully transferred ownership to us so that we can continue to maintain and build on this project.
