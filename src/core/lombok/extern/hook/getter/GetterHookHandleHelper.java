package lombok.extern.hook.getter;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import lombok.Getter;
import lombok.core.AnnotationValues;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;

import static lombok.javac.Javac.CTC_BOT;

/**
 * @author: rangu.dl 代林
 * @email: rangu.dl@alibaba-inc.com
 * @description: GetterHookHandleHelper
 * @createDate: 2018年12月11日 2:58 PM
 */
public class GetterHookHandleHelper {

    public static final ThreadLocal<AnnotationValues<Getter>> THREAD_LOCAL = new ThreadLocal();

    public static boolean needInjectHook() {
        AnnotationValues<Getter> annotation = GetterHookHandleHelper.THREAD_LOCAL.get();
        if (annotation == null) {
            return false;
        }

        String hookClass = annotation.getRawExpression("hook");
        if (hookClass == null || hookClass.equals("null") || hookClass.equals("GetterHook.class")) {
            return false;
        }

        return true;
    }

    public static void injectHook(JCMethodDecl getterMethodDecl, JavacNode fieldNode) {
        String hookClassName = getHookClassName(fieldNode, GetterHookHandleHelper.THREAD_LOCAL.get());
        JCStatement hookStatement = generateHookStatement(hookClassName, "hook", fieldNode);

        //替换getter的方法体
        ListBuffer<JCStatement> listBuffer = new ListBuffer<JCTree.JCStatement>();
        listBuffer.append(hookStatement);
        getterMethodDecl.body.stats = listBuffer.toList();
    }

    private static String getHookClassName(JavacNode fieldNode, AnnotationValues<Getter> annotation) {
        String hookClass = annotation.getRawExpression("hook");
        hookClass = hookClass.substring(0, hookClass.lastIndexOf("."));

        String hookClassName = null;
        List<JCImport> imports = ((List)((JCCompilationUnit)fieldNode.up().up().get()).getImports());
        for (JCImport jcImport : imports) {
            int i = jcImport.toString().trim().lastIndexOf(hookClass + ";");
            if (i > 0) {
                hookClassName = jcImport.toString().trim().split(" ")[1];
                hookClassName = hookClassName.substring(0, hookClassName.length() - 1);
                break;
            }
        }
        if (hookClassName == null) {//默认为这个，如果Hook类和使用的地方在同一个包类，就是这个
            hookClassName = hookClass;
            if (hookClassName.indexOf(".") < 0) {//类不带包名，需要添加包名
                String packageName = ((JCCompilationUnit)fieldNode.up().up().get()).getPackageName().toString();
                hookClassName = packageName + "." + hookClassName;
            }
        }

        return hookClassName;
    }

    /**
     * 生成hook方法
     */
    private static JCStatement generateHookStatement(String hookClssName, String hookMethod, JavacNode fieldNode) {
        JavacTreeMaker treeMaker = fieldNode.getTreeMaker();
        JavacNode typeNode = fieldNode.up();
        String proxyHookMethodName = GetterHookProxy.class.getCanonicalName() + "." + hookMethod;
        JCTree.JCExpression proxyHookMethod = JavacHandlerUtil.chainDotsString(typeNode, proxyHookMethodName);

        List<JCExpression> args;
        {
            String className = typeNode.up().getAst().getPackageDeclaration() + "." + typeNode.getName();
            String fieldName = fieldNode.getName();

            JCExpression classInstance;
            JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();
            if ((fieldDecl.mods.flags & Flags.STATIC) == Flags.STATIC) {
                classInstance = treeMaker.Literal(CTC_BOT, null);
            } else {
                classInstance = treeMaker.Ident(fieldNode.toName("this"));
            }

            JCExpression fieldValue = treeMaker.Ident(fieldDecl);

            args = List.<JCExpression>of(
                treeMaker.Literal(hookClssName)
                , treeMaker.Literal(className)
                , treeMaker.Literal(fieldName)
                , classInstance
                , fieldValue
            );
        }

        JCTree.JCExpression hookMethodInvocation = treeMaker.Apply(List.<JCExpression>nil(), proxyHookMethod, args);

        return treeMaker.Return(hookMethodInvocation);
    }
}
