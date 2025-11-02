```
 $$$$$$\             $$$$$$\  $$$$$$$\   $$$$$$\  
$$  __$$\           $$  __$$\ $$  __$$\ $$  __$$\ 
$$ |  $$ |$$$$$$$\  $$ |  \__|$$ |  $$ |$$ |  \__|
$$$$$$$$ |$$  __$$\ \$$$$$$\  $$$$$$$  |\$$$$$$\  
$$  __$$ |$$ |  $$ | \____$$\ $$  __$$<  \____$$\ 
$$ |  $$ |$$ |  $$ |$$\   $$ |$$ |  $$ |$$\   $$ |
$$ |  $$ |$$ |  $$ |\$$$$$$  |$$ |  $$ |\$$$$$$  |
\__|  \__|\__|  \__| \______/ \__|  \__| \______/
========ANOTHER SPACED REPETITION SYSTEM==========

AnSRS Version 1.0.0
Author: Sairamkumar M
Email: sairamkumar.m@outlook.com
Github: https://github.com/sairamkumarm/AnSRS

Usage: ansrs [-dhV] [COMMAND]

AnSRS (Pronounced "Answers") is a spaced repetition system.
There are 3 Store of data here.
A WorkingSet, where Items set for recall during a session are stored.
A CompletedSet, where items recalled, are stored, waiting to be commited.
A Database, where items are persisted for further recollection.

  -d, --debug     Prints set and db state
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  add       Add new items into the item database or update an existing one
  complete  Marks item as completed, and transfers them to the CompletedSet
  delete    Remove from WorkingSet, CompletedSet and db, depending on the
              flags, by default it removes from WorkingSet
  commit    Save completed items in WorkingSet to the database
  recall    Loads items from database into WorkingSet for recall
  rollback  Rolls back items from completed state to WorkingSet state
  import    Import a csv into the database.

```
