# AnSRS: Another Spaced Repetition System

```
                                                    
  ██████\             ██████\  ███████\   ██████\   
 ██  __██\           ██  __██\ ██  __██\ ██  __██\  
 ██ |  ██ |███████\  ██ |  \__|██ |  ██ |██ |  \__| 
 ████████ |██  __██\ \██████\  ███████  |\██████\   
 ██  __██ |██ |  ██ | \____██\ ██  __██<  \____██\  
 ██ |  ██ |██ |  ██ |██\   ██ |██ |  ██ |██\   ██ | 
 ██ |  ██ |██ |  ██ |\██████  |██ |  ██ |\██████  | 
 \__|  \__|\__|  \__| \______/ \__|  \__| \______/  
                                                    
 ANOTHER SPACED REPETITION SYSTEM          
 AnSRS Version 1.3.1                 
                                                    
Author: Sairamkumar M
Email: sairamkumar.m@outlook.com
Github: https://github.com/sairamkumarm/AnSRS

AnSRS (Pronounced "Ans-er-es") is a spaced repetition system, designed for quick item
tracking and recall scheduling. It uses a lightweight local database and cache files to manage
items. The system supports batch importing items, and grouping for quick recall sessions.

There are 3 Store of data here.
A WorkingSet, where Items set for recall during a session are stored.
A CompletedSet, where items recalled, are stored, waiting to be commited.
A Database, where items are persisted in normal storage and archived  for further recollection.
                            
The tool is currently built for Windows only and compiled into a native binary for
zero-dependency execution. Linux support can be added easily, but testing is pending.

Usage: ansrs [-hlsV] [-i=ITEM_ID] [-n=ITEM_NAME_QUERY] [COMMAND]

  -h, --help         Show this help message and exit.
  -i, --id=ITEM_ID   Print a specific Item
  -l, --list         Lists set and db state
  -n, --name=ITEM_NAME_QUERY
                     Find an Item by it's name, query must be longer than one
                       character
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
  archive   Manage archive operations
  group     Group management

========================================================
Add Command

Add new items into the item database or update an existing one
Usage: ansrs add [-huV] [--last-recall=ITEM_LAST_RECALL] [--link=ITEM_LINK]
                 [--name=ITEM_NAME] [--pool=ITEM_POOL]
                 [--total-recalls=ITEM_TOTAL_RECALLS] ITEM_ID [ITEM_NAME]
                 [ITEM_LINK] [ITEM_POOL]
Parameter Order: ITEM_ID ITEM_NAME ITEM_LINK ITEM_POOL
[--last-recall=ITEM_RECALL_DATE] [--total-recalls=ITEM_TOTAL_RECALLS]
To update, use the flag versions or name, link, pool, last_recall and
total_recalls

      ITEM_ID            Unique identifier of an item
      [ITEM_NAME]        Name of the item, required for insert
      [ITEM_LINK]        Link to the item, required for insert
      [ITEM_POOL]        Pick from H, M, and L, required for insert
  -h, --help             Show this help message and exit.
      --last-recall=ITEM_LAST_RECALL
                         Set custom last recall date (YYYY-MM-DD)
      --link=ITEM_LINK   Update item link (must start with https://)
      --name=ITEM_NAME   Update item name
      --pool=ITEM_POOL   Update item pool (H/M/L)
      --total-recalls=ITEM_TOTAL_RECALLS
                         Set custom total recall count
  -u, --update           Update an existing item
  -V, --version          Print version information and exit.

========================================================
Complete Command

Transfer items from WorkingSet into the CompletedSet
Usage: ansrs complete [-afhV] [--date=ITEM_LAST_RECALL] [--update=ITEM_POOL]
                      ITEM_ID
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

Remove from WorkingSet, CompletedSet or Database
Usage: ansrs delete [-cdhV] [--completed-all] [--hard-reset] --sure
                    [--working-all] ITEM_ID
      ITEM_ID           Unique identifier of an item
  -c, --completed       Removes from items that are completed but, are yet to
                          be commited to the database.
      --completed-all   Removes all items from CompletedSet
  -d, --database        Removes a item from the database and sets
  -h, --help            Show this help message and exit.
      --hard-reset      Hard resets all the persistent data, sets included
      --sure            A defensive fallback to prevent accidental deletions
  -V, --version         Print version information and exit.
      --working-all     Removes all items from WorkingSet

========================================================
Commit Command

Save items in CompletedSet to the database
Usage: ansrs commit [-fhV]
  -f, --force     allows you to commit when there are pending items in the
                    WorkingSet
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

========================================================
Recall Command

Loads items from database into WorkingSet for recall
Usage: ansrs recall [-ahoV] [-c=ITEM_ID[,ITEM_ID...]...]... RECALL_COUNT
      RECALL_COUNT   The amount of items to load into WorkingSet for recall
  -a, --append       Append to an existing non empty WorkingSet, only unique
                       items are added
  -c, --custom=ITEM_ID[,ITEM_ID...]...
                     Custom ITEM_ID(s) to recall, use space or comma separated
                       values
  -h, --help         Show this help message and exit.
  -o, --overwrite    Overwrite existing non-empty WorkingSet
  -V, --version      Print version information and exit.

========================================================
Rollback Command

Roll back items from CompletedSet to WorkingSet
Usage: ansrs rollback [-ahV] ITEM_ID
      ITEM_ID     Unique identifier of an item
  -a, --all       Rollback all items from WorkingSet
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

========================================================
Import Command

Import a batch of items in csv form into the database.
Usage: ansrs import [-hV] --path=CSV_FILE_PATH --preserve=PRESERVE_OPTION
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
      --preserve=PRESERVE_OPTION
                             ["csv"/"db"] Pick between overwriting csv values
                               in db or preserving db values in the even of
                               duplicate ITEM_IDs.
  -V, --version              Print version information and exit.

========================================================
Archive Command

Manage operations for items in archive
Usage: ansrs archive [-hV] [--all] [--list] [--restore-all] [--sure]
                     [--add=ITEM_ID] [--delete=ITEM_ID] [--id=ITEM_ID]
                     [--name=ITEM_NAME_QUERY] [--restore=ITEM_ID]
      --add=ITEM_ID       Move ITEM_ID from DB to archive
      --all               Archive all items from DB (excluding those in sets)
      --delete=ITEM_ID    Delete ITEM_ID from archive
  -h, --help              Show this help message and exit.
      --id=ITEM_ID        Get archived ITEM_ID details
      --list              List all archived items
      --name=ITEM_NAME_QUERY
                          Search archived items by name
      --restore=ITEM_ID   Restore ITEM_ID from archive to DB
      --restore-all       Restore all items from archive to DB
      --sure              Confirm destructive operation for --delete, --all,
                            and --restore-all
  -V, --version           Print version information and exit.

========================================================
Group Command

Manage groups, items in groups, and recall from group
Usage: ansrs group [-hV] [--create] [--delete] [--show] [--show-all]
                   [--show-items] [--update] [--add-item=ITEM_ID]
                   [--id=GROUP_ID] [--link=GROUP_LINK] [--name=GROUP_NAME]
                   [--recall=RECALL_MODE] [--remove-item=ITEM_ID]
                   [--add-batch=ITEM_IDS[,ITEM_IDS...]...]...
Manage items groups, for quick loading into WorkingSet for recall
NOTE: destructive actions are not carried onto the items themselves
      --add-batch=ITEM_IDS[,ITEM_IDS...]...
                             Add items in space separated or comma separated
                               fashion
      --add-item=ITEM_ID     Add an item into a group, requires --id=GROUPS_ID
      --create               Create a new group
      --delete               Delete a group (does not affect items in them)
  -h, --help                 Show this help message and exit.
      --id=GROUP_ID          Target group id
      --link=GROUP_LINK      Optional group link (https only)
      --name=GROUP_NAME      Group name (required for create)
      --recall=RECALL_MODE   Add group items to WorkingSet with mode
                               "overwrite" | "append"
      --remove-item=ITEM_ID  Remove an item from a specified group (does not
                               affect the item itself)
      --show                 Show a group's metadata
      --show-all             Show all groups in collection
      --show-items           Show items in a specific group
      --update               Update group name or link
  -V, --version              Print version information and exit.

```
## Build steps
### Package the project
```shell
mvn clean package
```
### Optional trace(for changes that use reflection)
```shell
& "$env:GRAALVM_HOME\bin\java" -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/ -jar target/ansrs-{version}.jar
```
### Build the exe
```shell
native-image --no-fallback -jar ./target/ansrs-{version}.jar ansrs
```
## Planned Additions
- [ ] Export Command
- [ ] Stats Command
- [ ] Configuration File for Recall Algorithm

Thank you for your time.

--   
With sincere regard,   
Sairamkumar M   
[GitHub](https://github.com/sairamkumarm) | [LinkedIn](https://www.linkedin.com/in/sairamkumarm/) | [Website](https://sairamkumarm.com)
