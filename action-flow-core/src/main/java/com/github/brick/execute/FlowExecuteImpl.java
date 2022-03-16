package com.github.brick.execute;

import com.github.brick.entity.*;
import com.github.brick.enums.FLowModel;
import com.github.brick.extract.Extract;
import com.github.brick.extract.ExtractImpl;
import com.github.brick.factory.ActionFlowParseApiFactory;
import com.github.brick.factory.Factory;
import com.github.brick.format.Format;
import com.github.brick.parse.api.ActionFlowParseApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlowExecuteImpl implements FlowExecute {
    private static final Logger logger = LoggerFactory.getLogger(FlowExecuteImpl.class);
    ActionFlowParseApi actionFlowParseApi;

    SpelExpressionParser parser = new SpelExpressionParser();
    Extract extract = new ExtractImpl();
    /**
     * ActionFlowParseApi factory
     */
    Factory<FLowModel, ActionFlowParseApi> actionFlowParseApiFactory =
            new ActionFlowParseApiFactory<>();

    @Override
    public Object execute(String file, String flowId, FLowModel module) throws Exception {
        init(module);
        AllEntity parse = null;
        try {
            parse = actionFlowParseApi.parse(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Map<String, Object> result = getStringObjectMap(flowId, parse);
        return result;
    }

    private void init(FLowModel module) {
        this.actionFlowParseApi = actionFlowParseApiFactory.gen(module);
    }

    private Map<String, Object> getStringObjectMap(String flowId, AllEntity parse)
            throws Exception {
        ParamsEntity paramsEntity = parse.getParams();
        FlowsEntity flows = parse.getFlows();
        ActionsEntity actionsEntity = parse.getActions();
        ExtractsEntity extractsEntity = parse.getExtracts();
        WatchersEntity watchersEntity = parse.getWatchers();

        ResultEntity resultEntity = parse.getResult();

        Map<String, FlowEntity> collect = flows.getFlowEntities().stream().collect(Collectors.toMap(
                FlowEntity::getId, x -> x));
        FlowEntity flowEntity = collect.get(flowId);
        List<WatcherEntity> list = watchersEntity.getList();
        Map<String, WatcherEntity> watcherMap = list.stream().collect(Collectors.toMap(
                WatcherEntity::getId, x -> x));

        Map<String, ActionEntity> actionTagMap =
                actionsEntity.getList().stream().collect(Collectors.toMap(
                        ActionEntity::getId, s -> s));

        Map<String, ExtractEntity> exMap =
                extractsEntity.getExtractEntities().stream().collect(Collectors.toMap(
                        ExtractEntity::getId, s -> s));


        Map<String, List<ParamEntity>> paramsMap =
                paramsEntity.getList().stream().collect(Collectors.groupingBy(
                        ParamEntity::getGroup));


        // 执行
        List<WorkEntity> workEntities = flowEntity.getWorkEntities();

        // 用于存储执行器执行结果
        // key: 执行器id
        // value: 执行器处理结果
        Map<String, Object> actionResult = new HashMap<>();


        for (WorkEntity workEntity : workEntities) {
            run(workEntity, watcherMap, exMap, actionTagMap, paramsMap, actionResult);

        }


        Map<String, Object> result = handlerResult(resultEntity, exMap, actionResult);
        return result;
    }

    /**
     * 组装响应结果
     *
     * @param resultEntity 相应结果组装依据
     * @param exMap        提取器集合
     * @param actionResult action执行结果集合
     * @return
     */
    private Map<String, Object> handlerResult(ResultEntity resultEntity,
                                              Map<String, ExtractEntity> exMap,
                                              Map<String, Object> actionResult) {
        // 组装结果信息
        Map<String, Object> result = new HashMap<>();
        List<ResultEntity.Key> keys = resultEntity.getKeys();
        for (ResultEntity.Key key : keys) {
            String exId = key.getExId();
            String name = key.getName();
            ExtractEntity extractEntity = exMap.get(exId);
            String el = extractEntity.getEl();
            String fromAction = extractEntity.getFromAction();
            Object rs = actionResult.get(fromAction);
            Object extract;
            // 异常组装异常信息
            if (rs instanceof Throwable) {
                extract = ((Throwable) rs).getMessage();
            }
            // 正常情况下走提取策略
            else {
                extract = this.extract.extract(rs, el);
            }
            result.put(name, extract);
        }
        return result;
    }

    private Object runAction(ActionEntity actionEntity) throws Exception {
        Class<?> clazz = actionEntity.getClazz();
        Method method = actionEntity.getMethod();
        Map<String, Object> methodArg = actionEntity.getMethodArg();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        if (methodArg != null) {


            List<ActionEntity.Param> params = actionEntity.getParams();
            params.sort(Comparator.comparing(ActionEntity.Param::getIndex));

            // fixme: 值提取抽象
            for (int i = 0; i < params.size(); i++) {
                ActionEntity.Param param = params.get(i);
                String argName = param.getArgName();
                Object o = methodArg.get(argName);
                args[i] = o;
            }
        }

        return method.invoke(clazz.newInstance(), args);

    }

    /**
     * 填充ActionEntity缺少的数据
     */
    private void fillActionTag(Map<String, List<ParamEntity>> paramsMap, ActionEntity actionEntity)
            throws Exception {
        // 解析param
        List<ActionEntity.Param> params = actionEntity.getParams();

        if (params != null) {

            Map<String, Object> methodArgs = new HashMap<>();
            for (ActionEntity.Param param : params) {
                String argName = param.getArgName();
                String paramGroup = param.getParamGroup();
                FormatEntity formatEntity = param.getFormatEntity();
                String valueValue = null;
                if (paramGroup != null) {
                    String ex = param.getEx();
                    List<ParamEntity> paramEntities = paramsMap.get(paramGroup);
                    Map<String, ParamEntity> collect =
                            paramEntities.stream().collect(Collectors.toMap(
                                    ParamEntity::getKey, s -> s));
                    ParamEntity value = collect.get(ex);
                    if (value != null) {

                        valueValue = value.getValue();
                        methodArgs.put(argName, valueValue);
                    }
                } else {
                    valueValue = param.getValue();
                    methodArgs.put(argName, valueValue);
                }
                Class<?> typeClass = param.getTypeClass();


                if (formatEntity != null) {
                    // FIXME: 2022/3/15 编写Format搜索器
                    String classStr = formatEntity.getClassStr();
                    Class<?> formatClass = Class.forName(classStr);
                    if (Format.class.isAssignableFrom(formatClass)) {
                        Object o = formatClass.newInstance();
                        Format format = (Format) o;
                        Object format1 = format.format(valueValue, typeClass);
                        methodArgs.put(argName, format1);

                    }

                }


            }

            actionEntity.setMethodArg(methodArgs);
        } else {

        }
    }

    private void run(WorkEntity workEntity,
                     Map<String, WatcherEntity> watcherMap,
                     Map<String, ExtractEntity> exMap,
                     Map<String, ActionEntity> actionTagMap,
                     Map<String, List<ParamEntity>> paramsMap,
                     Map<String, Object> actionResult) throws Exception {
        // 获取类型做出不同操作
        String type = workEntity.getType();
        // 嵌套执行存在问题
        // 数据监控类型
        if ("watcher".equalsIgnoreCase(type)) {
            wrapperRunWatcher(workEntity, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
        }
        // 执行器类型
        else if ("action".equalsIgnoreCase(type)) {
            runWithAction(workEntity, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
        }


    }

    /**
     * 包装的watcher执行器
     */
    private void wrapperRunWatcher(WorkEntity workEntity,
                                   Map<String, WatcherEntity> watcherMap,
                                   Map<String, ExtractEntity> exMap,
                                   Map<String, ActionEntity> actionTagMap,
                                   Map<String, List<ParamEntity>> paramsMap,
                                   Map<String, Object> actionResult) throws Exception {
        try {
            runWithWatcher(workEntity, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
            List<WorkEntity> then = workEntity.getThen();
            // 监控执行器正常的情况下执行
            for (WorkEntity tag : then) {
                run(tag, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
            }
        } catch (Exception e) {
            logger.error("e", e);
            // 监控执行器执行异常的情况下执行
            List<WorkEntity> catchs = workEntity.getCatchs();
            for (WorkEntity aCatch : catchs) {
                run(aCatch, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
            }
        }
    }

    /**
     * 基础action执行函数
     */
    private void runWithAction(WorkEntity workEntity,
                               Map<String, WatcherEntity> watcherMap,
                               Map<String, ExtractEntity> exMap,
                               Map<String, ActionEntity> actionTagMap,
                               Map<String, List<ParamEntity>> paramsMap,
                               Map<String, Object> actionResult) throws Exception {
        ActionEntity actionEntity = actionTagMap.get(workEntity.getRefId());
        // 补充反射信息
        fillActionTag(paramsMap, actionEntity);
        // 执行函数
        try {
            Object res = runAction(actionEntity);
            actionResult.put(actionEntity.getId(), res);
            List<WorkEntity> then = workEntity.getThen();

            for (WorkEntity tag : then) {
                run(tag, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
            actionResult.put(actionEntity.getId(), e);
            List<WorkEntity> catchs = workEntity.getCatchs();
            for (WorkEntity tag : catchs) {
                run(tag, watcherMap, exMap, actionTagMap, paramsMap, actionResult);
            }
        }
    }

    /**
     * 执行watcher相关节点
     */
    private void runWithWatcher(WorkEntity workEntity,
                                Map<String, WatcherEntity> watcherMap,
                                Map<String, ExtractEntity> exMap,
                                Map<String, ActionEntity> actionTagMap,
                                Map<String, List<ParamEntity>> paramsMap,
                                Map<String, Object> actionResult) throws Exception {
        // 从监控集合中找到对应的详细数据
        WatcherEntity watcherEntity = watcherMap.get(workEntity.getRefId());

        String exId = watcherEntity.getExId();
        ExtractEntity extractEntity = exMap.get(exId);
        String el = extractEntity.getEl();
        String fromAction = extractEntity.getFromAction();


        // 从action执行结果集合中获取数据
        Object rs = actionResult.get(fromAction);
        // 提取结果如果是异常
        if (rs instanceof Throwable) {
            //            // todo: 结果如果是异常建议增加error节点
            //            List<WatcherEntity.Error> errors = watcherEntity.getErrors();
            //            for (WatcherEntity.Error error : errors) {
            //                String actionId = error.getActionId();
            //                commonRunAction(actionTagMap, actionId, paramsMap);
            //            }
            return;

        }
        // 提取结果不是异常
        else {
            Object extract = null;

            extract = this.extract.extract(rs, el);


            Expression expression = parser.parseExpression(extract + watcherEntity.getCondition());
            EvaluationContext context = new StandardEvaluationContext();
            Boolean value = expression.getValue(context, Boolean.class);
            if (value) {
                // 如果符合条件表达式执行then相关内容
                List<WatcherEntity.Then> thens = watcherEntity.getThens();
                for (WatcherEntity.Then then : thens) {
                    String actionId = then.getActionId();
                    commonRunAction(actionTagMap, actionId, paramsMap);
                }
            } else {
                // 如果不符合条件表达式执行catch相关内容
                List<WatcherEntity.Catch> catchs = watcherEntity.getCatchs();
                for (WatcherEntity.Catch aCatch : catchs) {
                    String actionId = aCatch.getActionId();
                    commonRunAction(actionTagMap, actionId, paramsMap);
                }
            }
        }

    }

    /**
     * 通用的action执行器
     *
     * @param actionTagMap action实体集合
     * @param actionId     actionId
     * @param paramsMap    参数集合
     * @see FlowExecuteImpl#fillActionTag(java.util.Map, ActionEntity)}
     * @see FlowExecuteImpl#runAction(ActionEntity)}
     */
    private void commonRunAction(Map<String, ActionEntity> actionTagMap,
                                 String actionId,
                                 Map<String, List<ParamEntity>> paramsMap) throws Exception {
        ActionEntity actionEntity = actionTagMap.get(actionId);
        fillActionTag(paramsMap, actionEntity);
        runAction(actionEntity);
    }

}
