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

package com.github.brick.action.flow.parse.swagger;

import com.github.brick.action.flow.model.enums.ParamIn;
import com.github.brick.action.flow.model.swagger.ApiEntity;
import com.github.brick.action.flow.model.swagger.ApiParamEntity;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.SwaggerParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SwaggerParseImpl implements SwaggerParse {


    private List<ApiParamEntity> handlerParamEntity(Operation operation, Map<String, Model> modelMap) {
        List<ApiParamEntity> res = new ArrayList<>();
        if (operation != null) {

            List<Parameter> parameters = operation.getParameters();
            for (Parameter parameter : parameters) {
                String in = parameter.getIn();
                ParamIn paramIn = ParamIn.valueOf(in);


                // todo: 应用对象支持
                if (paramIn == ParamIn.body) {
                    BodyParameter bodyParameter = (BodyParameter) parameter;
                    Model schema = bodyParameter.getSchema();
                    // 引用对象的处理，即嵌套对象
                    if (schema instanceof RefModel) {
                        String simpleRef = ((RefModel) schema).getSimpleRef();
                        List<ApiParamEntity> extracted = handlerParamEntity(modelMap, simpleRef);
                        ApiParamEntity apiParamEntity = new ApiParamEntity();
                        apiParamEntity.setIn(paramIn);
                        apiParamEntity.setName(bodyParameter.getName());
                        apiParamEntity.setRequire(bodyParameter.getRequired());
                        apiParamEntity.setParamEntities(extracted);
                        res.add(apiParamEntity);
                    }
                    else {
                        ApiParamEntity apiParamEntity = new ApiParamEntity();
                        apiParamEntity.setIn(paramIn);
                        apiParamEntity.setName(bodyParameter.getName());
                        apiParamEntity.setRequire(bodyParameter.getRequired());
                        res.add(apiParamEntity);
                    }
                }
                else if (paramIn == ParamIn.path) {
                    ApiParamEntity apiParamEntity = new ApiParamEntity();
                    apiParamEntity.setIn(paramIn);
                    PathParameter pathParameter = (PathParameter) parameter;
                    String name = pathParameter.getName();
                    boolean required = pathParameter.getRequired();
                    apiParamEntity.setName(name);
                    apiParamEntity.setRequire(required);
                    apiParamEntity.setType(pathParameter.getType());
                    res.add(apiParamEntity);
                }
                else if (paramIn == ParamIn.formdata) {
                    ApiParamEntity apiParamEntity = new ApiParamEntity();
                    apiParamEntity.setIn(paramIn);
                    FormParameter formParameter = (FormParameter) parameter;
                    String type = formParameter.getType();

                    String name = formParameter.getName();
                    boolean required = formParameter.getRequired();
                    apiParamEntity.setName(name);
                    apiParamEntity.setType(type);
                    apiParamEntity.setRequire(required);
                    res.add(apiParamEntity);
                }


            }
        }

        return res;
    }

    private List<ApiParamEntity> handlerParamEntity(Map<String, Model> modelMap, String simpleRef) {
        List<ApiParamEntity> res = new ArrayList<>();
        Model model = modelMap.get(simpleRef);
        if (model instanceof ModelImpl) {

            Map<String, Property> properties = model.getProperties();
            List<String> required = ((ModelImpl) model).getRequired();

            for (Map.Entry<String, Property> entry : properties.entrySet()) {
                String k = entry.getKey();
                Property v = entry.getValue();
                ApiParamEntity apiParamEntity = handlerPropery(modelMap, simpleRef, required, k, v);
                res.add(apiParamEntity);

            }

        }
        return res;
    }

    private ApiParamEntity handlerPropery(Map<String, Model> modelMap, String simpleRef, List<String> required, String k, Property v) {
        ApiParamEntity p = new ApiParamEntity();
        String type = v.getType();
        p.setType(type);

        String name = k;
        p.setFlag(simpleRef + "." + name);
        boolean require = false;
        if (required != null) {
            require = required.contains(name);
        }

        if (v instanceof RefProperty) {
            String simpleRef1 = ((RefProperty) v).getSimpleRef();

            List<ApiParamEntity> extracted = handlerParamEntity(modelMap, simpleRef1);
            p.setName(k);
            p.setParamEntities(extracted);
            p.setType("object");
        }
        else if (v instanceof ArrayProperty) {
            p.setName(name);
            p.setRequire(require);
            p.setType("array");
            Property items = ((ArrayProperty) v).getItems();
            if (items instanceof StringProperty) {

            }
            else if (items instanceof RefProperty) {
                ApiParamEntity apiParamEntity = handlerPropery(modelMap, simpleRef, required, k, items);
                p.setParamEntities(Arrays.asList(apiParamEntity));
            }
        }
        else {
            p.setName(name);
            p.setRequire(require);
        }
        return p;
    }

    @Override
    public List<ApiEntity> parseFile(String filePath) {
        SwaggerParser sw = new SwaggerParser();
        Swagger read = sw.read(filePath);
        return handlerResult(read);
    }

    @Override
    public List<ApiEntity> parseData(String swaggerData) {
        SwaggerParser sw = new SwaggerParser();
        Swagger parse = sw.parse(swaggerData);
        return handlerResult(parse);
    }

    private List<ApiEntity> handlerResult(Swagger parse) {
        Map<String, Path> paths = parse.getPaths();
        Map<String, Model> definitions = parse.getDefinitions();
        List<ApiEntity> res = new ArrayList<>();

        paths.forEach(
                (k, v) -> {
                    res.addAll(genSwaggerApiEntity(k, v, definitions));
                }
        );

        return res;
    }

    @Override
    public List<ApiEntity> parseUrl(String url) {
        SwaggerParser sw = new SwaggerParser();
        Swagger read = sw.read(url);
        return handlerResult(read);
    }

    private List<ApiEntity> genSwaggerApiEntity(String k, Path v, Map<String, Model> modelMap) {


        Operation get = v.getGet();
        Operation post = v.getPost();
        Operation put = v.getPut();
        Operation delete = v.getDelete();

        String[] httpMethodSupport = {
                get != null ? "get" : null,
                post != null ? "post" : null,
                put != null ? "put" : null,
                delete != null ? "delete" : null
        };


        List<ApiParamEntity> getParamList = handlerParamEntity(get, modelMap);
        List<ApiParamEntity> postParamList = handlerParamEntity(post, modelMap);
        List<ApiParamEntity> putParamList = handlerParamEntity(put, modelMap);
        List<ApiParamEntity> deleteParamList = handlerParamEntity(delete, modelMap);


        ApiEntity getApi = new ApiEntity();
        getApi.setUrl(k);
        getApi.setDesc(get != null ? get.getSummary() : null);
        getApi.setMethod("get");
        getApi.setParams(getParamList);


        ApiEntity postApi = new ApiEntity();
        postApi.setUrl(k);
        postApi.setDesc(post != null ? post.getSummary() : null);
        postApi.setMethod("post");
        postApi.setParams(postParamList);


        ApiEntity putApi = new ApiEntity();
        putApi.setUrl(k);
        putApi.setDesc(put != null ? put.getSummary() : null);
        putApi.setMethod("put");
        putApi.setParams(putParamList);


        ApiEntity deleteApi = new ApiEntity();
        deleteApi.setUrl(k);
        deleteApi.setDesc(delete != null ? delete.getSummary() : null);
        deleteApi.setMethod("delete");
        deleteApi.setParams(deleteParamList);


        List<ApiEntity> res = new ArrayList<>();

        if (get != null) {

            res.add(getApi);
        }
        if (post != null) {

            res.add(postApi);
        }
        if (put != null) {

            res.add(putApi);
        }
        if (delete != null) {

            res.add(deleteApi);
        }

        return res;
    }

}
