/*
 * Copyright 2022 Google LLC All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.gszutil

import java.nio.ByteBuffer

import com.google.cloud.imf.gzos.Ebcdic
import com.google.cloud.imf.gzos.pb.GRecvProto.Record.Field
import org.apache.commons.codec.binary.Hex
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector
import org.apache.orc.TypeDescription

trait Decoder {
  val size: Int
  def filler: Boolean

  /** Read a field into a mutable output builder
    *
    * @param buf ByteBuffer
    * @param col ColumnVector
    * @param i row index
    */
  def get(buf: ByteBuffer, col: ColumnVector, i: Int): Unit

  def columnVector(maxSize: Int): ColumnVector

  def typeDescription: TypeDescription

  /** Proto Representation */
  def toFieldBuilder: Field.Builder

  protected var fieldName: String = ""
  protected var fieldType: String = ""
  protected var fieldId: Int = -1
  def withName(name: String): Decoder = {
    fieldName = name
    return this
  }
  def withType(typeName: String): Decoder = {
    fieldType = typeName
    return this
  }

  def withId(id: Int): Decoder = {
    fieldId = id
    return this
  }

  /** Print field information for diagnostic purposes
    *
    * @param buf ByteBuffer containing record bytes
    * @param sb StringBuilder to append to
    */
  def append(buf: ByteBuffer, sb: StringBuilder): Unit = {
    val bytes = new Array[Byte](size)
    buf.get(bytes)
    sb.append(s"\nField name: ")
    sb.append(fieldName)
    sb.append(s"\nField type: ")
    sb.append(fieldType)
    sb.append("\nHexadecimal:\n")
    sb.appendAll(Hex.encodeHex(bytes))
    sb.append("\nCharacter:\n")
    sb.append(Ebcdic.decodeBytes(bytes))
  }
}
