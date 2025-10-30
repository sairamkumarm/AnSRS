package dev.sai.srs.set;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class WorkingSet {
    private final Path setPath;
    private LocalDate setDate;
    public Set<Integer> itemIds;

    public WorkingSet(Path path) {
        this.setPath = path;
        this.itemIds = new HashSet<>();
        if (!Files.exists(path)) {
            initSet(setPath, LocalDate.now());
        } else {
            loadSet(setPath);
        }
    }

    private void initSet(Path setPath, LocalDate date) {
        try (BufferedWriter writer = Files.newBufferedWriter(setPath)) {
            writer.write(date.toString());
            setDate = date;
            writer.newLine();
            writer.write(String.valueOf(0));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error Creating Set");
        }
    }

    private void loadSet(Path setPath) {
        try {
            List<String> lines = Files.readAllLines(setPath);
            if (lines.size() < 2) throw new RuntimeException("Set File Contents Malformed");
            setDate = LocalDate.parse(lines.getFirst());
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) itemIds.add(Integer.parseInt(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading set");
        }
    }

    public void fillSet(List<Integer> items){
        itemIds.addAll(items);
        reloadSet();
    }

    public boolean fillSet(Set<Integer> items){
        itemIds.addAll(items);
        reloadSet();
        return true;
    }

    public boolean clearSet(){
        itemIds.clear();
        reloadSet();
        return itemIds.isEmpty();
    }

    public boolean addItem(Integer itemId){
        itemIds.add(itemId);
        reloadSet();
        return itemIds.contains(itemId);
    }

    public boolean removeItem(Integer itemId) {
        if (itemIds.contains(itemId)) {
            boolean stat = itemIds.remove(itemId);
            reloadSet();
            return stat;
        }
        return false;
    }

    public void reloadSet() {
        try (BufferedWriter writer = Files.newBufferedWriter(setPath)) {
            writer.write(setDate.toString());
            writer.newLine();
            writer.write(String.valueOf(itemIds.size()));
            writer.newLine();
            for (int i : itemIds) {
                writer.write(String.valueOf(i));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error Updating Set");
        }
    }

    public Path getSetPath() {
        return setPath;
    }

    public LocalDate getSetDate() {
        return setDate;
    }

    public List<Integer> getItemIdList() {
        return itemIds.stream().toList();
    }

    public Set<Integer> getItemIdSet() {return itemIds;}

    public void setSetDate(LocalDate setDate) {
        this.setDate = setDate;
        reloadSet();
    }



    @Override
    public String toString() {
        return "WorkingSet{" +
                "setDate=" + setDate +
                ", itemIds=" + itemIds +
                '}';
    }
}
