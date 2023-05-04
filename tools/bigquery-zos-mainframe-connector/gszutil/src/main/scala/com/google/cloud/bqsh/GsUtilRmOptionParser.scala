/*
 * Copyright 2023 Google LLC All Rights Reserved
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

package com.google.cloud.bqsh

import scopt.OptionParser

import java.net.URI


object GsUtilRmOptionParser extends OptionParser[GsUtilConfig]("gsutil rm") with ArgParser[GsUtilConfig] {
  override def parse(args: Seq[String], env: Map[String,String]): Option[GsUtilConfig] = {
    parse(args, GsUtilConfig(mode = "rm"))
  }

  head("gsutil rm", Bqsh.UserAgent)

  help("help").text("prints this usage text")

  note("Delete Cloud Storage objects")

  opt[Unit]('r', "recursive")
    .optional()
    .action{(_, c) => c.copy(recursive = true)}
    .text("Delete all Cloud Storage objects with locations starting with a given prefix")

  arg[String]("gcsUri")
    .required()
    .text("Cloud Storage location in format (gs://bucket/path)")
    .validate { x =>
      val uri = new URI(x)
      if (uri.getScheme != "gs" || uri.getAuthority.isEmpty)
        failure("invalid GCS URI")
      else
        success
    }
    .action((x, c) => c.copy(gcsUri = x))

  checkConfig{x =>
    if (x.statsTable.nonEmpty) {
      val statsTable = BQ.resolveTableSpec(x.statsTable, x.projectId, x.datasetId)
      if (x.projectId.isEmpty && statsTable.getProject.isEmpty)
        failure("must specify --project_id if project id not included in --statsTable arg")
      else success
    } else success
  }
}
