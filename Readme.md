# Wiki.java

[ ![Codeship Status for MER-C/wiki-java](https://codeship.com/projects/46dd6420-bb60-0132-1d73-5ea58638974e/status?branch=master)](https://codeship.com/projects/72144)
[![Build Status](https://travis-ci.org/MER-C/wiki-java.svg?branch=master)](https://travis-ci.org/MER-C/wiki-java?branch=master)
[![codecov.io](http://codecov.io/github/MER-C/wiki-java/coverage.svg?branch=master)](http://codecov.io/github/MER-C/wiki-java?branch=master)


A Java wiki bot framework that is only one file -- [org/wikipedia/Wiki.java](src/org/wikipedia/Wiki.java).
Some functionality provided by MediaWiki extensions deployed on Wikimedia sites
is available in [org/wikipedia/WMFWiki.java](src/org/wikipedia/WMFWiki.java). 
This project also contains the source code to the tools hosted at 
https://wikipediatools.appspot.com and other Wikipedia-related bits and pieces. Requires JDK >= 1.8.

Latest stable version: [0.33](https://github.com/MER-C/wiki-java/releases/tag/0.33) -- 
MediaWiki versions 1.28+

## Bug reports

Bug reports may be filed in the Issue tracker or at [my talk page](https://en.wikipedia.org/wiki/User_talk:MER-C). 
Please read [this essay on filing bug reports effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html)
if you are not already familiar with its contents. Bugs regarding bot things
should have a short test case that demonstrates the problem. Before reporting 
character encoding problems, please display the output in a JOptionPane to 
isolate your development environment.

## Licenses

* The package [org.wikipedia.servlets](src/org/wikipedia/servlets) is licensed 
  under [AGPLv3+](COPYING.AGPL)
* Everything else: [GPLv3+](COPYING.GPL)

## Documentation

* [Javadoc](https://wikipediatools.appspot.com/doc/index.html)
* [Extended documentation](https://github.com/MER-C/wiki-java/wiki/Extended-documentation),
  including an example program
* See [the page on Wikipedia](https://en.wikipedia.org/wiki/User:MER-C/Wiki.java)
  for some old revision history.
