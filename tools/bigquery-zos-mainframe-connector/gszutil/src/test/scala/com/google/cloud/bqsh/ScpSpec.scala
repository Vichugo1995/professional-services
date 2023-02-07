package com.google.cloud.bqsh

import com.google.cloud.bqsh.cmd.Scp
import org.scalatest.flatspec.AnyFlatSpec

class ScpSpec extends AnyFlatSpec {
  "scp" should "write object" in {
    val uri = Scp.mkOutUri("gs://bucket/out.txt","ABC.XYZ", "")
    assert(uri == "gs://bucket/out.txt")
    val blobId = Scp.blobId(uri)
    assert(blobId.getBucket == "bucket")
    assert(blobId.getName == "out.txt")
  }

  it should "append DSN" in {
    val uri = Scp.mkOutUri("gs://bucket/", "ABC.XYZ", "")
    assert(uri == "gs://bucket/ABC.XYZ")
    val blobId = Scp.blobId(uri)
    assert(blobId.getBucket == "bucket")
    assert(blobId.getName == "ABC.XYZ")
  }

  it should "append DSN and member name" in {
    val uri = Scp.mkOutUri("gs://bucket/", "ABC.XYZ", "MBR1")
    assert(uri == "gs://bucket/ABC.XYZ/MBR1")
    val blobId = Scp.blobId(uri)
    assert(blobId.getBucket == "bucket")
    assert(blobId.getName == "ABC.XYZ/MBR1")
  }

  it should "append member name" in {
    val uri = Scp.mkOutUri("gs://bucket/pds", "ABC.XYZ", "MBR1")
    assert(uri == "gs://bucket/pds/MBR1")
    val blobId = Scp.blobId(uri)
    assert(blobId.getBucket == "bucket")
    assert(blobId.getName == "pds/MBR1")
  }
}
