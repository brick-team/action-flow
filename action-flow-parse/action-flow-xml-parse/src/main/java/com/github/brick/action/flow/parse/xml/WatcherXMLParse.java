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

package com.github.brick.action.flow.parse.xml;

import com.github.brick.action.flow.model.enums.ExtractModel;
import com.github.brick.action.flow.model.xml.WatcherXML;
import org.dom4j.Element;

public class WatcherXMLParse extends CommonParseAndValidateImpl<WatcherXML>
        implements ParseXML<WatcherXML>, ValidateXMLParseData<WatcherXML> {
    private static final String CONDITION_ATTR = "condition";
    private static final String EL_TYPE_ATTR = "elType";

    @Override
    public WatcherXML parse(Element element) throws Exception {
        WatcherXML watcherXML = new WatcherXML();
        String condition = element.attributeValue(CONDITION_ATTR);
        watcherXML.setCondition(condition);
        String elType = element.attributeValue(EL_TYPE_ATTR);
        if ("".equals(elType) || elType == null) {
            watcherXML.setElType(ExtractModel.JSON_PATH);
        }
        else {
            watcherXML.setElType(ExtractModel.valueOf(elType.toUpperCase()));
        }
        return watcherXML;
    }

    /**
     * 验证watcher数据
     *
     * @param watcherXml watcherXml
     * @throws IllegalArgumentException 非法参数异常
     */
    @Override
    public void validate(WatcherXML watcherXml) throws IllegalArgumentException {

        if (watcherXml.getCondition() == null || "".equals(watcherXml.getCondition())){
            throw new IllegalArgumentException("watcher中condition不能为空");
        }

        if (watcherXml.getElType() == null){
            throw new IllegalArgumentException("watcher中type不能为空");
        }
    }
}

