package Examples.myDivisionDeathMutation;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Gui.UIGrid;
import HAL.Tools.FileIO;
import HAL.Rand;
import HAL.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.github.cliftonlabs.json_simple.JsonObject;

//cells grow and mutate
class CellEx extends AgentSQ2Dunstackable<myDivisionDeathMutation>{
    int nMutations;

    void Mutate(){
        if(nMutations< G.MAX_MUTATIONS && G.rn.Double()< G.MUT_PROB){
            nMutations++;
            Draw();
        }
    }

    void Draw(){
        G.vis.SetPix(Isq(), Util.CategorialColor(nMutations));//sets a single pixel
    }

    void Divide(){
        int nOpts=MapEmptyHood(G.hood);//finds von neumann neighborhood indices around cell.
        if(nOpts>0){
            int iDaughter= G.hood[G.rn.Int(nOpts)];
            CellEx daughter= G.NewAgentSQ(iDaughter);//generate a daughter, the other is technically the original cell
            daughter.nMutations=nMutations;//start both daughters with same number of mutations
            daughter.Draw();
            Mutate();//during division, there is a possibility of mutation of one or both daughters
            daughter.Mutate();
        }
    }
}

public class myDivisionDeathMutation extends AgentGrid2D<CellEx> {
    final static int BLACK= Util.RGB(0,0,0);
    double DIV_PROB =0.2;
    double MUT_PROB =0.003;
    double DIE_PROB =0.1;
    double MUT_ADVANTAGE =1.08;
    int MAX_MUTATIONS =19;
    int[]mutCounts=new int[MAX_MUTATIONS+1];//+1 to count for un-mutated type
    int[]hood=Util.GenHood2D(new int[]{1,0,-1,0,0,1,0,-1}); //equivalent to int[]hood=Util.VonNeumannHood(false);
    Rand rn=new Rand(1);
    UIGrid vis;
    FileIO outputFile=null;
    public myDivisionDeathMutation(int x, int y, UIGrid vis) {
        super(x, y, CellEx.class);
        this.vis=vis;
    }
    public myDivisionDeathMutation(int x, int y, UIGrid vis, double div_prob, double mut_prob, double die_prob, double mut_advantage) {
        super(x, y, CellEx.class);
        this.vis=vis;
        this.DIV_PROB = div_prob;
        this.MUT_PROB = mut_prob;
        this.DIE_PROB = die_prob;
        this.MUT_ADVANTAGE = mut_advantage;
    }
    public myDivisionDeathMutation(int x, int y, UIGrid vis, String outputFileName) {
        super(x, y, CellEx.class);
        this.vis=vis;
        outputFile=new FileIO(outputFileName,"w");
    }
    public void InitTumor(double radius){
        //places tumor cells in a circle
        int[]circleHood= Util.CircleHood(true,radius);//generate circle neighborhood [x1,y1,x2,y2,...]
        int len=MapHood(circleHood,xDim/2,yDim/2);
        for (int i = 0; i < len; i++) {
            CellEx c=NewAgentSQ(circleHood[i]);
            c.nMutations=0;
            c.Draw();
        }
//        int nStartPos=HoodToEmptyIs(circleHood,indices,xDim/2,yDim/2);//map indices to neighborhood centered around the middle
//        for (int i = 0; i <nStartPos ; i++) {
//            CellEx c=NewAgentSQ(indices[i]);
//            c.nMutations=0;
//            c.Draw();
//        }
    }
    public void StepCells(){
        Arrays.fill(mutCounts,0);//clear the mutation counts
        for (CellEx c : this) {//iterate over all cells in the grid
            mutCounts[c.nMutations]++;//count up all cell types for this timestep
            if(rn.Double()< DIE_PROB){
                vis.SetPix(c.Isq(),BLACK);
                c.Dispose();//removes cell from sptial grid and iteration
            }
            else if(rn.Double()< DIV_PROB*Math.pow(MUT_ADVANTAGE,c.nMutations)){//application of mutational advantage
                c.Divide();
            }
        }
        if(outputFile!=null){
            outputFile.Write(Util.ArrToString(mutCounts,",")+"\n");//write populations every timestep
        }
        ShuffleAgents(rn);//shuffles order of for loop iteration
//        IncTick();//increments timestep, including newly generated cells in the next round of iteration
    }

    public static void main(String[]args){
        long startTime = System.currentTimeMillis();
        ArrayList<Double[]>out=new ArrayList<>();
        //int x=500,y=500,scaleFactor=2;
        int x=1000,y=1000,scaleFactor=1;
        int TOTAL_TIME = 1500;
        int MAX_TRIES = 100;
        int MAX_POP_SIZE = 300000;
        int TOTAL_MIN_AGENTS = 75000;
        int TOTAL_GOOD_SIMS = 10000;
        int numGoodSims = 0;
        int BOX_SIZE = 50;
        int snapFrequency = 10;
        double reqDensity = 0.7;
        while (numGoodSims < TOTAL_GOOD_SIMS) {
            int TOTAL_WRITTEN_AGENTS = 0;
            GridWindow vis=new GridWindow(x,y,scaleFactor, false);//used for visualization
            String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
            Rand rand = new Rand();

            double div_prob = rand.Double(0.2) + 0.1;
            double mut_prob = rand.Double(0.1);
            double die_prob = rand.Double(0.2) + 0.05;
            double mut_advantage = rand.Double(0.3) + 0.9;

            String dataPath = "/home/joeleliason/Dropbox (University of Michigan)/Projects/tumorp2p/data/raw/HAL/DivisionDeathMutation/";
            String guid = java.util.UUID.randomUUID().toString();
            String filename =  dataPath +
                    date + "_" + guid +  "_" + "agents.csv";

            JsonObject paramsJson = new JsonObject();
            paramsJson.put("DIV_PROB", div_prob);
            paramsJson.put("MUT_PROB", mut_prob);
            paramsJson.put("DIE_PROB", die_prob);
            paramsJson.put("MUT_ADVANTAGE", mut_advantage);
            String outputParamsPath = dataPath + date + "_" + guid + "_" + "params.json";
            try {
                FileWriter outputParams2 = new FileWriter(outputParamsPath);
                outputParams2.write(paramsJson.toJson());
                outputParams2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileIO outputFile=new FileIO(filename,"w");

            myDivisionDeathMutation grid=new myDivisionDeathMutation(x,y,vis, div_prob, mut_prob, die_prob, mut_advantage);
//            myDivisionDeathMutation grid=new myDivisionDeathMutation(x,y,vis, 0.5, 0.003, 0.5, 0.9);
//            myDivisionDeathMutation grid=new myDivisionDeathMutation(x,y,vis);

            grid.InitTumor(5);
            ArrayList<CellEx> sampledCells = new ArrayList<>();
            int[] outputLine = new int[4];
            String outputString = "";
            System.out.println("Starting simulation...");
            for (int tick = 0; tick < TOTAL_TIME; tick++) {
                if (grid.Pop() > MAX_POP_SIZE) {
                    break;
                }
                vis.TickPause(0);//set to nonzero value to cap tick rate.
                grid.StepCells();
                if (tick % snapFrequency == 0) {
//                    System.out.println(tick);
//                    System.out.println(grid.Pop());
                    int tries = 0;
                    int xCorner = 0;
                    int yCorner = 0;
                    outputString = "";
                    while (tries < MAX_TRIES && (double) sampledCells.size() / BOX_SIZE / BOX_SIZE < reqDensity) {
                        sampledCells.clear();
                        xCorner = grid.rn.Int(x - BOX_SIZE);
                        yCorner = grid.rn.Int(y - BOX_SIZE);
                        grid.GetAgentsRect(sampledCells, xCorner, yCorner, BOX_SIZE, BOX_SIZE);
                        tries++;
                    }
                    TOTAL_WRITTEN_AGENTS += sampledCells.size();
                    for (CellEx cell : sampledCells) {
                        outputLine[0] = cell.Xsq() - xCorner;
                        outputLine[1] = cell.Ysq() - yCorner;
                        outputLine[2] = cell.nMutations;
                        outputLine[3] = tick;
                        outputString = outputString + Util.ArrToString(outputLine, ",") + "\n";
                    }
                    outputFile.Write(outputString);
                    sampledCells.clear();
                }
            }
            outputFile.Close();
            System.out.println("Finished sim, end population size: " + grid.Pop());
            if (TOTAL_WRITTEN_AGENTS >= TOTAL_MIN_AGENTS) {
                numGoodSims += 1;
            }
            System.out.println("Number of good sims: " + numGoodSims);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("All sims finished, took " + (duration / 1000) + " seconds (" + duration + " ms)");
        System.exit(0);
    }
}