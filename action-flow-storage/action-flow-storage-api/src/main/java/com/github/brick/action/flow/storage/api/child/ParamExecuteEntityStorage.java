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

package com.github.brick.action.flow.storage.api.child;

import com.github.brick.action.flow.model.execute.ParamExecuteEntity;

import java.util.List;

/**
 * 参数处理
 *
 * @author xupenggao
 */
public interface ParamExecuteEntityStorage {

    void save(List<ParamExecuteEntity> params, Integer actionId) throws Exception;
}
