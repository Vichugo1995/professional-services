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

package com.google.cloud.imf.gzos

import com.google.cloud.gszutil
import com.google.cloud.gszutil.io.{ChannelRecordReader, ChannelRecordWriter, ZRecordReaderT, ZRecordWriterT}
import com.google.cloud.gszutil.{CopyBook, Utf8}
import com.google.cloud.imf.gzos.MVSStorage.{DSN,MVSDataset,MVSPDSMember}
import com.google.cloud.imf.gzos.Util.DefaultCredentialProvider
import com.google.cloud.imf.gzos.pb.GRecvProto.ZOSJobInfo
import com.google.cloud.imf.util.{Logging, Services, StatsUtil}
import com.google.common.base.Charsets
import com.google.common.io.ByteStreams

import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.Date

object Linux extends MVS with Logging {
  override def isIBM: Boolean = false

  override def init(): Unit = {
    System.setProperty("java.net.preferIPv4Stack", "true")
    Console.out.println("Build Info:\n" + Util.readS("build.txt"))
  }

  override def ddExists(dd: String): Boolean = {
    sys.env.contains(dd) && sys.env.contains(dd + "_LRECL") && sys.env.contains(dd + "_BLKSIZE")
  }

  override def getDSN(dd: String): String = dd

  override def dsInfo(dd: String): Option[DataSetInfo] = {
    sys.env.get(s"${dd}_DSN") match {
      case Some(dsn) =>
        Option(DataSetInfo(
          dsn = dsn,
          lrecl = sys.env.getOrElse(s"${dd}_LRECL","80").toInt
        ))
      case None =>
        None
    }
  }
  override def readDD(dd: String): ZRecordReaderT = {
    ddFile(dd, sys.env)
  }

  override def readCloudDD(dd: String): ZRecordReaderT = {
    // Get DD information from z/OS
    dsInfo(dd) match {
      case Some(ddInfo) =>
        // Obtain Cloud Storage client
        val gcs = Services.storage(getCredentialProvider().getCredentials)

        // check if DD exists in Cloud Storage
        CloudDataSet.readCloudDD(gcs, dd, ddInfo) match {
          case Some(r) =>
            // Prefer Cloud Data Set if DSN exists in GCS
            r
          case _ =>
            ddFile(dd, sys.env)
        }
      case None =>
        throw new RuntimeException(s"DD:$dd not found")
    }
  }

  override def readStdin(): String = {
    val in = ByteStreams.toByteArray(System.in)
    new String(in, Charsets.UTF_8)
  }

  override def readDDString(dd: String, recordSeparator: String): String = {
    val in = readDD(dd)
    val bytes = Util.readAllBytes(in)
    Util.records2string(bytes, in.lRecl, Charsets.UTF_8, recordSeparator)
  }

  override def getPrincipal(): String = System.getProperty("user.name")

  override def getCredentialProvider(): CredentialProvider = new DefaultCredentialProvider

  override def readKeyfile(): Array[Byte] = Array.empty

  override def loadCopyBook(dd: String, encoding: Option[String] = None, picTCharset: Option[String] = Some("UTF-8")): CopyBook = {
    val ddValue = System.getenv(dd)
    require(ddValue != null, s"$dd environment variable not defined")
    val ddPath = Paths.get(ddValue)
    require(ddPath.toFile.exists(), s"$ddPath doesn't exist")
    CopyBook(new String(Files.readAllBytes(ddPath), Charsets.UTF_8), transcoder, picTCharset = picTCharset)
  }

  /** On Linux a DD is interpreted as a file reference or filename
    * If an environment variable is set, the value will be used as
    * the path to a file, otherwise the DD is interpreted as a filename.
    */
  protected def ddFile(dd: String, env: Map[String,String]): ZRecordReaderT = {
    val path = Paths.get(env.getOrElse(dd, dd))
    require(Files.exists(path), s"Unable to read DD $dd from '$path': doesn't exist")
    require(Files.isRegularFile(path), s"Unable to read DD $dd from '$path': not a file")
    new ChannelRecordReader(FileChannel.open(path, StandardOpenOption.READ),
      env.getOrElse(s"${dd}_LRECL", "80").toInt,
      env.getOrElse(s"${dd}_BLKSIZE", "80000").toInt)
  }

  override val jobName: String = sys.env.getOrElse("JOBNAME","JOBNAME")

  override val jobDate: String = StatsUtil.JobDateFormat.format(new Date())

  override val jobTime: String = StatsUtil.JobTimeFormat.format(new Date())

  override def getInfo: ZOSJobInfo = ZOSJobInfo.newBuilder
    .setJobid(jobId)
    .setJobdate(jobDate)
    .setJobtime(jobTime)
    .setJobname(jobName)
    .setStepName(sys.env.getOrElse("JOB_STEP","STEP01"))
    .setProcStepName(sys.env.getOrElse("PROC_STEP","STEP01"))
    .setUser(System.getProperty("user.name"))
    .build

  override def getSymbol(s: String): Option[String] = throw new NotImplementedError()

  override def substituteSystemSymbols(s: String): String = s

  override def exists(dsn: DSN): Boolean = throw new NotImplementedError()

  /** On Linux a DSN is interpreted as a path */
  override def readDSN(dsn: DSN): ZRecordReaderT = {
    dsn match {
      case MVSDataset(p) =>
        val path = Paths.get(p)
        require(Files.exists(path), s"Unable to read DSN from '$path': does not exist")
        require(Files.isRegularFile(path), s"Unable to read DSN from '$path': not a file")
        new ChannelRecordReader(FileChannel.open(path, StandardOpenOption.READ), 80, 80000)
      case MVSPDSMember(pdsDsn, member) =>
        throw new NotImplementedError()
    }
  }
  override def readDSNLines(dsn: DSN): Iterator[String] = throw new NotImplementedError()

  /** On Linux a DSN is interpreted as a path */
  override def writeDSN(dsn: DSN): ZRecordWriterT = {
    val env = sys.env
    dsn match {
      case MVSDataset(p) =>
        val path = Paths.get(p)
        Option(path.getParent).foreach(Files.createDirectories(_))
        import StandardOpenOption.{CREATE, WRITE}
        ChannelRecordWriter(channel = FileChannel.open(path, CREATE, WRITE), lrecl = 80, blksize = 80000)
      case _ =>
        throw new NotImplementedError()
    }
  }

  /** On Linux a DD is interpreted as a file reference or filename
    * If an environment variable is set, the value will be used as
    * the path to a file, otherwise the DD is interpreted as a filename.
    */
  override def writeDD(ddName: String): ZRecordWriterT = {
    val env = sys.env
    val path = Paths.get(env.getOrElse(ddName, ddName))
    logger.info(s"Writing $ddName DD to '$path'")
    if (Files.isDirectory(path))
      throw new IllegalStateException(s"Unable to write DD:$ddName because $path is a directory")
    import StandardOpenOption.{CREATE, WRITE}
    ChannelRecordWriter(channel = FileChannel.open(path, CREATE, WRITE),
      lrecl = env.getOrElse(s"${ddName}_LRECL", "80").toInt,
      blksize = env.getOrElse(s"${ddName}_BLKSIZE", "80000").toInt)
  }
  override def listPDS(dsn: DSN): Iterator[PDSMemberInfo] = throw new NotImplementedError()

  override def submitJCL(jcl: Seq[String]): Option[ZMVSJob] = throw new NotImplementedError()

  override def transcoder: gszutil.Transcoder = Utf8
}
