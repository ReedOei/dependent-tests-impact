"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""

# Python Imports

# Local Imports
from resource_management.libraries.script.dummy import Dummy


class DataNode(Dummy):
  """
  Dummy script that simulates a slave component.
  """

  def __init__(self):
    super(DataNode, self).__init__()
    self.component_name = "DATANODE"
    self.principal_conf_name = "hdfs-site"
    self.principal_name = "dfs.datanode.kerberos.principal"
    self.keytab_conf_name = "hdfs-site"
    self.keytab_name = "dfs.datanode.keytab.file"

if __name__ == "__main__":
  DataNode().execute()
