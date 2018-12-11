package lombok.extern.hook.getter;

/**
 * @author: rangu.dl 代林
 * @email: rangu.dl@alibaba-inc.com
 * @description: GetterHook
 * @createDate: 2018年12月11日 2:45 PM
 */
public interface GetterHook {

    <T> T hook(String className, String fieldName, Object classInstance, T fieldValue);

}
