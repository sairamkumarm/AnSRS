```
                                                    
  $$$$$$\             $$$$$$\  $$$$$$$\   $$$$$$\   
 $$  __$$\           $$  __$$\ $$  __$$\ $$  __$$\  
 $$ |  $$ |$$$$$$$\  $$ |  \__|$$ |  $$ |$$ |  \__| 
 $$$$$$$$ |$$  __$$\ \$$$$$$\  $$$$$$$  |\$$$$$$\   
 $$  __$$ |$$ |  $$ | \____$$\ $$  __$$<  \____$$\  
 $$ |  $$ |$$ |  $$ |$$\   $$ |$$ |  $$ |$$\   $$ | 
 $$ |  $$ |$$ |  $$ |\$$$$$$  |$$ |  $$ |\$$$$$$  | 
 \__|  \__|\__|  \__| \______/ \__|  \__| \______/  
                                                    
          ANOTHER SPACED REPETITION SYSTEM          
                AnSRS Version 1.0.0                 
                                                    
Author: Sairamkumar M
Email: sairamkumar.m@outlook.com
Github: https://github.com/sairamkumarm/AnSRS

ansrs is a command-line spaced repetition system designed for quick item tracking and recall
scheduling. It uses a lightweight local database to manage three sets: working, completed,
and recall. The system supports a jump-start feature that allows new users to begin recall
sessions immediately using predefined items, bypassing the usual initialization delay.

The tool is currently built for Windows only and compiled into a native binary for
zero-dependency execution. Linux support can be added easily, but testing is pending.

Usage: ansrs [-hlsV] [-i=ITEM_ID] [COMMAND]

AnSRS (Pronounced "Answers") is a spaced repetition system.

There are 3 Store of data here.
A WorkingSet, where Items set for recall during a session are stored.
A CompletedSet, where items recalled, are stored, waiting to be commited.
A Database, where items are persisted for further recollection.

  -h, --help         Show this help message and exit.
  -i, --id=ITEM_ID   Print a specific Item
  -l, --list         Lists set and db state
  -s, --set          Use this flag with --list to print only set
  -V, --version      Print version information and exit.
Commands:
  add       Add new items into the item database or update an existing one
  complete  Marks item as completed, and transfers them to the CompletedSet
  delete    Remove from WorkingSet, CompletedSet and db, depending on the
              flags, by default it removes from WorkingSet
  commit    Save completed items in WorkingSet to the database
  recall    Loads items from database into WorkingSet for recall
  rollback  Rolls back items from completed state to WorkingSet state
  import    Import a csv into the database.

========================================================
Add Command

Usage: ansrs add [-huV] [--last-recall=ITEM_LAST_RECALL]
                 [--total-recalls=ITEM_TOTAL_RECALL] ITEM_ID ITEM_NAME
                 ITEM_LINK ITEM_POOL
Add new items into the item database or update an existing one
      ITEM_ID     Unique identifier of an item
      ITEM_NAME   Name of the item, as in title, use Quotes
      ITEM_LINK   Link to the item, optional quotes
      ITEM_POOL   Pick from H, M, and L, (HIGH/MEDIUM/LOW)
  -h, --help      Show this help message and exit.
      --last-recall=ITEM_LAST_RECALL
                  Set an optional custom last recall date, by default it is set
                    to today.
      --total-recalls=ITEM_TOTAL_RECALL
                  Set an optional custom total recall count, by default it is
                    set to 0
  -u, --update    To be used while updating an existing item
  -V, --version   Print version information and exit.

========================================================
Complete Command

Usage: ansrs complete [-afhV] [--date=ITEM_LAST_RECALL] [--update=ITEM_POOL]
                      ITEM_ID
Marks item as completed, and transfers them to the CompletedSet
      ITEM_ID              Unique identifier of an item
  -a, --all                Completes all items in the WorkingSet, but you lose
                             the ability to update item pools
      --date=ITEM_LAST_RECALL
                           Optional ITEM_LAST_RECALL specification, use
                             YYYY-MM-DD format
  -f, --force              Forces completion of a item non existent in
                             WorkingSet.
  -h, --help               Show this help message and exit.
      --update=ITEM_POOL   Optional ITEM_POOL value updated for item, choose
                             from H M and L
  -V, --version            Print version information and exit.

========================================================
Delete Command

Usage: ansrs delete [-cdhV] [--hard-reset] --sure ITEM_ID
Remove from WorkingSet, CompletedSet and db, depending on the flags, by default
it removes from WorkingSet
      ITEM_ID        Unique identifier of an item
  -c, --completed    Removes from items that are completed but, are yet to be
                       commited to the database.
  -d, --database     Removes a item from the database and sets
  -h, --help         Show this help message and exit.
      --hard-reset   Hard resets all the persistent data, sets included
      --sure         A defensive fallback to prevent accidental deletions
  -V, --version      Print version information and exit.

========================================================
Commit Command

Usage: ansrs commit [-fhV]
Save completed items in WorkingSet to the database
  -f, --force     allows you to commit when there are pending items in the
                    WorkingSet
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

========================================================
Recall Command

Usage: ansrs recall [-afhV] RECALL_COUNT
Loads items from database into WorkingSet for recall
      RECALL_COUNT   The amount of items to load into WorkingSet for recall
  -a, --append       use --append, to append to an existing non empty
                       WorkingSet, only unique items are added
  -f, --force        use --force, to overwrite existing non-empty WorkingSet
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.

========================================================
Rollback Command

Usage: ansrs rollback [-ahV] ITEM_ID
Rolls back items from completed state to WorkingSet state
      ITEM_ID     Unique identifier of an item
  -a, --all       Rollback all items from WorkingSet
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

========================================================
Import Command

Usage: ansrs import [-hV] --path=CSV_FILE_PATH --preserve=PRESERVE_SOURCE
Import a csv into the database.
The following format is mandatory.
Header(Optional)
(ITEM_ID, ITEM_NAME, ITEM_LINK, ITEM_POOL, ITEM_LAST_RECALL, totalRecalls)
ITEM_ID: Integer > 0
ITEM_NAME: Non-empty String
ITEM_LINK: Non-empty String starting with 'https://'
ITEM_POOL: Non-empty value belonging and limited to [H, M, L]
ITEM_LAST_RECALL: Date string of format YYYY-MM-DD, or cannot be in the future,
leave empty for today's date.
ITEM_TOTAL_RECALLS: Integer >= 0

  -h, --help                 Show this help message and exit.
      --path=CSV_FILE_PATH   Path to the csv file
      --preserve=PRESERVE_SOURCE
                             [csv/db] Pick between overwriting csv values in db
                               or preserving db values in the even of duplicate
                               ITEM_IDs.
  -V, --version              Print version information and exit.

```

Planned Additions
- [ ] Export Command
- [ ] Stats Command
- [ ] Configuration File for Recall Algorithm

Thank you for your time.   

--   
With sincere regard,   
Sairamkumar M   
[GitHub](https://github.com/sairamkumarm) | [LinkedIn](https://www.linkedin.com/in/sairamkumarm/) | [Website](https://sairamkumar.vercel.app)
