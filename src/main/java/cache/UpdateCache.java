package cache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UpdateCache implements Cache {
    private final Path cachePath;
    private LocalDate cacheDate;
    private HashMap<Integer, String> problems;

    public UpdateCache(Path path){
        this.cachePath = path;
        this.problems = new HashMap<>();
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
            cacheDate = date;
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
                    problems.put(Integer.parseInt(temp[0]),temp[1]);
                }
            }
        } catch (IOException e){
            throw new RuntimeException("Error loading cache");
        }
    }

    public void reloadCache(){
        try(BufferedWriter writer = Files.newBufferedWriter(cachePath)){
            writer.write(cacheDate.toString());
            writer.newLine();
            writer.write(String.valueOf(problems.size()));
            writer.newLine();
            for(Map.Entry<Integer, String> e: problems.entrySet()){
                writer.write(e.getKey() + " " + e.getValue());
                writer.newLine();
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public boolean addProblem(Integer problemId, String pool){
        if (problems.containsKey(problemId)){
            return false;
        } else {
            problems.put(problemId, pool);
            reloadCache();
            return true;
        }
    }

    public Path getCachePath() {
        return cachePath;
    }

    public LocalDate getCacheDate() {
        return cacheDate;
    }

    public HashMap<Integer, String> getProblems() {
        return problems;
    }

    public void setCacheDate(LocalDate cacheDate) {
        this.cacheDate = cacheDate;
        reloadCache();
    }

    @Override
    public String toString() {
        return "SessionCache{" +
                "cacheDate=" + cacheDate +
                ", problemIds=" + problems +
                '}';
    }
}
