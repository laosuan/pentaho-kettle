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


package org.pentaho.di.engine.api.remote;

import org.apache.commons.codec.binary.Base64OutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class that contains conviencce and helper methods that handle encoding and decoding strings and binary
 * data.
 */
public class EncodeUtil {
  private static final int ZIP_BUFFER_SIZE = 8192;

  /**
   * Base 64 decode, unzip and extract bytes using predefined charset value for byte-wise
   * multi-byte character handling.
   *
   * @param string base64 zip archive string representation
   * @return byte array from zip archive
   * @throws IOException
   */
  public static byte[] decodeBase64Zipped( String string ) throws IOException {
    if ( string == null || string.isEmpty() ) {
      return new byte[0];
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // base 64 decode
    byte[] bytes64 = org.apache.commons.codec.binary.Base64.decodeBase64( string.getBytes(  ) );
    ByteArrayInputStream zip = new ByteArrayInputStream( bytes64 );

    try (
      GZIPInputStream unzip = new GZIPInputStream( zip, ZIP_BUFFER_SIZE );
      BufferedInputStream in = new BufferedInputStream( unzip, ZIP_BUFFER_SIZE );
    ) {
      byte[] buff = new byte[ ZIP_BUFFER_SIZE ];
      for ( int length = 0; ( length = in.read( buff ) ) > 0; ) {
        baos.write( buff, 0, length );
      }
    } catch ( Exception e ) {
      throw new RuntimeException( "Unexpected error trying to decode object.", e );
    }

    return baos.toByteArray();
  }

  /**
   * Base64 encodes then zips a byte array into a compressed string
   *
   * @param src the source byte array
   * @return a compressed, base64 encoded string
   * @throws IOException
   */
  public static String encodeBase64Zipped( byte[] src ) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );
    try ( Base64OutputStream base64OutputStream = new Base64OutputStream( baos );
          GZIPOutputStream gzos = new GZIPOutputStream( base64OutputStream ) ) {
      gzos.write( src );
    }
    return baos.toString();
  }
}
