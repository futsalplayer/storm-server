package me.stormma.core.http.util;

import com.google.common.base.Objects;
import com.sun.org.apache.xpath.internal.operations.Bool;
import me.stormma.exception.StormServerException;
import me.stormma.support.helper.ApplicationHelper;
import me.stormma.core.http.annotation.JsonParam;
import me.stormma.core.http.annotation.RequestParam;
import me.stormma.core.http.model.HttpContext;
import me.stormma.support.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author stormma
 * @description 参数绑定工具类
 * @date 2017/8/17
 */
public class BindingParameterUtils {

    private static final Logger logger = LoggerFactory.getLogger(BindingParameterUtils.class);

    /**
     * @param parameter
     * @param context
     * @return object 返回绑定数据
     * @description 给parameter绑定数据
     */
    public static Object bindParam(Parameter parameter, HttpContext context) throws StormServerException {
        //判断是不是HttpContext
        if (parameter.getType() == HttpContext.class) {
            return context;
        }
        if (!Objects.equal(null, parameter.getAnnotation(RequestParam.class))) {
            return getRequestParamValue(parameter, context);
        }
        if (!Objects.equal(null, parameter.getAnnotation(JsonParam.class))) {
            Object object = getJsonParamValue(parameter, context);
            JsonParam jsonParam = parameter.getAnnotation(JsonParam.class);
            boolean required = jsonParam.required();
            if (Objects.equal(null, object) && required) {
                logger.error("the required json param: {} not found", parameter);
                throw new RuntimeException(String.format("the required json param: %s not found", parameter));
            }
        }
        return null;
    }

    /**
     * @description 给RequestParam类型绑定参数，返回绑定的数据
     * @param parameter
     * @param context
     * @return
     */
    private static Object getRequestParamValue(Parameter parameter, HttpContext context) throws StormServerException {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String name = requestParam.name();
        boolean required = requestParam.required();

        //获得前端传过来的参数
        Object value = context.params.get(name);
        List<String> paramList;
        if (!(value instanceof List)) {
            paramList = new ArrayList<>();
            paramList.add((String) value);
        } else {
            paramList = (List<String>) value;
        }

        //必需参数没有传
        if (Objects.equal(null, value) && required) {
            logger.error("the required param: {} not found", name);
            throw new RuntimeException(String.format("the required param: %s not found", name));
        }
        //获得参数化类型
        Type type = parameter.getParameterizedType();
        //参数的类型
        Class<?> parameterType = parameter.getType();

        //判断接收参数的是不是集合类型, 这里判断的主体应该是按照参数的类型进行判断，讲道理这段代码我是拒绝这么写的，但是真的没有好的办法了，先搁着，后面有时间再重构
        if (Collections.class.isAssignableFrom(parameterType)) {
            Type actualType = null;
            //如果是的话, 接着判断是不是泛型类型
            if (type instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                if (types.length > 1) {
                    //@RequestParam不支持泛型实际参数为多个的情况，暂不支持
                    throw new StormServerException(String.format("%s is not valid", parameter));
                }
                actualType = types[0];
            }
            paramList = (List<String>) value;
            return listConvert2CollectionType(actualType, parameterType, paramList);
        }

        //如果接收参数的类型是String[]
        if (parameterType == String[].class) {
            String[] param = new String[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                param[i] = paramList.get(i);
            }
            return param;
        }

        //如果接收参数的类型是int[] // Integer[]
        if (parameterType == int[].class || parameterType == Integer[].class) {
            Integer[] param = new Integer[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                param[i] = (Integer) stringConvert2OtherType(paramList.get(i), Integer.class);
            }
            if (parameterType == Integer[].class) {
                return param;
            }
            //Integer[] 转 int[]
            int[] result = new int[param.length];
            for (int i = 0; i < param.length; i++) {
                result[i] = param[i];
            }
            return result;
        }

        //如果接收参数的类型是float // Float[]
        if (parameterType == float[].class || parameterType == Float[].class)  {
            Float[] param = new Float[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                param[i] = (Float) stringConvert2OtherType(paramList.get(i), Float.class);
            }
            if (parameterType == Float[].class) {
                return param;
            }
            float[] result = new float[param.length];
            for (int i = 0; i < param.length; i++) {
                result[i] = param[i];
            }
            return result;
        }

        //如果接收参数的类型是double[] // Double[]
        if (parameterType == double[].class || parameterType == Double[].class) {
            Double[] param = new Double[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                param[i] = (Double) stringConvert2OtherType(paramList.get(i), Double.class);
            }
            if (parameterType == Double[].class) {
                return param;
            }
            double[] result = new double[param.length];
            for (int i = 0; i < param.length; i++) {
                result[i] = param[i];
            }
            return result;
        }

        //如果接收参数的类型是boolean[] // Boolean[]
        if (parameterType == boolean[].class || parameterType == Boolean[].class) {
            Boolean[] param = new Boolean[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                param[i] = (Boolean) stringConvert2OtherType(paramList.get(i), Boolean.class);
            }
            if (parameterType == Boolean.class) {
                return param;
            }
            boolean[] result = new boolean[param.length];
            for (int i = 0; i < param.length; i++) {
                result[i] = param[i];
            }
            return result;
        }
        //其余情况按照转换器支持的类型进行转换，如果转换失败抛出异常.
        return stringConvert2OtherType((String)value, parameterType);
    }

    /**
     * @description 给JsonParam型的参数绑定参数，返回绑定的数据
     * @param parameter
     * @param context
     * @return
     */
    private static Object getJsonParamValue(Parameter parameter, HttpContext context) {
        byte[] body = context.requestBody;
        Class<?> parameterType = parameter.getType();
        String str = new String(body);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return JsonUtil.jsonStrConvert2Obj(str, parameterType);
    }

    /**
     * @description 给@PathVariable类型的参数绑定，返回绑定的数据
     * @param parameter
     * @param context
     * @return
     */
    private static Object getPathVariableValue(Parameter parameter, HttpContext context) {
        String url = context.requestPath;
        return null;
    }

    /**
     * @description String转换成其他支持的类型
     * @param source
     * @param paramType
     * @return
     */
    private static Object stringConvert2OtherType(String source, Class<?> paramType) throws StormServerException {
        Map<Class<?>, Class<?>> converterMap = ApplicationHelper.converterMap;
        Object result = null;
        if (paramType == String.class) {
            return source;
        }
        for (Class type : converterMap.keySet()) {
            if (type.isAssignableFrom(paramType)) {
                try {
                    Class converterClass = converterMap.get(type);
                    Method method = converterClass.getMethod("convert", String.class);
                    Object instance;
                    if (type == Number.class) {
                        instance = converterClass.getConstructor(Class.class).newInstance(paramType);
                    } else {
                        instance = converterClass.newInstance();
                    }
                    result = method.invoke(instance, source);
                } catch (Exception e) {
                    logger.error("storm server get converter's convert method failed: {}", e.getMessage());
                    throw new StormServerException(e);
                }
            }
        }
        return result;
    }

    /**
     * @description List<String>转换成其他集合类型的泛型类参数比如Set<Integer>
     * @param actualType
     * @param paramType
     * @param params
     * @return
     */
    private static Object listConvert2CollectionType(Type actualType, Class<?> paramType, List<String> params) throws StormServerException {
        if (Objects.equal(null, actualType)) {
            actualType = String.class;
        }
        List<Object> data = new ArrayList<>();
        for (String param : params) {
            data.add(stringConvert2OtherType(param, (Class<?>) actualType));
        }
        if (paramType == List.class || List.class.isAssignableFrom(paramType)) {
            return data;
        }
        if (paramType == Set.class || Set.class.isAssignableFrom(paramType)) {
            return new HashSet<>(data);
        }
        throw new StormServerException(String.format("%s not supported convert.", paramType));
    }
}
