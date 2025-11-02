package ansrs.service;

import ansrs.data.Item;
import ansrs.util.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.awt.event.FocusEvent;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class CSVImporter {

    private final Reader reader;

    public CSVImporter(String filePath) {
        try {
            Path path = Path.of(filePath).toAbsolutePath().normalize();
            this.reader = new FileReader(String.valueOf(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    public List<Item> parse() {
        try {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
            List<Item> items = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            int badRows=0;
            boolean first = true;
            for (CSVRecord record : records) {
                if (first) {
                    first = false;
                    try {
                        Integer.parseInt(record.get(0).trim());
                    } catch (Exception e) {
                        if (isValidHeader(record)) continue;
                        else throw new RuntimeException(Log.errorMsg("Invalid CSV Header"));
                    }
                }

                Optional<Item> itemOptional = validateRecord(record);
                if (itemOptional.isEmpty()) {
                    sb.append(Log.warnMsg("Invalid ")).append(record.toString()).append("\n");
                    badRows++;
                }
                else items.add(itemOptional.get());
            }
            if (!sb.isEmpty()) Log.warn(badRows + " invalid row(s)\n" + sb.toString());
            return items;
        } catch (Exception e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    private boolean isValidHeader(CSVRecord record) {
        if (record.size() != 6) return false;
        for (int i = 0; i < 6; i++) {
            if (record.get(i).trim().isEmpty()) return false;
        }
        return true;
    }

    private Optional<Item> validateRecord(CSVRecord record) {
        try {if (record.size()!=6) throw new ArrayIndexOutOfBoundsException();
            int id = Integer.parseInt(sanitize(record.get(0)));
            String name = sanitize(record.get(1));
            String link = sanitize(record.get(2));
            Item.Pool pool = Item.Pool.valueOf(sanitize(record.get(3)).toUpperCase());
            LocalDate lastRecall = LocalDate.now();
            if (!sanitize(record.get(4)).isEmpty()) lastRecall = LocalDate.parse(sanitize(record.get(4)));
            int totalRecalls =0;
            if (!sanitize(record.get(5)).isEmpty()) totalRecalls = Integer.parseInt(sanitize(record.get(5)));
            if (id <= 0 || name.isEmpty() || !link.startsWith("https://") || totalRecalls < 0 || lastRecall.isAfter(LocalDate.now())) {
                return Optional.empty();
            }
            return Optional.of(new Item(id, name, link, pool, lastRecall, totalRecalls));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(Log.errorMsg("ERROR: CSV Malformed"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sanitize(String input){
        return input.replace("\"","").replace("'","").trim();
    }
}
