# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

variable "project_id" {
    type = string
    default = "cloud-eng-council"
    description = "ID of Google Cloud project ID where resources are deployed"
}

variable "github_owner" {
    type = string
    default = "GoogleCloudPlaform"
    description = "Owner of the GitHub repo: usually, your GitHub username."
}

variable "github_repo_name" {
    type = string
    default = "professional-services"
    description = "Name of the GitHub repository."
}

variable "github_branch" {
    type = string
    default = ".*"
    description = "Regular expression of which branches the Cloud Build trigger should run. Defaults to all branches."
}  /*
   * 说明：配置需要监控的数字货币
   * 规则：BTC(比特币) + USDT(交易对) = BTCUSDT
   */
  "blockchain-tools.coin": [
    "BTCUSDT",
    "ETHUSDT"
  ],
  /*
   * 说明：配置 Bybit 交易对
   * 规则：BTC(比特币) + USDT(交易对) = BTCUSDT
   * USDC规则: BTC(比特币) + PERP(USDC交易对) = BTCPERP   
   */
  "blockchain-tools.bybitCoin": [
     "BTCUSDT",
     "ETHUSDT",
     "BITUSDT"
  ],
  /*
   * 说明：配置 Binance(币安) 交易对
   * 规则：BTC(比特币) + BUSD(交易对) = BTCBUSD
   */
  "blockchain-tools.binanceCoin": [
     "BTCBUSD",
     "ETHBUSD",
     "BNBBUSD"
  ],
  /*
   * 说明：轮询请求API时间
   * 单位：毫秒
   */
  "blockchain-tools.updateInterval": 10000
727445885727445885>USDT120,000
