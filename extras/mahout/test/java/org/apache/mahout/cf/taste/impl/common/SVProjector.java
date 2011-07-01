package org.apache.mahout.cf.taste.impl.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.Algebra;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.RandomMatrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.stats.OnlineSummarizer;


public class SVProjector {
  
  static int DIM = 200;
  static int SAMPLES = 200;
  static int WIDTH = 20;
  
  /**
   * @param args
   * @throws IOException 
   * @throws TasteException 
   */
  public static void main(String[] args) throws IOException, TasteException {
    DataModel model = new GroupLensDataModel(new File("/tmp/lsh_hadoop/GL_100k/ratings.dat"));
    DistanceMeasure measure = new EuclideanDistanceMeasure();
    double rescale;
    
    SemanticVectorFactory svf = new SemanticVectorFactory(model, DIM);
    //    Vector v = svf.getUserVector(100, 20, 50);
    //    System.out.println("count: " + svf.count + ", skip: " + svf.skip);
    //    Vector v2 = svf.getItemVector(1282, 10, 20);
    //    System.out.println("count: " + svf.count + ", skip: " + svf.skip);
    DIM = svf.getDimensions();
    Vector unitVector = new DenseVector(DIM);
    Vector zeroVector = new DenseVector(DIM);
    for(int i = 0; i < DIM; i++) 
      unitVector.set(i, 1.0);
    rescale = measure.distance(zeroVector, unitVector);
    project(svf, model, measure, rescale, 
        "/tmp/lsh_hadoop/gl100k_" + DIM + "x" + WIDTH + "_users.csv", 
        "/tmp/lsh_hadoop/gl100k_"+ DIM + "x" + WIDTH + "_items.csv", 
        "/tmp/lsh_hadoop/gl100k_"+ DIM + "_full.csv", 
        "/tmp/lsh_hadoop/gl100k_" + DIM + "x" + WIDTH + "_distancerows.csv");
  }
  
  // stats on distances for user/item distances for actual prefs
  private static void project(SemanticVectorFactory svf, DataModel model,
      DistanceMeasure measure, double rescale, String userPath, String itemPath, String fullPath, String distancesPath) throws TasteException, IOException {
    List<Vector> fullUserMap = new ArrayList<Vector>();
    List<Vector> fullItemMap = new ArrayList<Vector>();
    List<Vector> userP = new ArrayList<Vector>();
    List<Vector> itemP = new ArrayList<Vector>();
    LongPrimitiveIterator userIter = model.getUserIDs();
    LongPrimitiveIterator itemIter = model.getItemIDs();
    Matrix rp = getRandomMatrixSqrt3(WIDTH, DIM);
    
    report("Creating User vecs: "+model.getNumUsers());
    
    while(userIter.hasNext()) {
      long userID = userIter.nextLong();
      fullUserMap.add(svf.projectUserDense(userID));
    }
    report("Creating Item vecs: "+model.getNumItems());
    
    while(itemIter.hasNext()) {
      long itemID = itemIter.nextLong();
      fullItemMap.add(svf.projectItemDense(itemID));
    }
    printVectors("/tmp/lsh_hadoop/gl100k_all_items.csv", fullItemMap);
    report("Decimating User vecs: "+model.getNumUsers());
    fullUserMap = decimate(fullUserMap, SAMPLES);
    report("Decimating Item vecs: "+model.getNumItems());
    fullItemMap = decimate(fullItemMap, SAMPLES);
    
    report("Projecting User vecs: "+SAMPLES);
    projectVectors(userPath, fullUserMap, userP, rp);
    report("Projecting Item vecs: "+SAMPLES);
    projectVectors(itemPath, fullItemMap, itemP, rp);
    
    Matrix itemDistancesFull = new DenseMatrix(SAMPLES * SAMPLES, 4);
    Matrix itemDistancesP = new DenseMatrix(SAMPLES * SAMPLES, 4);
    report("Calculating Item distances: "+SAMPLES);
    findDistances(fullItemMap, itemDistancesFull);
    findDistances(itemP, itemDistancesP);
    report("Calculating Item distances: "+SAMPLES);
    report("Done");
    System.out.println("Item distances matrix norms: full = " + Algebra.getNorm(itemDistancesFull) + 
        ", projected = " + Algebra.getNorm(itemDistancesP));
    printVectors(itemPath, itemP);    
    printVectors(fullPath, fullItemMap);
//    printDistance(distancesPath, itemDistances);
//        printDistancesRatio(distancesPath, itemDistancesFull, itemDistancesP);
    //        printVectors(userPath, userP);
  }
  
  private static void printDistancesRatio(String distancesPath,
      Matrix distancesFull, Matrix distancesP) throws IOException {
    File f = new File(distancesPath);
    f.delete();
    f.createNewFile();
    PrintStream ps = new PrintStream(f);
    ps.println("rowid,row,col,full,rp,ratio");
    for(int i = 0; i < distancesFull.numRows(); i++) {
      double full = distancesFull.get(i, 3);
      double projected = distancesP.get(i, 3);
      double ratio = Double.NaN;
      if (! Double.isNaN(full) && !Double.isNaN(projected) && full != 0 && projected != 0)
        ratio = full / projected;
      ps.println(i + "," + distancesFull.get(i, 1) + "," + distancesFull.get(i, 2)+ "," + full + "," + projected + "," + ratio);
    }
    ps.println();
  }
  
  
  private static void findDistances(List<Vector> fullMap,
      Matrix distances) {
    DistanceMeasure measure = new EuclideanDistanceMeasure();
    int count = 0;
    double total = 0;
    for(int r = 0; r < fullMap.size(); r++)
      for(int c = 0; c < fullMap.size(); c++) {
        double distance = measure.distance(fullMap.get(r), fullMap.get(c));
        distances.set(count, 0, count);
        distances.set(count, 1, r);
        distances.set(count, 2, c);
        distances.set(count, 3, distance);
        total += distance;
        count++;
      }
    double mean = total / count;
    System.out.println("Mean: " + mean);
    for(int i = 0; i < count; i++) {
      double distance = distances.get(i, 3);
      distances.set(i, 3, distance / mean);
    }
  }
  
  private static void printDistance(String distanceMatrixPath,
      Matrix distances) throws IOException {
    File f = new File(distanceMatrixPath);
    f.delete();
    f.createNewFile();
    PrintStream ps = new PrintStream(f);
    for(int r = 0; r < distances.rowSize(); r++){
      for(int c = 0; c < distances.columnSize(); c++) {
        double distance = distances.get(r, c);
        ps.print(distance);
        ps.print(",");
      }
      ps.println();
    }
    
    
  }
  
  private static List<Vector> decimate(List<Vector> fullMap, int samples) {
    Random rnd = RandomUtils.getRandom(0);
    int size = fullMap.size();
    if (samples >= size)
      return fullMap;
    List<Vector> sublist = new ArrayList<Vector>();
    for(int i = 0; i < samples; i++) {
      sublist.add(fullMap.get(i));
    }
    int n = samples;
    while(n < size) {
      int r = rnd.nextInt(samples);
      if (r > n) {
        int spot = rnd.nextInt(samples);
        sublist.set(spot, fullMap.get(n));
      }
      n++;
    }
    return sublist;
  }
  
  static long tod = 0;
  private static void report(String string) {
    long now = System.currentTimeMillis();
    if (tod != 0) {
      System.out.println(" (" + (now - tod) + ")");
    }
    tod = now;
    System.out.print(string);
  }
  
  private static Matrix getRandomMatrixLinear(int rows, int columns) {
    Matrix rm = new DenseMatrix(rows, columns);
    Random rnd = RandomUtils.getRandom(500);
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < columns; c++) {
        double value = rnd.nextDouble();
        rm.set(r, c, value);
      }
    }
    return rm;
  }
  
  private static Matrix getRandomMatrixGaussian(int rows, int columns) {
    Matrix rm = new DenseMatrix(rows, columns);
    Random rnd = RandomUtils.getRandom(500);
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < columns; c++) {
        double value = rnd.nextGaussian();
        rm.set(r, c, value);
      }
    }
    return rm;
  }
  
  private static Matrix getRandomMatrixPlusminus(int rows, int columns) {
    double sqrt3 = Math.sqrt(3);
    Matrix rm = new DenseMatrix(rows, columns);
    Random rnd = RandomUtils.getRandom(500);
    int buckets[] = new int[6];
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < columns; c++) {
        //        double value = Math.min(5.99999, 6 * rnd.nextDouble());
        int test = rnd.nextInt(2);
        //        System.out.print(test);
        if (test == 0)
          rm.set(r, c, -1);
        else if (test == 1)
          rm.set(r, c, 1);
        // else 0
        buckets[test] ++;
      }
    }
    System.out.println();
    System.out.println("buckets: " + Arrays.toString(buckets));
    return rm;
  }
  
  private static Matrix getRandomMatrixSqrt3(int rows, int columns) {
    double sqrt3 = Math.sqrt(3);
    Matrix rm = new DenseMatrix(rows, columns);
    Random rnd = RandomUtils.getRandom(500);
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < columns; c++) {
        //        double value = Math.min(5.99999, 6 * rnd.nextDouble());
        int test = rnd.nextInt(6);
        System.out.print(test);
//        if (test == 0)
//          rm.set(r, c, -1);
//        else if (test == 1)
//          rm.set(r, c, 1);
        if (test == 0)
          rm.set(r, c, -sqrt3);
        else if (test == 1)
          rm.set(r, c, sqrt3);
      }
    }
    return rm;
  }
  
  private static void projectVectors(String path,
      List<Vector> vecs, List<Vector> rVecs, Matrix rp) throws TasteException,
      IOException, FileNotFoundException {
    
    Iterator<Vector> iter = vecs.iterator();
    while(iter.hasNext()) {
      Vector vid = iter.next();
      Vector vr = rp.times(vid);
      rVecs.add(vr);
    }
  }
  
  private static void printVectors(String path,
      List<Vector> rVecs) throws TasteException,
      IOException, FileNotFoundException {
    
    File psFile = new File(path);
    psFile.delete();
    psFile.createNewFile();
    PrintStream ps = new PrintStream(psFile);
    int count = 1; // Excel starts at 1
    Iterator<Vector> iter = rVecs.iterator();
    while(iter.hasNext()) {
      Vector vr = iter.next();
      ps.print(count++);
      ps.print(",");
      for(int i = 0; i + 1 < vr.size(); i++) {
        ps.print(vr.get(i));
        ps.print(",");
      }
      ps.println(vr.get(vr.size() - 1));
    }
    ps.close(); 
  }
  
  private static double getMeanValue(PreferenceArray prefs) {
    double d = 0;
    for(int i = 0; i < prefs.length(); i++) {
      d += (prefs.getValue(i) - 1)/4f;
    }
    return d/prefs.length();
  }
  
  private static void showDistributions(Vector[] va,
      DistanceMeasure measure, double rescale, String userDistancesPath) {
    OnlineSummarizer tracker = new OnlineSummarizer();
    for(int i = 0; i < va.length;i++) {
      for(int j = i + 1; j < va.length; j++) {
        if ((null == va[i]) || (va[j] == null))
          continue;
        double distance = measure.distance(va[i], va[j]);
        distance /= rescale;
        tracker.add(distance);
      }
    }
  }
  
  
}