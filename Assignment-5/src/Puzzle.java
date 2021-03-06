import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;


public class Puzzle {
    private Board initial;
    private int[] cost;
    private HashMap<Board, Vertex> cloud = new HashMap<>();
    private PriorityQueue<Vertex> queue = new PriorityQueue<>();

    private class Vertex implements Comparable<Vertex>{
        public Board board;
        public double distance = Double.POSITIVE_INFINITY;
        public int steps = Integer.MAX_VALUE;
        public Vertex prev = null;
        public Boolean known = false;

        public Vertex(Board board) { this.board = board; }

        @Override
        public int compareTo(Vertex that) {
            int cmp = Double.compare(this.distance, that.distance);
            if (cmp!=0) { return cmp; }
            return Integer.compare(this.steps, that.steps);
        }

        public void reset() {
            this.distance = Double.POSITIVE_INFINITY;
            this.steps = Integer.MAX_VALUE;
            this.prev = null;
            this.known = false;
        }
    }

    private int edge(Board first, Board second) {
        int cost;
        try { cost = this.cost[Character.getNumericValue(first.state.charAt(second.state.indexOf('G'))) - 1]; }
        catch (ArrayIndexOutOfBoundsException e) { cost = 0; }

        return cost;
    }

    private void parseCost(String cost) {
        String[] split = cost.split("\\s");
        int N = this.initial.state.length() - 1;
        this.cost = new int[N];

        for (int i = 0; i < N; i++) {
            this.cost[i] = Integer.parseInt(split[i]);
        }
    }

    private void cleanup() { for (Board b: this.cloud.keySet()) { this.cloud.get(b).reset(); } }

    private void dijkstra(String goal) {
        Vertex s = new Vertex(this.initial);
        s.distance = 0;
        s.steps = 0;
        cloud.put(this.initial, s);
        queue.add(s);
        Vertex v;

        while (!queue.isEmpty()) {
            v = queue.remove();
            v.known = true;
            if (v.board.state.equals(goal)) return;

            for (Board n: v.board.neighbors()) {
                if (!cloud.containsKey(n)) { cloud.put(n, new Vertex(n)); }
            }

            for (Board n: v.board.neighbors()) {
                Vertex nv = cloud.get(n);
                if (!nv.known) {
                    Double dist = v.distance + edge(v.board, n);
                    int steps = v.steps + 1;
                    int cmp = Double.compare(dist, nv.distance);
                    if (cmp < 0 || ((cmp==0) && steps<nv.steps)) {
                        nv.distance = dist;
                        nv.prev = v;
                        nv.steps = steps;
                    }
                    queue.add(nv);
                }
            }
        }
    }

    public void solve(String initial, String cost) {
        this.initial = new Board(initial);
        this.parseCost(cost);
        this.cleanup();
        this.dijkstra(null);
    }

    public Solution solve(String initial, String cost, String goal) {
        if (!isSolvable(initial, goal)) { return new Solution(false); }

        this.initial = new Board(initial);
        this.parseCost(cost);
        this.dijkstra(goal);
        Solution sol = new Solution(goal);
        this.cleanup();
        this.queue.clear();
        return sol;
    }

    private static Boolean isSolvable(String start, String goal) {
        int N = start.length();
        ArrayList<Integer> s = new ArrayList<Integer>(), g = new ArrayList<Integer>();

        for (int i=0; i<N; i++) {
            Character c = start.charAt(i);
            if (c!='G') { s.add(Character.getNumericValue(c)); }
        }
        for (int i=0; i<N; i++) {
            Character c = goal.charAt(i);
            if (c!='G') { g.add(Character.getNumericValue(c)); }
        }

        int inversion = 0;
        for (int i=0; i<N-1; i++) {
            for (int j=i+1; j<N-1; j++) {
                if (g.indexOf(s.get(j)) < g.indexOf(s.get(i))) { inversion++; }
            }
        }
        return ((inversion%2)==0);
    }

    public Solution solution(String goal) {
        if (!isSolvable(this.initial.state, goal)) { return new Solution(false);}
        if (!cloud.get(new Board(goal)).known)     { this.dijkstra(goal); }
        return new Solution(goal);
    }

    private static String move(Board first, Board second) {
        int N = first.dimension;
        String ret = String.valueOf(first.state.charAt(second.state.indexOf('G')));
        int i = second.state.indexOf('G');
        int f = first.state.indexOf('G');
        int xi = i%N;
        int yi = i/N;
        int xf = f%N;
        int yf = f/N;

        if      (xf > xi) { ret = ret + 'R'; }
        else if (xf < xi) { ret = ret + 'L'; }
        else if (yf > yi) { ret = ret + 'D'; }
        else if (yf < yi) { ret = ret + 'U'; }

        return ret;
    }

    private class Solution {
        public int cost;
        public int steps;
        public ArrayList<Board> path;
        private Vertex fv;
        private Boolean reachable = true;

        public Solution(Boolean reachable) {
            this.reachable = reachable;
            if (!reachable) {
                this.cost = -1;
                this.steps = -1;
            }
        }

        public Solution(String goal) {
            this.fv = cloud.get(new Board(goal));
            this.cost = (int) this.fv.distance;
            this.backtrace();
        }

        private void backtrace() {
            ArrayList<Board> path = new ArrayList<>();
            int count = 0;
            path.add(0, this.fv.board);

            while (this.fv.prev != null) {
                fv = fv.prev;
                count++;
                path.add(0, fv.board);
            }

            this.steps = count;
            this.path = path;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Integer.toString(steps) + " " + Integer.toString(cost) + System.lineSeparator());

            if (reachable) {
                for (int i=0; i<this.steps; i++) {
                    sb.append(move(path.get(i), path.get(i+1)));
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        long st = System.currentTimeMillis();
        Puzzle p = new Puzzle();
        File file = new File(args[0]);
        PrintWriter writer = new PrintWriter(args[1]);
        Scanner sc = new Scanner(file);
        int N = Integer.parseInt(sc.nextLine());
        String initial, goal, cost, out, pi, pc;

        initial = sc.next();
        goal = sc.next();
        sc.nextLine();
        cost = sc.nextLine();
        out = p.solve(initial, cost, goal).toString();
//        System.out.println(out);
        writer.println(out);
        pi = initial;
        pc = cost;


        for (int i=0; i<N-1; i++) {
            initial = sc.next();
            goal = sc.next();
            sc.nextLine();
            cost = sc.nextLine();
            if (initial.equals(pi) && cost.equals(pc)) { out = p.solution(goal).toString();             }
            else                                       { out = p.solve(initial, cost, goal).toString(); }
//            System.out.println(out);
            writer.println(out);
        }
        writer.close();
        long et = System.currentTimeMillis();
        /*System.out.println();
        System.out.println("Time taken (in milliseconds): " + (et-st));*/
    }
}