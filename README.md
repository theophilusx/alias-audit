# alias-audit

Simple Clojure script which takes 3 data input files containing details about
email aliases in a postfix and exchange email environment. 

The three data files are 

- aliases :: The Postfix aliases file
- distributionList.csv :: A CSV file containning details about defined Exchange
  distribution lists (extracted from exchange using powershell script)
- personalaliases.csv :: A CSV file containing personal aliases defined in
  Exchange. 
  
## Background

We are moving from a Postfix based email system to one based on Exchange. Due to
the number of users i.e. 40k, the migration has been performed in stages. One of
the more problematic areas has been with migration of Postfix email
aliases. This is partly due to poor management of alias information (nobody
knows what many of the aliases were for or who to contact - especially
problematic with deeply nested aliases). The other problem is that Exchange has
restrictions and conditions which do not match 1-to-1 with Postfix aliases,
making migration complicated. 

In fact, Exchange has made E-Mail complicated!

This utility is a tool to assist in the migration and cleanup. It will be
modified as and when needed to meet specific requirements. This is not a general
purpose tool, but it might provide some reference material or ideas which others
may be able to use. In particular, it does demonstrate processing of CSV files,
querying Oracle databases and possibly other integration points. 



## Installation

Simple script - most of the time, just use lein run .... or if you really want
to, create a standalone jar with uberjar and run it directly in Java 

## Usage

    $ java -jar alias-audit-0.1.0-standalone.jar [args]

### Bugs

bound to be lots! 


## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
