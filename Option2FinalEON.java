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

public class Option2FinalEON {

	public static void main(String[] args) throws FileNotFoundException, IOException, CorruptedConfigFileException, IllegalParameterValueException, InvalidFiberIdException {
		// TODO Auto-generated method stub
		String netName = "DT"; //alternate with US
		String trafficType ="HOMOGENOUS"; //alternate with Gaussian
		String option ="Option2"; 
		
		
	//	AstarConfig aConfigMLR = AstarConfig.readAstarConfig("./Inputs/Reinhardt/AstarConfigMLR_DT.txt");
	//	AuxGraph agMLR = new AuxGraph(aConfigMLR);
		
		AstarConfig aConfigElastic = AstarConfig.readAstarConfig("./Inputs/Reinhardt/AstarConfigElastic_DT.txt");
		AuxGraph agElastic = new AuxGraph(aConfigElastic);
		
		aConfigElastic._totalLoad= 10000;
		aConfigElastic._normalizedCapacity= false;
		aConfigElastic._capacityGranularity= 5.0;
		double N = 0.20;
		
		for (int seed=1; seed <=1; seed++) {
			BufferedWriter fileout1 = new BufferedWriter (new FileWriter(new File("./Outputs/ReinhardtResults/Elastic_Analysis_"+netName+"_"+option+"_" +trafficType+"_seed" +seed+ ".txt")));
			
			fileout1.write("BlockedConnectionsMRL" + "\t"+"Connections at T0"+ "\t"+"Connections at  T1"+"\t"+ "Connections at T2"+"\t" +"Connections at  T3"+"\t"+"Connections at  T4");
			fileout1.newLine();
			
			TrafficGeneration MLR = new TrafficGeneration(agElastic, TrafficType._HomDistribution);
			MLR.writeTrafficFile(".\\Inputs\\TrafficMatrices\\Homogeneous"  + "\\","TrafficMatrix2" + "_seed" + seed + option +".txt", false);
			
			String inputTrafficFile = ".\\Inputs\\TrafficMatrices\\Homogeneous" + "\\" + "TrafficMatrix2" + "_seed" + seed+ option +".txt";
			DemandList dList = DemandList.getMatrixFromFile(agElastic, inputTrafficFile);
			
			//Elastic part
	         DemandList dlistElastic1 = new DemandList(dList._demands);
	         DemandList dlistElastic2 = new DemandList(dList._demands);
	         DemandList dlistElastic3 = new DemandList(dList._demands);
	         DemandList dlistElastic4 = new DemandList(dList._demands);
	         
	         double previousLoad =aConfigElastic._totalLoad;
	         for (int i =1; i<=4; i++) {
	        	 double newLoad = previousLoad* N;
	        	 TrafficGeneration mediumGeneration = new TrafficGeneration(agElastic, TrafficType._HomDistribution);
	        	 DemandList medium = DemandList.getDemandListFromTrafficGeneration(agElastic, mediumGeneration);
	        	 DemandList list = null;
	        	 switch(i) {
	        	 case 1:
	        		 list = dlistElastic1;
	        		 break;
	        	 case 2:
	        		 dlistElastic2 = new DemandList(dlistElastic1._demands);
	        		 list = dlistElastic2;
	        		 break;
	        	 case 3:
	        		 dlistElastic3 = new DemandList(dlistElastic2._demands);
	        		 list = dlistElastic3;
	        		 break;
	        	 case 4:
	        		 dlistElastic4 = new DemandList(dlistElastic3._demands);
	        		 list = dlistElastic4;
	        		 break;
	        	 }
	        	 
	        	 for (Demand d : medium._demands) {
	        		 boolean found = false;
	        		 for (Demand d2 : list._demands) {
	        			 if((d.source().equals(d2.source()) && (d.dest().equals(d2.dest()))) || (d.source().equals(d2.dest()) && (d.dest().equals(d2.source()))) ) {
	        				 d2.set_capacity(d2.capacity() + d.capacity());
	        				 found = true;
	        				 
	        			 }
	        		 }
	        		 if (!found)
	        			 list._demands.add(d); 
	        	 }
	        	 previousLoad = newLoad;
	         }
	         //Different costs
				double[] costVector = {4.1, 3.7, 3.3, 2.9, 2.6};
				double [] networkCostFirstCriteriaEl = {0,0, 0, 0, 0};
				int[] routedConnectionsEl = {0, 0, 0, 0, 0};
				int numRegPrev = 0;
				int numConnectionPrev =0;
				int blockedConnectionEON = 0;
			    double [] fiberOccupation= {0,0,0,0,0};
				int [] numRegRate10= {0,0,0,0,0};
				int [] numRegRate40= {0,0,0,0,0};
				int [] numRegRate100= {0,0,0,0,0};
				int[] connections10= {0,0,0,0,0};
				int[] connections40= {0,0,0,0,0};
				int[] connections100= {0,0,0,0,0};
				
				
				for (int i=0; i<=4; i++) {
					DemandList list = null;
					switch(i) {
					case 0:
						list = dList;
						break;
					case 1:
						list = dlistElastic1;
						break;
					case 2:
						list = dlistElastic2;
						break;
					case 3:
						list = dlistElastic3;
						break;
					case 4:
						list = dlistElastic4;
						break;
					
				}
					
					int numReg = 0;
					int numConnection = 0;
					
					SingleLayerAdaptiveBitRate slAbr = new SingleLayerAdaptiveBitRate(aConfigElastic);
				      VirtualGraph vg = new VirtualGraph(null, agElastic, slAbr.getSingleLayerRoutine()._routingParameters);
				      for(Demand d:list._demands) {
				    	  ArrayList<OpticalConnection> opticalConnections = slAbr.RoutingTunableBitRate(d, 1);
				    	  for (OpticalConnection oc: opticalConnections) {
				    		  if(!oc.isEmpty()) {
				    			 vg.reserveOpticalConnection((OpticalPath) oc);
				    			 oc.display();
				    			 String rateLabel ="";
				    			 for(String label : aConfigElastic._dataRates.getRateLabels())
				    				 if (oc.demandsList().get(0).dataRateValue() == aConfigElastic._dataRates.getDataRateList().get(label).getRateValue() ) {
				    					 rateLabel =label;
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
				    			 
				    			 networkCostFirstCriteriaEl[i] += 2*(1 + oc.getRegNumber())* costVector[i];
				    			 routedConnectionsEl[i]++;
				    			 numReg +=oc.getRegNumber();
				    			 numConnection++;
				    		  }
				    		  else {
				    			  blockedConnectionEON++;
				    			 
				    	  }
				    		  fiberOccupation[i]= slAbr.getSingleLayerRoutine()._auxGraph.getAverageAndStdLinkOccupancy()[0];	  
			}
		      
		      }
				    //updated cost
				      
				      networkCostFirstCriteriaEl[i] -= 2*(numRegPrev + numConnectionPrev)* costVector[i];
				      numRegPrev = numReg;
				      numConnectionPrev = numConnection;
				     
				      
				      /**
				      System.out.println("Analysis of the Elastic Rate Scenario");
						System.out.println("BlockedConnectionsElastic: "+ blockedConnectionEON);
						System.out.println("Elastic Rate Scenario: "+"Total number of Connections T1: "+ routedConnectionsEl[0]+ "\t" + "Total number of Connections T2: "+ routedConnectionsEl[1]+ "\t" + "Total number of Connections T3: "+ routedConnectionsEl[2]+ "\t" + "Total number of Connections T4: "+ routedConnectionsEl[3]);
						System.out.println("Total Cost T1: " + networkCostFirstCriteriaEl[0] + "\tTotal Cost T2: " + networkCostFirstCriteriaEl[1]+ "\tTotal Cost T3: " + networkCostFirstCriteriaEl[2] + "\tTotal Cost T4: " + networkCostFirstCriteriaEl[3]);
						**/
	      
						
				
				}
				//writing a file
				fileout1.write(blockedConnectionEON + "\t"+routedConnectionsEl[0]+ "\t" + routedConnectionsEl[1] + "\t"+routedConnectionsEl[2] + "\t"+routedConnectionsEl[3] + "\t"+routedConnectionsEl[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();
				
				fileout1.write("Number of Regenerators");
				fileout1.newLine();
				fileout1.write(numRegPrev+ "\t");
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();
				
				
				fileout1.write("Total Cost T0" + "\t" + "Total Cost T1" +"\t" + "Total Cost T2"+"\t" + "Total Cost T3"+"\t"+ "Total Cost T4");
				fileout1.newLine();
				fileout1.write(networkCostFirstCriteriaEl[0] + "\t" + networkCostFirstCriteriaEl[1] + "\t" + networkCostFirstCriteriaEl[2] + "\t" + networkCostFirstCriteriaEl[3]+"\t" + networkCostFirstCriteriaEl[4] );
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();
				
				System.out.println("The Mean fiber occupancy at T0  is :"+ fiberOccupation[0] + "\tThe Mean fiber occupancy at T1  is : " + fiberOccupation[1] + "\tThe Mean fiber occupancy at T2  is : "  + fiberOccupation[2] + "\tThe Mean fiber occupancy at T3 is :"+ fiberOccupation[3]+  "\tThe Mean fiber occupancy at T4 is :"+ fiberOccupation[4]);
				
				fileout1.write("Mean fiber occupancy at T0"+ "\t"+"Mean fiber occupancy at T1"+"\t"+ "Mean fiber occupancy at T2"+"\t" +"Mean fiber occupancy at T3"+"\t"+ "Mean fiber occupancy at T4");
				fileout1.newLine();
				fileout1.write(fiberOccupation[0] +"\t"+ fiberOccupation[1] +"\t"+fiberOccupation[2] +"\t"+fiberOccupation[3] +"\t"+fiberOccupation[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();
				
				//writing regenerators per rate on the file
				fileout1.write("The number of 10Gb/s regenerators at T0"+ "\t"+"The number of 10Gb/s regenerators at T1"+"\t"+"The number of 10Gb/s regenerators at T2"+ "\t"+"The number of 10Gb/s regenerators at T3"+ "\t"+"The number of 10Gb/s regenerators at T4");
				fileout1.newLine();
				fileout1.write(numRegRate10[0]+"\t"+numRegRate10[1]+"\t"+numRegRate10[2]+"\t"+numRegRate10[3]+"\t"+numRegRate10[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();

				fileout1.write("The number of 40Gb/s regenerators at T0"+ "\t"+"The number of 40Gb/s regenerators at T1"+"\t"+"The number of 40Gb/s regenerators at T2"+ "\t"+"The number of 40Gb/s regenerators at T3"+ "\t"+"The number of 40Gb/s regenerators at T4");
				fileout1.newLine();
				fileout1.write(numRegRate40[0]+"\t"+numRegRate40[1]+"\t"+numRegRate40[2]+"\t"+numRegRate40[3]+"\t"+numRegRate40[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();

				fileout1.write("The number of 100Gb/s regenerators at T0"+ "\t"+"The number of 100Gb/s regenerators at T1"+"\t"+"The number of 100Gb/s regenerators at T2"+ "\t"+"The number of 100Gb/s regenerators at T3"+ "\t"+"The number of 100Gb/s regenerators at T4");
				fileout1.newLine();
				fileout1.write(numRegRate100[0]+"\t"+numRegRate100[1]+"\t"+numRegRate100[2]+"\t"+numRegRate100[3]+"\t"+numRegRate100[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();

				//writing number of connections per rate.
				fileout1.write("The number of 10Gb/s connections at T0"+ "\t"+"The number of 10Gb/s connections at T1"+"\t"+"The number of 10Gb/s connections at T2"+ "\t"+"The number of 10Gb/s connections at T3"+ "\t"+"The number of 10Gb/s connections at T4");
				fileout1.newLine();
				fileout1.write(connections10[0]+"\t"+connections10[1]+"\t"+connections10[2]+"\t"+connections10[3]+"\t"+connections10[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();


				fileout1.write("The number of 40Gb/s connections at T0"+ "\t"+"The number of 40Gb/s connections at T1"+"\t"+"The number of 40Gb/s connections at T2"+ "\t"+"The number of 40Gb/s connections at T3"+ "\t"+"The number of 40Gb/s connections at T4");
				fileout1.newLine();
				fileout1.write(connections40[0]+"\t"+connections40[1]+"\t"+connections40[2]+"\t"+connections40[3]+"\t"+connections40[4]);
				fileout1.newLine();
				fileout1.newLine();
				fileout1.newLine();



				fileout1.write("The number of 100Gb/s connections at T0"+ "\t"+"The number of 100Gb/s connections at T1"+"\t"+"The number of 100Gb/s connections at T2"+ "\t"+"The number of 100Gb/s connections at T3"+ "\t"+"The number of 100Gb/s connections at T4");
				fileout1.newLine();
				fileout1.write(connections100[0]+"\t"+connections100[1]+"\t"+connections100[2]+"\t"+connections100[3]+"\t"+connections100[4]);
				fileout1.newLine();
				fileout1.flush();
				fileout1.close();
				
				
				
				
				

			      //end of Elastic part
		}
		
						
		
		System.out.println("Finished Simulation");
	}
	
}
