/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

public class NetworkCliqueMS {

  private Logger logger = Logger.getLogger(getClass().getName());


  private final HashMap<Pair<Integer,Integer>, Double> edges = new HashMap<>();//Edges of the undirected graph network
  private final HashMap<Integer,Integer> nodes = new HashMap<>(); // Indicates if edge is inside or not a clique
  private final HashMap<Pair<Integer,Integer>, Boolean> edgeClique = new HashMap<>(); // Hashmap of edges containing if they belong or not to a clique
  private final HashMap<Integer, List<Integer>> neighbours = new HashMap<>();// Hashmap containing list of nodes for each node
  private final HashMap<Integer, List<Integer>> cliques = new HashMap<>(); // unordered map of key clique name with a vector of the nodes of that clique
  private final HashMap<Pair<Integer,Integer>, Double> logEdges = new HashMap<>();  // log value of weigth powered to some exponent
  private final HashMap<Pair<Integer,Integer>, Double> minusLogEdges = new HashMap<>();  // 1 - log value of weight powered to some exponent

  //Result of Kernighan and aggregate algorithm
  private final List<Pair<Integer,Integer>> resultNode_clique = new ArrayList<>();

  private void createEdges(double[][] adjacencyMatrix, List<Integer> nodeIDList){
    for(int i=0; i<adjacencyMatrix.length ; i++){
      for(int j=i+1; j<adjacencyMatrix[0].length ; j++ ){
        if(adjacencyMatrix[i][j]>0.0){
          Pair<Integer, Integer> p = new Pair(nodeIDList.get(i), nodeIDList.get(j));
          this.edges.put(p,adjacencyMatrix[i][j]);
        }
      }
    }
  }

// Function createNodes, with node x you can access clique value y
  private void createNodesFromEdges(){
    for(Pair<Integer,Integer> p : this.edges.keySet()){
      if(!this.nodes.containsKey(p.getKey())){
        this.nodes.put(p.getKey(),p.getKey());
      }
      if(!this.nodes.containsKey(p.getValue())){
        this.nodes.put(p.getValue(),p.getValue());
      }
    }
  }

  //create neighbour for each node
  private void createNeighboursFromEdges(){
    for(Pair<Integer,Integer> p : this.edges.keySet()){
      Integer n1 = p.getKey();
      Integer n2 = p.getValue();
      {
        if (this.neighbours.containsKey(n1)) {
          this.neighbours.get(n1).add(n2);
        } else {
          List<Integer> l = new ArrayList();
          l.add(n2);
          this.neighbours.put(n1, l);
        }
      }
      {
        if (this.neighbours.containsKey(n2)) {
          this.neighbours.get(n2).add(n1);
        } else {
          List<Integer> l = new ArrayList();
          l.add(n1);
          this.neighbours.put(n2, l);
        }
      }
    }
  }


// Function createEdgeclique for creating boolean variable to indicate if this edge is inside or not a clique
  private void createEdgeCliques (){
    for(Pair<Integer,Integer> p : this.edges.keySet()){
      this.edgeClique.put(p, false);// at the beginning, all edges are outside of cliques
    }
  }

// Function createCliques to create an unordered map of key clique name with a vector of the nodes of that clique
  private void createCliques(){
    for(Integer k : this.nodes.keySet()){
      if(this.cliques.keySet().contains(k)){
        this.cliques.get(k).add(this.nodes.get(k));
      }
      else{
        List <Integer> l = new ArrayList<>();
        l.add(this.nodes.get(k));
        this.cliques.put(k,l);
      }
    }
  }

  //initializeNetwork
  private void createNetwork(double [][] adjacencyMatrix, List<Integer> nodeIDList){
    //import edges
    double exp = 2.0;
    createEdges(adjacencyMatrix, nodeIDList);
    createNodesFromEdges();
    createNeighboursFromEdges();
    createCliques();
    createEdgeCliques();
    for(Pair<Integer,Integer> edge : this.edges.keySet()){
      Double weight, logPower, minuslogPower;
      weight = this.edges.get(edge);
      logPower = Math.log10(Math.pow(weight,exp));
      minuslogPower = Math.log10(1.0 - Math.pow(weight,exp));
      logEdges.put(edge,logPower);
      minusLogEdges.put(edge,minuslogPower);
    }
  }

  // Function to calculate log likelihood of the whole network
  private Double logltotal(){
    Double inside = 0.0, outside = 0.0 , logl = 0.0;
    for(Pair<Integer,Integer> edge : edges.keySet()){
      if(this.edgeClique.get(edge)){
        inside += this.logEdges.get(edge);
      }
      else{
        outside += this.minusLogEdges.get(edge);
      }
    }
    logl = inside + outside;
    return logl;
  }

  private Pair<Integer,Integer> sortEdge(Pair<Integer,Integer> edge){
    if(edge.getKey() > edge.getValue()){
      Pair <Integer,Integer> e = new Pair(edge.getValue(),edge.getKey());
      return e;
    }
    return edge;
  }

  // Function to compute the change of likelihood if we add node2 to clique of node1
  private Nodelogl calcNodelogl(Integer node1, Integer node2){
    Nodelogl nResult = new Nodelogl();
    Double logl = -1.0, logl_change = 0.0, logl_before = 0.0;
    Boolean complete = true;
    Double newlinks_change = 0.0, nolinks_change = 0.0, newlinks_before = 0.0, nolinks_before = 0.0;
    List<Pair<Integer,Integer>> newEdges = new ArrayList<>();
    List<Pair<Integer,Integer>> oldEdges = new ArrayList<>();
    Integer clique1 = this.nodes.get(node1);
    Integer clique2 = this.nodes.get(node2);
    List<Integer> nClique1 = new ArrayList<>(this.cliques.get(clique1));
    nClique1.remove(node1);// remove the node1 apart from the other nodes of its clique
    List<Integer> nClique2 = new ArrayList<>(this.cliques.get(clique2));
    nClique2.remove(node2);// remove the node1 apart from the other nodes of its clique
    if(nClique1.size() > 0){
      for(Integer i : nClique1){
        Pair<Integer,Integer> edge = new Pair<>(i,0);
        edge = sortEdge(edge);
        if(!logEdges.containsKey(edge)){
          complete = false;
          break;
        }
        else{
          // edges that now will be part of the clique
          newlinks_change += this.logEdges.get(edge);
          // this edges were before outside cliques
          newlinks_before += this.minusLogEdges.get(edge);
          newEdges.add(edge);
        }
      }
    }
    if(nClique2.size() > 0){
      for(Integer i : nClique2){
        Pair<Integer,Integer> edge = new Pair<>(i,node2);
        edge = sortEdge(edge);
        // this edges now will be outside cliques
        nolinks_change += this.minusLogEdges.get(edge);
        // this edges between node2 and its old clique members were inside cliques before
        nolinks_before += this.logEdges.get(edge);
        oldEdges.add(edge);
      }
    }
    if(complete){
      Pair<Integer,Integer> edge = new Pair<>(node1 , node2);
      edge = sortEdge(edge);
      newlinks_change += this.logEdges.get(edge);
      newlinks_before += this.minusLogEdges.get(edge);
      newEdges.add(edge);
      logl_change = newlinks_change + nolinks_change;
      logl_before = newlinks_before + nolinks_before;
      logl = logl_change - logl_before;
    }
    nResult.logl = logl;
    nResult.newnode = node1; // initialize results values in case there is a break
    nResult.newedges = newEdges;
    nResult.oldedges = oldEdges;
    return nResult;
  }

  // function to move the current node to another clique
  Double reassignNode(Integer node, Double logl){
    Nodelogl maxChange = new Nodelogl();
    maxChange.logl = 0.0;
    if(this.neighbours.get(node).size() > 0){ // reassign this node if it has neighbors
      Integer ownClique = this.nodes.get(node);
      Set<Integer> diffCliques = new HashSet<>();
      for(Integer n:neighbours.get(node)){
        Integer cliqueCandidate = this.nodes.get(n);
        if(!cliqueCandidate.equals(ownClique))
            diffCliques.add(cliqueCandidate);
      }
      if(diffCliques.size() > 0){
        for(Integer n: diffCliques){
          Integer node2 = this.cliques.get(n).get(0); // first node in the clique candidate
          Nodelogl nodeChange = calcNodelogl(node2 , node); // logl change if we move node to clique of node2
          if(nodeChange.logl > maxChange.logl){ // we search for the max change in logl bigger than 0
            maxChange = nodeChange;
          }
        }
      }
      if(maxChange.logl > 0){ // if there is a positive change in logl by moving a node, now execute this change
        for(Pair<Integer, Integer> edge : maxChange.newedges)
          this.edgeClique.put(edge,true); // change makes new edges are inside cliques
        for(Pair<Integer, Integer> edge : maxChange.oldedges)
          this.edgeClique.put(edge,false); // change puts edges outside cliques
        Integer newClique = this.nodes.get(maxChange.newnode);
        this.nodes.put(node, newClique); // now the clique of node is the clique of node2
        this.cliques.get(ownClique).remove((Integer) node); // remove the node from clique nodes of old clique
        this.cliques.get(newClique).add(node); // add node to the list of nodes of the new clique
      }
    }
    Double newlogl = logl + maxChange.logl;
    return newlogl;
  }

  private Nodelogl calcCliquelogl(Integer clique1 ,Integer clique2){
    Nodelogl cResult = new Nodelogl();
    Double logl = -1.0, logl_change = 0.0, logl_before = 0.0;
    Boolean complete = true;
    List<Pair<Integer,Integer>> newEdges = new ArrayList<>();
    List<Pair<Integer,Integer>> oldEdges = new ArrayList<>();
    if(clique2.equals(0)){
      complete = false;
    }
    else{
      Outer: for(Integer v1 : this.cliques.get(clique1)){
        for(Integer v2 : this.cliques.get(clique2)){
          Pair<Integer, Integer> edge = new Pair(v1,v2);
          edge = sortEdge(edge);
          if(!this.logEdges.containsKey(edge)){
            complete = false;
            break Outer; // exit the two loops if one link between the two cliques does not exist
          }
          else{
            logl_change += this.logEdges.get(edge); // edges that now will be part of the clique
            logl_before += this.minusLogEdges.get(edge); // this edges were before outside cliques
            newEdges.add(edge);
          }
        }
      }
    }
    if(complete)
      logl = logl_change - logl_before;
    cResult.newnode = clique1; // initialize results values in case there is a break
    cResult.logl = logl;
    cResult.newedges = newEdges;
    return cResult;
  }

  Double meanClique(Integer clique1, Integer clique2){
    Double meanV = 0.0; Double meanR = -1.0;
    Integer size = 0;
    Boolean complete = true;
    Outer : for(Integer v1 : this.cliques.get(clique1)){
      for(Integer v2 : this.cliques.get(clique2)){
        Pair<Integer,Integer>  edge = new Pair<>(v1,v2);
        edge = sortEdge(edge);
        if(!this.logEdges.containsKey(edge)){
          complete = false;
          break Outer;
        }
        else{
          Double weight = this.edges.get(edge);
          meanV += Math.pow(weight,2.0);
          size++;
        }
      }
    }
    if(complete)
      meanR = meanV/Double.valueOf(size);
    return meanR;
  }


  Double reassignClique(Integer clique, Double logl){
    Double loglchange = 0.0; // the change in logl if we accept joining clique to another clique
    Integer node = this.cliques.get(clique).get(0);
    Set<Integer> diffCliques = new HashSet<>();
    for(Integer v : this.neighbours.get(node)){
      Integer cliqueCandidate = this.nodes.get(v);
      if(!cliqueCandidate.equals(clique))
        diffCliques.add(cliqueCandidate);
    }
    if(diffCliques.size() > 0 ){
      Double maxMean = 0.0;
      Integer maxClique = 0;
      for(Integer c : diffCliques){
        Double meanCandidate = meanClique(clique,c);
        if(meanCandidate > maxMean){
          maxClique = c;
          maxMean = meanCandidate;
        }
      }
      Nodelogl loglCandidate = calcCliquelogl(clique,maxClique);
      if(loglCandidate.logl > 0){
        loglchange = loglCandidate.logl;
        for(Pair<Integer,Integer> edge : loglCandidate.newedges)
          this.edgeClique.put(edge, true);// change makes new edges be inside cliques
        for(Integer c : this.cliques.get(clique)){
          this.nodes.put(c,maxClique); // change clique value of old clique nodes to the new
          this.cliques.get(maxClique).add(c); // add nodes of old clique to the list of nodes of the new clique
        }
        this.cliques.remove(clique); // delete old clique because it is empty
      }
    }
    Double loglReturn = logl + loglchange;
    return loglReturn;
  }

  //csample_integer function of R  code makes same effect as Collections.shuffle when size and x.size() are same and replace is set false

  List<Double> itReassign(Double tol, Double logl){
    Double currentlogl = logl;
    List<Double> loglResult = new ArrayList<>();
    loglResult.add(currentlogl);
    List<Integer> allnodes = new ArrayList<>(this.nodes.keySet());
    List<Integer> randallnodes = new ArrayList<>(allnodes);
    Collections.shuffle(randallnodes);
    for(Integer v : randallnodes){
      currentlogl = reassignNode(v,currentlogl);
      loglResult.add(currentlogl);
    }
    Double firstlogl = loglResult.get(0);
    Double diff = 1 - Math.abs(currentlogl/firstlogl); // difference in log likelihood after one complete round of node reassignments
    Integer rcount = 1; // counter of the number of rounds
    while( diff > tol ){
      Double lastlogl = loglResult.get(loglResult.size()-1);
      Collections.shuffle(randallnodes);
      for(Integer v : randallnodes){
        currentlogl = reassignNode(v,currentlogl); // move nodes to different cliques
        loglResult.add(currentlogl); // store results of change in logl
      }
      diff = 1 - Math.abs(currentlogl/lastlogl); // difference in log likelihood after one complete round of node reassignments
      rcount++;
    }
    logger.log(Level.INFO, "Kernighan-Lin done with "+rcount+" rounds");
    return loglResult;
  }

  List<Double> aggregateAndKernighan (Double tol, Integer step, Boolean silent ){
    Double currentlogl = logltotal();
    List<Double> loglResult = new ArrayList<>();
    loglResult.add(currentlogl);
    // round 1
    List<Integer> randallNodes;
    // insert allnodes values in list of nodes
    List<Integer> allNodes = new ArrayList<>(this.nodes.keySet());
    randallNodes = new ArrayList<>(allNodes);
    Collections.shuffle(randallNodes);
    int scount = 1; // counter of the number of rounds that are clique joining, it starts with 1
    int tcount = 1; // total number of rounds
    for(int randpos = 0; randpos < randallNodes.size(); randpos++) {
      Integer nodev = randallNodes.get(randpos);
      Integer cliquec = this.nodes.get(nodev); // clique that will be joined to another clique
      if (scount == step){
        currentlogl = reassignNode(nodev, currentlogl);
        loglResult.add(currentlogl);
        scount = 1;
        tcount++;
      }
      else{
        currentlogl = reassignClique(cliquec,currentlogl);
        loglResult.add(currentlogl);
        scount++;
        tcount++;
      }
    }
    Double firstlogl = loglResult.get(0);
    Double diff = 1.0 - Math.abs(currentlogl/firstlogl); // difference in log likelihood after one complete round of node reassignments
      // rest of rounds
    if(!silent){
      logger.log(Level.INFO,"After " + tcount + " rounds logl is " + currentlogl);
      logger.log(Level.INFO,"Still computing cliques");
    }
    while( diff > tol ){
      Set<Integer> cliquesRound = new HashSet<>();
      Double lastlogl = loglResult.get(loglResult.size()-1);
      Collections.shuffle(randallNodes);
      for(int randposw = 0; randposw < randallNodes.size();randposw++){
        Integer nodevw = randallNodes.get(randposw);
        Integer cliquecw = this.nodes.get(nodevw); // clique that will be joined to another clique
        if(scount == step){
          currentlogl = reassignNode(nodevw , currentlogl);
          loglResult.add(currentlogl);
          scount = 1;
          tcount ++;
        }
        else{ // join this clique if it has not been joined in this round
          if(!cliquesRound.contains(cliquecw)){
            cliquesRound.add(cliquecw);
            currentlogl = reassignClique(cliquecw , currentlogl);
            loglResult.add(currentlogl);
            scount++;
            tcount++;
          }
        }
      }
      cliquesRound.clear(); // remove all cliques from the set for the next while round
      diff = 1.0 - Math.abs(currentlogl/lastlogl);
      if(!silent){
        logger.log(Level.INFO,"After "+tcount+" rounds logl is "+currentlogl );
        logger.log(Level.INFO, "Still computing cliques");
      }
    }
    logger.log(Level.INFO,"Aggregate cliques done, with "+tcount+" rounds.");
    // Kernighan-Lin after aggregation of cliques
    List<Double> loglLast = itReassign(tol,currentlogl);
    loglResult.addAll(loglLast);
    return loglResult;
  }

  public void returnCliques(double[][] adjacencyMatrix, List<Integer> nodeIDList,  double tolerance, boolean silent){
    try{
      createNetwork(adjacencyMatrix, nodeIDList);
      Double logl = logltotal();
      logger.log(Level.INFO,"Beginning value of logl is "+logl);
      int step = 10;
      List<Double> loglList = aggregateAndKernighan(tolerance, step, silent);
      for(Integer v: this.nodes.keySet()){
        Pair<Integer,Integer> p = new Pair<>(v,this.nodes.get(v));
        resultNode_clique.add(p);
      }
      Double loglfinal = logltotal();
      logger.log(Level.INFO,"Finishing value of logl is "+ loglfinal);
    }
    catch (Exception e){
      e.printStackTrace();
    }
  }

  public List<Pair<Integer, Integer>> getResultNode_clique() {
    return resultNode_clique;
  }
}

// Nodelogl class to return an object with the change in logl, the new edges inside clique and the
// oldedges than will become outside edges
class Nodelogl {
  Double logl;
  Integer newnode;
  List<Pair<Integer,Integer>> newedges;
  List<Pair<Integer,Integer>> oldedges;
};
