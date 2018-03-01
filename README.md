[![Build Status](https://api.travis-ci.org/bitronix/btm.png?branch=master)](https://travis-ci.org/bitronix/btm)

The master version of the current source was built and published on maven central over [here](https://mvnrepository.com/artifact/com.github.marcus-nl.btm).

### Help!
![help wanted](https://static.wixstatic.com/media/7dc740_3a94c9b3ccd14f939b7595843dc7c72d~mv2.png/v1/fill/w_240,h_240,al_c/7dc740_3a94c9b3ccd14f939b7595843dc7c72d~mv2.png)

BTM is looking for a new motivated team to look after it. If you would like to take over the project, please by all means contact us: [@bitronix](https://twitter.com/bitronix) and [@BrettWooldridge](https://twitter.com/BrettWooldridge).

### Home of BTM, the Bitronix JTA Transaction Manager ###

The Bitronix Transaction Manager (BTM) is a simple but complete implementation of the JTA 1.1 API. It is a fully 
working XA transaction manager that provides all services required by the JTA API while trying to keep the code 
as simple as possible for easier understanding of the XA semantics.

#### What's New ####
The BTM 3.0 release has two primary goals: performance improvements, and code modernization.  The codebase was moved from
a Java 1.4 codebase to Java 5, taking advantage of generics for type-safety and `java.util.concurrent` classes for improved
performance.  Notable improvements are:
* Greater use of low-contention lock collections throughout the code and the removal of most large-grained locks.
* A new connection pool with a focus on zero-wait connection acquisition and concurrent expansion/contraction. For one large
workload compared to BTM 2.1 total connection wait time went from 76 seconds to 372ms, and lock contentions went from 26269
to just 21.
* High-performance proxies around `javax.sql` entities via bytecode generation with support for Javassist, and cglib, with 
fallback to `java.lang.reflect.proxy`.  Javassist and cglib offer substantial performance increases and are strongly recommended.
* High-performance transaction log journaling using a new design that allows concurrent appenders through a 
write-reservation model.  The new journal is 3-14x faster than BTM 2.1.
* Support for all levels of JDBC upto and including JDBC 4.1
* OSGi support
* A change of license from LGPL v3 to Apache 2.

#### General Information ####
* [Overview](https://github.com/bitronix/btm/wiki/Overview)
* [FAQ](https://github.com/bitronix/btm/wiki/FAQ)

#### Configuration ####
* [Transaction manager configuration](https://github.com/bitronix/btm/wiki/Transaction-manager-configuration)
* [Resource loader configuration](https://github.com/bitronix/btm/wiki/Resource-loader-configuration)
