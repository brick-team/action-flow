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

package com.github.brick.action.flow.model.execute;

import com.github.brick.action.flow.model.enums.ActionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
public abstract class ActionExecuteEntity {

    private ActionType type;
    private ForRestApi restApi;
    private ForJavaMethod javaMethod;

    public abstract Serializable getId();


    @Data
    @NoArgsConstructor
    public static class ForRestApi {
        private String url;
        private String method;
        private List<ParamExecuteEntity> param;
    }


    @Data
    @NoArgsConstructor
    public static class ForJavaMethod {
        private String className;
        private String method;
        /**
         * 该属性用于和spring 进行整合使用，在一个类多实例的情况下提供候选机制
         */
        private String qualifier;
        private List<ParamExecuteEntity> param;

    }
}
