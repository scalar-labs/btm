Bitronix-HP (High Performance)
==============================

The official Bitronix distribution is located [here](http://docs.codehaus.org/display/BTM/Home), so why this distribution?

I am a contributor and committer on the official Bitronix project, created and led by Ludovic Orban.  The company I work for and the product we sell has high performance demands, so we are constantly profiling and tuning.  We have been using Bitronix for more than 5 years, and during profiling runs, I got so used to Bitronix showing up in the list of "hotspots" that I put a filter into my profiler to basically ignore all Bitronix classes so I could focus on improving the performance of our application code.

However, as the application got faster and faster (due to profiling and tuning), just this past year I turned off the filter for Bitronix and to my surprise, I found Bitronix was now becoming a bottleneck.  I had contributed some significate pieces of code to Bitronix in the past, so being familiar with the code, I dug in.  Bitronix has quite a lot of high-level locking using synchronized constructs, which impedes performance.  The decision of the Bitronix project to move off of JDK 1.4 and to JDK 1.5 allowed me to take advantage of some of concurrency improvements there, and in fact my goal became to remove every 'synchronized' keyword I could find, while still maintaining correctness.

The result is Bitronix-HP.  So, still the question: why this distribution?  I like Ludovic, and I like working on Bitronix, but sometimes developers disagree.  My changes touched a lot of code.  I almost completely rewrote the XAPool class.  I removed the use of reflection that was used as a shim to support JDBC3 and JDBC4 targets while still compiling against only JDBC3 interfaces.  I wrote that hairy code originally, so it was a pleasure to rip it out.
I can't say exactly why Ludovic has not taken these improvements, but I think it is a combination of things.  First, he has stated he doesn't have the bandwidth to review all of these changes.  Second, because of that he is reluctant to just merge them given that Bitronix is 'stable' and doesn't want to destabilze the code.  And finally, he may just disagree with some of the improvements.

I put this repository here, not just for other people, but also because I (and my company) don't like sitting on private changes to an open source project.  If I get hit by a bus, finding the improvements we made to Bitronix should be easy.

Is it stable?
-------------
I would say, absolutely.  I say that because my company has our product deployed in some of the largest telecom providers in the world running high transaction rates, and running (24/7) on this code-base for the past year.  All test cases in the Bitronix suite pass, of course.

Technical Details
-----------------
You can find a technical analysis of Bitronix-HP (aka BTM-2.2 branch) and Bitronix 2.1.2 [here](http://docs.codehaus.org/display/BTM/BTM-2.2).  Included in the analysis is a detailed description of the XAPool changes.

