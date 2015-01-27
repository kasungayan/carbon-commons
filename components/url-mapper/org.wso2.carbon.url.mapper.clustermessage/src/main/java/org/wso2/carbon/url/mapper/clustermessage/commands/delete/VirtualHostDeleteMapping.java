/*
*  Copyright (c), WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.url.mapper.clustermessage.commands.delete;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tomcat.ext.utils.URLMappingHolder;
import org.wso2.carbon.url.mapper.clustermessage.util.VirtualHostClusterUtil;

/**
 *  Cluster command to delete host mapping from url-mapper
 */
public class VirtualHostDeleteMapping extends ClusteringMessage {
    private static final Log log = LogFactory.getLog(VirtualHostDeleteMapping.class);
    private String hostName;
    
    public VirtualHostDeleteMapping(String hostName) {
         this.hostName = hostName;
    }
    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    @Override
    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
        VirtualHostClusterUtil.removeVirtualHost(this.hostName);
        URLMappingHolder.getInstance().removeUrlMappingMap(this.hostName);
    }
}
