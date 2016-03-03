package eu.openanalytics.jupyter.console.util;

import java.lang.reflect.Method;

public class ReflectionUtils {

	public static Object invoke(String methodName, Object object) {
		return invoke(methodName, object, (Object[])null);
	}

	public static Object invoke(String methodName, Object object, Object... args) {
		Class<?>[] argClasses = null;
		if (args != null && args.length > 0) {
			argClasses = new Class[args.length];
			for (int i=0; i<args.length; i++) {
				argClasses[i] = args[i].getClass();
			}
		}
		return invoke(methodName, object, args, argClasses);
	}

	public static Object invoke(String methodName, Object object, Object[] args, Class<?>[] argClasses) {
		Object result = null;
		if (object == null) return result;
		Class<?> objectClass = object.getClass();

		Method method = null;
		while (objectClass != null && method == null) {
			try {
				method = objectClass.getDeclaredMethod(methodName, argClasses);
			} catch (NoSuchMethodException e) {
				// Method not here. Check the superclass.
				objectClass = objectClass.getSuperclass();
			}
		}
		try {
			if (method != null) {
				if (!method.isAccessible()) method.setAccessible(true);
				result = method.invoke(object, args);
			}
		} catch (Exception e) {}
		return result;
	}

}
