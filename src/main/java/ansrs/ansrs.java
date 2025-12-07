// AnSRS Another Spaced Repetition System
// Copyright (c) 2025 Sairamkumar M <sairamkumar.m@outlook.com>

// This Program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This Program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.

// IMPORTANT: This Program is also subject to the Additional Terms defined
// in the TRADEMARK_AND_ATTRIBUTION_POLICY.md file included in this distribution,
// specifically concerning the use of the names "AnSRS" and "Another Spaced Repetition System", 
// misrepresentation, and required attribution.
package ansrs;

import ansrs.db.ArchiveManager;
import ansrs.db.DatabaseInitialiser;
import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.cli.SRSCommand;
import ansrs.db.DBManager;
import ansrs.util.Banner;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;

public class ansrs {

    public static final Path srsDir;
    public static final Path workingSetPath;
    public static final Path completedSetPath;
    public static final Path databasePath;

    static {
        String baseDir = System.getenv("APPDATA");
        srsDir = Path.of(baseDir, "AnSRS");
        workingSetPath = srsDir.resolve("working.set");
        completedSetPath = srsDir.resolve("completed.set");
        databasePath = srsDir.resolve("ansrs.db");
    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(srsDir);

            WorkingSet workingSet = new WorkingSet(workingSetPath);
            CompletedSet completedSet = new CompletedSet(completedSetPath);
            Connection conn = DatabaseInitialiser.initEmbeddedDb(databasePath);
            DBManager db = new DBManager(conn);
            ArchiveManager archiveManager = new ArchiveManager(conn);
            if (args.length == 0) {
                Banner.colorrizedBanner("1.3.0-SNAPSHOT");
                if (workingSet.getItemIdSet().isEmpty()) {
                    Banner.initHelp();
                } else {
                    System.out.println("Current WorkingSet:");
                    Printer.printItemsList(db.getItemsFromList(workingSet.getItemIdList()).orElse(new ArrayList<>()));
                }
            }
            SRSCommand root = new SRSCommand(workingSet, completedSet, db, archiveManager);
            int exitCode = new CommandLine(root).execute(args);
            db.close();
            archiveManager.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Error initializing environment"), e);
        } catch (Exception e) {
            throw new RuntimeException(Log.errorMsg("Fatal Startup Error"),e);
        }
    }

}
