package mains;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import multiLayer.connection.OpticalConnection;
import multiLayer.connection.OpticalPath;
import multiLayer.connection.VirtualGraph;
import multiLayer.exceptions.CorruptedConfigFileException;
import multiLayer.exceptions.IllegalParameterValueException;
import multiLayer.exceptions.InvalidFiberIdException;
import multiLayer.networkArchitecture.AuxGraph;
import routing.AstarConfig;
import routing.SingleLayerAdaptiveBitRate;
import traffic.Demand;
import traffic.DemandList;
import traffic.TrafficGeneration;
import traffic.TrafficGeneration.TrafficType;

public class Option1MLRHomogenousUS {

	public static void main(String[] args) throws FileNotFoundException, IOException, CorruptedConfigFileException, IllegalParameterValueException, InvalidFiberIdException {
		// TODO Auto-generated method stub
		
		String netName ="AN"; 
		String trafficType = "Homogenous";
		String option ="Option1";
		AstarConfig aConfigMLR = AstarConfig.readAstarConfig("./Inputs/Reinhardt/AstarConfigMLR_AN1.txt"); //AstarConfigMLR_AN.txt
		AuxGraph agMLR = new AuxGraph(aConfigMLR);
		
	/*	AstarConfig aConfigElastic = AstarConfig.readAstarConfig("./Inputs/Reinhardt/AstarConfigElastic_AN.txt"); //AstarConfigElastic_AN.txt
		AuxGraph agElastic = new AuxGraph(aConfigElastic);*/
		
		aConfigMLR._totalLoad= 2000;
		aConfigMLR._normalizedCapacity = false;
		aConfigMLR._capacityGranularity = 40;
		double N =0.20;
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(new File("./Outputs/ReinhardtResults/MLR_Analysis_"+netName+"_"+option+"_"+trafficType+ ".txt")));
		
		fileout.write("Number of Regenerators T0"+ "\t"+"Number of Regenerators T1"+"\t"+ "Number of Regenerators T2"+"\t" +"Number of Regenerators T3"+"\t"+"Number of Regenerators T4");
		fileout.newLine();
		
		
		TrafficGeneration MLR = new TrafficGeneration(agMLR, TrafficType._HomDistribution);
		MLR.writeTrafficFile(".\\Inputs\\TrafficMatrices\\", "TrafficIncrease_MLR_Option1_US_T0.txt", false);
		
		
		DemandList dList =DemandList.getDemandListFromTrafficGeneration(agMLR, MLR);
		
		DemandList dListMRL1 = new DemandList(dList._demands);
		DemandList dListMRL2 = new DemandList(dList._demands);
		DemandList dListMRL3 = new DemandList(dList._demands);
		DemandList dListMRL4 = new DemandList(dList._demands);
		
		for (int i= 1; i <= 4; i++) {
			double Ni = N;
			DemandList list = null;
			DemandList listPrev = null;
			switch(i) {
			case 1:
				list = dListMRL1;
				listPrev = dList;
				break;
			case 2:
				list = dListMRL2;
				listPrev = dListMRL1;
				break;
			case 3:
				list = dListMRL3;
				listPrev = dListMRL2;
				break;
			case 4:
				list = dListMRL4;
				listPrev = dListMRL3;
				break;
				
			}
			for (int index =0; index <list._demands.size(); index++) {
				list.get(index).set_capacity(Ni * listPrev.get(index).capacity()); 
			}
		}
		Hashtable<String, double[]> costErosion = new Hashtable<String, double[]> ();
		for (String label :aConfigMLR._dataRates.getRateLabels()) {
			if(label.equals("TSP10")) {
				double[] costVectorMLR = {1,1,1,1,1};
				costErosion.put(label, costVectorMLR);
			}else if (label.equals("TSP40")) {
				double[] costVectorMLR = {3, 2.8, 2.5, 2, 1.8};
				costErosion.put(label, costVectorMLR);
			}else if(label.equals("TSP100")) {
				double[] costvectorMLR = {7, 4.8, 3.8, 2.8, 2.5};
				costErosion.put(label, costvectorMLR);
				
			}
		}
		double[] networkCostFirstCriteriaMLR  = {0, 0, 0, 0, 0};
		int[] routedConnectionsMLR = {0, 0, 0, 0, 0};
		int blockedConnectionsMLR = 0;
		int[] numReg ={0, 0, 0, 0, 0};
		double [] fiberOccupation= {0,0,0,0,0};
		int [] numRegRate10= {0,0,0,0,0};
		int [] numRegRate40= {0,0,0,0,0};
		int [] numRegRate100= {0,0,0,0,0};
		int[] connections10= {0,0,0,0,0};
		int[] connections40= {0,0,0,0,0};
		int[] connections100= {0,0,0,0,0};
		
		
		SingleLayerAdaptiveBitRate slAbr = new SingleLayerAdaptiveBitRate(aConfigMLR);
		VirtualGraph vg = new VirtualGraph(null, agMLR, slAbr.getSingleLayerRoutine()._routingParameters);
		for (int i =0; i <= 4; i++) {
			DemandList list = null;
			switch(i) {
			case 0:
				list = dList;
				break;
			case 1:
				list = dListMRL1;
				break;
			case 2:
				list = dListMRL2;
				break;
			case 3:
				list = dListMRL3;
				break;
			case 4:
				list = dListMRL4;
				break;
				
			}
			for (Demand d:list._demands) {
				ArrayList<OpticalConnection> opticalConnections = slAbr.RoutingTunableBitRate(d, 1);
				for (OpticalConnection oc : opticalConnections) {
					if(!oc.isEmpty()) {
						vg.reserveOpticalConnection((OpticalPath) oc);
						oc.display();
						String rateLabel ="";
						for (String label : aConfigMLR._dataRates.getRateLabels())
							if (oc.demandsList().get(0).dataRateValue() == aConfigMLR._dataRates.getDataRateList().get(label).getRateValue()) {
								rateLabel = label;
								break;
							}
						//adding code for computing the different routing rates
						if(rateLabel.equals("TSP10")) {
							numRegRate10[i]= oc.getRegNumber();
							connections10[i]=slAbr.getVirtualGraph().getVirtualLinks().size()/2;
			
						}else if(rateLabel.equals("TSP40")) {
							numRegRate40[i]= oc.getRegNumber();
							connections40[i]=slAbr.getVirtualGraph().getVirtualLinks().size()/2;
						}else if (rateLabel.equals("TSP100")) {
							numRegRate100[i]= oc.getRegNumber();
							connections100[i]=slAbr.getVirtualGraph().getVirtualLinks().size()/2;
							
						}
						
						
						networkCostFirstCriteriaMLR[i] += 2 * (1+ oc.getRegNumber()) *costErosion.get(rateLabel)[i];
						routedConnectionsMLR[i]++;
						 numReg[i] = oc.getRegNumber();
						
							
						 
					}
					else {
						blockedConnectionsMLR++;
					}
				}
				 
				
			}
			 fiberOccupation[i]= slAbr.getSingleLayerRoutine()._auxGraph.getAverageAndStdLinkOccupancy()[0];
		}
		System.out.println("Analysis of the Mixed Line Rate Scenario");
		System.out.println("BlockedConnectionsMRL: "+ blockedConnectionsMLR);
		System.out.println("Mixed Rate Scenario: "+"Total number of Connections T0: "+ routedConnectionsMLR[0]+ "\t" + "Total number of Connections T1: "+ routedConnectionsMLR[1]+ "\t" + "Total number of Connections T2: "+ routedConnectionsMLR[2]+ "\t" + "Total number of Connections T3: "+ routedConnectionsMLR[3] + "\t" + "Total number of Connections T4: "+ routedConnectionsMLR[4]);
		System.out.println("Total Cost T0: " + networkCostFirstCriteriaMLR[0] + "\tTotal Cost T1: " + networkCostFirstCriteriaMLR[1]+ "\tTotal Cost T2: " + networkCostFirstCriteriaMLR[2] + "\tTotal Cost T3: " + networkCostFirstCriteriaMLR[3]+ "\tTotal Cost T4: " + networkCostFirstCriteriaMLR[4]);
		System.out.println("The number of regenerators of 10Gb/s at T0: "+ numRegRate10[0]+ "\tThe number of regenerators at 40Gb/s at T0: "+ numRegRate40[0]+  "\tThe number of regenerators at 100Gb/s at T0: "+ numRegRate100[0] + "\tThe number of regenerators at 10Gb/s at T1: "+ numRegRate10[1]+ "\tThe number of regenerators at 40Gb/s at T1: "+ numRegRate40[1]+ "\tThe number of regenerators at 100Gb/s at T1: "+ numRegRate100[1]+ "\tThe number of regenerators at 10Gb/s at T2: "+ numRegRate10[2]+ "\tThe number of regenerators at 40Gb/s at T2: "+ numRegRate40[2]+ "\tThe number of regenerators at 100Gb/s at T2: "+ numRegRate100[2]+ "\tThe number of regenerators at 10Gb/s at T3: "+ numRegRate10[3]+ "\tThe number of regenerators at 40Gb/s at T3: "+ numRegRate40[3]+ "\tThe number of regenerators at 100Gb/s at T3: "+ numRegRate100[3]+ "\tThe number of regenerators at 10Gb/s at T4: "+ numRegRate10[4]+ "\tThe number of regenerators at 40Gb/s at T4: "+ numRegRate40[4]+ "\tThe number of regenerators at 100Gb/s at T4: "+ numRegRate100[4]);
				
				
				//"\tThe number of connections at 10Gb/s: "+ connections10[0]+"\tThe number of regenerators at 40Gb/s: "+ numRegRate40[1]+  "\tThe number of connections at 40Gb/s: "+ connections40[1] + "\tThe number of regenerators at 100Gb/s: "+ numRegRate100[2]+"\tThe number of connections at 100Gb/s: "+ connections100[1] );
		
		
		System.out.println("The Mean fiber occupancy at T0  is :"+ fiberOccupation[0] + "\tThe Mean fiber occupancy at T1  is : " + fiberOccupation[1] + "\tThe Mean fiber occupancy at T2  is : "  + fiberOccupation[2] + "\tThe Mean fiber occupancy at T3 is :"+ fiberOccupation[3]+  "\tThe Mean fiber occupancy at T4 is :"+ fiberOccupation[4]);
		fileout.write(numReg[0] + "\t" + numReg[1]+ "\t" + numReg[2] + "\t"+ numReg[3] + "\t"+ numReg[4]);
		fileout.newLine();
		fileout.newLine();
		fileout.newLine();
		
		//writing connections on the file
		fileout.write("BlockedConnectionsMRL" + "\t" + "Total number of Connections T0" + "\t" + "Total number of Connections T1" + "\t" + "Total number of Connections T2" + "\t" + "Total number of Connections T3" +"\t"+ "Total number of Connections T4");
		fileout.newLine();
		fileout.write(blockedConnectionsMLR + "\t" + routedConnectionsMLR[0] + "\t" + routedConnectionsMLR[1]+ "\t" + routedConnectionsMLR[2] + "\t" + routedConnectionsMLR[3] + "\t" + routedConnectionsMLR[4]);
		fileout.newLine();
		fileout.newLine();
		fileout.newLine();
		
		//writing costs on the file
		fileout.write("Total Cost T0" + "\t" + "Total Cost T1" +"\t" + "Total Cost T2"+"\t" + "Total Cost T3"+ "\t" + "Total Cost T4");
		fileout.newLine();
		fileout.write(networkCostFirstCriteriaMLR[0] + "\t" + networkCostFirstCriteriaMLR[1] + "\t" + networkCostFirstCriteriaMLR[2] + "\t" + networkCostFirstCriteriaMLR[3]+"\t" + networkCostFirstCriteriaMLR[4] );
		fileout.newLine();
		fileout.newLine();
		
		

		
		//writing fiber occupancy on the file
		
		fileout.write("Mean fiber occupancy at T0"+ "\t"+"Mean fiber occupancy at T1"+"\t"+ "Mean fiber occupancy at T2"+"\t" +"Mean fiber occupancy at T3"+"\t"+ "Mean fiber occupancy at T4");
		fileout.newLine();
		fileout.write(fiberOccupation[0] +"\t"+ fiberOccupation[1] +"\t"+fiberOccupation[2] +"\t"+fiberOccupation[3] +"\t"+fiberOccupation[4]);
		fileout.newLine();
		fileout.newLine();
		fileout.newLine();

		//writing regenerators per rate on the file
	    fileout.write("The number of 10Gb/s regenerators at T0"+ "\t"+"The number of 10Gb/s regenerators at T1"+"\t"+"The number of 10Gb/s regenerators at T2"+ "\t"+"The number of 10Gb/s regenerators at T3"+ "\t"+"The number of 10Gb/s regenerators at T4");
	    fileout.newLine();
	    fileout.write(numRegRate10[0]+"\t"+numRegRate10[1]+"\t"+numRegRate10[2]+"\t"+numRegRate10[3]+"\t"+numRegRate10[4]);
	    fileout.newLine();
		fileout.newLine();
		fileout.newLine();
		
		    fileout.write("The number of 40Gb/s regenerators at T0"+ "\t"+"The number of 40Gb/s regenerators at T1"+"\t"+"The number of 40Gb/s regenerators at T2"+ "\t"+"The number of 40Gb/s regenerators at T3"+ "\t"+"The number of 40Gb/s regenerators at T4");
		    fileout.newLine();
		    fileout.write(numRegRate40[0]+"\t"+numRegRate40[1]+"\t"+numRegRate40[2]+"\t"+numRegRate40[3]+"\t"+numRegRate40[4]);
		    fileout.newLine();
			fileout.newLine();
			fileout.newLine();
			
			fileout.write("The number of 100Gb/s regenerators at T0"+ "\t"+"The number of 100Gb/s regenerators at T1"+"\t"+"The number of 100Gb/s regenerators at T2"+ "\t"+"The number of 100Gb/s regenerators at T3"+ "\t"+"The number of 100Gb/s regenerators at T4");
		    fileout.newLine();
		    fileout.write(numRegRate100[0]+"\t"+numRegRate100[1]+"\t"+numRegRate100[2]+"\t"+numRegRate100[3]+"\t"+numRegRate100[4]);
		    fileout.newLine();
			fileout.newLine();
			fileout.newLine();
			
			//writing number of connections per rate.
			 fileout.write("The number of 10Gb/s connections at T0"+ "\t"+"The number of 10Gb/s connections at T1"+"\t"+"The number of 10Gb/s connections at T2"+ "\t"+"The number of 10Gb/s connections at T3"+ "\t"+"The number of 10Gb/s connections at T4");
			    fileout.newLine();
			    fileout.write(connections10[0]+"\t"+connections10[1]+"\t"+connections10[2]+"\t"+connections10[3]+"\t"+connections10[4]);
			    fileout.newLine();
				fileout.newLine();
				fileout.newLine();
				
				
				 fileout.write("The number of 40Gb/s connections at T0"+ "\t"+"The number of 40Gb/s connections at T1"+"\t"+"The number of 40Gb/s connections at T2"+ "\t"+"The number of 40Gb/s connections at T3"+ "\t"+"The number of 40Gb/s connections at T4");
				    fileout.newLine();
				    fileout.write(connections40[0]+"\t"+connections40[1]+"\t"+connections40[2]+"\t"+connections40[3]+"\t"+connections40[4]);
				    fileout.newLine();
					fileout.newLine();
					fileout.newLine();
					
					
					
					fileout.write("The number of 100Gb/s connections at T0"+ "\t"+"The number of 100Gb/s connections at T1"+"\t"+"The number of 100Gb/s connections at T2"+ "\t"+"The number of 100Gb/s connections at T3"+ "\t"+"The number of 100Gb/s connections at T4");
				    fileout.newLine();
				    fileout.write(connections100[0]+"\t"+connections100[1]+"\t"+connections100[2]+"\t"+connections100[3]+"\t"+connections100[4]);
				    fileout.newLine();
				    fileout.flush();
					fileout.close();
System.out.println("End of Simulation");
		

	}

}
