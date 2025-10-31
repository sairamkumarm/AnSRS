package dev.sai.srs.service;

import dev.sai.srs.data.Item;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import picocli.CommandLine;

import java.io.File;
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
            throw new RuntimeException(e);
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
                        else throw new RuntimeException("");
                    }
                }

                Optional<Item> itemOptional = validateRecord(record);
                if (itemOptional.isEmpty()) {
                    sb.append("WARNING: Invalid ").append(record.toString()).append("\n");
                    badRows++;
                }
                else items.add(itemOptional.get());
            }
//            System.out.println(items);
            if (!sb.isEmpty()) System.err.println("WARNING: "+badRows + " invalid row(s)\n" + sb.toString());
            return items;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            if (id <= 0 || name.isEmpty() || !link.startsWith("https://") || totalRecalls < 0) {
                return Optional.empty();
            }
            return Optional.of(new Item(id, name, link, pool, lastRecall, totalRecalls));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("ERROR: CSV Malformed");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sanitize(String input){
        return input.replace("\"","").replace("'","").trim();
    }
}
