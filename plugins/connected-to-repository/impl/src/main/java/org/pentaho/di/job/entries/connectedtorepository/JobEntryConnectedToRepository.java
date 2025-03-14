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


package org.pentaho.di.job.entries.connectedtorepository;

import java.util.List;

import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.annotations.JobEntry;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Job entry connected to repositoryb.
 *
 * @author Samatar
 * @since 23-06-2008
 */

@JobEntry(id = "CONNECTED_TO_REPOSITORY",name = "JobEntry.ConnectedToRepository.TypeDesc",image = "CREP.svg",
description = "JobEntry.ConnectedToRepository.Tooltip",categoryDescription = "i18n:org.pentaho.di.job:JobCategory.Category.Repository",
i18nPackageName = "i18n:org.pentaho.di.job.entry:JobEntry.ConnectedToRepository",documentationUrl = "http://wiki.pentaho.com/display/EAI/Check+if+connected+to+Repository")
public class JobEntryConnectedToRepository extends JobEntryBase implements Cloneable, JobEntryInterface {
  private static Class<?> PKG = JobEntryConnectedToRepository.class; // for i18n purposes, needed by Translator2!!

  private boolean isspecificrep;
  private String repname;
  private boolean isspecificuser;
  private String username;

  public JobEntryConnectedToRepository( String n, String scr ) {
    super( n, "" );
    isspecificrep = false;
    repname = null;
    isspecificuser = false;
    username = null;
  }

  public JobEntryConnectedToRepository() {
    this( "", "" );
  }

  public void setSpecificRep( boolean isspecificrep ) {
    this.isspecificrep = isspecificrep;
  }

  public String getRepName() {
    return repname;
  }

  public void setRepName( String repname ) {
    this.repname = repname;
  }

  public String getUserName() {
    return username;
  }

  public void setUserName( String username ) {
    this.username = username;
  }

  public boolean isSpecificRep() {
    return isspecificrep;
  }

  public boolean isSpecificUser() {
    return isspecificuser;
  }

  public void setSpecificUser( boolean isspecificuser ) {
    this.isspecificuser = isspecificuser;
  }

  public Object clone() {
    JobEntryConnectedToRepository je = (JobEntryConnectedToRepository) super.clone();
    return je;
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder( 100 );
    retval.append( "      " ).append( XMLHandler.addTagValue( "isspecificrep", isspecificrep ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "repname", repname ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "isspecificuser", isspecificuser ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "username", username ) );

    retval.append( super.getXML() );

    return retval.toString();
  }

  public void loadXML( Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers,
    Repository rep, IMetaStore metaStore ) throws KettleXMLException {
    try {
      super.loadXML( entrynode, databases, slaveServers );
      isspecificrep = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "isspecificrep" ) );
      repname = XMLHandler.getTagValue( entrynode, "repname" );
      isspecificuser = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "isspecificuser" ) );
      username = XMLHandler.getTagValue( entrynode, "username" );

    } catch ( Exception e ) {
      throw new KettleXMLException( BaseMessages.getString(
        PKG, "JobEntryConnectedToRepository.Meta.UnableToLoadFromXML" ), e );
    }
  }

  public void loadRep( Repository rep, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases,
    List<SlaveServer> slaveServers ) throws KettleException {
    try {
      isspecificrep = rep.getJobEntryAttributeBoolean( id_jobentry, "isspecificrep" );
      repname = rep.getJobEntryAttributeString( id_jobentry, "repname" );
      isspecificuser = rep.getJobEntryAttributeBoolean( id_jobentry, "isspecificuser" );
      username = rep.getJobEntryAttributeString( id_jobentry, "username" );

    } catch ( KettleDatabaseException dbe ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "JobEntryConnectedToRepository.Meta.UnableToLoadFromRep" )
        + id_jobentry, dbe );

    }
  }

  // Save the attributes of this job entry
  //
  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_job ) throws KettleException {
    try {
      rep.saveJobEntryAttribute( id_job, getObjectId(), "isspecificrep", isspecificrep );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "repname", repname );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "isspecificuser", isspecificuser );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "username", username );
    } catch ( KettleDatabaseException dbe ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "JobEntryConnectedToRepository.Meta.UnableToSaveToRep" )
        + id_job, dbe );
    }
  }

  /**
   * Execute this job entry and return the result. In this case it means, just set the result boolean in the Result
   * class.
   *
   * @param previousResult
   *          The result of the previous execution
   * @return The Result of the execution.
   */
  public Result execute( Result previousResult, int nr ) {
    Result result = previousResult;
    result.setNrErrors( 1 );
    result.setResult( false );

    if ( rep == null ) {
      logError( BaseMessages.getString( PKG, "JobEntryConnectedToRepository.Log.NotConnected" ) );
      return result;
    }
    if ( isspecificrep ) {
      if ( Utils.isEmpty( repname ) ) {
        logError( BaseMessages.getString( PKG, "JobEntryConnectedToRepository.Error.NoRep" ) );
        return result;
      }
      String Reponame = environmentSubstitute( repname );
      if ( !Reponame.equals( rep.getName() ) ) {
        logError( BaseMessages.getString(
          PKG, "JobEntryConnectedToRepository.Error.DiffRep", rep.getName(), Reponame ) );
        return result;
      }
    }
    if ( isspecificuser ) {
      if ( Utils.isEmpty( username ) ) {
        logError( BaseMessages.getString( PKG, "JobEntryConnectedToRepository.Error.NoUser" ) );
        return result;
      }
      String realUsername = environmentSubstitute( username );

      if ( rep.getUserInfo() != null
        && !realUsername.equals( rep.getUserInfo().getLogin() ) ) {
        logError( BaseMessages.getString( PKG, "JobEntryConnectedToRepository.Error.DiffUser", rep
          .getUserInfo().getLogin(), realUsername ) );
        return result;
      }
    }

    if ( log.isDetailed() ) {
      logDetailed( BaseMessages.getString( PKG, "JobEntryConnectedToRepository.Log.Connected", rep.getName(), rep
        .getUserInfo() != null ? rep.getUserInfo().getLogin() : "" ) );
    }

    result.setResult( true );
    result.setNrErrors( 0 );

    return result;
  }

  public boolean evaluates() {
    return true;
  }

  public boolean isUnconditional() {
    return false;
  }
}
