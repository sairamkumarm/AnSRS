package ansrs.set;

import ansrs.data.Item;
import ansrs.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CompletedSet {
    private final Path setPath;
    private LocalDate setDate;
    private HashMap<Integer, Pair<Item.Pool, LocalDate>> items;

    public CompletedSet(Path path) {
        this.setPath = path;
        this.items = new HashMap<>();
        if (!Files.exists(path)) {
            initSet(setPath, LocalDate.now());
        } else {
            loadSet(setPath);
        }
    }

    private void initSet(Path setPath, LocalDate date) {
        try (BufferedWriter writer = Files.newBufferedWriter(setPath)) {
            writer.write(date.toString());
            writer.newLine();
            setDate = date;
            writer.write(String.valueOf(0));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Error: CompletedSet Creation Failed"));
        }
    }

    private void loadSet(Path setPath) {
        try {
            List<String> lines = Files.readAllLines(setPath);
            if (lines.size() < 2) throw new RuntimeException(Log.errorMsg("CompletedSet Malformed: Too few lines"));
            setDate = LocalDate.parse(lines.getFirst());
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    String[] itemHolder = line.split(" ");
                    if (itemHolder.length != 3) throw new RuntimeException(Log.errorMsg("CompletedSet Item Malformed: Not enough data"));
                    try{
                       items.put(Integer.parseInt(itemHolder[0]),
                               new Pair<Item.Pool, LocalDate>(
                                       itemHolder[1].equals("null")?null: Item.Pool.valueOf(itemHolder[1].toUpperCase()),
                                       LocalDate.parse(itemHolder[2])));
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException(Log.errorMsg("CompletedSet Item Malformed: Pool Value Invalid"));
                    } catch (DateTimeParseException e){
                        throw new RuntimeException(Log.errorMsg("CompletedSet Item Malformed: Date unparsable"));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Error loading set"));
        }
    }

    public void reloadItem() {
        try (BufferedWriter writer = Files.newBufferedWriter(setPath)) {
            writer.write(setDate.toString());
            writer.newLine();
            writer.write(String.valueOf(items.size()));
            writer.newLine();
            for (Map.Entry<Integer, Pair<Item.Pool, LocalDate>> e : items.entrySet()) {
                if (e.getValue()==null || e.getValue().getLast_recall()==null) throw new RuntimeException(Log.errorMsg("CompletedSet Object Malformed"));
                String pid = String.valueOf(e.getKey());
                String pool = (e.getValue().getPool()==null) ? "null" :e.getValue().getPool().name();
                String date = e.getValue().getLast_recall().toString();
                writer.write(pid + " " + pool + " " + date);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    public boolean addItem(Integer itemId, Item.Pool pool) {
        if (items.containsKey(itemId)) {
            return false;
        } else {
            items.put(itemId, new Pair<>(pool, LocalDate.now()));
            reloadItem();
            return true;
        }
    }

    public boolean addItem(Integer itemId, Item.Pool pool, LocalDate date) {
        if (items.containsKey(itemId)) {
            return false;
        } else {
            items.put(itemId, new Pair<>(pool, date));
            reloadItem();
            return true;
        }
    }

    public boolean removeItem(Integer itemID) {
        if (!items.containsKey(itemID)) return false;
        items.remove(itemID);
        reloadItem();
        return true;
    }

    public boolean containsItem(Integer itemID){
        return items.containsKey(itemID);
    }

    public boolean clearSet(){
        items.clear();
        reloadItem();
        return items.isEmpty();
    }

    public Path getSetPath() {
        return setPath;
    }

    public LocalDate getSetDate() {
        return setDate;
    }

    public HashMap<Integer, Pair<Item.Pool, LocalDate>> getItems() {
        return items;
    }

    public void setSetDate(LocalDate setDate) {
        this.setDate = setDate;
        reloadItem();
    }

    @Override
    public String toString() {
        return "CompletedSet{" +
                "setDate=" + setDate +
                ", itemIds=" + items +
                '}';
    }

    public static class Pair<T,V>{
        private final T pool;
        private final V last_recall;

        public Pair(T pool, V last_recall) {
            this.pool = pool;
            this.last_recall = last_recall;
        }

        public T getPool() {
            return pool;
        }

        public V getLast_recall() {
            return last_recall;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "pool=" + pool +
                    ", last_recall=" + last_recall +
                    '}';
        }
    }
}
