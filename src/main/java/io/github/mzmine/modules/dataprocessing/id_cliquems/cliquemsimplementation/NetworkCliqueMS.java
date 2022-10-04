/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * This class builds undirected graph network for finding the cliques or groups of features. The
 * network is a weighted graph, whose nodes are features and weight are cosine similarity between
 * the nodes. Probabilistic methods are applied to find the arrangement of cliques (or groups of
 * nodes) that maxmimize the log likelihood function of the network.
 * <p>
 * See https://github.com/osenan/cliqueMS/blob/master/src/networkCliqueMSR.h for the Rcpp code
 * corresponding to this class
 */
public class NetworkCliqueMS {

  private Logger logger = Logger.getLogger(getClass().getName());


  private final HashMap<Pair<Integer, Integer>, Double> edges = new HashMap<>();//Edges of the undirected graph network
  private final HashMap<Integer, Integer> nodes = new HashMap<>(); // Indicates if edge is inside or not a clique
  private final HashMap<Pair<Integer, Integer>, Boolean> edgeClique = new HashMap<>(); // Hashmap of edges containing if they belong or not to a clique
  private final HashMap<Integer, List<Integer>> neighbours = new HashMap<>();// Hashmap containing list of nodes for each node
  private final HashMap<Integer, List<Integer>> cliques = new HashMap<>(); // unordered map of key clique name with a vector of the nodes of that clique
  private final HashMap<Pair<Integer, Integer>, Double> logEdges = new HashMap<>();  // log value of weigth powered to some exponent
  private final HashMap<Pair<Integer, Integer>, Double> minusLogEdges = new HashMap<>();  // 1 - log value of weight powered to some exponent

  //For recording progress;
  private MutableDouble progress;

  private CliqueMSTask driverTask;

  //Result of Kernighan and aggregate algorithm
  private final List<Pair<Integer, Integer>> resultNodeClique = new ArrayList<>();

  public HashMap<Pair<Integer, Integer>, Double> getEdges() {
    return edges;
  }

  private void createEdges(double[][] adjacencyMatrix, List<Integer> nodeIDList) {
    for (int i = 0; i < adjacencyMatrix.length; i++) {
      for (int j = i + 1; j < adjacencyMatrix[0].length; j++) {
        if (adjacencyMatrix[i][j] > 0.0) {
          Pair<Integer, Integer> p = new Pair(nodeIDList.get(i), nodeIDList.get(j));
          if (adjacencyMatrix[i][j] == 1) {
            // change similarity of 1 to 0.99999999999 to non avoid NaN
            this.edges.put(p, 0.99999999999);
          } else {
            this.edges.put(p, adjacencyMatrix[i][j]);
          }
        }
      }
    }
  }

  // Function createNodes, with node x you can access clique value y
  private void createNodesFromEdges() {
    for (Pair<Integer, Integer> p : this.edges.keySet()) {
      if (!this.nodes.containsKey(p.getKey())) {
        this.nodes.put(p.getKey(), p.getKey());
      }
      if (!this.nodes.containsKey(p.getValue())) {
        this.nodes.put(p.getValue(), p.getValue());
      }
    }
  }

  //create neighbour for each node
  private void createNeighboursFromEdges() {
    for (Pair<Integer, Integer> p : this.edges.keySet()) {
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
  private void createEdgeCliques() {
    for (Pair<Integer, Integer> p : this.edges.keySet()) {
      this.edgeClique.put(p, false);// at the beginning, all edges are outside of cliques
    }
  }

  // Function createCliques to create an unordered map of key clique name with a vector of the nodes of that clique
  private void createCliques() {
    for (Integer k : this.nodes.keySet()) {
      if (this.cliques.containsKey(k)) {
        this.cliques.get(k).add(this.nodes.get(k));
      } else {
        List<Integer> l = new ArrayList<>();
        l.add(this.nodes.get(k));
        this.cliques.put(k, l);
      }
    }
  }

  //initializeNetwork
  private void createNetwork(double[][] adjacencyMatrix, List<Integer> nodeIDList) {
    //import edges
    double exp = 2.0;
    createEdges(adjacencyMatrix, nodeIDList);
    createNodesFromEdges();
    createNeighboursFromEdges();
    createCliques();
    createEdgeCliques();
    for (Pair<Integer, Integer> edge : this.edges.keySet()) {
      Double weight, logPower, minusLogPower;
      weight = this.edges.get(edge);
      logPower = Math.log10(Math.pow(weight, exp));
      minusLogPower = Math.log10(1.0 - Math.pow(weight, exp));
      logEdges.put(edge, logPower);
      minusLogEdges.put(edge, minusLogPower);
    }

  }

  // Function to calculate log likelihood of the whole network
  private Double loglTotal() {
    Double inside = 0.0, outside = 0.0, logl = 0.0;
    for (Pair<Integer, Integer> edge : edges.keySet()) {
      if (this.edgeClique.get(edge)) {
        inside += this.logEdges.get(edge);
      } else {
        outside += this.minusLogEdges.get(edge);
      }
    }
    logl = inside + outside;
    return logl;
  }

  private Pair<Integer, Integer> sortEdge(Pair<Integer, Integer> edge) {
    if (edge.getKey() > edge.getValue()) {
      Pair<Integer, Integer> e = new Pair(edge.getValue(), edge.getKey());
      return e;
    }
    return edge;
  }

  // Function to compute the change of likelihood if we add node2 to clique of node1
  private NodeLogL calcNodelogl(Integer node1, Integer node2) {
    NodeLogL nResult = new NodeLogL();
    Double logl = -1.0, logl_change = 0.0, logl_before = 0.0;
    Boolean complete = true;
    Double newlinks_change = 0.0, nolinks_change = 0.0, newlinks_before = 0.0, nolinks_before = 0.0;
    List<Pair<Integer, Integer>> newEdges = new ArrayList<>();
    List<Pair<Integer, Integer>> oldEdges = new ArrayList<>();
    Integer clique1 = this.nodes.get(node1);
    Integer clique2 = this.nodes.get(node2);
    List<Integer> nClique1 = new ArrayList<>(this.cliques.get(clique1));
    nClique1.remove(node1);// remove the node1 apart from the other nodes of its clique
    List<Integer> nClique2 = new ArrayList<>(this.cliques.get(clique2));
    nClique2.remove(node2);// remove the node1 apart from the other nodes of its clique
    if (nClique1.size() > 0) {
      for (Integer i : nClique1) {
        Pair<Integer, Integer> edge = new Pair<>(i, 0);
        edge = sortEdge(edge);
        if (!logEdges.containsKey(edge)) {
          complete = false;
          break;
        } else {
          // edges that now will be part of the clique
          newlinks_change += this.logEdges.get(edge);
          // this edges were before outside cliques
          newlinks_before += this.minusLogEdges.get(edge);
          newEdges.add(edge);
        }
      }
    }
    if (nClique2.size() > 0) {
      for (Integer i : nClique2) {
        Pair<Integer, Integer> edge = new Pair<>(i, node2);
        edge = sortEdge(edge);
        // this edges now will be outside cliques
        nolinks_change += this.minusLogEdges.get(edge);
        // this edges between node2 and its old clique members were inside cliques before
        nolinks_before += this.logEdges.get(edge);
        oldEdges.add(edge);
      }
    }
    if (complete) {
      Pair<Integer, Integer> edge = new Pair<>(node1, node2);
      edge = sortEdge(edge);
      newlinks_change += this.logEdges.get(edge);
      newlinks_before += this.minusLogEdges.get(edge);
      newEdges.add(edge);
      logl_change = newlinks_change + nolinks_change;
      logl_before = newlinks_before + nolinks_before;
      logl = logl_change - logl_before;
    }
    nResult.logL = logl;
    nResult.newNode = node1; // initialize results values in case there is a break
    nResult.newEdges = newEdges;
    nResult.oldEdges = oldEdges;
    return nResult;
  }

  // function to move the current node to another clique
  Double reassignNode(Integer node, Double logL) {
    NodeLogL maxChange = new NodeLogL();
    maxChange.logL = 0.0;
    if (this.neighbours.get(node).size() > 0) { // reassign this node if it has neighbors
      Integer ownClique = this.nodes.get(node);
      Set<Integer> diffCliques = new HashSet<>();
      for (Integer n : neighbours.get(node)) {
        Integer cliqueCandidate = this.nodes.get(n);
        if (!cliqueCandidate.equals(ownClique)) {
          diffCliques.add(cliqueCandidate);
        }
      }
      if (diffCliques.size() > 0) {
        for (Integer n : diffCliques) {
          Integer node2 = this.cliques.get(n).get(0); // first node in the clique candidate
          NodeLogL nodeChange = calcNodelogl(node2,
              node); // logL change if we move node to clique of node2
          if (nodeChange.logL
              > maxChange.logL) { // we search for the max change in logL bigger than 0
            maxChange = nodeChange;
          }
        }
      }
      if (maxChange.logL
          > 0) { // if there is a positive change in logL by moving a node, now execute this change
        for (Pair<Integer, Integer> edge : maxChange.newEdges) {
          this.edgeClique.put(edge, true); // change makes new edges are inside cliques
        }
        for (Pair<Integer, Integer> edge : maxChange.oldEdges) {
          this.edgeClique.put(edge, false); // change puts edges outside cliques
        }
        Integer newClique = this.nodes.get(maxChange.newNode);
        this.nodes.put(node, newClique); // now the clique of node is the clique of node2
        this.cliques.get(ownClique)
            .remove((Integer) node); // remove the node from clique nodes of old clique
        this.cliques.get(newClique).add(node); // add node to the list of nodes of the new clique
      }
    }
    Double newLogL = logL + maxChange.logL;
    return newLogL;
  }

  private NodeLogL calcCliquelogl(Integer clique1, Integer clique2) {
    NodeLogL cResult = new NodeLogL();
    Double logl = -1.0, logl_change = 0.0, logl_before = 0.0;
    Boolean complete = true;
    List<Pair<Integer, Integer>> newEdges = new ArrayList<>();
    List<Pair<Integer, Integer>> oldEdges = new ArrayList<>();
    if (clique2.equals(0)) {
      complete = false;
    } else {
      Outer:
      for (Integer v1 : this.cliques.get(clique1)) {
        for (Integer v2 : this.cliques.get(clique2)) {
          Pair<Integer, Integer> edge = new Pair(v1, v2);
          edge = sortEdge(edge);
          if (!this.logEdges.containsKey(edge)) {
            complete = false;
            break Outer; // exit the two loops if one link between the two cliques does not exist
          } else {
            logl_change += this.logEdges.get(edge); // edges that now will be part of the clique
            logl_before += this.minusLogEdges.get(edge); // this edges were before outside cliques
            newEdges.add(edge);
          }
        }
      }
    }
    if (complete) {
      logl = logl_change - logl_before;
    }
    cResult.newNode = clique1; // initialize results values in case there is a break
    cResult.logL = logl;
    cResult.newEdges = newEdges;
    return cResult;
  }

  Double meanClique(Integer clique1, Integer clique2) {
    Double meanV = 0.0;
    Double meanR = -1.0;
    Integer size = 0;
    Boolean complete = true;
    Outer:
    for (Integer v1 : this.cliques.get(clique1)) {
      for (Integer v2 : this.cliques.get(clique2)) {
        Pair<Integer, Integer> edge = new Pair<>(v1, v2);
        edge = sortEdge(edge);
        if (!this.logEdges.containsKey(edge)) {
          complete = false;
          break Outer;
        } else {
          Double weight = this.edges.get(edge);
          meanV += Math.pow(weight, 2.0);
          size++;
        }
      }
    }
    if (complete) {
      meanR = meanV / Double.valueOf(size);
    }
    return meanR;
  }


  Double reassignClique(Integer clique, Double logL) {
    Double loglChange = 0.0; // the change in logL if we accept joining clique to another clique
    Integer node = this.cliques.get(clique).get(0);
    Set<Integer> diffCliques = new HashSet<>();
    for (Integer v : this.neighbours.get(node)) {
      Integer cliqueCandidate = this.nodes.get(v);
      if (!cliqueCandidate.equals(clique)) {
        diffCliques.add(cliqueCandidate);
      }
    }
    if (diffCliques.size() > 0) {
      Double maxMean = 0.0;
      Integer maxClique = 0;
      for (Integer c : diffCliques) {
        Double meanCandidate = meanClique(clique, c);
        if (meanCandidate > maxMean) {
          maxClique = c;
          maxMean = meanCandidate;
        }
      }
      NodeLogL loglCandidate = calcCliquelogl(clique, maxClique);
      if (loglCandidate.logL > 0) {
        loglChange = loglCandidate.logL;
        for (Pair<Integer, Integer> edge : loglCandidate.newEdges) {
          this.edgeClique.put(edge, true);// change makes new edges be inside cliques
        }
        for (Integer c : this.cliques.get(clique)) {
          this.nodes.put(c, maxClique); // change clique value of old clique nodes to the new
          this.cliques.get(maxClique)
              .add(c); // add nodes of old clique to the list of nodes of the new clique
        }
        this.cliques.remove(clique); // delete old clique because it is empty
      }
    }
    Double loglReturn = logL + loglChange;
    return loglReturn;
  }

  //csample_integer function of R  code makes same effect as Collections.shuffle when size and x.size() are same and replace is set false

  List<Double> itReassign(Double tol, Double logL) {
    Double currentlogl = logL;
    List<Double> loglResult = new ArrayList<>();
    loglResult.add(currentlogl);
    List<Integer> allnodes = new ArrayList<>(this.nodes.keySet());
    List<Integer> randallnodes = new ArrayList<>(allnodes);
    Collections.shuffle(randallnodes);
    for (Integer v : randallnodes) {
      currentlogl = reassignNode(v, currentlogl);
      loglResult.add(currentlogl);
    }
    Double firstlogl = loglResult.get(0);
    Double diff = 1 - Math.abs(currentlogl
        / firstlogl); // difference in log likelihood after one complete round of node reassignments
    Integer rcount = 1; // counter of the number of rounds
    while (diff > tol) {
      Double lastlogl = loglResult.get(loglResult.size() - 1);
      Collections.shuffle(randallnodes);
      for (Integer v : randallnodes) {
        currentlogl = reassignNode(v, currentlogl); // move nodes to different cliques
        loglResult.add(currentlogl); // store results of change in logL
      }
      diff = 1 - Math.abs(currentlogl
          / lastlogl); // difference in log likelihood after one complete round of node reassignments
      rcount++;
    }
    logger.log(Level.FINEST, "Kernighan-Lin done with " + rcount + " rounds");
    return loglResult;
  }

  List<Double> aggregateAndKernighan(Double tol, Integer step, Boolean silent) {
    Double currentLogL = loglTotal();
    List<Double> loglResult = new ArrayList<>();
    loglResult.add(currentLogL);
    // round 1
    List<Integer> randallNodes;
    // insert allnodes values in list of nodes
    List<Integer> allNodes = new ArrayList<>(this.nodes.keySet());
    randallNodes = new ArrayList<>(allNodes);
    Collections.shuffle(randallNodes);
    int scount = 1; // counter of the number of rounds that are clique joining, it starts with 1
    int tcount = 1; // total number of rounds

    for (int randpos = 0; randpos < randallNodes.size(); randpos++) {
      Integer nodev = randallNodes.get(randpos);
      Integer cliquec = this.nodes.get(nodev); // clique that will be joined to another clique
      if (scount == step) {
        currentLogL = reassignNode(nodev, currentLogL);
        loglResult.add(currentLogL);
        scount = 1;
        tcount++;
      } else {
        currentLogL = reassignClique(cliquec, currentLogL);
        loglResult.add(currentLogL);
        scount++;
        tcount++;
      }

      //update progress
      this.progress.setValue(driverTask.EIC_PROGRESS + driverTask.MATRIX_PROGRESS +
          driverTask.NET_PROGRESS * ((double) (randpos + 1) / (double) randallNodes.size()));
    }
    Double firstlogl = loglResult.get(0);
    Double diff = 1.0 - Math.abs(currentLogL
        / firstlogl); // difference in log likelihood after one complete round of node reassignments
    // rest of rounds
    if (!silent) {
      logger.log(Level.FINEST, "After " + tcount + " rounds logl is " + currentLogL);
      logger.log(Level.FINEST, "Still computing cliques");
    }
    while (diff > tol) {
      Set<Integer> cliquesRound = new HashSet<>();
      Double lastlogl = loglResult.get(loglResult.size() - 1);
      Collections.shuffle(randallNodes);
      for (int randposw = 0; randposw < randallNodes.size(); randposw++) {
        Integer nodevw = randallNodes.get(randposw);
        Integer cliquecw = this.nodes.get(nodevw); // clique that will be joined to another clique
        if (scount == step) {
          currentLogL = reassignNode(nodevw, currentLogL);
          loglResult.add(currentLogL);
          scount = 1;
          tcount++;
        } else { // join this clique if it has not been joined in this round
          if (!cliquesRound.contains(cliquecw)) {
            cliquesRound.add(cliquecw);
            currentLogL = reassignClique(cliquecw, currentLogL);
            loglResult.add(currentLogL);
            scount++;
            tcount++;
          }
        }
      }
      cliquesRound.clear(); // remove all cliques from the set for the next while round
      diff = 1.0 - Math.abs(currentLogL / lastlogl);
      if (!silent) {
        logger.log(Level.FINEST, "After " + tcount + " rounds logl is " + currentLogL);
        logger.log(Level.FINEST, "Still computing cliques");
      }
    }
    logger.log(Level.FINEST, "Aggregate cliques done, with " + tcount + " rounds.");
    // Kernighan-Lin after aggregation of cliques
    List<Double> loglLast = itReassign(tol, currentLogL);
    loglResult.addAll(loglLast);
    return loglResult;
  }

  public void returnCliques(double[][] adjacencyMatrix, List<Integer> nodeIDList, double tolerance,
      boolean silent, MutableDouble progress, CliqueMSTask task) {
    try {
      this.progress = progress;
      this.driverTask = task;
      createNetwork(adjacencyMatrix, nodeIDList);
      Double logl = loglTotal();
      logger.log(Level.FINEST, "Beginning value of logl is " + logl);
      int step = 10;
      List<Double> loglList = aggregateAndKernighan(tolerance, step, silent);
      for (Integer v : this.nodes.keySet()) {
        Pair<Integer, Integer> p = new Pair<>(v, this.nodes.get(v));
        resultNodeClique.add(p);
      }
      Double loglfinal = loglTotal();
      logger.log(Level.FINEST, "Finishing value of logl is " + loglfinal);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public List<Pair<Integer, Integer>> getResultNodeClique() {
    return resultNodeClique;
  }
}

// Nodelogl class to return an object with the change in logl, the new edges inside clique and the
// oldedges than will become outside edges
class NodeLogL {

  Double logL;
  Integer newNode;
  List<Pair<Integer, Integer>> newEdges;
  List<Pair<Integer, Integer>> oldEdges;
};
