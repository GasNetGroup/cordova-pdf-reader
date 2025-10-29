package net.kuama.pdf;

import android.content.Context;

/**
 * Helper class per accedere alle risorse R dell'app in un plugin Cordova.
 * Usa reflection per trovare la classe R nel package dell'applicazione.
 */
public class RHelper {
    private static Class<?> RClass;
    private static Class<?> stringClass;
    private static Class<?> drawableClass;
    private static Class<?> layoutClass;
    private static Class<?> idClass;
    private static Class<?> menuClass;
    
    private static void init(Context context) {
        if (RClass != null) return;
        
        try {
            String packageName = context.getPackageName();
            RClass = Class.forName(packageName + ".R");
            stringClass = RClass.getDeclaredClasses()[0]; // Prendiamo il primo, dovrebbe essere string o drawable
            drawableClass = null;
            
            // Cerchiamo le classi interne string, drawable, layout, id, menu
            Class<?>[] innerClasses = RClass.getDeclaredClasses();
            for (Class<?> innerClass : innerClasses) {
                String simpleName = innerClass.getSimpleName();
                if ("string".equals(simpleName)) {
                    stringClass = innerClass;
                } else if ("drawable".equals(simpleName)) {
                    drawableClass = innerClass;
                } else if ("layout".equals(simpleName)) {
                    layoutClass = innerClass;
                } else if ("id".equals(simpleName)) {
                    idClass = innerClass;
                } else if ("menu".equals(simpleName)) {
                    menuClass = innerClass;
                }
            }
        } catch (Exception e) {
            // Se non riesce a trovare R, useremo getIdentifier come fallback
            RClass = null;
        }
    }
    
    public static int getStringId(Context context, String name) {
        init(context);
        if (stringClass == null) {
            return context.getResources().getIdentifier(name, "string", context.getPackageName());
        }
        
        try {
            return stringClass.getField(name).getInt(null);
        } catch (Exception e) {
            return context.getResources().getIdentifier(name, "string", context.getPackageName());
        }
    }
    
    public static int getDrawableId(Context context, String name) {
        init(context);
        if (drawableClass == null) {
            return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        }
        
        try {
            return drawableClass.getField(name).getInt(null);
        } catch (Exception e) {
            return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        }
    }
    
    public static int getId(Context context, String name, String type) {
        init(context);
        if (RClass == null) {
            return context.getResources().getIdentifier(name, type, context.getPackageName());
        }
        
        Class<?> targetClass = null;
        
        try {
            Class<?>[] innerClasses = RClass.getDeclaredClasses();
            for (Class<?> innerClass : innerClasses) {
                if (type.equals(innerClass.getSimpleName())) {
                    targetClass = innerClass;
                    break;
                }
            }
            
            if (targetClass != null) {
                return targetClass.getField(name).getInt(null);
            }
        } catch (Exception e) {
            // Fallback a getIdentifier
        }
        
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }
    
    public static int getLayoutId(Context context, String name) {
        init(context);
        if (layoutClass == null) {
            return context.getResources().getIdentifier(name, "layout", context.getPackageName());
        }
        
        try {
            return layoutClass.getField(name).getInt(null);
        } catch (Exception e) {
            return context.getResources().getIdentifier(name, "layout", context.getPackageName());
        }
    }
    
    public static int getId(Context context, String name) {
        return getId(context, name, "id");
    }
    
    public static int getMenuId(Context context, String name) {
        init(context);
        if (menuClass == null) {
            return context.getResources().getIdentifier(name, "menu", context.getPackageName());
        }
        
        try {
            return menuClass.getField(name).getInt(null);
        } catch (Exception e) {
            return context.getResources().getIdentifier(name, "menu", context.getPackageName());
        }
    }
}

