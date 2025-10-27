package dev.sai.srs.service;

import dev.sai.srs.data.Problem;
import dev.sai.srs.db.DuckDBManager;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RecallService {

    private final Map<Problem.Pool, Double> map = Map.of(Problem.Pool.H,3.0,Problem.Pool.M,2.0,Problem.Pool.L,1.0);
    private PriorityQueue<Problem> problemQueue = new PriorityQueue<>((a,b)->{
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
    public RecallService(DuckDBManager dbManager){
        loadQueue(dbManager);
    }

    public RecallService(DuckDBManager dbManager, Double alpha, Double beta, Double gamma, LocalDate date){
        this.alpha = alpha;
        this.beta= beta;
        this.gamma = gamma;
        this.date = date;
        loadQueue(dbManager);
    }

    private void loadQueue(DuckDBManager dbManager){
        Optional<List<Problem>> list = dbManager.getAllProblems();
        if (list.isEmpty()) throw new RuntimeException("Invalid recall attempt");
        for (Problem p: list.get()) problemQueue.add(p);
    }

    public double getRating(Problem p){
        long daysSinceLast = Math.max(1,ChronoUnit.DAYS.between(p.getLastRecall(), date)+1);
        double res = ((map.get(p.getProblemPool()) * alpha) * Math.pow((double)daysSinceLast,beta))/(p.getTotalRecalls()+gamma);
        return res;
    }

    public List<Integer> recall(int x){
        List<Integer> res = new ArrayList<>();
        while (x-->0) res.add(Objects.requireNonNull(problemQueue.poll()).getProblemId());
        return res;
    }

    public Map<Problem.Pool, Double> getMap() {
        return map;
    }

    public PriorityQueue<Problem> getProblemQueue() {
        return problemQueue;
    }

    public void setProblemQueue(PriorityQueue<Problem> problemQueue) {
        this.problemQueue = problemQueue;
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
