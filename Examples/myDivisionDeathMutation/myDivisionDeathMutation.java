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

    public static void main(String[] args){
        int x=1000,y=1000,scaleFactor=1;
        GridWindow vis=new GridWindow(x,y,scaleFactor, false);//used for visualization

        String output_filename = args[0];
        int write_frequency = Integer.parseInt(args[1]);
        int TOTAL_TIME = Integer.parseInt(args[2]);
        int MAX_POP_SIZE = Integer.parseInt(args[3]);

        double div_prob = Double.parseDouble(args[4]);
        double mut_prob = Double.parseDouble(args[5]);
        double die_prob = Double.parseDouble(args[6]);
        double mut_advantage = Double.parseDouble(args[7]);


        FileIO outputFile=new FileIO(output_filename,"w");

        myDivisionDeathMutation grid=new myDivisionDeathMutation(x,y,vis, div_prob, mut_prob, die_prob, mut_advantage);

        grid.InitTumor(5);
        ArrayList<CellEx> sampledCells = new ArrayList<>();
        int[] outputLine = new int[4];
        System.out.println("Starting simulation...");
        for (int tick = 0; tick < TOTAL_TIME; tick++) {
            if (grid.Pop() > MAX_POP_SIZE) {
                break;
            }
            grid.StepCells();
            if (tick % write_frequency == 0) {
                    System.out.println(tick);
                for (CellEx cell : grid) {
                    outputLine[0] = cell.Xsq();
                    outputLine[1] = cell.Ysq();
                    outputLine[2] = cell.nMutations;
                    outputLine[3] = tick;
                    outputFile.Write(Util.ArrToString(outputLine, ",") + "\n");
                }
                sampledCells.clear();
            }
        }
        outputFile.Close();
    }
}