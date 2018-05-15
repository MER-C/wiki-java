## Design philosophy

Wiki.java is a bot framework contained entirely within one file, with no dependencies.
Only vanilla MediaWiki is supported in Wiki.java. Any WMF specific stuff
([Echo](https://mediawiki.org/wiki/Extension:Echo), 
[GlobalUsage](https://mediawiki.org/wiki/Extension:GlobalUsage), etc.) should go
to WMFWiki.java. Any support for extensions not on WMF sites will go in separate
classes.

Please do not add any dependencies on Java libraries or MediaWiki extensions to
any class without asking first.

Servlets currently run on [Google App Engine](https://cloud.google.com/appengine/docs). 
They should not use any Google-specfic classes and fit entirely within the free
quotas. There is a soft cap of 80 network requests per servlet invocation.

## Tests

Please accompany any new functionality with [unit tests](test/org/wikipedia/) if
a query has an answer that can be verified automatically to be correct. This is 
not possible in all cases because wikis have dynamic content. Please place unit 
tests in subpages of (test.wikipedia.org/wiki/User:MER-C), unless other 
languages or configs are involved (where they should go to the relevant WMF 
project). 

## Coding style

- The opening brace for classes, methods, conditionals, and loops go on the next 
  line. Array initializer lists and lambda expressions (but not anonymous inner 
  classes) the brace can go on the same line.
- Don't collapse single line conditional or loop bodies onto the same line as 
  its header. Put it on the next line.
  - Yes:

    ```java
    if (condition)
        return 0;

    while (var != 0)
        var--;
    ```
  - No:

    ```java
    if (condition) return 0;

    while (var != 0) var--;
    ```
- All compile time constants should be fully uppercased. With constants that
  have more than one word in them, use an underscore to separate them.
  - `public static final double PI = 3.14159;`

- For do-while loops, place 'while' on the next line after the closing brackets

  ```java
  do
  {
  }
  while (false);
  ```
- One space goes between any reserved word, such as if, for or catch, and an 
  open parenthesis (() that follows it.
- One space goes between operators and the variable(s) they operate on, apart 
  from ++ and --.
- Use four spaces per indent.

