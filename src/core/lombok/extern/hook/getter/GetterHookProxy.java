package lombok.extern.hook.getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: rangu.dl 代林
 * @email: rangu.dl@alibaba-inc.com
 * @description: GetterHookProxy
 * @createDate: 2018年12月11日 2:45 PM
 */
public class GetterHookProxy {

    private static final Map<String, GetterHook> getterHookMap = new HashMap<String, GetterHook>();

    public static <T> T hook(String getterHookClassName, String className, String fieldName, Object classInstance, T fieldValue) {
        GetterHook getterHook = getGetterHook(getterHookClassName);
        getterHook.hook(className, fieldName, classInstance, fieldValue);
        return fieldValue;
    }

    private static GetterHook getGetterHook(String className) {
        GetterHook getterHook = getterHookMap.get(className);
        if (getterHook == null) {
            synchronized (GetterHookProxy.class) {
                if (getterHookMap.get(className) == null) {
                    try {
                        getterHook = (GetterHook)Class.forName(className).newInstance();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    getterHookMap.put(className, getterHook);
                } else {
                    getterHook = getterHookMap.get(className);
                }
            }
        }
        return getterHook;
    }

}
