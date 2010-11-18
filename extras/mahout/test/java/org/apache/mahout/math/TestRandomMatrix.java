/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.math;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.function.VectorFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class TestRandomMatrix extends MahoutTestCase {

  protected static final int ROW = AbstractMatrix.ROW;

  protected static final int COL = AbstractMatrix.COL;

  protected Matrix test;
  
  int rows = 4;
  int columns = 5;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    test = new RandomMatrix(rows, columns);
  }

  @Test
  public void testCardinality() {
    int[] c = test.size();
    assertEquals("row cardinality", rows, c[ROW]);
    assertEquals("col cardinality", columns, c[COL]);
  }

  @Test
  public void testCopy() {

  }

  @Test
  public void testIterate() {
    Iterator<MatrixSlice> it = test.iterator();
    MatrixSlice m;
    while(it.hasNext() && (m = it.next()) != null) {
      Vector v = m.vector();
      Vector w = test instanceof SparseColumnMatrix ? test.getColumn(m.index()) : test.getRow(m.index());
      assertEquals("iterator: " + v.asFormatString() + ", randomAccess: " + w, v, w);
    }
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testLike() {
    Matrix like = test.like();
    assertSame("type", like.getClass(), test.getClass());
    assertEquals("rows", test.size()[ROW], like.size()[ROW]);
    assertEquals("columns", test.size()[COL], like.size()[COL]);
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testLikeIntInt() {
    Matrix like = test.like(4, 4);
    assertSame("type", like.getClass(), test.getClass());
    assertEquals("rows", 4, like.size()[ROW]);
    assertEquals("columns", 4, like.size()[COL]);
  }

  @Test
  public void testSize() {
    int[] c = test.getNumNondefaultElements();
    assertEquals("row size", rows, c[ROW]);
    assertEquals("col size", columns, c[COL]);
  }

  @Test
  public void testViewPart() {
    int[] offset = {1, 1};
    int[] size = {2, 1};
    Matrix view = test.viewPart(offset, size);
    assertEquals(2, view.rowSize());
    assertEquals(1, view.columnSize());
    int[] c = view.size();
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']',
            test.getQuick(row + 1, col + 1), view.getQuick(row, col), EPSILON);
      }
    }
  }

  @Test(expected = IndexException.class)
  public void testViewPartCardinality() {
    int[] offset = {1, 1};
    int[] size = {rows, columns};
    test.viewPart(offset, size);
  }

  @Test(expected = IndexException.class)
  public void testViewPartIndexOver() {
    int[] offset = {1, 1};
    int[] size = {rows, columns};
    test.viewPart(offset, size);
  }

  @Test(expected = IndexException.class)
  public void testViewPartIndexUnder() {
    int[] offset = {-1, -1};
    int[] size = {rows+10, columns+10};
    test.viewPart(offset, size);
  }

   @Test
  public void testRowView() {
    int[] c = test.size();
    for (int row = 0; row < c[ROW]; row++) {
    	Vector v = new DenseVector(columns);
    	for(int i = 0; i < columns; i++)
    		v.setQuick(i, test.getQuick(row, i));
      assertEquals(0.0, v.minus(test.viewRow(row)).norm(1), EPSILON);
    }

    assertEquals(c[COL], test.viewRow(3).size());
    assertEquals(c[COL], test.viewRow(5).size());

//    Random gen = RandomUtils.getRandom();
//    for (int row = 0; row < c[ROW]; row++) {
//      int j = gen.nextInt(c[COL]);
//      double old = test.get(row, j);
//      double v = gen.nextGaussian();
//      test.viewRow(row).set(j, v);
//      assertEquals(v, test.get(row, j), 0);
//      assertEquals(v, test.viewRow(row).get(j), 0);
//      test.set(row, j, old);
//      assertEquals(old, test.get(row, j), 0);
//      assertEquals(old, test.viewRow(row).get(j), 0);
//    }
  }

  @Test
  public void testColumnView() {
    int[] c = test.size();
	Matrix result = copyMatrix(c);

    for (int col = 0; col < c[COL]; col++) {
      assertEquals(0.0, result.getColumn(col).minus(test.viewColumn(col)).norm(1), EPSILON);
    }

    assertEquals(c[ROW], test.viewColumn(3).size());
    assertEquals(c[ROW], test.viewColumn(5).size());

  }

private Matrix copyMatrix(int[] c) {
	Matrix result = new DenseMatrix(test.rowSize(), test.columnSize());
	for (int row = 0; row < c[ROW]; row++) {
		for (int col = 0; col < c[COL]; col++) {
			result.setQuick(row, col, test.getQuick(row, col));
		}
	}
	return result;
}

  @Test
  public void testAggregateRows() {
    Vector v = test.aggregateRows(new VectorFunction() {
      public double apply(Vector v) {
        return v.zSum();
      }
    });

    for (int i = 0; i < test.numRows(); i++) {
      assertEquals(test.getRow(i).zSum(), v.get(i), EPSILON);
    }
  }

  @Test
  public void testAggregateCols() {
    Vector v = test.aggregateColumns(new VectorFunction() {
      public double apply(Vector v) {
        return v.zSum();
      }
    });

    for (int i = 0; i < test.numCols(); i++) {
      assertEquals(test.getColumn(i).zSum(), v.get(i), EPSILON * 100000);
  	System.out.println(test.getColumn(i).zSum() + "," + v.get(i) + "," + EPSILON * 100);

    }
  }

  @Test
  public void testAggregate() {
    double total = test.aggregate(Functions.PLUS, Functions.IDENTITY);
    assertEquals(test.aggregateRows(new VectorFunction() {
      public double apply(Vector v) {
        return v.zSum();
      }
    }).zSum(), total, EPSILON);
  }

  @Test
  public void testDivide() {
    int[] c = test.size();
    Matrix value = test.divide(4.53);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']',
            test.getQuick(row, col) / 4.53, value.getQuick(row, col), EPSILON);
      }
    }
  }

  @Test(expected = IndexException.class)
  public void testGetIndexUnder() {
    int[] c = test.size();
    for (int row = -1; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        test.get(row, col);
      }
    }
  }

  @Test(expected = IndexException.class)
  public void testGetIndexOver() {
    int[] c = test.size();
    for (int row = 0; row < c[ROW] + 1; row++) {
      for (int col = 0; col < c[COL]; col++) {
        test.get(row, col);
      }
    }
  }

  @Test
  public void testMinus() {
    int[] c = test.size();
    Matrix result = copy();
    Matrix value = result.minus(test);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']', 0.0, value.getQuick(
            row, col), EPSILON);
      }
    }
  }

private Matrix copy() {
	Matrix result = new DenseMatrix(test.rowSize(), test.columnSize());
    for (int i = 0; i < test.rowSize(); i++) {
    	for(int j = 0; j < test.columnSize(); j++) {
    		result.setQuick(i, j, test.getQuick(i, j));
    	}
    }
	return result;
}

  @Test(expected = CardinalityException.class)
  public void testMinusCardinality() {
    test.minus(test.transpose());
  }

  @Test
  public void testPlusDouble() {
    int[] c = test.size();
    Matrix dense = copy();

    Matrix value = dense.plus(4.53);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']',
            test.getQuick(row, col) + 4.53, value.getQuick(row, col), EPSILON);
      }
    }
  }

  @Test
  public void testPlusMatrix() {
	Matrix result = copy();
    int[] c = test.size();
    Matrix dense = copy();

    Matrix value = dense.plus(result);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']', test.getQuick(row, col) * 2,
            value.getQuick(row, col), EPSILON);
      }
    }
  }

  @Test(expected = CardinalityException.class)
  public void testPlusMatrixCardinality() {
    test.plus(test.transpose());
  }

  @Test(expected = IndexException.class)
  public void testSetUnder() {
    int[] c = test.size();
    for (int row = -1; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        test.set(row, col, 1.23);
      }
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSetOver() {
    int[] c = test.size();
    for (int row = 0; row < c[ROW] + 1; row++) {
      for (int col = 0; col < c[COL]; col++) {
        test.set(row, col, 1.23);
      }
    }
  }

  @Test
  public void testTimesDouble() {
    int[] c = test.size();
    Matrix value = test.times(4.53);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']',
        		test.getQuick(row, col) * 4.53, value.getQuick(row, col), EPSILON);
      }
    }
  }

//  @Test
//  public void testTimesMatrix() {
//    int[] c = test.size();
//    Matrix transpose = test.transpose();
//    Matrix value = test.times(transpose);
//    int[] v = value.size();
//    assertEquals("rows", c[COL], v[ROW]);
//    assertEquals("cols", c[ROW], v[COL]);
//
//    Matrix expected = new DenseMatrix(new double[][]{{5.0, 11.0, 17.0},
//        {11.0, 25.0, 39.0}, {17.0, 39.0, 61.0}}).times(1.21);
//
//    for (int i = 0; i < expected.numCols(); i++) {
//      for (int j = 0; j < expected.numRows(); j++) {
//        assertTrue("Matrix times transpose not correct: " + i + ", " + j
//            + "\nexpected:\n\t" + expected.asFormatString() + "\nactual:\n\t"
//            + value.asFormatString(),
//            Math.abs(expected.get(i, j) - value.get(i, j)) < 1.0e-12);
//      }
//    }
//
//    Matrix timestest = new DenseMatrix(10, 1);
//    /* will throw ArrayIndexOutOfBoundsException exception without MAHOUT-26 */
//    timestest.transpose().times(timestest);
//  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTimesMatrixCardinality() {
    Matrix other = test.like(5, 8);
    test.times(other);
  }

  @Test
  public void testTranspose() {
    int[] c = test.size();
    Matrix transpose = test.transpose();
    int[] t = transpose.size();
    assertEquals("rows", c[COL], t[ROW]);
    assertEquals("cols", c[ROW], t[COL]);
    for (int row = 0; row < c[ROW]; row++) {
      for (int col = 0; col < c[COL]; col++) {
        assertEquals("value[" + row + "][" + col + ']',
            test.getQuick(row, col), transpose.getQuick(col, row), EPSILON);
      }
    }
  }

//  @Test
//  public void testZSum() {
//    double sum = test.zSum();
//    assertEquals("zsum", 23.1, sum, EPSILON);
//  }

//  @Test(expected = IndexException.class)
//  public void testAssignRow() {
//    double[] data = {2.1, 3.2};
//    test.assignRow(1, new DenseVector(data));
//    assertEquals("test[1][0]", 2.1, test.getQuick(1, 0), EPSILON);
//    assertEquals("test[1][1]", 3.2, test.getQuick(1, 1), EPSILON);
//  }

  @Test
  public void testGetRow() {
    Vector row = test.getRow(1);
    assertEquals("row size", columns, row.getNumNondefaultElements());
  }

  @Test(expected = IndexException.class)
  public void testGetRowIndexUnder() {
    test.getRow(-1);
  }

  @Test(expected = IndexException.class)
  public void testGetRowIndexOver() {
    test.getRow(5);
  }

  @Test
  public void testGetColumn() {
    Vector column = test.getColumn(1);
    assertEquals("row size", rows, column.getNumNondefaultElements());
  }

  @Test(expected = IndexException.class)
  public void testGetColumnIndexUnder() {
    test.getColumn(-1);
  }

  @Test(expected = IndexException.class)
  public void testGetColumnIndexOver() {
    test.getColumn(5);
  }

  @Test
  public void testAsFormatString() {
    String string = test.asFormatString();
    int[] cardinality = {rows, columns};
    Matrix m = AbstractMatrix.decodeMatrix(string);
    for (int row = 0; row < cardinality[ROW]; row++) {
      for (int col = 0; col < cardinality[COL]; col++) {
        assertEquals("m[" + row + ',' + col + ']', test.get(row, col), m.get(
            row, col), EPSILON);
      }
    }
  }

  @Test
  public void testLabelBindings() {
    Matrix m = new RandomMatrix(3,3);
    assertNull("row bindings", m.getRowLabelBindings());
    assertNull("col bindings", m.getColumnLabelBindings());
    Map<String, Integer> rowBindings = new HashMap<String, Integer>();
    rowBindings.put("Fee", 0);
    rowBindings.put("Fie", 1);
    rowBindings.put("Foe", 2);
    m.setRowLabelBindings(rowBindings);
    assertEquals("row", rowBindings, m.getRowLabelBindings());
    Map<String, Integer> colBindings = new HashMap<String, Integer>();
    colBindings.put("Foo", 0);
    colBindings.put("Bar", 1);
    colBindings.put("Baz", 2);
    m.setColumnLabelBindings(colBindings);
    assertEquals("row", rowBindings, m.getRowLabelBindings());
    assertEquals("Fee", m.get(0, 1), m.get("Fee", "Bar"), EPSILON);

//    double[] newrow = {9, 8, 7};
//    m.set("Foe", newrow);
//    assertEquals("FeeBaz", m.get(0, 2), m.get("Fee", "Baz"), EPSILON);
  }

//  @Test(expected = UnboundLabelException.class)
//  public void testSettingLabelBindings() {
//    Matrix m = new RandomMatrix(3,3);
//    assertNull("row bindings", m.getRowLabelBindings());
//    assertNull("col bindings", m.getColumnLabelBindings());
//    m.set("Fee", "Foo", 1, 2, 9);
//    assertNotNull("row", m.getRowLabelBindings());
//    assertNotNull("row", m.getRowLabelBindings());
//    assertEquals("Fee", 1, m.getRowLabelBindings().get("Fee").intValue());
//    assertEquals("Fee", 2, m.getColumnLabelBindings().get("Foo").intValue());
//    assertEquals("FeeFoo", m.get(1, 2), m.get("Fee", "Foo"), EPSILON);
//    m.get("Fie", "Foe");
//  }

  @Test
  public void testLabelBindingSerialization() {
	    Matrix m = new RandomMatrix(3,3);

    assertNull("row bindings", m.getRowLabelBindings());
    assertNull("col bindings", m.getColumnLabelBindings());
    Map<String, Integer> rowBindings = new HashMap<String, Integer>();
    rowBindings.put("Fee", 0);
    rowBindings.put("Fie", 1);
    rowBindings.put("Foe", 2);
    m.setRowLabelBindings(rowBindings);
    assertEquals("row", rowBindings, m.getRowLabelBindings());
    Map<String, Integer> colBindings = new HashMap<String, Integer>();
    colBindings.put("Foo", 0);
    colBindings.put("Bar", 1);
    colBindings.put("Baz", 2);
    m.setColumnLabelBindings(colBindings);
    String json = m.asFormatString();
    Matrix mm = AbstractMatrix.decodeMatrix(json);
    assertEquals("Fee", m.get(0, 1), mm.get("Fee", "Bar"), EPSILON);
  }
}