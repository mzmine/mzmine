package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.datamodel.RawDataFile;
import it.unimi.dsi.fastutil.Hash;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javolution.io.Struct.Bool;

public class NetworkCliqueMS {

  private Logger logger = Logger.getLogger(getClass().getName());
  //Edges and Vertices for undirected graph network
  private HashMap<Pair<Integer,Integer>, Double> edges;
  private HashMap<Integer,Integer> nodes;
  //Indicates if edge is inside or not a clique
  private HashMap<Pair<Integer,Integer>, Boolean> edgeClique;
  private HashMap<Integer, List<Integer>> neighbours;
  private HashMap<Integer, List<Integer>> cliques;
  private HashMap<Pair<Integer,Integer>, Double> logEdges;
  private HashMap<Pair<Integer,Integer>, Double> minusLogEdges;

  private void addEdge(Integer a, Integer b, Double weight){
    Pair<Integer, Integer> p = new Pair(a,b);
    edges.put(p,weight);
  }

  private void computeNodes(){
    for(Pair<Integer,Integer> p : edges.keySet()){
      if(!nodes.containsKey(p.getKey())){
        nodes.put(p.getKey(),p.getKey());
      }
      if(!nodes.containsKey(p.getValue())){
        nodes.put(p.getValue(),p.getValue());
      }
    }
  }

  private void computeNeighbours(){
    for(Pair<Integer,Integer> p : edges.keySet()){
      Integer n1 = p.getKey();
      Integer n2 = p.getValue();
      {
        if (neighbours.containsKey(n1)) {
          neighbours.get(n1).add(n2);
        } else {
          List<Integer> l = new ArrayList();
          l.add(n2);
          neighbours.put(n1, l);
        }
      }
      {
        if (neighbours.containsKey(n2)) {
          neighbours.get(n2).add(n1);
        } else {
          List<Integer> l = new ArrayList();
          l.add(n1);
          neighbours.put(n2, l);
        }
      }
    }
  }


  //TODO initializeEdgeCliques?
  private void computeEdgeCliques (){
    for(Pair<Integer,Integer> p : edges.keySet()){
      edgeClique.put(p, false);
    }
  }

  private void computeClique(){
    for(Integer k : nodes.keySet()){
      if(cliques.keySet().contains(k)){
        cliques.get(k).add(nodes.get(k));
      }
      else{
        List <Integer> l = new ArrayList<>();
        l.add(nodes.get(k));
        cliques.put(k,l);
      }
    }
  }

  //initializeNetwork
  private void createNetwork(RawDataFile df, Double exp ){
    //import edges
    computeNodes();
    computeNeighbours();
    computeClique();
    computeEdgeCliques();
    for(Pair<Integer,Integer> edge : edges.keySet()){
      Pair<Integer, Integer> edgeEntry = edge;
      Double weight, logPower, minuslogPower;
      weight = edges.get(edge);
      logPower = Math.log10(Math.pow(weight,exp));
      minuslogPower = Math.log10(1 - Math.pow(weight,exp));
      logEdges.put(edgeEntry,logPower);
      minusLogEdges.put(edgeEntry,minuslogPower);
    }
  }

  // Function to calculate log likelihood of the whole network
  private Double logltotal(){
    Double inside = 0.0, outside = 0.0 , logl = 0.0;
    for(Pair<Integer,Integer> edge : edges.keySet()){
      if(edgeClique.get(edge)){
        inside += logEdges.get(edge);
      }
      else{
        outside += minusLogEdges.get(edge);
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
    Integer clique1 = nodes.get(node1);
    Integer clique2 = nodes.get(node2);
    List<Integer> nClique1 = cliques.get(clique1);
    nClique1.remove(node1);// remove the node1 apart from the other nodes of its clique
    List<Integer> nClique2 = cliques.get(clique2);
    nClique2.remove(node2);// remove the node1 apart from the other nodes of its clique
    if(nClique1.size() > 0){
      for(Integer i : nClique1){
        Pair<Integer,Integer> edge = new Pair<>(i,node2);
        edge = sortEdge(edge);
        if(!logEdges.containsKey(edge)){
          complete = false;
          break;
        }
        else{
          // edges that now will be part of the clique
          newlinks_change += logEdges.get(edge);
          // this edges were before outside cliques
          newlinks_before += minusLogEdges.get(edge);
          newEdges.add(edge);
        }
      }
    }
    if(nClique2.size() > 0){
      for(Integer i : nClique2){
        Pair<Integer,Integer> edge = new Pair<>(i,node2);
        edge = sortEdge(edge);
        // this edges now will be outside cliques
        nolinks_change += minusLogEdges.get(edge);
        // this edges between node2 and its old clique members were inside cliques before
        nolinks_before += logEdges.get(edge);
        oldEdges.add(edge);
      }
    }
    if(complete){
      Pair<Integer,Integer> edge = new Pair<>(node1 , node2);
      edge = sortEdge(edge);
      newlinks_change += logEdges.get(edge);
      newlinks_before += minusLogEdges.get(edge);
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
  Double reassignNode(int node, double logl){
    Nodelogl maxChange = new Nodelogl();
    maxChange.logl = 0.0;
    if(neighbours.get(node).size() > 0){ // reassign this node if it has neighbors
      Integer ownClique = nodes.get(node);
      Set<Integer> diffCliques = new HashSet<>();
      for(Integer n:neighbours.get(node)){
        Integer cliqueCandidate = nodes.get(n);
        if(cliqueCandidate != ownClique)
            diffCliques.add(cliqueCandidate);
      }
      if(diffCliques.size() > 0){
        for(Integer n: diffCliques){
          Integer node2 = cliques.get(n).get(0); // first node in the clique candidate
          Nodelogl nodeChange = calcNodelogl(node2 , node); // logl change if we move node to clique of node2
          if(nodeChange.logl > maxChange.logl){ // we search for the max change in logl bigger than 0
            maxChange = nodeChange;
          }
        }
      }
      if(maxChange.logl > 0){ // if there is a positive change in logl by moving a node, now execute this change
        for(Pair<Integer, Integer> edge : maxChange.newedges)
          edgeClique.put(edge,true); // change makes new edges are inside cliques
        for(Pair<Integer, Integer> edge : maxChange.oldedges)
          edgeClique.put(edge,false); // change puts edges outside cliques
        Integer newClique = nodes.get(maxChange.newnode);
        nodes.put(node, newClique); // now the clique of node is the clique of node2
        cliques.get(ownClique).remove(node); // remove the node from clique nodes of old clique
        cliques.get(newClique).add(node); // add node to the list of nodes of the new clique
      }
    }
    Double newlogl = logl + maxChange.logl;
    return newlogl;
  }

  Nodelogl calcCliquelogl(int clique1 ,int clique2){
    Nodelogl cResult = new Nodelogl();
    Double logl = -1.0, logl_change = 0.0, logl_before = 0.0;
    Boolean complete = true;
    List<Pair<Integer,Integer>> newEdges = new ArrayList<>();
    List<Pair<Integer,Integer>> oldEdges = new ArrayList<>();
    Outer: for(Integer v1 : cliques.get(clique1)){
      for(Integer v2 : cliques.get(clique2)){
        Pair<Integer, Integer> edge = new Pair(v1,v2);
        edge = sortEdge(edge);
        if(!logEdges.containsKey(edge)){
          complete = false;
          break Outer; // exit the two loops if one link between the two cliques does not exist
        }
        else{
          logl_change += logEdges.get(edge);
          logl_before += minusLogEdges.get(edge);
          newEdges.add(edge);
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
    Outer : for(Integer v1 : cliques.get(clique1)){
      for(Integer v2 : cliques.get(clique2)){
        Pair<Integer,Integer>  edge = new Pair<>(v1,v2);
        sortEdge(edge);
        if(logEdges.containsKey(edge)){
          complete = false;
          break Outer;
        }
        else{
          Double weight = edges.get(edge);
          meanV += Math.pow(weight,2.0);
          size++;
        }
      }
    }
    if(complete)
      meanR = meanV/Double.valueOf(size);
    return meanR;
  }


  //TBChecked
  Double reassignClique(Integer clique, Double logl){
    Double loglchange = 0.0; // the change in logl if we accept joining clique to another clique
    Integer node = cliques.get(clique).get(0);
    Set<Integer> diffCliques = new HashSet<>();
    for(Integer v : neighbours.get(node)){
      Integer cliqueCandidate = nodes.get(v);
      if(cliqueCandidate != clique)
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
          edgeClique.put(edge, true);// change makes new edges be inside cliques
        for(Integer c : cliques.get(clique)){
          nodes.put(c,maxClique); // change clique value of old clique nodes to the new
          cliques.get(maxClique).add(c); // add nodes of old clique to the list of nodes of the new clique
        }
        cliques.remove(clique); // delete old clique because it is empty
      }
    }
    Double loglReturn = logl + loglchange;
    return loglReturn;
  }

  //csample_integer function make same effect as Collections.shuffle when size and x.size() are same and replace is set false

  //TODO code efficient possible
  List<Double> itReassign(Double tol, Double logl){
    Double currentlogl = logl;
    List<Double> loglResult = new ArrayList<>();
    loglResult.add(currentlogl);
    List<Integer> allnodes = new ArrayList<>();
    for(Integer v : nodes.keySet())
      allnodes.add(v);
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
    // round 1
    List<Integer> allNodes = new ArrayList<>();
    List<Integer> randallNodes;
    for(Integer v : nodes.keySet())
      allNodes.add(v); // insert allnodes values in vector of nodes
    randallNodes = new ArrayList<>(allNodes);
    Collections.shuffle(allNodes);
    int scount = 1; // counter of the number of rounds that are clique joining, it starts with 1
    int tcount = 1; // total number of rounds
    for(int randpos = 0; randpos < randallNodes.size(); randpos++) {
      Integer nodev = randallNodes.get(randpos);
      Integer cliquec = nodes.get(nodev); // clique that will be joined to another clique
      if (scount == step){
        currentlogl = reassignNode(nodev, currentlogl);
        loglResult.add(currentlogl);
        scount = 1;
        tcount++;
      }
      else{
        currentlogl = reassignClique(nodev,currentlogl);
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
        Integer cliquecw = nodes.get(nodevw); // clique that will be joined to another clique
        if(scount == step){
          currentlogl = reassignNode(nodevw , currentlogl);
          loglResult.add(currentlogl);
          scount = 1;
          tcount ++;
        }
        else{ // join this clique if it has not been joined in this round
          if(!cliquesRound.contains(cliquecw)){
            cliquesRound.add(cliquecw);
            currentlogl = reassignNode(nodevw , currentlogl);
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
    for(Double logl: loglLast)
      loglResult.add(logl);
    return loglResult;
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
