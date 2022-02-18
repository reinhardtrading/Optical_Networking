package mains;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import multiLayer.connection.OpticalConnection;
import multiLayer.connection.VirtualGraph;
import multiLayer.exceptions.CorruptedConfigFileException;
import multiLayer.networkArchitecture.AuxGraph;
import multiLayer.networkArchitecture.Graph;
import multiLayer.networkArchitecture.GraphLink;
import multiLayer.networkArchitecture.GraphNode;
import multiLayer.networkArchitecture.ListLinkCable;
import routing.RoutingRoutine;
import routing.SingleLayerRoutine;
import routing.Restoration.NumberFailure;
import routing.Restoration.RestorationType;
import routing.RoutingOption.RegenCondition;
import traffic.Demand;
import traffic.DemandList;
import routing.AstarConfig;
import routing.Restoration;
import routing.SingleLayerAdaptiveBitRate;


public class FirstLessonEx {

	public static void main(String[] args) throws Exception {

		int[] wavelengthsNbSimulations = {3, 10, 30, 50, 80};
		int[] reaches = {1, 2, 3, 4, 10};
		int[] demandLoad = {20, 50, 100, 200, 300, 400};
		String[] configName = {"AstarConfig_6Nodes", "AstarConfig_AN", "AstarConfig_DT", "AstarConfig_EU", "AstarConfig_Italian"};

		String FileName = ".//Outputs//Lesson1_OpticalNetworking.txt";
		BufferedWriter out = new BufferedWriter (new FileWriter(new File(FileName)));

		out.write("NetScenario \t WavelengthNb \t Reaches \t Load \t RoutedDemands \t BlockedDemands \t RegNb \t MeanFibOccup \tStdFibOccup \tComputationTime \tComputationSystemTime");
		out.newLine();
		
		for(int kdx = 0; kdx < 1; kdx++){ // configName.length
			System.out.println("Network : " + configName[kdx]);
			for(int wNb = 1; wNb < 2; wNb++) //  wavelengthsNbSimulations.length
			{
				System.out.println("WavelengthNb" + wavelengthsNbSimulations[wNb]);
				for(int rNb = 0; rNb < 1; rNb++) // reaches.length
				{
					System.out.println("Reaches" + reaches[rNb]);
					for(int trLoad = 0; trLoad < 1; trLoad++) //demandLoad.length
					{
						System.out.println("Load" + demandLoad[trLoad]);
						for(int loadSim = 0; loadSim <= 1; loadSim++){
												
							long d1 = System.currentTimeMillis();	
							long startSystemTimeNano = System.nanoTime(); //getSystemTime( );
							
							AstarConfig aConfig = AstarConfig.readAstarConfig("./Inputs/"+configName[kdx]+".txt");
							aConfig._numberOfWavelength = wavelengthsNbSimulations[wNb];
							aConfig._regenCondition = RegenCondition.MaxHops;
							if( (aConfig._regenCondition == RegenCondition.MaxHops) ||
									(aConfig._regenCondition == RegenCondition.DistMaxHops) ) {
								aConfig._defaultThreshold2 = reaches[rNb]; 
							}
							
							if( (aConfig._regenCondition == RegenCondition.Distance) ||
									(aConfig._regenCondition == RegenCondition.DistMaxHops) ) {
								aConfig._defaultThreshold = reaches[rNb]; 
							}
							
							
							AuxGraph ag = new AuxGraph(aConfig);
							
							RoutingRoutine routine = new SingleLayerRoutine(ag);
							
							
							String netConf = "";
							
							switch(configName[kdx]){
							case "AstarConfig_6Nodes":
								netConf = "6Nodes"; break;
							case "AstarConfig_AN":
								netConf = "AN"; break;
							case "AstarConfig_EU":
								netConf = "EU"; break;
							case "AstarConfig_DT":
								netConf = "DT"; break;
							}

							DemandList demands = DemandList.getMatrixFromFile(routine.getAuxGraph(), ".\\Inputs\\Traffic_" + netConf + ".txt"); 
					
							SingleLayerRoutine slr = new SingleLayerRoutine(ag);
														
		
							VirtualGraph vg = new VirtualGraph(null, ag, routine._routingParameters);
							vg = slr.findAndReserveTunnel(demands, vg);
							for(OpticalConnection path : vg.getOpticalConnectionsList().values())
								path.display();
								
							int numOcc = 0;
							
							int routedConnections = vg.getVirtualLinks().size();
							int blockedConnections = vg.getBlockedDemands().size();
							int numReg = vg.regeneratorCount();
							
							Iterator<GraphLink> it = slr.getAuxGraph().getGraphLinkList().iterator();
							while(it.hasNext()){
								GraphLink al = it.next();
								System.out.print("Source: " + al.getSourceNode() + " Destination " + al.getDestNode() + " OccupiedWavelenghts " + al.setOfUsedWavelengths() + " FreeWavelengths " + al.setOfFreeWavelengths());
								for(int idx = 0; idx < al.getWavelengthNb(); idx++)
									System.out.print(al.isWavelengthFree(idx) + "\t" );
								System.out.println(";");
								numOcc += al.getNumUsedWavelengths();
							}
								System.out.println(numOcc);					
							
							
							long d2 = System.currentTimeMillis();
							long startSystemTimeNano2 = System.nanoTime(); //getSystemTime( );
							
							long diff = d2 - d1;
							long diff2 = (startSystemTimeNano2 - startSystemTimeNano) / 1000000;

							double[] fiberOccupation = routine._auxGraph.getAverageAndStdLinkOccupancy();
							// OutForComparisons
							out.write(netConf + "\t" + routine._auxGraph.ASTARCONFIG._numberOfWavelength + "\t" + reaches[rNb] + "\t" + demandLoad[trLoad] + "\t" + routedConnections + "\t" + blockedConnections + "\t" + numReg + "\t" + fiberOccupation[0] + "\t" + fiberOccupation[1] + "\t" + diff + "\t" + diff2);
							out.newLine();
							out.flush();
							
							System.out.print(".");					

						}					
						
					}
					
					
				}
			}
		}
		
		out.close();

	}
	
	public static long getSystemTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        (bean.getCurrentThreadCpuTime( ) - bean.getCurrentThreadUserTime( )) : 0L;
	}
	
	
}
