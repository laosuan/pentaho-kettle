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


package org.pentaho.di.trans.steps.combinationlookup;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.loadsave.LoadSaveTester;
import org.pentaho.di.trans.steps.loadsave.initializer.InitializerInterface;
import org.pentaho.di.trans.steps.loadsave.validator.ArrayLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.DatabaseMetaLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.FieldLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.StringLoadSaveValidator;

public class CombinationLookupMetaTest implements InitializerInterface<StepMetaInterface> {
  LoadSaveTester loadSaveTester;
  Class<CombinationLookupMeta> testMetaClass = CombinationLookupMeta.class;

  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();

  @Before
  public void setUpLoadSave() throws Exception {
    KettleEnvironment.init();
    List<String> attributes =
      Arrays.asList( "schemaName", "tableName", "databaseMeta", "replaceFields", "keyField", "keyLookup",
        "useHash", "hashField", "technicalKeyField", "sequenceFrom", "commitSize", "preloadCache", "cacheSize",
        "useAutoinc", "techKeyCreation", "lastUpdateField" );

    Map<String, String> getterMap = new HashMap<String, String>() {
      {
        put( "replaceFields", "replaceFields" );
        put( "useHash", "useHash" );
      }
    };
    Map<String, String> setterMap = new HashMap<String, String>() {
      {
        put( "tableName", "setTablename" );
      }
    };
    FieldLoadSaveValidator<String[]> stringArrayLoadSaveValidator =
      new ArrayLoadSaveValidator<String>( new StringLoadSaveValidator(), 5 );


    Map<String, FieldLoadSaveValidator<?>> attrValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();
    attrValidatorMap.put( "keyField", stringArrayLoadSaveValidator );
    attrValidatorMap.put( "keyLookup", stringArrayLoadSaveValidator );
    attrValidatorMap.put( "databaseMeta", new DatabaseMetaLoadSaveValidator() );

    Map<String, FieldLoadSaveValidator<?>> typeValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();

    loadSaveTester =
      new LoadSaveTester( testMetaClass, attributes, new ArrayList<String>(), new ArrayList<String>(),
        getterMap, setterMap, attrValidatorMap, typeValidatorMap, this );
  }

  // Call the allocate method on the LoadSaveTester meta class
  @Override
  public void modify( StepMetaInterface someMeta ) {
    if ( someMeta instanceof CombinationLookupMeta ) {
      ( (CombinationLookupMeta) someMeta ).allocate( 5 );
    }
  }

  @Test
  public void testSerialization() throws KettleException {
    loadSaveTester.testSerialization();
  }

  @Test
  public void testProvidesModelerMeta() throws Exception {

    final RowMeta rowMeta = Mockito.mock( RowMeta.class );
    final CombinationLookupMeta combinationLookupMeta = new CombinationLookupMeta() {
      @Override Database createDatabaseObject() {
        return Mockito.mock( Database.class );
      }

      @Override protected RowMetaInterface getDatabaseTableFields( Database db, String schemaName, String tableName )
        throws KettleDatabaseException {
        assertEquals( "aSchema", schemaName );
        assertEquals( "aDimTable", tableName );
        return rowMeta;
      }
    };
    combinationLookupMeta.setKeyLookup( new String[] { "f1", "f2", "f3" } );
    combinationLookupMeta.setKeyField( new String[] { "s4", "s5", "s6" } );
    combinationLookupMeta.setSchemaName( "aSchema" );
    combinationLookupMeta.setTablename( "aDimTable" );

    final CombinationLookupData dimensionLookupData = new CombinationLookupData();
    assertEquals( rowMeta, combinationLookupMeta.getRowMeta( dimensionLookupData ) );
    assertEquals( 3, combinationLookupMeta.getDatabaseFields().size() );
    assertEquals( "f1", combinationLookupMeta.getDatabaseFields().get( 0 ) );
    assertEquals( "f2", combinationLookupMeta.getDatabaseFields().get( 1 ) );
    assertEquals( "f3", combinationLookupMeta.getDatabaseFields().get( 2 ) );
    assertEquals( 3, combinationLookupMeta.getStreamFields().size() );
    assertEquals( "s4", combinationLookupMeta.getStreamFields().get( 0 ) );
    assertEquals( "s5", combinationLookupMeta.getStreamFields().get( 1 ) );
    assertEquals( "s6", combinationLookupMeta.getStreamFields().get( 2 ) );
  }

  @Test
  public void testPDI16559() throws Exception {
    CombinationLookupMeta combinationLookup = new CombinationLookupMeta();
    combinationLookup.setKeyField( new String[] { "test_field" } );
    combinationLookup.setKeyLookup( new String[] {} );
    combinationLookup.setCacheSize( 15 );
    combinationLookup.setSchemaName( "test_schema" );
    combinationLookup.setTablename( "test_table" );
    combinationLookup.setReplaceFields( true );
    combinationLookup.setPreloadCache( false );

    try {
      String badXml = combinationLookup.getXML();
      Assert.fail( "Before calling afterInjectionSynchronization, should have thrown an ArrayIndexOOB" );
    } catch ( Exception expected ) {
      // Do Nothing
    }
    combinationLookup.afterInjectionSynchronization();
    //run without a exception
    String ktrXml = combinationLookup.getXML();

    assertEquals( combinationLookup.getKeyField().length, combinationLookup.getKeyLookup().length );

  }

}
