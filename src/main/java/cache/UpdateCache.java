package cache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class UpdateCache implements Cache {
    Path cachePath;
    LocalDate cacheDate;
    List<Pair<Integer, String>> problems;
    public UpdateCache(Path path){
        this.cachePath = path;
        this.problems = new ArrayList<>();
        if(!Files.exists(path)){
            initCache(cachePath,LocalDate.now());
        }else{
            loadCache(cachePath);
        }
    }
    private void initCache(Path cachePath, LocalDate date){
        try(BufferedWriter writer = Files.newBufferedWriter(cachePath)){
            writer.write(date.toString());
            writer.newLine();
            writer.write(String.valueOf(0));
            writer.newLine();
        } catch (IOException e){
            throw new RuntimeException("Error Creating Cache");
        }
    }

    private void loadCache(Path cachePath) {
        try{
            List<String> lines = Files.readAllLines(cachePath);
            if(lines.size() <2) throw new RuntimeException("Cache File Contents Malformed");
            cacheDate = LocalDate.parse(lines.getFirst());
            for(int i=2; i< lines.size(); i++){
                String line = lines.get(i).trim();
                if(!line.isEmpty()){
                    String[] temp = line.split(" ");
                    if (temp.length!=2) throw new RuntimeException("Problem malformed in cache");
                    problems.add(new Pair<>(Integer.parseInt(temp[0]),temp[1]));
                }
            }
        } catch (IOException e){
            throw new RuntimeException("Error loading cache");
        }
    }

    @Override
    public String toString() {
        return "SessionCache{" +
                "cacheDate=" + cacheDate +
                ", problemIds=" + problems +
                '}';
    }

    public static class Pair<T,V>{
        private final T key;
        private final V value;

        public Pair(T key, V value) {
            this.key = key;
            this.value = value;
        }

        public T getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }
}
