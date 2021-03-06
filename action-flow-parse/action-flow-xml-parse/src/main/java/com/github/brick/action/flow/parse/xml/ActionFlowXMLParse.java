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

import com.github.brick.action.flow.model.xml.ActionFlowXML;
import org.dom4j.Element;

public class ActionFlowXMLParse implements ParseXML<ActionFlowXML> {
    private final ActionXMLParse actionXMLParse = new ActionXMLParse();
    private final ResultXMLParse resultXMLParse = new ResultXMLParse();
    private final FlowXMLParse flowXMLParse = new FlowXMLParse();

    @Override
    public ActionFlowXML parse(Element element) throws Exception {
        ActionFlowXML actionFlowXML = new ActionFlowXML();
        actionFlowXML.setActions(actionXMLParse.parasAndValidate(element));

        actionFlowXML.setResults(resultXMLParse.parasAndValidate(element));
        actionFlowXML.setFlows(flowXMLParse.parasAndValidate(element));


        return actionFlowXML;
    }
}
