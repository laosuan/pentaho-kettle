/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBigNumber;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaDate;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.TransMeta.TransformationType;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMetaDataCombi;

import junit.framework.TestCase;

public class TransSingleThreadIT extends TestCase {

  public void testSingleThreadedTrans() throws Exception {

    KettleEnvironment.init();

    //
    // Create a new transformation...
    //
    TransMeta transMeta = new TransMeta( "src/it/resources/SingleThreadedTest - Stream Lookup.ktr" );
    transMeta.setTransformationType( TransformationType.SingleThreaded );

    long transStart = System.currentTimeMillis();

    // Now execute the transformation...
    Trans trans = new Trans( transMeta );
    trans.setLogLevel( LogLevel.MINIMAL );

    trans.prepareExecution( null );

    StepInterface si = trans.getStepInterface( "OUTPUT", 0 );
    RowStepCollector rc = new RowStepCollector();
    si.addRowListener( rc );

    RowProducer rp = trans.addRowProducer( "INPUT", 0 );
    trans.startThreads();

    // The single threaded transformation type expects us to run the steps
    // ourselves.
    //
    SingleThreadedTransExecutor executor = new SingleThreadedTransExecutor( trans );

    // Initialize all steps
    //
    executor.init();

    int iterations = 1000000;
    long totalWait = 0;
    List<RowMetaAndData> inputList = createData();

    for ( int i = 0; i < iterations; i++ ) {
      // add rows
      for ( RowMetaAndData rm : inputList ) {
        Object[] copy = rm.getRowMeta().cloneRow( rm.getData() );
        rp.putRow( rm.getRowMeta(), copy );
      }

      long start = System.currentTimeMillis();

      boolean cont = executor.oneIteration();
      if ( !cont ) {
        fail( "We don't expect any step or the transformation to be done before the end of all iterations." );
      }

      long end = System.currentTimeMillis();
      long delay = end - start;
      totalWait += delay;
      if ( i > 0 && ( i % 100000 ) == 0 ) {
        long rowsProcessed = trans.findRunThread( "bottles" ).getLinesRead();
        double speed = Const.round( ( rowsProcessed ) / ( (double) ( end - transStart ) / 1000 ), 1 );
        int totalRows = 0;
        for ( StepMetaDataCombi combi : trans.getSteps() ) {
          for ( RowSet rowSet : combi.step.getInputRowSets() ) {
            totalRows += rowSet.size();
          }
          for ( RowSet rowSet : combi.step.getOutputRowSets() ) {
            totalRows += rowSet.size();
          }
        }
        System.out.println( "#"
          + i + " : Finished processing one iteration in " + delay + "ms, average is: "
          + Const.round( ( (double) totalWait / ( i + 1 ) ), 1 ) + ", speed=" + speed
          + " row/s, total rows buffered: " + totalRows );
      }

      List<RowMetaAndData> resultRows = rc.getRowsWritten();

      // Result has one row less because we filter out one.
      // We also join with 3 identical rows in a data grid, giving 9 rows of which 3 are filtered out
      //
      assertEquals( "Error found in iteration " + i, 6, resultRows.size() );
      rc.clear();
    }

    rp.finished();

    // Dispose all steps.
    //
    executor.dispose();

    long rowsProcessed = trans.findRunThread( "bottles" ).getLinesRead();

    long transEnd = System.currentTimeMillis();
    long transTime = transEnd - transStart;
    System.out.println( "Average delay before idle : " + Const.round( ( (double) totalWait / iterations ), 1 ) );
    double transTimeSeconds = Const.round( ( (double) transTime / 1000 ), 1 );
    System.out.println( "Total transformation runtime for "
      + iterations + " iterations :" + transTimeSeconds + " seconds" );
    double transTimePerIteration = Const.round( ( (double) transTime / iterations ), 2 );
    System.out.println( "Runtime per iteration: " + transTimePerIteration + " miliseconds" );
    double rowsPerSecond = Const.round( ( rowsProcessed ) / ( (double) transTime / 1000 ), 1 );
    System.out.println( "Average speed: " + rowsPerSecond + " rows/second" );
  }

  public RowMetaInterface createRowMetaInterface() {
    RowMetaInterface rm = new RowMeta();

    ValueMetaInterface[] valuesMeta =
    {
      new ValueMetaString( "field1" ), new ValueMetaInteger( "field2" ),
      new ValueMetaNumber( "field3" ), new ValueMetaDate( "field4" ),
      new ValueMetaBoolean( "field5" ),
      new ValueMetaBigNumber( "field6" ),
      new ValueMetaBigNumber( "field7" ), };

    for ( int i = 0; i < valuesMeta.length; i++ ) {
      rm.addValueMeta( valuesMeta[i] );
    }

    return rm;
  }

  public List<RowMetaAndData> createData() {
    List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();

    RowMetaInterface rm = createRowMetaInterface();

    Object[] r1 =
      new Object[] {
        "KETTLE1", new Long( 123L ), new Double( 10.5D ), new Date(), Boolean.TRUE,
        BigDecimal.valueOf( 123.45 ), BigDecimal.valueOf( 123.60 ) };
    Object[] r2 =
      new Object[] {
        "KETTLE2", new Long( 500L ), new Double( 20.0D ), new Date(), Boolean.FALSE,
        BigDecimal.valueOf( 123.45 ), BigDecimal.valueOf( 123.60 ) };
    Object[] r3 =
      new Object[] {
        "KETTLE3", new Long( 501L ), new Double( 21.0D ), new Date(), Boolean.FALSE,
        BigDecimal.valueOf( 123.45 ), BigDecimal.valueOf( 123.70 ) };

    list.add( new RowMetaAndData( rm, r1 ) );
    list.add( new RowMetaAndData( rm, r2 ) );
    list.add( new RowMetaAndData( rm, r3 ) );

    return list;
  }
}
