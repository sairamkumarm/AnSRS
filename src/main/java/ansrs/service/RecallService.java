package ansrs.service;

import ansrs.data.Item;
import ansrs.db.DBManager;
import ansrs.util.Log;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RecallService {

    private final Map<Item.Pool, Double> map = Map.of(Item.Pool.H,3.0, Item.Pool.M,2.0, Item.Pool.L,1.0);
    private PriorityQueue<Item> itemQueue = new PriorityQueue<>((a, b)->{
        double aRate=getRating(a);
        double bRate=getRating(b);
        if (aRate > bRate) return -1;
        else if (bRate > aRate) return 1;
        else return 0;
    });;

    private Double alpha = 10.0;
    private Double beta = 1.2;
    private Double gamma = 1.0;
    private LocalDate date = LocalDate.now();
    public RecallService(DBManager dbManager){
        loadQueue(dbManager);
    }

    public RecallService(DBManager dbManager, Double alpha, Double beta, Double gamma, LocalDate date){
        this.alpha = alpha;
        this.beta= beta;
        this.gamma = gamma;
        this.date = date;
        loadQueue(dbManager);
    }

    private void loadQueue(DBManager dbManager){
        Optional<List<Item>> list = dbManager.getAllItems();
        if (list.isEmpty()) throw new RuntimeException(Log.errorMsg("Invalid recall attempt"));
        for (Item p: list.get()) itemQueue.add(p);
    }

    public double getRating(Item p){
        long daysSinceLast = Math.max(1,ChronoUnit.DAYS.between(p.getLastRecall(), date)+1);
        double res = ((map.get(p.getItemPool()) * alpha) * Math.pow((double)daysSinceLast,beta))/(p.getTotalRecalls()+gamma);
        return res;
    }

    public List<Integer> recall(int x){
        List<Integer> res = new ArrayList<>();
        while (x-- >0 && !itemQueue.isEmpty()) res.add(itemQueue.poll().getItemId());
        return res;
    }

    public Map<Item.Pool, Double> getMap() {
        return map;
    }

    public PriorityQueue<Item> getItemQueue() {
        return itemQueue;
    }

    public void setItemQueue(PriorityQueue<Item> itemQueue) {
        this.itemQueue = itemQueue;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public Double getBeta() {
        return beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Double getGamma() {
        return gamma;
    }

    public void setGamma(Double gamma) {
        this.gamma = gamma;
    }
}
