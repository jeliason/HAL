package Testing.ConvergenceTesting;


import Framework.GridsAndAgents.PDEGrid1D;

@FunctionalInterface
public interface ConvergenceDiffusionAdvection1D {
    void DiffusionAdvection1D(PDEGrid1D grid,double[]rateConstants);
}
