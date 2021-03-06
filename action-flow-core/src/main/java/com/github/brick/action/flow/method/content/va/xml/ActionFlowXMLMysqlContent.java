/*
 *    Copyright [2022] [brick-team]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.brick.action.flow.method.content.va.xml;

import com.github.brick.action.flow.method.factory.storage.StorageFactory;
import com.github.brick.action.flow.method.resource.ResourceLoader;
import com.github.brick.action.flow.method.resource.impl.JDBCResourceLoaderImpl;
import com.github.brick.action.flow.model.config.JdbcConfig;
import com.github.brick.action.flow.model.enums.StorageType;
import com.github.brick.action.flow.model.xml.ActionFlowXML;
import com.github.brick.action.flow.storage.api.ActionExecuteEntityStorage;
import com.github.brick.action.flow.storage.api.FlowExecuteEntityStorage;
import com.github.brick.action.flow.storage.api.ResultExecuteEntityStorage;
import com.github.brick.action.flow.storage.mysql.config.MysqlConfig;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * action flow xml 存储媒介：关系型数据库Mysql
 *
 * @author Zen Huifer
 */
public class ActionFlowXMLMysqlContent extends ActionFlowXMLContent {
    public static final String MYSQL_CONFIG_FILE = "action_flow_jdbc.properties";
    protected static StorageType storageType;

    static {
        storageType = StorageType.MYSQL;
    }

    ResourceLoader<JdbcConfig, Map<String, JdbcConfig>> resourceLoader =
            new JDBCResourceLoaderImpl();
    private JdbcConfig jdbcConfig;

    public ActionFlowXMLMysqlContent(String[] actionFlowFileNames) throws Exception {
        super(actionFlowFileNames);
    }

    public ActionFlowXMLMysqlContent(String[] actionFlowFileNames, boolean beanFromSpring,
                                     ApplicationContext context)
            throws Exception {

        super(actionFlowFileNames);
        this.beanFromSpring = beanFromSpring;
        this.context = context;
    }

    public ActionFlowXMLMysqlContent(JdbcConfig jdbcConfig) {
        this.jdbcConfig = jdbcConfig;
    }

    @Override
    protected void storage(Map<String, ActionFlowXML> loads) throws Exception {
        beforeStorage();
        ActionExecuteEntityStorage actionExecuteEntityStorage =
                StorageFactory.factory(storageType, ActionExecuteEntityStorage.class);
        this.actionExecuteEntityStorage = actionExecuteEntityStorage;
        FlowExecuteEntityStorage flowExecuteEntityStorage =
                StorageFactory.factory(storageType, FlowExecuteEntityStorage.class);
        this.flowExecuteEntityStorage = flowExecuteEntityStorage;
        ResultExecuteEntityStorage resultExecuteEntityStorage =
                StorageFactory.factory(storageType, ResultExecuteEntityStorage.class);
        this.resultExecuteEntityStorage = resultExecuteEntityStorage;

        for (Map.Entry<String, ActionFlowXML> entry : loads.entrySet()) {
            String key = entry.getKey();
            ActionFlowXML value = entry.getValue();

            actionExecuteEntityStorage.save(key, value.getActions());
            flowExecuteEntityStorage.save(key, value.getFlows());
            resultExecuteEntityStorage.save(key, value.getResults());
        }
    }

    protected void beforeStorage() throws Exception {
        if (jdbcConfig == null) {
            JdbcConfig load = this.resourceLoader.load(MYSQL_CONFIG_FILE);
            this.jdbcConfig = load;
        }

        MysqlConfig.init(
                jdbcConfig.getUsername(), jdbcConfig.getPassword(), jdbcConfig.getUrl(),
                jdbcConfig.getDriver()
        );
    }
}
