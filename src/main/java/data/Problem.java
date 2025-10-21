package data;

import java.time.LocalDate;

public class Problem {
    private int problemId;
    private String problemName;
    private String problemLink;
    private Pool problemPool;
    private LocalDate lastRecall;
    private int totalRecalls;

    public enum Pool{H, M, L};

    public Problem() {
    }

    public Problem(int problemId, String problemName, String problemLink, Pool problemPool, LocalDate lastRecall, int totalRecalls) {
        this.problemId = problemId;
        this.problemName = problemName;
        this.problemLink = problemLink;
        this.problemPool = problemPool;
        this.lastRecall = lastRecall;
        this.totalRecalls = totalRecalls;
    }

    public int getProblemId() {
        return problemId;
    }

    public void setProblemId(int problemId) {
        this.problemId = problemId;
    }

    public String getProblemName() {
        return problemName;
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public String getProblemLink() {
        return problemLink;
    }

    public void setProblemLink(String problemLink) {
        this.problemLink = problemLink;
    }

    public Pool getProblemPool() {
        return problemPool;
    }

    public void setProblemPool(Pool problemPool) {
        this.problemPool = problemPool;
    }

    public LocalDate getLastRecall() {
        return lastRecall;
    }

    public void setLastRecall(LocalDate lastRecall) {
        this.lastRecall = lastRecall;
    }

    public int getTotalRecalls() {
        return totalRecalls;
    }

    public void setTotalRecalls(int totalRecalls) {
        this.totalRecalls = totalRecalls;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "problemId=" + problemId +
                ", problemName='" + problemName + '\'' +
                ", problemLink='" + problemLink + '\'' +
                ", problemPool=" + problemPool +
                ", lastRecall=" + lastRecall +
                ", totalRecalls=" + totalRecalls +
                '}';
    }
}
